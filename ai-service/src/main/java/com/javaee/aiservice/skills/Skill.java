package com.javaee.aiservice.skills;

public interface Skill {
    String getName();
    String getDescription();
    Object execute(Object... parameters);
}
