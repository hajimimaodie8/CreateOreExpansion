package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 范围AOE策略 - 用于镐子和铲子的范围挖掘
 *
 * <p>支持：
 * <ul>
 *   <li>平面范围 - 3x3, 5x5 等</li>
 *   <li>深度范围 - 多层挖掘</li>
 *   <li>线性挖掘 - 一列挖掘</li>
 * </ul>
 *
 * <p>基于 {@link DualDirection} 计算范围，与实际挖掘行为一致。
 */
public class AreaAoeStrategy extends ConfigStrategy<AreaAoeConfig> {

    private int width;
    private int height;
    private int depth;
    private DualDirection.From directionSource;

    /**
     * 无参构造器 - 通过config加载参数
     */
    public AreaAoeStrategy() {
        // 默认值
        this.width = 3;
        this.height = 3;
        this.depth = 1;
        this.directionSource = DualDirection.From.BLOCK_FACE;
    }

    /**
     * 从config加载参数
     * @param config 配置对象
     */
    @Override
    public void load(AreaAoeConfig config) {
        this.width = config.width;
        this.height = config.height;
        this.depth = config.depth;
        this.directionSource = config.directionSource;
    }

    @Override
    public Set<BlockPos> calculatePositions(DataSkill skill, BlockPos center, BlockHitResult hit, Player player) {
        DualDirection dualDirection = DualDirection.from(player, hit, directionSource);
        return dualDirection.collect(center, width, height, depth);
    }

    @Override
    public boolean shouldRender(DataSkill data, ClientLevel world, BlockPos pos, BlockState state, Player player) {
        ItemSkill skill = data.skill;
        if (!(skill instanceof AreaAoeSkill aoeSkill)) return false;
        return state.is(aoeSkill.getMineableTag());
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public DualDirection.From getDirectionSource() { return directionSource; }
}
