package com.teamsheet.repo;

import com.teamsheet.domain.Play;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PlayRepository extends JpaRepository<Play, UUID> {

    /** Idempotent per device/day: a second call for the same (puzzle, anonId) is a no-op. */
    @Modifying
    @Query(value = """
            INSERT INTO play (puzzle_number, anon_id, started_at, guess_opened_at)
            VALUES (:puzzleNumber, :anonId, :now, :now)
            ON CONFLICT (puzzle_number, anon_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(int puzzleNumber, UUID anonId, Instant now);

    Optional<Play> findByPuzzleNumberAndAnonId(Integer puzzleNumber, UUID anonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Play p where p.id = :id")
    Optional<Play> findByIdForUpdate(UUID id);

    long countByPuzzleNumberAndFinishedAtIsNotNull(Integer puzzleNumber);
}
