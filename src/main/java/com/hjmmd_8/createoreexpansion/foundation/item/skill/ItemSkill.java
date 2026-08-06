package com.hjmmd_8.createoreexpansion.foundation.item.skill;

public interface ItemSkill {

    /**
     * 释放技能
     * @param context 技能上下文
     * @throws ClassCastException 技能上下文类型错误
     */
    void release(Object context);

    SkillType getType();
}
