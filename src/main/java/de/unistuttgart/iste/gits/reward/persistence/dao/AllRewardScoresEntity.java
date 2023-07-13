package de.unistuttgart.iste.gits.reward.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RewardScoreEntity health;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RewardScoreEntity fitness;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RewardScoreEntity growth;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RewardScoreEntity strength;
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RewardScoreEntity power;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrimaryKey implements Serializable {
        private UUID courseId;
        private UUID userId;
    }
}
