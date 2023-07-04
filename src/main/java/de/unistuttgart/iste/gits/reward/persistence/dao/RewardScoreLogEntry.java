package de.unistuttgart.iste.gits.reward.persistence.dao;

import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "RewardScoreLogEntry")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardScoreLogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(nullable = false)
    private int difference;

    @Column(nullable = false)
    private int oldValue;

    @Column(nullable = false)
    private int newValue;

    @Column(nullable = false)
    private RewardChangeReason reason;

    @ElementCollection
    private List<UUID> associatedContents;
}
