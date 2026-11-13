package com.teamsheet.repo;

import com.teamsheet.domain.MatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, MatchPlayer.Key> {
    @Query("select mp from MatchPlayer mp join fetch mp.player where mp.matchId = :matchId")
    List<MatchPlayer> findPool(String matchId);
}
