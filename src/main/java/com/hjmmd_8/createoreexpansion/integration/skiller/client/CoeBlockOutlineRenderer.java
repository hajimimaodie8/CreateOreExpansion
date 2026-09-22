package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
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
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * <p><b>坐标系陷阱（必读）</b>：{@code player.pick(...)} 的底层是 Sable 覆写过的
 * {@code BlockGetter#clip}，它命中结构时返回的 {@link BlockHitResult} 里
 * <b>位置与方块坐标已经是结构局部（plot）坐标</b>（射线被逆变换进 plot 空间后才求交，
 * 结果没有投影回世界）；站在结构上的玩家，其自身位置/视线/所击面同样在局部空间。
 * 所以这里先用 {@code queryLocalBlock} 判别拾取点是否已在结构本地系，是则直接当局部坐标用；
 * 否则（玩家在世界系、看着结构）才走世界坐标的 {@code query}/{@code toLocal}。
 * 早先只走后者，局部点会被再逆变换一次 → 采样到空气 → 结构命中丢失 → 预览整个不显示。</p>
 * <p>未装 Sable（{@link SableBridges#get()} 为 null）时：不查询、不加矩阵、不走局部渲染分支，
 * 与改动前逐字一致。</p>
 *
 * @since 1.0.0
 */
public class CoeBlockOutlineRenderer implements StrategyRenderer<BlockOutlineRenderContext> {

    /** 与释放路径同一个拾取距离（{@code AoeExcavationSkill.PICK_DISTANCE}） */
    private static final double PICK_DISTANCE = 20.0D;

    /** 穿透层的 alpha 系数（旧 {@code BlockToolOutlineRenderer} 的第二层） */
    private static final float TRANSPARENT_ALPHA_FACTOR = 0.3F;

    @Override
    public Optional<BlockOutlineRenderContext> getContext(Minecraft mc, ClientLevel level, Player player,
                                                          ISkillInstance<BlockOutlineRenderContext> instance) {
        if (level == null || player == null || instance == null) {
            return Optional.empty();
        }
        // TODO 定位后整体删除：动作栏诊断收集器。
        //  显示统一放在 finally 里，是为了让**中途提前 return Optional.empty()**（例如 canCollect=false）
        //  时用户照样能看到"卡在哪一步"那一句——目的就是让用户不用翻日志、一句话念回来即可。
        ActionBarDiag bar = new ActionBarDiag();
        try {
            return collectContext(mc, level, player, instance, bar);
        } finally {
            bar.show();
        }
    }

    /** {@link #getContext} 的实际计算体（逻辑与重构前逐字一致，只是多了一个诊断出口）。 */
    private Optional<BlockOutlineRenderContext> collectContext(Minecraft mc, ClientLevel level, Player player,
                                                              ISkillInstance<BlockOutlineRenderContext> instance,
                                                              ActionBarDiag bar) {
        // 不按技能键就不显示预览（旧 SkillsStrategyRenderer 的门）。
        // 新内核的 schedule() 是"把身上所有策略技能都排上"，没有这道门，缺了它预览会常亮。
        if (!AllKeys.SKILL_RELEASE.isPressed()
                && !AllKeys.SKILL_RELEASE_2.isPressed()
                && !AllKeys.SKILL_RELEASE_3.isPressed()) {
            bar.gate = false;
            // TODO 定位后整体删除：松开键时补一条收尾读数（由 getContext 的 finally 统一显示，
            //  即这次提前 return **之前**就打出来），随后动作栏约 3 秒自然淡出。
            //  这一支**不碰桥接**（未按键的帧零开销），显示优先级也把"没按键"排在"无桥接"前面。
            diag("gate", "技能键未按下（含 Shift/R/G 三个键位） → 预览不显示", GATE_LOG_MS);
            return Optional.empty();
        }
        bar.gate = true;
        // 桥接取在**拾取之前**：SableBridges.get() 只是读一个静态字段（不触发任何 Sable 类加载），
        // 提前取才能让"无桥接"按优先级排在"没对着方块"之前，并在为 null 时整份读数只说那一句。
        SubLevelBridge bridge = SableBridges.get();
        bar.bridgePresent = bridge != null;
        // 准星拾取：旧调度器就是从玩家的 BlockHitResult 拿中心方块的
        HitResult hit = player.pick(PICK_DISTANCE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            diag("pick-type", "pick 不是方块命中：type=" + hit.getType());
            return Optional.empty();
        }
        bar.pickIsBlock = true;
        Vec3 pickPoint = blockHit.getLocation();
        // ⚠ 坐标系：Sable 的 level.clip（player.pick 的底层）命中物理结构时，返回的
        // BlockHitResult 的位置/方块坐标是**结构局部（plot）坐标**，不是世界坐标——
        // 它内部把射线逆变换到 plot 空间后对结构方块求出命中，全程没有把结果投影回世界。
        // 站在结构上的玩家自身位置/视线同样在局部空间（Sable 的 entities_stick_sublevels）。
        // 所以先判一次"这个拾取点是不是已经在结构本地系里"，是的话直接当局部坐标用，
        // 不再走世界坐标的 query/toLocal（那会把局部点再逆变换一次 → 采样到空气 → 结构命中丢失）。
        SubLevelBridge.Hit localPick = bridge == null ? null : bridge.queryLocalBlock(level, pickPoint);
        SubLevelBridge.Hit playerSub = bridge == null ? null : bridge.locateSubLevel(level, player.position());
        // "拾取方块在主世界是空气" = 结构方块被搬走了、原地只剩空气（站在结构上/看结构方块的最强信号）
        // TODO 定位后整体删除：下面 query-null 那条日志会把主世界读到的方块原样打出来——
        //  若那里显示"空气"而准星明明对着结构上的方块，就直接证明 pick 给的是局部（plot）坐标。
        diag("pick", "type=" + hit.getType() + " loc=" + diagVec(pickPoint) + " blockPos=" + blockHit.getBlockPos()
                + " bridge=" + (bridge != null) + " playerInSubLevel=" + (playerSub != null)
                + " pickInSubLevelBlock=" + (localPick != null));
        SubLevelBridge.Hit structureHit;
        Vec3 localPoint;
        if (localPick != null) {
            structureHit = localPick;
            localPoint = pickPoint;
            diag("local-pick", "拾取结果已在结构局部系 → center(局部)=" + BlockPos.containing(localPoint));
        } else {
            structureHit = bridge == null ? null : bridge.query(level, pickPoint);
            localPoint = structureHit == null ? null : bridge.toLocal(structureHit, pickPoint);
            if (structureHit != null) {
                diag("world-hit", "query(世界坐标) 命中结构；toLocal=" + diagVec(localPoint));
            } else {
                boolean air = level.getBlockState(blockHit.getBlockPos()).isAir();
                diag("query-null", "bridge=" + (bridge != null) + " query(世界坐标)=null；主世界该处是空气=" + air
                        + " blockPos=" + blockHit.getBlockPos()
                        + (air ? " ← 方块很可能在结构上、但没被认出来（或结构不存在）" : ""));
            }
        }
        BlockPos center = structureHit != null
                ? resolveLocalCenter(bridge, structureHit, localPoint)
                : blockHit.getBlockPos();
        bar.structureFound = structureHit != null;
        if (structureHit != null) {
            diag("center", "center=" + center + " 该处方块=" + bridge.getBlockState(structureHit, center));
        }
        ExcavationSkillContext probe = new ExcavationSkillContext(
                level, center, player.getMainHandItem(), player, structureHit);

        if (!(instance.skill().getSkill() instanceof StrategySkill<?, ?> strategySkill)) {
            diag("no-strategy", "技能不是 StrategySkill → 无法取策略：" + instance.skill().getSkill());
            return Optional.empty();
        }
        SkillStrategy<?, ?> raw = strategySkill.strategy();
        if (raw == null) {
            diag("no-strategy", "策略为 null：" + strategySkill);
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        SkillStrategy<BlockPos, ExcavationSkillContext> strategy =
                (SkillStrategy<BlockPos, ExcavationSkillContext>) raw;
        boolean canCollect = strategy.canCollect(probe, castInstance(instance));
        bar.canCollect = canCollect;
        diag("can-collect", "canCollect=" + canCollect + " center=" + center
                + " centerState=" + probe.blockState(center) + " structureHit=" + (structureHit != null));
        if (!canCollect) {
            return Optional.empty();
        }
        Set<BlockPos> positions = new HashSet<>();
        strategy.collect(positions, probe, castInstance(instance));
        bar.collected = positions.size();
        diag("collect", "collect 出 " + positions.size() + " 个方块（不含 center）"
                + (positions.isEmpty() ? " → 空集合，预览不显示" : ""));
        if (positions.isEmpty()) {
            return Optional.empty();
        }
        // 旧实现把中心方块也加进去（strategy.calculate 的结果之外再 add(center)）
        positions.add(center);

        CompoundTag color = instance.data() == null ? null : instance.data().getCompound("OutlineColor");
        float red = color != null && color.contains("r") ? color.getFloat("r") : 1.0F;
        float green = color != null && color.contains("g") ? color.getFloat("g") : 1.0F;
        float blue = color != null && color.contains("b") ? color.getFloat("b") : 1.0F;
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
            return;
        }
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

    // ======================= 临时诊断（TODO 定位后删） =======================
    // 目的：定位"倾斜物理结构上拿镐按住技能键没有预览框"。整段可直接删，
    // 删除时同步清掉 collectContext 里所有 diag(...) 调用与 Util/HashMap/Map 三个 import；
    // 并删掉下面「动作栏读数」小节（含 getContext 的 finally、bar.* 赋值与 Component 一个 import）。

    /** 同原因最短输出间隔：2 秒最多一条（不逐帧刷屏）。 */
    private static final long DIAG_INTERVAL_MS = 2000L;

    /** 键位门那条特别吵（拿镐站着不动也会每 2 秒来一条），单独放宽到 20 秒。 */
    private static final long GATE_LOG_MS = 20000L;

    /** 原因 → 上次输出时刻（只在渲染线程访问，故用普通 HashMap）。 */
    private static final Map<String, Long> DIAG_LAST = new HashMap<>();

    /** 临时诊断：统一前缀 {@code [SkillerRender]}，同原因限流输出。 */
    private static void diag(String reason, String message) {
        diag(reason, message, DIAG_INTERVAL_MS);
    }

    private static void diag(String reason, String message, long intervalMs) {
        long now = Util.getMillis();
        Long last = DIAG_LAST.get(reason);
        if (last != null && now - last < intervalMs) {
            return;
        }
        DIAG_LAST.put(reason, now);
        CreateOreExpansion.LOGGER.info("[SkillerRender] {} | {}", reason, message);
    }

    /** 坐标格式化（局部坐标是大数，取两位小数足够判读）。 */
    private static String diagVec(Vec3 v) {
        return String.format("%.2f/%.2f/%.2f", v.x, v.y, v.z);
    }

    // --------------------- 动作栏读数（给用户看的，TODO 定位后整体删除） ---------------------
    // 只显示**一句**"卡在哪一步"的极短提示（前缀 [预览]，均 ≤20 字符），按阶段优先级取
    // 第一个不成立的阶段：没按键 → 无桥接 → 没对着方块 → 未识别结构 → 结构/不可收集
    // → 结构/集合0 → 结构+N格。
    // **桥接为 null（未装 Sable 或没装上）时整份读数只有"无桥接"这一句**，绝不会再出现
    // "未识别结构"（那会误导）——这是本轮最关键的一条。为 null 时也不调用任何桥接方法。
    // 显示方式/频率照旧：displayClientMessage(..., true) 打动作栏；按住期间同一句 1 秒最多一条、
    // 句变则立即刷新、250ms 反闪烁；松开只补一条收尾句，动作栏读数约 3 秒后自然淡出。
    // 删除时同步清掉：getContext/collectContext 里的 ActionBarDiag 与 bar.* 赋值、
    // 本小节全部内容，以及 Component 一个 import。

    /** 同一状态最短重复间隔：1 秒最多一条（句变时立即刷新，不受此限）。 */
    private static final long ACTION_BAR_INTERVAL_MS = 1000L;

    /**
     * 反闪烁下限：同一帧里"多个挖掘技能实例"会各调一次 getContext（内核按实例调度），
     * 若不加下限，两句读数会逐帧互相盖掉、用户根本没法念。250ms 内只让最先出现的那个说话。
     */
    private static final long ACTION_BAR_MIN_GAP_MS = 250L;

    /** 上一次显示的读数句（只在渲染线程访问）。 */
    private static String DIAG_LAST_LINE = "";

    /** 上一次显示的读数句时刻（{@link System#currentTimeMillis()}）。 */
    private static long DIAG_LAST_LINE_MS;

    /** 上一次显示的是不是"松开技能键"的收尾句 —— 是的话就不再重复（松开后约 3 秒自然淡出）。 */
    private static boolean DIAG_LAST_WAS_RELEASE;

    /**
     * 一次 getContext 采到的**阶段状态**（不再拼现场字段）。所有 return 路径都填它，由
     * {@link CoeBlockOutlineRenderer#getContext} 的 finally 统一交给 {@link #show()} 显示，
     * 因此"提前 return（没对着方块 / 未识别结构 / canCollect=false / 空集合 / 没策略）"
     * 也不会漏掉断点。
     */
    private static final class ActionBarDiag {

        /** 阶段 1：技能键门（Shift/R/G 任一按下）。 */
        boolean gate;

        /** 阶段 2：桥接是否存在（{@code SableBridges.get() != null}；为 null 时只显示"无桥接"）。 */
        boolean bridgePresent;

        /** 阶段 3：准星拾取是不是方块命中。 */
        boolean pickIsBlock;

        /** 阶段 4：是否识别出结构（queryLocalBlock 命中 或 世界坐标 query 命中）。 */
        boolean structureFound;

        /** 阶段 5：策略 canCollect。 */
        boolean canCollect;

        /** 阶段 6：策略 collect 出的方块数（不含 center）；-1 = 还没走到这一步。 */
        int collected = -1;

        /** 显示：整份读数**只有一句**，按 {@link #line()} 的优先级取第一个不成立的阶段。 */
        void show() {
            String line = line();
            long now = System.currentTimeMillis();
            if (!gate) {
                // 松开技能键：只在"按住 → 松开"这一次变化时补发一条收尾句，之后不再刷新，
                // 动作栏读数约 3 秒后自然淡出。
                if (DIAG_LAST_WAS_RELEASE) {
                    return;
                }
            } else if (line.equals(DIAG_LAST_LINE) && now - DIAG_LAST_LINE_MS < ACTION_BAR_INTERVAL_MS) {
                return;   // 同一句：每秒最多一条
            } else if (now - DIAG_LAST_LINE_MS < ACTION_BAR_MIN_GAP_MS) {
                return;   // 反闪烁：同一帧的其它技能实例让位
            }
            DIAG_LAST_LINE = line;
            DIAG_LAST_LINE_MS = now;
            DIAG_LAST_WAS_RELEASE = !gate;
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // true = 动作栏（第三人称上方的短暂提示），不刷聊天框
                player.displayClientMessage(Component.literal(line), true);
            }
        }

        /**
         * 阶段优先级（与需求表逐行对应，只返回第一个不成立的阶段）。
         *
         * <p>桥接为 null 时**必然**停在第二句上，后面的结构判定一个字都不会出现。</p>
         */
        private String line() {
            if (!gate) {
                return "[预览] 没按键";
            }
            if (!bridgePresent) {
                return "[预览] 无桥接";
            }
            if (!pickIsBlock) {
                return "[预览] 没对着方块";
            }
            if (!structureFound) {
                return "[预览] 未识别结构";
            }
            if (!canCollect) {
                return "[预览] 结构/不可收集";
            }
            if (collected <= 0) {
                return "[预览] 结构/集合0";
            }
            return "[预览] 结构+" + collected + "格";
        }
    }
    // ===================== 临时诊断结束（TODO 定位后删） =====================
}
