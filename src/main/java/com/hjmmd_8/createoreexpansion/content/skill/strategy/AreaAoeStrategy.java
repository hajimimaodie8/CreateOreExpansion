package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
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
public class AreaAoeStrategy extends ConfigStrategy<BlockPos, AreaAoeConfig> implements AreaStrategy {

    private int width;
    private int height;
    private int depth;
    private DualDirection.From directionSource;

    /**
     * 无参构造器 - 通过config加载参数
     */
    public AreaAoeStrategy() {}

    /**
     * 从config加载参数
     * @param config 配置对象
     */
    @Override
    public void load(AreaAoeConfig config, DataSkill data) {
        this.width = config.width;
        this.height = config.height;
        this.depth = config.depth;
        this.directionSource = config.directionSource;
    }

    @Override
    protected Class<AreaAoeConfig> getConfigType() {
        return AreaAoeConfig.class;
    }

    @Override
    public Set<BlockPos> calculate(IParams params) {
        Player player = params.get("Player", Player.class);
        BlockHitResult hit = params.get("BlockHitResult", BlockHitResult.class);
        BlockPos center = params.get("Center", BlockPos.class);
        DualDirection dualDirection = DualDirection.from(player, hit, directionSource);
        return dualDirection.collect(center, width, height, depth);
    }

    @Override
    public boolean shouldRender(ClientLevel world, IParams params) {
        BlockState state = params.get("CenterState", BlockState.class);
        if (!(skill instanceof AreaAoeSkill aoeSkill)) return false;
        return state.is(aoeSkill.getMineableTag());
    }
}
