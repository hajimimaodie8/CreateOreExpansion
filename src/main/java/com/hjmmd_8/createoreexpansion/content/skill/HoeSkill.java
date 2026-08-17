package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.config.HoeConfig;
import com.hjmmd_8.createoreexpansion.content.skill.context.UseOnBlockContext;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.HoeStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import com.hjmmd_8.createoreexpansion.foundation.util.FarmlandHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 锄头技能 —— 右键农田的四优先级智能操作。
 *
 * 优先级（严格从上到下，命中即执行）：
 * <ol>
 *     <li><b>成熟作物</b>：收割范围内全部成熟作物并原地重种（概率催熟），
 *     同时对范围内耕地空位补种背包种子；</li>
 *     <li><b>未成熟作物</b>：背包有骨粉时批量施肥——对范围内全部未成熟作物
 *     逐株使用骨粉催熟（骨粉用完即停）；</li>
 *     <li><b>耕地</b>：按背包顺序将可种植物品种植到范围内所有耕地空位；</li>
 *     <li><b>泥土/草地</b>：将范围内所有可犁方块转换为耕地（不播种不收割）。</li>
 * </ol>
 *
 * 能量约束：
 * <ul>
 *     <li>能量校验在任何实际操作之前——不足直接返回，绝不扣费、不触发逻辑；</li>
 *     <li>先确认范围内存在可操作目标，再扣费执行，保证每次消耗都有实际效果。</li>
 * </ul>
 */
public class HoeSkill extends AbstractStrategySkill<BlockPos, HoeStrategy>
        implements ConfigSkill<UseItemContext<?>, HoeConfig> {

    protected DataSkill data;
    private int energyCost;

    public HoeSkill(HoeStrategy strategy) {
        super(strategy);
    }

    @Override
    public void load(HoeConfig config, DataSkill data) {
        this.energyCost = config.energyCost;
        this.data = data;
    }

    @Override
    public Class<HoeConfig> getConfigType() {
        return HoeConfig.class;
    }

    @Override
    public void release(UseItemContext<?> context) {
        if (!(context instanceof UseOnBlockContext blockContext)) return;

        Player player = blockContext.getPlayer();
        if (player == null) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        // 目标方块与作用范围（纯读操作）
        BlockPos center = blockContext.event().getPos();
        BlockState centerState = level.getBlockState(center);
        HoeConfig config = data != null ? data.getConfig(getConfigType()) : null;
        if (config == null) return; // 配置缺失（异常状态），放弃本次释放
        List<BlockPos> area = FarmlandHelper.collectArea(center, config.rangeWidth, config.rangeDepth);

        // ========== 优先级判定 ==========
        switch (FarmlandHelper.resolvePriority(centerState)) {
            case 1 -> releaseHarvest(level, area, player, config);
            case 2 -> releaseFertilize(level, area, player);
            case 3 -> releasePlant(level, area, player);
            case 4 -> releaseTill(level, area, player);
            default -> {
                // 目标不可操作：不消耗能量、不执行任何逻辑
            }
        }
    }

    // ========== 优先级 1：一键收割 + 原地重种 + 空耕地补种 ==========

    private void releaseHarvest(Level level, List<BlockPos> area, Player player, HoeConfig config) {
        // 预览：范围内必须存在可收割目标，否则不扣费
        if (!FarmlandHelper.hasHarvestable(level, area)) return;

        // 能量校验：不足直接返回（不扣费、不执行）
        if (!tryConsumeEnergy(player)) return;

        // 一键收割全部成熟作物 + 原地重种 + 范围内空耕地用背包种子补种
        FarmlandHelper.harvestAndReplant(level, area, player, config);
    }

    // ========== 优先级 2：批量施肥（未成熟作物 + 背包有骨粉） ==========

    private void releaseFertilize(Level level, List<BlockPos> area, Player player) {
        // 预览：范围内必须有未成熟作物且背包有骨粉，否则不扣费
        if (!FarmlandHelper.hasImmatureCrop(level, area)) return;
        if (!FarmlandHelper.hasBonemeal(player)) return;

        // 能量校验：不足直接返回（不扣费、不执行）
        if (!tryConsumeEnergy(player)) return;

        // 批量施肥：对范围内全部未成熟作物逐株使用骨粉催熟
        FarmlandHelper.fertilizeAll(level, area, player);
    }

    // ========== 优先级 3：播种 ==========

    private void releasePlant(Level level, List<BlockPos> area, Player player) {
        // 预览：范围内必须有可播种空位，否则不扣费
        if (!FarmlandHelper.hasPlantableSpot(level, area, player)) return;

        // 能量校验：不足直接返回（不扣费、不执行）
        if (!tryConsumeEnergy(player)) return;

        FarmlandHelper.plantAll(level, area, player);
    }

    // ========== 优先级 4：犁地 ==========

    private void releaseTill(Level level, List<BlockPos> area, Player player) {
        // 预览：范围内必须有可犁方块，否则不扣费
        if (!FarmlandHelper.hasTillable(level, area)) return;

        // 能量校验：不足直接返回（不扣费、不执行）
        if (!tryConsumeEnergy(player)) return;

        FarmlandHelper.tillAll(level, area);
    }

    /**
     * 统一的能量校验与消耗。
     *
     * 调用前必须已确认范围内存在可操作目标，保证扣费必有实际效果。
     *
     * @return true=能量足够且已消耗，false=能量不足（已发送提示，未扣费）
     */
    private boolean tryConsumeEnergy(Player player) {
        var stack = player.getMainHandItem();
        if (!ToolEnergy.canAfford(stack, energyCost)) {
            ToolEnergy.sendLowEnergy(player, stack);
            return false;
        }
        ToolEnergy.tryConsume(player, stack, this);
        return true;
    }

    @Override
    public int getCost() {
        return energyCost;
    }

    @Override
    public SkillType getType() {
        return SkillType.USE_SKILL;
    }
}
