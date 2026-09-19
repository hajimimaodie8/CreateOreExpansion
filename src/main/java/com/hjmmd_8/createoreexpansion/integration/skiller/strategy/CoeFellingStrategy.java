package com.hjmmd_8.createoreexpansion.integration.skiller.strategy;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockSearch;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.CoeSkillSupport;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 砍伐（伐树）策略（新内核版）——斧头连锁砍树的"砍哪些方块"计算。
 *
 * <p>与旧 {@code content/skill/strategy/FellingStrategy} 同源同算法：以所击方块为起点
 * BFS 收集相连的、匹配 {@link FellingConfig.BlockPredicate} 的方块（{@code maxBlocks} 上限 /
 * {@code searchRange} 曼哈顿半径），并把<b>起点自身剔除</b>（起点由触发端那次普通挖掘负责）。
 * 差别只在数据来源——配置从技能实例按<b>有效等级</b>取（等价旧
 * {@code SkillsComponent.applySkillBoost} 的效果），不再是旧
 * {@link com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill}。</p>
 *
 * <p><b>速度修正不在本类</b>：旧 {@code FellingSkill#load} 里注册的
 * {@code BreakBlockSpeedModifiableAttribute} 修饰器不在技能释放链路上（它只影响挖掘耗时），
 * 继续走旧路径，这里不复制。</p>
 *
 * @since 1.0.0
 */
public class CoeFellingStrategy implements SkillStrategy<BlockPos, ExcavationSkillContext> {

    /** 注册 id：{@code createoreexpansion:felling}（新策略自己的键，与旧技能 id {@code fell} 不同层） */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "felling");

    /** 该策略在 {@code skiller:skill_strategy} 注册表中的键（技能用 {@code getStrategy()} 引用它）。 */
    public static final ResourceKey<SkillStrategy<?, ?>> KEY = createKey();

    /** 客户端预览渲染器 id——与 {@link CoeAreaAoeStrategy} <b>同一个</b>（都是方块描边预览）。 */
    public static final ResourceLocation RENDERER_ID = CoeAreaAoeStrategy.RENDERER_ID;

    @Override
    public void collect(Set<BlockPos> set, ExcavationSkillContext context, ISkillInstance<ExcavationSkillContext> instance) {
        FellingConfig config = configOf(context, instance);
        Level level = context.level();
        BlockPos center = context.pos();
        if (config == null || level == null || center == null) {
            return;
        }
        // 与旧 FellingStrategy#calculateTreeBlocks 一致：BFS 结果里去掉起点自身
        Set<BlockPos> found = BlockSearch.collect(level, center, config.maxBlocks, config.searchRange,
                config.predicate);
        found.remove(center);
        set.addAll(found);
    }

    /**
     * 可行性判定——等价旧 {@code FellingStrategy#shouldRender}：
     * 中心方块必须是原木（{@code BlockTags.LOGS}），否则不预览、也不连锁。
     */
    @Override
    public boolean canCollect(ExcavationSkillContext context, ISkillInstance<ExcavationSkillContext> instance) {
        if (configOf(context, instance) == null || context.level() == null || context.pos() == null) {
            return false;
        }
        return FellingConfig.BlockPredicate.IS_LOG.test(context.level().getBlockState(context.pos()));
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    /**
     * 取该技能当前的等级配置：**优先按有效等级从注册表取**（等价旧
     * {@code SkillsComponent.applySkillBoost} —— 技艺提升附魔会让范围/上限/冷却一起升到该等级），
     * 注册表查不到时回落到物品 NBT 里的 {@code Config} 快照
     * （{@link FellingConfig} 是 {@code AutoSkillConfig} 子类，用 {@code loadFromNbt} 读同一个子标签）。
     */
    @Nullable
    public static FellingConfig configOf(ExcavationSkillContext context,
                                         ISkillInstance<ExcavationSkillContext> instance) {
        FellingConfig fromTable = CoeSkillSupport.configForLevel(context.tool(), instance, FellingConfig.class);
        if (fromTable != null) {
            return fromTable;
        }
        CompoundTag nbt = instance.data();
        if (nbt == null || !nbt.contains("Config")) {
            return null;
        }
        // 兜底：物品 NBT 上的配置快照（旧的"物品自带配置"路径）
        FellingConfig snapshot = new FellingConfig(0, 1, FellingConfig.BlockPredicate.IS_LOG, 0, 0.0F, 0.0F, 0);
        snapshot.loadFromNbt(nbt);
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillStrategy<?, ?>> createKey() {
        return (ResourceKey<SkillStrategy<?, ?>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.STRATEGY, ID);
    }
}
