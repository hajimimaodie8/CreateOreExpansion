package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeFellingStrategy;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.StrategySkill;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * 砍伐（伐树）技能（新内核版）——斧头连锁砍树。
 *
 * <p>算法与旧 {@code content/skill/FellingSkill}（继承自
 * {@code AoeExcavationSkill#causeAoe}）逐条对应：</p>
 * <table border="1">
 *   <caption>与旧实现的差异</caption>
 *   <tr><th>旧实现</th><th>新实现</th></tr>
 *   <tr><td>四类技能键的按下判定（{@code AllKeys.SKILL_RELEASE*}）</td>
 *       <td><b>去掉</b>：新路由只对"当前按下的按键槽位"调用，等价且更准（服务端权威按键）</td></tr>
 *   <tr><td>能量预检查 + {@code ToolEnergy.tryConsume}（在策略算出非空集合之后）</td>
 *       <td><b>移到</b> {@link #consumeResource}：新内核统一在 {@code release} 之前做
 *           「全部累加 → 校验 → 落账」，全有或全无</td></tr>
 *   <tr><td>冷却判定 {@code !creative && cooldown>0 && !isReady} → 放弃且不扣能；
 *       成功破坏后 {@code ToolSkillCooldown.start}</td>
 *       <td><b>逐条保留</b>，但拆成两处（{@link #consumeResource} 与 {@link #release}）
 *           都做同一判定——新内核先扣能后释放，只在 {@code release} 判冷却会"冷却中白扣能量"</td></tr>
 *   <tr><td>其余（服务端校验、创造模式硬度 0 跳过、原木目标判定、射线拾取、策略收集、逐块破坏）</td>
 *       <td>逐条保留</td></tr>
 * </table>
 *
 * <p><b>不动</b>旧 {@code FellingSkill#load} 注册的挖掘减速属性修饰器
 * （{@code BreakBlockSpeedModifiableAttribute}）：它不在技能释放链路上，继续走旧路径。</p>
 *
 * @since 1.0.0
 */
public class FellingItemSkill implements StrategySkill<BlockPos, ExcavationSkillContext> {

    @Override
    public ResourceKey<SkillStrategy<?, ?>> getStrategy() {
        return CoeFellingStrategy.KEY;
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
        // 旧 FellingSkill#isTargetBlock：只有原木能触发连锁砍伐
        if (!state.is(BlockTags.LOGS)) {
            return;
        }

        FellingConfig config = CoeFellingStrategy.configOf(context, instance);
        if (config == null) {
            return;
        }
        // 创造模式无冷却（与旧一致）；冷却中直接放弃（能量在 consumeResource 里已同样跳过，不会白扣）
        if (!player.isCreative() && config.cooldownSeconds > 0
                && !ToolSkillCooldown.isReady(player, context.tool())) {
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

        // 旧 FellingSkill 没有覆写 getMineableTag() → 不限制可破坏方块（tag 为 null）
        BlockBreaker.breakPositions(toDestroy, pos, context.tool(), level, player);

        // 真正执行过才启动冷却（创造模式无冷却，与旧一致）
        if (!player.isCreative() && config.cooldownSeconds > 0) {
            ToolSkillCooldown.start(player, context.tool(), config.cooldownSeconds);
        }
    }

    /**
     * 能量扣减：一级消耗取当前等级配置的 {@code Cost}，乘以有效等级与减耗附魔折扣
     * （{@link CoeSkillSupport#cost}），资源不足时由 {@link CoeSkillSupport#consume}
     * 补发低能量提示并让内核整体放弃本次释放。
     *
     * <p><b>为什么这里也要判冷却</b>：新内核的顺序是「{@code consumeResource} 全部累加 →
     * {@code canConsume} 校验 → 落账 → 才 {@code release}」。若这里照常扣能而
     * {@code release} 因冷却中直接返回，冷却中的每次挖掘都会白掉一份能量；所以</p>
     * <ul>
     *     <li>冷却中 → 不 {@code consume}（{@code release} 也会同样返回，口径必须一致）；</li>
     *     <li>中心方块不是原木 → 同样不扣（{@code release} 的第一道判定，旧实现也只在
     *         真正生效前才扣能）。</li>
     * </ul>
     */
    @Override
    public void consumeResource(ExcavationSkillContext context, Consumable consumable,
                                ISkillInstance<ExcavationSkillContext> instance) {
        if (context.getPlayer() == null || context.level() == null || context.pos() == null) {
            return;
        }
        FellingConfig config = CoeFellingStrategy.configOf(context, instance);
        if (config == null) {
            return;
        }
        if (!context.getPlayer().isCreative() && config.cooldownSeconds > 0
                && !ToolSkillCooldown.isReady(context.getPlayer(), context.tool())) {
            return;
        }
        // 只有"连锁确实会砍到树"时才扣能（旧 causeAoe 是先判 toDestroy 非空、再 tryConsume）。
        // 新内核先扣能后 release，缺这道判定时"砍一块孤零零的原木"会白掉一份能量。
        if (!CoeSkillSupport.willDoWork(context, instance, strategy())) {
            return;
        }
        int cost = CoeSkillSupport.cost(context.tool(), config.energyCost,
                CoeSkillSupport.effectiveLevel(context.tool(), instance));
        CoeSkillSupport.consume(context.getPlayer(), context.tool(), consumable, cost);
    }
}
