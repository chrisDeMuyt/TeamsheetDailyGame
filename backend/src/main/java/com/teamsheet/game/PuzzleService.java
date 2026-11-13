package com.teamsheet.game;

import com.teamsheet.api.Dtos.PuzzleView;
import com.teamsheet.api.Dtos.SlotView;
import com.teamsheet.domain.Match;
import com.teamsheet.domain.MatchPlayer;
import com.teamsheet.domain.Puzzle;
import com.teamsheet.repo.MatchPlayerRepository;
import com.teamsheet.repo.MatchRepository;
import com.teamsheet.repo.PuzzleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class PuzzleService {

    private final PuzzleRepository puzzles;
    private final MatchRepository matches;
    private final MatchPlayerRepository matchPlayers;
    private final Clock clock;
    private final GameProperties props;

    public PuzzleService(PuzzleRepository puzzles, MatchRepository matches,
                         MatchPlayerRepository matchPlayers, Clock clock, GameProperties props) {
        this.puzzles = puzzles;
        this.matches = matches;
        this.matchPlayers = matchPlayers;
        this.clock = clock;
        this.props = props;
    }

    /** The puzzle for the current calendar date in the configured zone (midnight US Central by default). */
    public Puzzle today() {
        LocalDate date = LocalDate.ofInstant(clock.instant(), props.puzzleZone());
        return puzzles.findByPuzzleDate(date)
                .orElseThrow(() -> new NotFoundException("No puzzle scheduled for " + date));
    }

    public Puzzle byNumber(int number) {
        return puzzles.findById(number).orElseThrow(() -> new NotFoundException("Unknown puzzle " + number));
    }

    public Match match(Puzzle puzzle) {
        return matches.findById(puzzle.getMatchId()).orElseThrow();
    }

    /** Pool in slot order (index i = slot i+1). */
    public List<MatchPlayer> orderedPool(Puzzle puzzle) {
        return SlotLayout.ordered(matchPlayers.findPool(puzzle.getMatchId()));
    }

    @Transactional(readOnly = true)
    public PuzzleView todayView() {
        Puzzle puzzle = today();
        Match match = match(puzzle);
        List<MatchPlayer> pool = orderedPool(puzzle);
        List<SlotView> slots = IntStream.range(0, pool.size())
                .mapToObj(i -> {
                    MatchPlayer mp = pool.get(i);
                    return new SlotView(i + 1, mp.getSlotPos(), mp.getRole().name(), mp.getSlotX(), mp.getSlotY());
                })
                .toList();
        return new PuzzleView(puzzle.getNumber(), match.getTeam(), match.getCompetition(),
                match.getMatchDate(), match.getFormation(), slots);
    }
}
