package de.unistuttgart.iste.gits.reward.persistence.repository;

import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllRewardScoresRepository extends JpaRepository<AllRewardScoresEntity, AllRewardScoresEntity.PrimaryKey> {

}
