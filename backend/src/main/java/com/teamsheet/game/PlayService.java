package com.teamsheet.game;

import com.teamsheet.api.Dtos.*;
import com.teamsheet.domain.*;
import com.teamsheet.repo.GuessRepository;
import com.teamsheet.repo.PickStatRepository;
import com.teamsheet.repo.PlayRepository;
import com.teamsheet.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PlayService {

    public static final int GUESSES = 3;

    private final PlayRepository plays;
    private final PlayerRepository players;
    private final GuessRepository guesses;
    private final PickStatRepository pickStats;
    private final PuzzleService puzzles;
    private final GuessMatcher matcher;
    private final ScoringService scoring;
    private final GameProperties props;
    private final Clock clock;

    public PlayService(PlayRepository plays, PlayerRepository players, GuessRepository guesses,
                       PickStatRepository pickStats, PuzzleService puzzles, GuessMatcher matcher,
                       ScoringService scoring, GameProperties props, Clock clock) {
        this.plays = plays;
        this.players = players;
        this.guesses = guesses;
        this.pickStats = pickStats;
        this.puzzles = puzzles;
        this.matcher = matcher;
        this.scoring = scoring;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Idempotent per (today's puzzle, anonId): repeat calls return the same play, without resetting its timer,
     * plus the guesses already made so a refreshed client can resume.
     */
    @Transactional
    public PlayView create(UUID anonId) {
        Puzzle puzzle = puzzles.today();
        plays.insertIfAbsent(puzzle.getNumber(), anonId, clock.instant());
        Play play = plays.findByPuzzleNumberAndAnonId(puzzle.getNumber(), anonId).orElseThrow();
        return view(play, puzzle);
    }

    /** Read-only: today's number and this device's play if it exists. Never creates a play (so no timer starts). */
    @Transactional(readOnly = true)
    public TodayStatus todayStatus(UUID anonId) {
        Puzzle puzzle = puzzles.today();
        PlayView play = plays.findByPuzzleNumberAndAnonId(puzzle.getNumber(), anonId)
                .map(p -> view(p, puzzle)).orElse(null);
        return new TodayStatus(puzzle.getNumber(), play);
    }

    private PlayView view(Play play, Puzzle puzzle) {
        List<PickView> history = pickViews(guesses.findByPlayIdOrderByGuessIndex(play.getId()),
                new PoolIndex(puzzles.orderedPool(puzzle)));
        return new PlayView(play.getId(), play.getPuzzleNumber(), history.size(), GUESSES - history.size(),
                play.isFinished(), play.getTotalScore(), deadline(play), history);
    }

    /** A player picked from the dropdown: a hit if he's in today's pool, otherwise a miss. Never ambiguous. */
    @Transactional
    public GuessResult guessPlayer(UUID playId, long playerId) {
        Player chosen = players.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Unknown player " + playerId));
        return resolve(playId, chosen.getDisplayName(), puzzle -> {
            boolean inPool = puzzles.orderedPool(puzzle).stream().anyMatch(mp -> mp.getPlayerId() == playerId);
            return inPool ? new MatchOutcome.Hit(playerId) : new MatchOutcome.Miss();
        });
    }

    /** Free text, fuzzy-matched within today's pool (kept for the timeout signal and older clients). */
    @Transactional
    public GuessResult guess(UUID playId, String text) {
        return resolve(playId, text, puzzle -> matcher.match(puzzle.getMatchId(), text));
    }

    private GuessResult resolve(UUID playId, String text, Function<Puzzle, MatchOutcome> match) {
        Play play = plays.findByIdForUpdate(playId)
                .orElseThrow(() -> new NotFoundException("Unknown play " + playId));
        if (play.isFinished()) throw new ConflictException("Play already finished");

        Puzzle puzzle = puzzles.byNumber(play.getPuzzleNumber());
        List<Guess> previous = guesses.findByPlayIdOrderByGuessIndex(playId);
        int index = previous.size() + 1;
        Instant now = clock.instant();

        // Server-side timer: the open guess expired, so whatever arrived counts as a timeout.
        // Empty text is the client saying its countdown ran out; it can only cost the player, so take it as a timeout.
        Instant hardDeadline = play.getGuessOpenedAt().plusSeconds(props.guessSeconds() + props.graceSeconds());
        if (now.isAfter(hardDeadline) || NameNormalizer.normalize(text).isEmpty()) {
            guesses.save(new Guess(playId, index, text, "timeout", null, null, 0, now));
            consume(play, index, now);
            return result(GuessResultType.timeout, index, play, null, null, 0, "Time's up");
        }

        MatchOutcome outcome = match.apply(puzzle);
        return switch (outcome) {
            case MatchOutcome.Miss m -> {
                guesses.save(new Guess(playId, index, text, "miss", null, null, 0, now));
                consume(play, index, now);
                yield result(GuessResultType.miss, index, play, null, null, 0, "Not in this lineup");
            }
            // Does not consume a guess, and never reveals who the candidates were.
            case MatchOutcome.Ambiguous a -> result(GuessResultType.ambiguous, null, play, null, null, 0,
                    "More than one player matches. Be more specific");
            case MatchOutcome.Hit h -> hit(play, puzzle, previous, index, text, h.playerId(), now);
        };
    }

    private GuessResult hit(Play play, Puzzle puzzle, List<Guess> previous, int index, String text,
                            long playerId, Instant now) {
        PoolIndex pool = new PoolIndex(puzzles.orderedPool(puzzle));
        MatchPlayer mp = pool.byId.get(playerId);

        boolean dupe = previous.stream().anyMatch(g -> Objects.equals(g.getPlayerId(), playerId));
        if (dupe) {
            return result(GuessResultType.duplicate, null, play, pool.slotted(mp), null, 0, "Already picked");
        }

        ScoringService.Score score = scoring.score(scoring.rarityFor(puzzle.getNumber()).of(mp), mp);
        guesses.save(new Guess(play.getId(), index, text, "hit", playerId, score.tier().label, score.points(), now));
        pickStats.increment(puzzle.getNumber(), playerId);
        play.addPoints(score.points());
        consume(play, index, now);
        return result(GuessResultType.hit, index, play, pool.slotted(mp), score.tier().label, score.points(), null);
    }

    private void consume(Play play, int index, Instant now) {
        play.openNextGuess(now);
        if (index >= GUESSES) play.finish(now);
    }

    private GuessResult result(GuessResultType type, Integer index, Play play, Slotted who,
                               String tier, int points, String message) {
        int used = index != null ? index : guesses.findByPlayIdOrderByGuessIndex(play.getId()).size();
        return new GuessResult(type, index, GUESSES - used,
                who == null ? null : who.slot(),
                who == null ? null : who.mp().getPlayer().getDisplayName(),
                who == null ? null : who.mp().getPlayer().getShortName(),
                who == null ? null : who.mp().isSub(),
                tier, points, play.getTotalScore(), play.isFinished(), deadline(play), message);
    }

    private Instant deadline(Play play) {
        return play.isFinished() ? null : play.getGuessOpenedAt().plusSeconds(props.guessSeconds());
    }

    @Transactional(readOnly = true)
    public RevealView reveal(UUID playId) {
        Play play = plays.findById(playId).orElseThrow(() -> new NotFoundException("Unknown play " + playId));
        if (!play.isFinished()) throw new ConflictException("Reveal is available after your third guess");

        Puzzle puzzle = puzzles.byNumber(play.getPuzzleNumber());
        Match match = puzzles.match(puzzle);
        PoolIndex pool = new PoolIndex(puzzles.orderedPool(puzzle));
        ScoringService.RarityLookup rarity = scoring.rarityFor(puzzle.getNumber());
        List<Guess> mine = guesses.findByPlayIdOrderByGuessIndex(playId);
        Set<Long> picked = mine.stream().map(Guess::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());

        String source = null;
        List<PoolEntry> entries = new ArrayList<>();
        for (MatchPlayer mp : pool.ordered) {
            ScoringService.Rarity r = rarity.of(mp);
            source = r.source().name();
            entries.add(new PoolEntry(pool.slotById.get(mp.getPlayerId()), mp.getPlayer().getDisplayName(),
                    mp.getPlayer().getShortName(), mp.getSlotPos(), mp.getRole().name(),
                    mp.getSlotX(), mp.getSlotY(), mp.getMinuteOn(), mp.getMinuteOff(),
                    mp.getReplacedId() == null ? null : pool.slotById.get(mp.getReplacedId()),
                    r.pct(), picked.contains(mp.getPlayerId())));
        }

        return new RevealView(puzzle.getNumber(), match.getTeam(), match.getOpponent(), match.getResult(),
                match.getCompetition(), match.getMatchDate(), match.getFormation(), source,
                entries, pickViews(mine, pool), play.getTotalScore());
    }

    private List<PickView> pickViews(List<Guess> list, PoolIndex pool) {
        return list.stream().map(g -> {
            MatchPlayer mp = g.getPlayerId() == null ? null : pool.byId.get(g.getPlayerId());
            return new PickView(g.getGuessIndex(), g.getRawText(), g.getResult(),
                    mp == null ? null : pool.slotById.get(mp.getPlayerId()),
                    mp == null ? null : mp.getPlayer().getDisplayName(),
                    mp == null ? null : mp.getPlayer().getShortName(),
                    mp == null ? null : mp.isSub(),
                    g.getTier(), g.getPoints());
        }).toList();
    }

    private record Slotted(MatchPlayer mp, int slot) {}

    /** A pool in slot order with lookups by player id. */
    private static final class PoolIndex {
        final List<MatchPlayer> ordered;
        final Map<Long, MatchPlayer> byId;
        final Map<Long, Integer> slotById;

        PoolIndex(List<MatchPlayer> ordered) {
            this.ordered = ordered;
            this.byId = ordered.stream().collect(Collectors.toMap(MatchPlayer::getPlayerId, Function.identity()));
            this.slotById = IntStream.range(0, ordered.size()).boxed()
                    .collect(Collectors.toMap(i -> ordered.get(i).getPlayerId(), i -> i + 1));
        }

        Slotted slotted(MatchPlayer mp) {
            return new Slotted(mp, slotById.get(mp.getPlayerId()));
        }
    }
}
