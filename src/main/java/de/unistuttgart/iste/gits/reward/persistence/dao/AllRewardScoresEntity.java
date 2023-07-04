package de.unistuttgart.iste.gits.reward.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity(name = "RewardScores")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllRewardScoresEntity {

    @EmbeddedId
    private PrimaryKey id;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private RewardScoreEntity health;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private RewardScoreEntity fitness;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private RewardScoreEntity growth;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private RewardScoreEntity strength;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private RewardScoreEntity power;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrimaryKey implements Serializable {
        private UUID userId;
        private UUID courseId;
    }
}
