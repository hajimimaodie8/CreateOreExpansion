package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * 技能策略 —— 负责计算技能需要处理的实体/方块集合，并提供对应渲染器。
 *
 * <p><b>双端接口</b>：本接口在服务端加载（技能注册/释放路径），因此
 * 方法签名<b>只能引用双端类型</b>（{@link Level} 而非客户端 {@code ClientLevel}）。
 * 渲染相关 API（{@link #getRenderer()}、{@link #shouldRender}）仅由客户端
 * 渲染代码调用，服务端从不调用；返回类型 {@link StrategyRenderer} 是客户端类型，
 * 属惰性解析，服务端加载本接口时不会触发其类加载。</p>
 *
 * @param <T> 被处理的对象类型（BlockPos / Entity）
 */
public interface SkillStrategy<T> {

    /**
     * 计算需要处理的对象集合（不包括中心方块）。
     *
     * @param skill  技能数据
     * @param params 上下文参数
     */
    Set<T> calculate(DataSkill skill, IParams params);

    /**
     * 是否需要渲染预览（仅客户端调用）。
     */
    default boolean shouldRender(DataSkill skill, Level world, IParams params) {
        return true;
    }

    /**
     * 策略对应的预览渲染器（方块范围框 / 实体轮廓 / 空）。
     * <b>仅客户端调用</b>：服务端调用会触发客户端渲染器类加载而崩溃。
     */
    StrategyRenderer getRenderer();
}
