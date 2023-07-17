package de.unistuttgart.iste.gits.reward.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
