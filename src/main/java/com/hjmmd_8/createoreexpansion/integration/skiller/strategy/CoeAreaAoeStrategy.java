package com.hjmmd_8.createoreexpansion.integration.skiller.strategy;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.CoeSkillSupport;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 范围 AOE 策略（新内核版）——开岩 / 引渠 / 平场共用的"挖哪些方块"计算。
 *
 * <p>与旧 {@code content/skill/strategy/AreaAoeStrategy} 同源同算法：
 * 由 {@link DualDirection}（跟随玩家朝向或所击方块面）在目标方块周围收集
 * {@code width × height × depth} 的方块集合。差别只在数据来源——配置从技能实例
 * 按<b>有效等级</b>取（等价旧 {@code applySkillBoost} 的效果），不再是旧
 * {@code DataSkill}。</p>
 *
 * <h2>物理结构（Sable / 航空学 sub-level）</h2>
 * <p>命中结构上的方块时，朝向来源（视线、所击方块面）先换到<b>结构局部坐标系</b>
 * 再解析（见 {@link #resolveDirection}），集合因此是"结构本地系里的矩形"；
 * 渲染端乘结构位姿矩阵，矩形便随结构一起倾斜。主世界路径不受影响。</p>
 *
 * @since 1.0.0
 */
public class CoeAreaAoeStrategy implements SkillStrategy<BlockPos, ExcavationSkillContext> {

    /** 注册 id：{@code createoreexpansion:area_aoe} */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "area_aoe");

    /** 该策略在 {@code skiller:skill_strategy} 注册表中的键（技能用 {@code getStrategy()} 引用它）。 */
    public static final ResourceKey<SkillStrategy<?, ?>> KEY = createKey();

    /** 客户端预览渲染器 id（W5 注册；本轮先给稳定 id，预览仍由旧渲染器提供）。 */
    public static final ResourceLocation RENDERER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "block_outline");

    /** 与旧 {@code AoeExcavationSkill.PICK_DISTANCE} 一致 */
    private static final double PICK_DISTANCE = 20.0D;

    @Override
    public void collect(Set<BlockPos> set, ExcavationSkillContext context, ISkillInstance<ExcavationSkillContext> instance) {
        AreaAoeConfig config = configOf(context, instance);
        Player player = context.getPlayer();
        if (config == null || player == null) {
            return;
        }
        BlockHitResult hit = pick(player);
        if (hit == null) {
            return;
        }
        DualDirection dualDirection = resolveDirection(context, player, hit, config);
        set.addAll(dualDirection.collect(context.pos(), config.width, config.height, config.depth));
    }

    /**
     * 解析"平面朝向"。两条路径：
     *
     * <ul>
     *   <li><b>主世界</b>（未装 Sable / 本次没命中物理结构，即 {@code !context.onSubLevel()}）：
     *       照旧 {@link DualDirection#from(Player, BlockHitResult, DualDirection.From)}
     *       ——用世界空间的偏航角 / 所击方块面，行为与引入结构场景之前逐字一致。</li>
     *   <li><b>物理结构</b>（Sable / 航空学 sub-level）：把两个朝向来源都经桥接
     *       {@code toLocalDir} 换算到<b>结构局部坐标系</b>再解析，于是
     *       {@link DualDirection#collect} 得到的是"结构本地系里的矩形"。渲染端
     *       （{@code CoeBlockOutlineRenderer}）本来就对这个集合
     *       {@code mulPose} 结构位姿矩阵，所以形状自然随结构倾斜：结构水平 → 平面水平，
     *       结构倾斜 → 平面跟着倾斜。</li>
     *   <li><b>玩家自身已在结构本地系</b>（站在结构上，Sable 的 entities_stick_sublevels
     *       让玩家位置/视线/所击面都留在局部空间）：两个来源本来就是局部向量，
     *       <b>不能</b>再 {@code toLocalDir}（那等于把结构旋转反向施加第二遍 → 平面朝向错掉），
     *       直接交给 {@link DualDirection#fromLocal}。</li>
     * </ul>
     */
    private static DualDirection resolveDirection(ExcavationSkillContext context, Player player,
                                                  BlockHitResult hit, AreaAoeConfig config) {
        SubLevelBridge bridge = SableBridges.get();
        SubLevelBridge.Hit structureHit = context.subLevelHit();
        // 与 ExcavationSkillContext#onSubLevel() 同一判据；不满足即主世界路径（一字未改）
        if (bridge == null || structureHit == null) {
            return DualDirection.from(player, hit, config.directionSource);
        }
        Vec3 worldFace = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        // 站在结构上的玩家**自身**就在结构本地系里（Sable 的 entities_stick_sublevels 会
        // 把玩家位置/视线/所击面都保持在结构局部空间），此时两个朝向来源本来就是局部向量，
        // 再换算一次等于把结构的旋转反向施加第二遍 → 平面朝向错掉。
        // 判据用"玩家位置是否落在结构本地系"，与拾取点是否局部无关（站在旁边看结构时玩家仍是世界系）。
        if (bridge.locateSubLevel(context.level(), player.position()) != null) {
            return DualDirection.fromLocal(config.directionSource, worldFace, player.getLookAngle());
        }
        // 世界 → 结构局部（与预览渲染同一条位姿：桥接的 toLocalDir）
        Vec3 localLook = bridge.toLocalDir(structureHit, player.getLookAngle());
        Vec3 localFace = bridge.toLocalDir(structureHit, worldFace);
        return DualDirection.fromLocal(config.directionSource, localFace, localLook);
    }

    @Override
    public boolean canCollect(ExcavationSkillContext context, ISkillInstance<ExcavationSkillContext> instance) {
        AreaAoeConfig config = configOf(context, instance);
        if (config == null || context.level() == null || config.mineableTag == null) {
            return false;
        }
        BlockState state = context.blockState(context.pos());
        return state.is(config.mineableTag);
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    /**
     * 取该技能当前的等级配置：**优先按有效等级从注册表取**（等价旧
     * {@code SkillsComponent.applySkillBoost} —— 技艺提升附魔会让范围/消耗一起升到该等级），
     * 注册表查不到时回落到物品 NBT 里的 {@code Config} 快照。
     */
    @Nullable
    public static AreaAoeConfig configOf(ExcavationSkillContext context,
                                         ISkillInstance<ExcavationSkillContext> instance) {
        AreaAoeConfig fromTable = CoeSkillSupport.configForLevel(context.tool(), instance, AreaAoeConfig.class);
        if (fromTable != null) {
            return fromTable;
        }
        CompoundTag nbt = instance.data();
        if (nbt == null || !nbt.contains("Config")) {
            return null;
        }
        // 兜底：物品 NBT 上的配置快照（旧的"物品自带配置"路径）
        AreaAoeConfig snapshot = new AreaAoeConfig(0, BlockTags.MINEABLE_WITH_PICKAXE, 1, 1, 1);
        snapshot.loadFromNbt(nbt);
        return snapshot;
    }

    /** 射线拾取（与旧 {@code AoeExcavationSkill} 的 {@code player.pick(20.0D, 0.0F, false)} 一致）。 */
    @Nullable
    private static BlockHitResult pick(Player player) {
        HitResult hit = player.pick(PICK_DISTANCE, 0.0F, false);
        return hit instanceof BlockHitResult blockHit ? blockHit : null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillStrategy<?, ?>> createKey() {
        return (ResourceKey<SkillStrategy<?, ?>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.STRATEGY, ID);
    }
}
