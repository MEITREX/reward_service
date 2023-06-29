package de.unistuttgart.iste.gits.reward.persistence.mapper;

import de.unistuttgart.iste.gits.reward.persistence.dao.TemplateEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateMapper {

    private final ModelMapper modelMapper;

    public Template entityToDto(TemplateEntity templateEntity) {
        // add specific mapping here if needed
        return modelMapper.map(templateEntity, Template.class);
    }

    public TemplateEntity dtoToEntity(Template template) {
        // add specific mapping here if needed
        return modelMapper.map(template, TemplateEntity.class);
    }
}
