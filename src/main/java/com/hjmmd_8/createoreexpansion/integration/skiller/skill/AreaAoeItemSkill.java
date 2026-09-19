package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeAreaAoeStrategy;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.StrategySkill;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * 范围挖掘技能（新内核版）——开岩 {@code shatter} / 引渠 {@code channel} / 平场 {@code grade}
 * 三个技能共用同一个实现类：它们的差异全在各自的等级配置（{@code SkillAoeConfigs}）里。
 *
 * <p>算法与旧 {@code content/skill/AoeExcavationSkill#causeAoe} 同源，逐项对应：</p>
 * <table border="1">
 *   <caption>与旧实现的差异</caption>
 *   <tr><th>旧实现</th><th>新实现</th></tr>
 *   <tr><td>四类技能键的按下判定（{@code AllKeys.SKILL_RELEASE*}）</td>
 *       <td><b>去掉</b>：新路由只对"当前按下的按键槽位"调用，等价且更准（服务端权威按键）</td></tr>
 *   <tr><td>能量预检查 + {@code ToolEnergy.tryConsume}</td>
 *       <td><b>移到</b> {@link #consumeResource}：新内核统一在 {@code release} 之前做
 *           「全部累加 → 校验 → 落账」，全有或全无</td></tr>
 *   <tr><td>冷却判定与启动（AoE 家族配置里冷却为 0，本就没走）</td>
 *       <td>同样不涉及</td></tr>
 *   <tr><td>其余（服务端/创造模式硬度 0 跳过、射线拾取、策略收集、逐块破坏）</td>
 *       <td>逐条保留</td></tr>
 * </table>
 *
 * @since 1.0.0
 */
public class AreaAoeItemSkill implements StrategySkill<BlockPos, ExcavationSkillContext> {

    @Override
    public ResourceKey<SkillStrategy<?, ?>> getStrategy() {
        return CoeAreaAoeStrategy.KEY;
    }

    @Override
    public void release(ExcavationSkillContext context, ISkillInstance<ExcavationSkillContext> instance) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Level level = context.level();
        BlockPos pos = context.pos();
        if (level == null || pos == null || level.isClientSide) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        // 生存模式跳过硬度为 0 的方块；创造模式允许破坏任何方块（与旧实现一致）
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) {
            return;
        }

        SkillStrategy<BlockPos, ExcavationSkillContext> strategy = strategy();
        if (strategy == null || !strategy.canCollect(context, instance)) {
            return;
        }
        Set<BlockPos> toDestroy = new HashSet<>();
        strategy.collect(toDestroy, context, instance);
        if (toDestroy.isEmpty()) {
            return;
        }

        AreaAoeConfig config = CoeAreaAoeStrategy.configOf(context, instance);
        BlockBreaker.breakPositions(toDestroy, pos, context.tool(), level, player,
                config == null ? null : config.mineableTag);
    }

    /**
     * 能量扣减：一级消耗取当前等级配置的 {@code Cost}，乘以有效等级与减耗附魔折扣
     * （{@link CoeSkillSupport#cost}），资源不足时由 {@link CoeSkillSupport#consume}
     * 补发低能量提示并让内核整体放弃本次释放。
     */
    @Override
    public void consumeResource(ExcavationSkillContext context, Consumable consumable,
                                ISkillInstance<ExcavationSkillContext> instance) {
        AreaAoeConfig config = CoeAreaAoeStrategy.configOf(context, instance);
        if (config == null) {
            return;
        }
        int cost = CoeSkillSupport.cost(context.tool(), config.energyCost,
                CoeSkillSupport.effectiveLevel(context.tool(), instance));
        CoeSkillSupport.consume(context.getPlayer(), context.tool(), consumable, cost);
    }
}
