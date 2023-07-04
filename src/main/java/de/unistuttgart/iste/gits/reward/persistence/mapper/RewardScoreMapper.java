package de.unistuttgart.iste.gits.reward.persistence.mapper;

import de.unistuttgart.iste.gits.generated.dto.RewardScores;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RewardScoreMapper {

    private final ModelMapper modelMapper;

    public RewardScores entityToDto(AllRewardScoresEntity templateEntity) {
        return modelMapper.map(templateEntity, RewardScores.class);
    }

}
