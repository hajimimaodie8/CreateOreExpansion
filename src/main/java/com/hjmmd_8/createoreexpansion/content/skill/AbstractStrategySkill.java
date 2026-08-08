package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.AbstractSkill;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 策略技能抽象基类 - 定义技能与Strategy的强类型依赖关系
 *
 * <p>每个具体技能类都应该继承此类并指定它使用的Strategy类型。
 * 这样可以确保类型安全，防止配置错误。</p>
 *
 * @param <S> 此技能使用的Strategy类型（必须继承AreaStrategy）
 */
public abstract class AbstractStrategySkill<S extends AreaStrategy> extends AbstractSkill {

    private final S strategy;

    /**
     * 创建策略技能
     * @param strategy 此技能使用的策略实例
     * @throws IllegalArgumentException 如果strategy为null
     */
    protected AbstractStrategySkill(S strategy) {
        super();
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        this.strategy = strategy;
    }

    /**
     * 获取此技能使用的策略
     * @return 策略实例
     */
    public final S getStrategy() {
        return strategy;
    }

    /**
     * 获取策略类型（用于反射等场景）
     * @return 策略的Class对象
     */
    @SuppressWarnings("unchecked")
    public final Class<S> getStrategyType() {
        return (Class<S>) strategy.getClass();
    }

    public final Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        return getStrategy().calculatePositions(this, center, hit, player);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "strategy=" + strategy +
                ", cost=" + getCost() +
                '}';
    }
}
