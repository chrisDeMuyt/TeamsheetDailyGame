package com.teamsheet.domain;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "player_alias")
@IdClass(PlayerAlias.Key.class)
public class PlayerAlias {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Id
    @Column(name = "alias_norm")
    private String aliasNorm;

    protected PlayerAlias() {}

    public PlayerAlias(Long playerId, String aliasNorm) {
        this.playerId = playerId;
        this.aliasNorm = aliasNorm;
    }

    public Long getPlayerId() { return playerId; }
    public String getAliasNorm() { return aliasNorm; }

    public static class Key implements Serializable {
        private Long playerId;
        private String aliasNorm;

        public Key() {}

        public Key(Long playerId, String aliasNorm) {
            this.playerId = playerId;
            this.aliasNorm = aliasNorm;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && java.util.Objects.equals(playerId, k.playerId) && java.util.Objects.equals(aliasNorm, k.aliasNorm);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(playerId, aliasNorm); }
    }
}
