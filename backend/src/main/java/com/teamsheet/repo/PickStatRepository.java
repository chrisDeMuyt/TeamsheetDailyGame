package com.teamsheet.repo;

import com.teamsheet.domain.PickStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PickStatRepository extends JpaRepository<PickStat, PickStat.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO pick_stat (puzzle_number, player_id, picks) VALUES (:puzzleNumber, :playerId, 1)
            ON CONFLICT (puzzle_number, player_id) DO UPDATE SET picks = pick_stat.picks + 1
            """, nativeQuery = true)
    void increment(int puzzleNumber, long playerId);
}
