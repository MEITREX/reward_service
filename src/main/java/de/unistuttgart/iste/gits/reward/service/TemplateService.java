package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.generated.dto.Template;
import de.unistuttgart.iste.gits.reward.persistence.dao.TemplateEntity;
import de.unistuttgart.iste.gits.reward.persistence.mapper.TemplateMapper;
import de.unistuttgart.iste.gits.reward.persistence.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;

    public List<Template> getAllTemplates() {
        List<TemplateEntity> templates = templateRepository.findAll();
        return templates.stream().map(templateMapper::entityToDto).toList();
    }

}
