package com.javaee.aiservice.skills;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SkillExecutorService {

    private final SkillRegistry skillRegistry;

    @Autowired
    public SkillExecutorService(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    public Object executeSkill(String skillName, Object... parameters) {
        Skill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("技能不存在: " + skillName);
        }
        return skill.execute(parameters);
    }

    public String getSkillDescription(String skillName) {
        Skill skill = skillRegistry.getSkill(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("技能不存在: " + skillName);
        }
        return skill.getDescription();
    }
}
