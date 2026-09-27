package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * 技能策略 —— 负责计算技能需要处理的实体/方块集合。
 *
 * <p><b>双端接口</b>：本接口在服务端加载（技能注册/释放路径），因此
 * 方法签名<b>只能引用双端类型</b>（{@link Level} 而非客户端 {@code ClientLevel}）。</p>
 *
 * <p><b>渲染 API 已移除（W2 渲染统一）</b>：旧的 {@code getRenderer()}（返回客户端渲染器
 * {@code client.tool.StrategyRenderer}）随旧渲染栈一起删除 —— 它是旧调度器
 * {@code SkillsStrategyRenderer} 与本接口之间唯一的那条边，调度器一删，本接口就成了
 * 旧渲染器唯一的持有者。客户端预览现在由 Skiller 的 {@code StrategyRenderer} 负责：
 * 它不认识旧 {@code ItemSkill}，只按 {@code integration/skiller/strategy/**} 的
 * {@code getRendererId()} 找渲染器，旧框架不再需要、也不可能再提供渲染器。</p>
 *
 * <p>{@link #shouldRender} 保留：{@code content/skill/strategy/**} 的既有实现仍在覆写它
 * （删掉会连带改动旧框架本体，属第二阶段 W6 的范围）。注意它现在<b>没有调用方</b>——
 * 仅有的两个调用点（旧 {@code BlockToolOutlineRenderer} / {@code EntityOutlineRenderer}）
 * 已随旧栈删除，这一条属 W6 的收尾清单。</p>
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
}
