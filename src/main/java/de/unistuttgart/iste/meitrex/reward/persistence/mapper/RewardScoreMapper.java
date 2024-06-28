package de.unistuttgart.iste.meitrex.reward.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.RewardScores;
import de.unistuttgart.iste.meitrex.reward.persistence.entity.AllRewardScoresEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RewardScoreMapper {

    private final ModelMapper modelMapper;

    public RewardScores entityToDto(AllRewardScoresEntity allRewardScoresEntity) {
        return modelMapper.map(allRewardScoresEntity, RewardScores.class);
    }

}
