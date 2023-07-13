package de.unistuttgart.iste.gits.reward.persistence.repository;

import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllRewardScoresRepository extends JpaRepository<AllRewardScoresEntity, AllRewardScoresEntity.PrimaryKey> {

    List<AllRewardScoresEntity> findAllRewardScoresEntitiesById_CourseId(UUID id_courseId);

}
