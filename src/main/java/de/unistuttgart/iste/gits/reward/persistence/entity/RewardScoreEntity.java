package de.unistuttgart.iste.gits.reward.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity(name = "RewardScore")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardScoreEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private int value;

    @Column(nullable = false)
    @Builder.Default
    private float percentage = 0f;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("date DESC")
    @Builder.Default
    private List<RewardScoreLogEntry> log = new ArrayList<>();

}
