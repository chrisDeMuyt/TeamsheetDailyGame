package com.teamsheet.repo;

import com.teamsheet.domain.PickPct;
import com.teamsheet.domain.PickStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickPctRepository extends JpaRepository<PickPct, PickStat.Key> {
    List<PickPct> findByPuzzleNumber(Integer puzzleNumber);
}
