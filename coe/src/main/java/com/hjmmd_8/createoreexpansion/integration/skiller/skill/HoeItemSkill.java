package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.HoeConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.FarmlandHelper;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.UseItemSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

import java.util.List;

/**
 * 锄头技能（新内核版）——右键农田的四优先级智能操作。
 *
 * <p>优先级与旧 {@code content/skill/HoeSkill} 完全一致（严格从上到下、命中即执行）：
 * ① 成熟作物收割+补种 ② 未成熟作物批量施肥 ③ 耕地播种 ④ 泥土/草地犁地。</p>
 *
 * <h2>与旧实现的差异</h2>
 * <ul>
 *   <li>去掉技能键判定（新路由只对按下的槽位调用）。</li>
 *   <li>只处理「右键方块」这条路径：旧实现是
 *       {@code if (!(context instanceof UseOnBlockContext)) return;}，
 *       新上下文把两条路径都带上，这里同样只认 {@link UseItemOnBlockEvent}。</li>
 *   <li><b>扣费拆到 {@link #consumeResource}</b>：旧实现在每个优先级分支里先做"范围内有没有
 *       可操作目标"的预览判定，通过后再 {@code tryConsumeEnergy}。新内核先扣费后释放，
 *       所以这里把"优先级 + 预览判定"整段搬到 {@code consumeResource}，判定通过才累加消耗；
 *       判定结果（优先级编号）通过上下文的 scratch 传给 {@link #release}，避免随机/重复判定
 *       两端不一致。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class HoeItemSkill implements ItemSkill<UseItemSkillContext> {

    /** 本次释放解析出的优先级（1~4；0 = 不可操作）。由 consumeResource 写入、release 读取。 */
    private static final String SCRATCH_PRIORITY = "coe:hoe_priority";

    @Override
    public void release(UseItemSkillContext context, ISkillInstance<UseItemSkillContext> instance) {
        UseItemOnBlockEvent event = context.useOnBlockEvent();
        Player player = context.getPlayer();
        if (event == null || player == null) {
            return; // 只处理"右键方块"这条路径（与旧实现一致）
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        Integer priority = context.getScratch(SCRATCH_PRIORITY, Integer.class);
        if (priority == null || priority <= 0) {
            return; // 不可操作 / 预览判定没过（此时也没扣费）
        }
        HoeConfig config = CoeSkillSupport.configForLevel(
                player.getMainHandItem(), instance, HoeConfig.class);
        if (config == null) {
            return;
        }
        List<BlockPos> area = FarmlandHelper.collectArea(
                event.getPos(), config.rangeWidth, config.rangeDepth);

        switch (priority) {
            case 1 -> FarmlandHelper.harvestAndReplant(level, area, player, config);
            case 2 -> FarmlandHelper.fertilizeAll(level, area, player);
            case 3 -> FarmlandHelper.plantAll(level, area, player);
            case 4 -> FarmlandHelper.tillAll(level, area);
            default -> { /* 不可操作，什么都不做 */ }
        }
    }

    /**
     * 解析优先级并做"预览判定"，通过才累加消耗。
     *
     * <p>与旧 {@code HoeSkill} 的 1/2/3/4 分支逐条对应：每个分支先用
     * {@code FarmlandHelper.hasXxx} 确认范围内确有可操作目标，再扣费 —— 保证"每次消耗都有实际效果"。</p>
     */
    @Override
    public void consumeResource(UseItemSkillContext context, Consumable consumable,
                                ISkillInstance<UseItemSkillContext> instance) {
        UseItemOnBlockEvent event = context.useOnBlockEvent();
        Player player = context.getPlayer();
        if (event == null || player == null) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        HoeConfig config = CoeSkillSupport.configForLevel(
                player.getMainHandItem(), instance, HoeConfig.class);
        if (config == null) {
            return;
        }
        BlockPos center = event.getPos();
        BlockState centerState = level.getBlockState(center);
        List<BlockPos> area = FarmlandHelper.collectArea(center, config.rangeWidth, config.rangeDepth);

        int priority = FarmlandHelper.resolvePriority(centerState);
        boolean actionable = switch (priority) {
            case 1 -> FarmlandHelper.hasHarvestable(level, area);
            case 2 -> FarmlandHelper.hasImmatureCrop(level, area) && FarmlandHelper.hasBonemeal(player);
            case 3 -> FarmlandHelper.hasPlantableSpot(level, area, player);
            case 4 -> FarmlandHelper.hasTillable(level, area);
            default -> false;
        };
        if (!actionable) {
            return; // 目标不可操作：不扣费、不执行（旧实现同样的 early-return）
        }
        context.putScratch(SCRATCH_PRIORITY, priority);

        int cost = CoeSkillSupport.cost(player.getMainHandItem(), config.energyCost,
                CoeSkillSupport.effectiveLevel(player.getMainHandItem(), instance));
        CoeSkillSupport.consume(player, player.getMainHandItem(), consumable, cost);
    }
}
