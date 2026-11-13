package com.teamsheet.repo;

import com.teamsheet.domain.Guess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuessRepository extends JpaRepository<Guess, Guess.Key> {
    List<Guess> findByPlayIdOrderByGuessIndex(UUID playId);
}
