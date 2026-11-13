package com.teamsheet.repo;

import com.teamsheet.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, String> {}
