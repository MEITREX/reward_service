package de.unistuttgart.iste.meitrex.reward.persistence.repository;

import de.unistuttgart.iste.meitrex.reward.persistence.entity.AllRewardScoresEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllRewardScoresRepository extends JpaRepository<AllRewardScoresEntity, AllRewardScoresEntity.PrimaryKey> {

    /**
     * Find all reward scores entities by course id.
     *
     * @param id_courseId the course id
     * @return the list of all reward scores entities that have the given course id
     */
    @SuppressWarnings("java:S117")
        // naming convention is violated because the Spring Data JPA naming convention is used
    List<AllRewardScoresEntity> findAllRewardScoresEntitiesById_CourseId(UUID id_courseId);

}
