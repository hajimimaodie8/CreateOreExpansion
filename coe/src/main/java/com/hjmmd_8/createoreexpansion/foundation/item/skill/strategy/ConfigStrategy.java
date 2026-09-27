package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * 配置型策略基类 —— 无状态设计。
 *
 * <p>策略实例被全局共享（AllSkills 注册时创建），服务端逻辑与客户端渲染
 * 可能并发调用，因此<b>禁止在策略内保存可变字段</b>。
 * 配置对象每次从 {@link DataSkill} 直接取出并传入子类方法。</p>
 */
public abstract class ConfigStrategy<P, C extends SkillConfig> implements SkillStrategy<P> {

    protected abstract Class<C> getConfigType();

    protected abstract Set<P> calculate(C config, IParams params);

    /** 仅客户端调用（预览渲染）；{@code world} 用双端 {@link Level} 类型避免服务端加载客户端类 */
    protected abstract boolean shouldRender(C config, Level world, IParams params);

    @Override
    public final Set<P> calculate(DataSkill skill, IParams params) {
        return calculate(skill.getConfig(getConfigType()), params);
    }

    @Override
    public final boolean shouldRender(DataSkill skill, Level world, IParams params) {
        return shouldRender(skill.getConfig(getConfigType()), world, params);
    }
}
