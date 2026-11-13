package com.teamsheet.repo;

import com.teamsheet.domain.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PuzzleRepository extends JpaRepository<Puzzle, Integer> {
    Optional<Puzzle> findByPuzzleDate(LocalDate date);
}
