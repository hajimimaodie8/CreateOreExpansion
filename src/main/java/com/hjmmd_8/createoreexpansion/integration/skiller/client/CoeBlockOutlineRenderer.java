package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.client.AllRenderTypes;
import com.hjmmd_8.createoreexpansion.client.tool.OutlineRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.leaf.skiller.foundation.renderer.StrategyRenderer;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.StrategySkill;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 挖掘类技能的方块描边预览（新内核版）。
 *
 * <p>把旧 {@code client/tool/SkillsStrategyRenderer} + {@code BlockToolOutlineRenderer} 的职责
 * 拆成新内核要求的形状：{@link #getContext} 负责"看哪里、会挖哪些、什么颜色"，
 * {@link #render} 只负责画线。画线本体直接复用现成的
 * {@link OutlineRenderer#renderOutline}（它只吃 {@code Set<BlockPos>} + {@code VertexConsumer}，
 * 与新旧框架无关，是这次唯一不用改的资产）。</p>
 *
 * <h2>与旧实现的两处必须知道的差异</h2>
 * <ul>
 *   <li><b>缓冲类型</b>：旧实现收的是 Create 的 {@code SuperRenderTypeBuffer} 并自己
 *       {@code buffer.draw(type)}；新接口给的是原版 {@link MultiBufferSource}，
 *       <b>不要调 draw</b>（由框架统一冲刷），只需按 RenderType 取 VertexConsumer。</li>
 *   <li><b>上色时机</b>：旧实现由调度器读 NBT 颜色再传进渲染器；新实现里上下文是自己造的，
 *       所以颜色在 {@link #getContext} 里读 {@code OutlineColor} 子标签，默认白色 +
 *       {@link SkillRendererConfig#ALPHA}（与旧 {@code defaultConfig} 兜底一致）。</li>
 * </ul>
 *
 * <h2>物理结构（Sable / 航空学 sub-level）</h2>
 * <p>结构上的方块被搬到了虚拟子世界（plot，pose 本地系），主世界那些坐标上什么都没有。
 * 因此准星命中结构时整条链路都换到<b>结构局部空间</b>：</p>
 * <ol>
 *   <li>{@link #getContext}：命中点经桥接 {@code query} → 局部中心（{@code toLocal}）→
 *       策略在局部空间算集合（方块状态经 {@code context.blockState(...)} 读结构子世界）；</li>
 *   <li>{@link #render}：{@code localToWorld} 拼出"列 = 三个基向量、平移 = 局部原点世界位置"
 *       的仿射矩阵，在<b>相机位移之后</b> {@code mulPose}，然后仍按局部坐标照旧画两层
 *       （合并必须发生在局部空间：先转世界坐标再合并，旋转后的 AABB 不再轴对齐、框会碎）。</li>
 * </ol>
 * <p>未装 Sable（{@link SableBridges#get()} 为 null）时：不查询、不加矩阵、不走局部渲染分支，
 * 与改动前逐字一致。</p>
 *
 * @since 1.0.0
 */
public class CoeBlockOutlineRenderer implements StrategyRenderer<BlockOutlineRenderContext> {

    /** 与释放路径同一个拾取距离（{@code AoeExcavationSkill.PICK_DISTANCE}） */
    private static final double PICK_DISTANCE = 20.0D;

    /** 临时诊断节流（同一原因每 2 秒最多一条日志）；定位完预览问题后可整体删除 */
    private static final long TRACE_INTERVAL_MS = 2000L;
    private static final java.util.Map<String, Long> TRACE_LAST = new java.util.HashMap<>();

    private static void trace(String reason) {
        long now = System.currentTimeMillis();
        Long last = TRACE_LAST.get(reason);
        if (last != null && now - last < TRACE_INTERVAL_MS) {
            return;
        }
        TRACE_LAST.put(reason, now);
        com.hjmmd_8.createoreexpansion.CreateOreExpansion.LOGGER.info("[SkillerRender] {}", reason);
    }

    /** 穿透层的 alpha 系数（旧 {@code BlockToolOutlineRenderer} 的第二层） */
    private static final float TRANSPARENT_ALPHA_FACTOR = 0.3F;

    @Override
    public Optional<BlockOutlineRenderContext> getContext(Minecraft mc, ClientLevel level, Player player,
                                                          ISkillInstance<BlockOutlineRenderContext> instance) {
        if (level == null || player == null || instance == null) {
            trace("getContext: level/player/instance 为空");
            return Optional.empty();
        }
        // 不按技能键就不显示预览（旧 SkillsStrategyRenderer 的门）。
        // 新内核的 schedule() 是"把身上所有策略技能都排上"，没有这道门，缺了它预览会常亮。
        if (!AllKeys.SKILL_RELEASE.isPressed()
                && !AllKeys.SKILL_RELEASE_2.isPressed()
                && !AllKeys.SKILL_RELEASE_3.isPressed()) {
            trace("getContext: 技能键都没按（预期行为，不显示预览）");
            return Optional.empty();
        }
        // 准星拾取：旧调度器就是从玩家的 BlockHitResult 拿中心方块的
        HitResult hit = player.pick(PICK_DISTANCE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            trace("getContext: 准星没落在方块上");
            return Optional.empty();
        }
        BlockPos center;
        // 物理结构（Sable / 航空学 sub-level）：准星命中结构方块时，中心与整个"挖哪些"
        // 的计算都改到**结构局部空间**（结构方块被搬到了虚拟子世界，主世界那些坐标上什么都没有）。
        // 未装 Sable（桥接为 null）或没命中结构 → structureHit 恒为 null，走原来的世界坐标路径。
        SubLevelBridge bridge = SableBridges.get();
        SubLevelBridge.Hit structureHit = bridge == null ? null : bridge.query(level, blockHit.getLocation());
        if (structureHit != null) {
            center = resolveLocalCenter(bridge, structureHit,
                    bridge.toLocal(structureHit, blockHit.getLocation()));
            trace("getContext: 命中物理结构，中心(局部)=" + center);
        } else {
            center = blockHit.getBlockPos();
        }
        ExcavationSkillContext probe = new ExcavationSkillContext(
                level, center, player.getMainHandItem(), player, structureHit);

        if (!(instance.skill().getSkill() instanceof StrategySkill<?, ?> strategySkill)) {
            trace("getContext: 技能本体不是 StrategySkill");
            return Optional.empty();
        }
        SkillStrategy<?, ?> raw = strategySkill.strategy();
        if (raw == null) {
            trace("getContext: 技能的策略为空（STRATEGIES 里查不到 " + strategySkill.getStrategy() + "）");
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        SkillStrategy<BlockPos, ExcavationSkillContext> strategy =
                (SkillStrategy<BlockPos, ExcavationSkillContext>) raw;
        if (!strategy.canCollect(probe, castInstance(instance))) {
            trace("getContext: canCollect=false（目标方块不匹配该等级配置的可挖标签，或配置取不到）");
            return Optional.empty();
        }
        Set<BlockPos> positions = new HashSet<>();
        strategy.collect(positions, probe, castInstance(instance));
        if (positions.isEmpty()) {
            trace("getContext: 策略收集结果为空");
            return Optional.empty();
        }
        // 旧实现把中心方块也加进去（strategy.calculate 的结果之外再 add(center)）
        positions.add(center);

        CompoundTag color = instance.data() == null ? null : instance.data().getCompound("OutlineColor");
        float red = color != null && color.contains("r") ? color.getFloat("r") : 1.0F;
        float green = color != null && color.contains("g") ? color.getFloat("g") : 1.0F;
        float blue = color != null && color.contains("b") ? color.getFloat("b") : 1.0F;
        trace("getContext: 产出预览，方块数=" + positions.size()
                + "，颜色=(" + red + "," + green + "," + blue + ")");
        return Optional.of(new BlockOutlineRenderContext(
                player, positions, red, green, blue, SkillRendererConfig.ALPHA, structureHit));
    }

    /**
     * 把「世界命中点换算出的局部坐标」落成一个<b>实心</b>的结构本地 BlockPos。
     *
     * <p>为什么不能直接 {@code BlockPos.containing(local)}：准星命中点在方块<b>面</b>上
     * （世界坐标里正好落在整数边界），结构又可能带任意旋转，直接取整会取到空气邻居
     * → 策略的"中心方块可挖"判定失败、预览整个消失。所以在命中点周围 3×3×3 里
     * 找最近的非空气方块，一个都没有才退回取整结果。</p>
     */
    private static BlockPos resolveLocalCenter(SubLevelBridge bridge, SubLevelBridge.Hit hit, Vec3 localPoint) {
        BlockPos guess = BlockPos.containing(localPoint);
        if (!bridge.getBlockState(hit, guess).isAir()) {
            return guess;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(guess.offset(-1, -1, -1), guess.offset(1, 1, 1))) {
            if (bridge.getBlockState(hit, candidate).isAir()) {
                continue;
            }
            double dist = Vec3.atCenterOf(candidate).distanceToSqr(localPoint);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate.immutable();
            }
        }
        return best != null ? best : guess;
    }

    @Override
    public void render(BlockOutlineRenderContext context, ISkillInstance<BlockOutlineRenderContext> instance,
                       Minecraft mc, PoseStack poseStack, Camera camera, MultiBufferSource buffer) {
        ClientLevel level = mc.level;
        if (level == null || context == null || context.positions().isEmpty()) {
            trace("render: level/context 为空或方块集合为空");
            return;
        }
        trace("render: 画线，方块数=" + context.positions().size());
        Set<BlockPos> positions = context.positions();
        float r = context.red();
        float g = context.green();
        float b = context.blue();
        float a = context.alpha();

        SubLevelBridge bridge = SableBridges.get();
        SubLevelBridge.Hit structureHit = bridge == null ? null : context.subLevelHit();

        // **必须做相机位移**：这一步原先在旧调度器 SkillsStrategyRenderer 里
        // （pushPose → translate(-camPos.x, -camPos.y, -camPos.z) → 画 → popPose）。
        // 移植时漏掉它的症状就是把世界坐标当成相机相对坐标画 → 框被画到"大约两倍距离"
        // 的地方：又远又小、几乎看不见。
        Vec3 camPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        if (structureHit != null) {
            // 结构场景：positions 是**结构局部坐标**，先乘上"局部 → 世界"的位姿矩阵再画。
            // 位姿用结构本体的**渲染位姿**（含 partialTick 插值），结构与框才会严格对齐、
            // 并且结构移动时框跟着一起动，不会领先/落后一个 tick。
            poseStack.mulPose(localToWorld(bridge, structureHit, mc));

            // 合并/去内部边必须在**局部空间**做完（先转世界坐标再合并，旋转后的 AABB 不再轴对齐、
            // 框会碎），所以这里仍按局部坐标交给 OutlineRenderer。
            // 能直接复用它的原因：结构局部系就是 Sable 的 plot 坐标系，而
            // EmbeddedPlotLevelAccessor#getBlockState(p) 的实现就是 level.getBlockState(p + plot 中心)——
            // 即局部坐标本来就落在同一个 Level 里，它的"空气过滤"读到的正是结构方块，依旧成立。
            draw(level, positions, poseStack, buffer, r, g, b, a);

            poseStack.popPose();
            flush(buffer);
            return;
        }

        draw(level, positions, poseStack, buffer, r, g, b, a);

        poseStack.popPose();

        flush(buffer);
    }

    /** 照旧画两层：不透明层（受深度测试影响）+ 半透明穿透层（渲染类型自带无深度写入/测试）。 */
    private static void draw(ClientLevel level, Set<BlockPos> positions, PoseStack poseStack,
                             MultiBufferSource buffer, float r, float g, float b, float a) {
        VertexConsumer solid = buffer.getBuffer(RenderType.LINES);
        OutlineRenderer.renderOutline(level, positions, poseStack, solid, r, g, b, a);

        VertexConsumer transparent = buffer.getBuffer(AllRenderTypes.LINES_TRANSPARENT);
        OutlineRenderer.renderOutline(level, positions, poseStack, transparent, r, g, b,
                a * TRANSPARENT_ALPHA_FACTOR);
    }

    /**
     * 结构"局部 → 世界"的仿射矩阵（列 = 三个基向量，平移 = 局部原点的世界位置）。
     *
     * <p>位姿是仿射的：{@code W(p) = M·p + t}，因此只需问桥接三个问题——
     * 局部原点、局部 (1,0,0)/(0,1,0)/(0,0,1) 的世界位置，基向量由它们与原点之差得到
     * （等价于 {@code toWorldDir}，但保证基向量与平移出自<b>同一条位姿</b>）。</p>
     */
    private static Matrix4f localToWorld(SubLevelBridge bridge, SubLevelBridge.Hit hit, Minecraft mc) {
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 origin = bridge.toWorld(hit, Vec3.ZERO, partialTick);
        Vec3 ex = bridge.toWorld(hit, new Vec3(1.0D, 0.0D, 0.0D), partialTick).subtract(origin);
        Vec3 ey = bridge.toWorld(hit, new Vec3(0.0D, 1.0D, 0.0D), partialTick).subtract(origin);
        Vec3 ez = bridge.toWorld(hit, new Vec3(0.0D, 0.0D, 1.0D), partialTick).subtract(origin);
        // JOML 构造器是行主序（m00, m01, ... ），这里按"列 = 基向量"填写
        return new Matrix4f(
                (float) ex.x, (float) ey.x, (float) ez.x, (float) origin.x,
                (float) ex.y, (float) ey.y, (float) ez.y, (float) origin.y,
                (float) ex.z, (float) ey.z, (float) ez.z, (float) origin.z,
                0.0F, 0.0F, 0.0F, 1.0F);
    }

    /**
     * **必须自己冲刷**：新接口给的是原版 MultiBufferSource，没有人替我们 endBatch；
     * 旧实现收的是 Create 的 SuperRenderTypeBuffer 并显式 buffer.draw(type)，
     * 所以以前能显示、现在算完却什么都看不到。按 RenderType 立即结算这两批。
     */
    private static void flush(MultiBufferSource buffer) {
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(RenderType.LINES);
            bufferSource.endBatch(AllRenderTypes.LINES_TRANSPARENT);
        }
    }

    @Override
    public RenderLevelStageEvent.Stage getStage() {
        return RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
    }

    /** 渲染上下文与技能实例用的都是"挖掘上下文"族，泛型擦除后可直接转（与内核内部渲染调用同一手法）。 */
    @SuppressWarnings("unchecked")
    private static ISkillInstance<ExcavationSkillContext> castInstance(
            ISkillInstance<BlockOutlineRenderContext> instance) {
        return (ISkillInstance<ExcavationSkillContext>) (ISkillInstance<?>) instance;
    }
}
