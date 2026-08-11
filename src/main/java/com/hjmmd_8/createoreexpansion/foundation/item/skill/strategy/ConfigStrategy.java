package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Set;

public abstract class ConfigStrategy<P, C extends SkillConfig> implements SkillStrategy<P> {
    protected ItemSkill skill;

    public abstract void load(C config, DataSkill data);

    protected abstract Class<C> getConfigType();

    protected abstract Set<P> calculate(IParams params);

    protected abstract boolean shouldRender(ClientLevel world, IParams params);

    @Override
    public Set<P> calculate(DataSkill skill, IParams params) {
        load(skill.getConfig(getConfigType()), skill);
        this.skill = skill.skill;
        return calculate(params);
    }

    @Override
    public boolean shouldRender(DataSkill skill, ClientLevel world, IParams params) {
        load(skill.getConfig(getConfigType()), skill);
        this.skill = skill.skill;
        return shouldRender(world, params);
    }
}
