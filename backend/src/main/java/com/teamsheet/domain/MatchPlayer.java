package com.teamsheet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.io.Serializable;

@Entity
@Table(name = "match_player")
@IdClass(MatchPlayer.Key.class)
public class MatchPlayer {

    @Id
    @Column(name = "match_id")
    private String matchId;

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private SquadRole role;

    @Column(name = "slot_pos", nullable = false)
    private String slotPos;

    @Column(name = "slot_x")
    private Integer slotX;

    @Column(name = "slot_y")
    private Integer slotY;

    @Column(name = "minute_on")
    private Integer minuteOn;

    @Column(name = "minute_off")
    private Integer minuteOff;

    @Column(name = "replaced_id")
    private Long replacedId;

    protected MatchPlayer() {}

    public String getMatchId() { return matchId; }
    public Long getPlayerId() { return playerId; }
    public Player getPlayer() { return player; }
    public SquadRole getRole() { return role; }
    public String getSlotPos() { return slotPos; }
    public Integer getSlotX() { return slotX; }
    public Integer getSlotY() { return slotY; }
    public Integer getMinuteOn() { return minuteOn; }
    public Integer getMinuteOff() { return minuteOff; }
    public Long getReplacedId() { return replacedId; }
    public boolean isSub() { return role == SquadRole.sub_used; }

    public static class Key implements Serializable {
        private String matchId;
        private Long playerId;

        public Key() {}

        public Key(String matchId, Long playerId) {
            this.matchId = matchId;
            this.playerId = playerId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && java.util.Objects.equals(matchId, k.matchId) && java.util.Objects.equals(playerId, k.playerId);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(matchId, playerId); }
    }
}
