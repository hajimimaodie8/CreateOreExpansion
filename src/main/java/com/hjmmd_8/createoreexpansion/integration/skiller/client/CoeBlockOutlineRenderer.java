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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import java.util.function.Supplier;

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
 *   <li>{@link #render}：{@code localToWorld} 拼出"列 = 三个基向量、第 4 列 = 世界平移"
 *       的仿射矩阵，在<b>相机位移之后</b> {@code mulPose}，然后仍按局部坐标照旧画两层
 *       （合并必须发生在局部空间：先转世界坐标再合并，旋转后的 AABB 不再轴对齐、框会碎）。</li>
 * </ol>
 * <p><b>超大坐标的浮点精度（2026-09-22 第二轮修复，必读）</b>：局部坐标在 {@code 2.048e7}
 * 量级，float 尾数 24 bit → 该量级整数 ulp 已经是 2，「顶点大数 + 位姿大数平移」在 float 里
 * 相加即灾难性抵消，框会被画到别处或退化成不可见（普通地面上的框正常，渲染管线本身没问题）。
 * 因此这里把链路的数量级整体压小：
 * <ul>
 *   <li>取相机在局部系的坐标 {@code camLocal}（double），整数部分 {@code shift} 当近处原点；</li>
 *   <li>顶点一律以 {@code pos − shift} 写出（传给
 *       {@link OutlineRenderer#renderOutline(Level, Set, PoseStack, VertexConsumer, float, float, float, float, BlockPos)}
 *       的 {@code lookupOffset}；<b>读方块状态仍用原坐标</b>）；</li>
 *   <li>位姿矩阵的平移列补成 {@code W(shift)}（{@code R·(p−shift) + W(shift) = W(p)}，位置不变），
 *       它与相机世界坐标同量级 → 平移列、旋转列、顶点全是小量。</li>
 * </ul>
 * 另有一处独立硬伤一并修掉：JOML 是<b>列主序</b>（平移在 {@code m30/m31/m32}，位置变换只读这三个），
 * 早先按"行主序"填 16 参构造器 → 得到转置的旋转 + 落在底行（被忽略）的平移 = 平移恒为 0，
 * 顶点直接落在 2.048e7 的纯旋转坐标上。详见 {@link #localToWorld}。</p>
 * <p><b>坐标系陷阱（必读）</b>：{@code player.pick(...)} 的底层是 Sable 覆写过的
 * {@code BlockGetter#clip}，它命中结构时返回的 {@link BlockHitResult} 里
 * <b>位置与方块坐标已经是结构局部（plot）坐标</b>（射线被逆变换进 plot 空间后才求交，
 * 结果没有投影回世界）；站在结构上的玩家，其自身位置/视线/所击面同样在局部空间。
 * 所以这里先用 {@code queryLocalBlock} 判别拾取点是否已在结构本地系，是则直接当局部坐标用；
 * 否则（玩家在世界系、看着结构）才走世界坐标的 {@code query}/{@code toLocal}。
 * 早先只走后者，局部点会被再逆变换一次 → 采样到空气 → 结构命中丢失 → 预览整个不显示。</p>
 * <p><b>兜底识别（不依赖 plot 语义）</b>：上面两条路都没认出结构时，若拾取点与玩家的距离
 * <b>超过正常拾取距离</b>（{@link #FAR_PICK_DISTANCE}；正常配 {@code player.pick(20)}，恒 ≤20），
 * 那唯一可观测的事实就是"两者不在同一坐标系"——此时直接在<b>玩家所在的结构</b>
 * （先 {@link SubLevelBridge#locateSubLevel}，再 {@link SubLevelBridge#query(Level, Vec3)} 玩家位置）上，
 * 把拾取点<b>原样</b>当结构局部坐标用。查不到结构句柄就拿不到位姿矩阵，
 * 此时宁可这一帧不画，也不能把框画到十几万格之外（那等于没画，还误导）。</p>
 * <p>未装 Sable（{@link SableBridges#get()} 为 null）时：不查询、不加矩阵、不走局部渲染分支，
 * 与改动前逐字一致（兜底同样不触发：没有桥接就没有位姿）。</p>
 *
 * @since 1.0.0
 */
public class CoeBlockOutlineRenderer implements StrategyRenderer<BlockOutlineRenderContext> {

    /** 与释放路径同一个拾取距离（{@code AoeExcavationSkill.PICK_DISTANCE}） */
    private static final double PICK_DISTANCE = 20.0D;

    /**
     * "拾取点与玩家不在同一坐标系"的距离阈值（兜底识别的唯一判据）。
     *
     * <p>正常游玩的一次拾取必然落在 {@link #PICK_DISTANCE}（20）以内，所以 64 这个值不可能
     * 被正常拾取触及；一旦超过，说明 Sable 的 {@code clip} 把命中结果留在了结构局部（plot）
     * 大数坐标系里，而玩家自身还在另一个系里。这条判据只依赖"距离异常"这一个可观测事实，
     * <b>不依赖</b> {@code plot.contains} 的坐标语义，因此能绕开至今没搞对的语义问题。</p>
     */
    private static final double FAR_PICK_DISTANCE = 64.0D;

    /** 穿透层的 alpha 系数（旧 {@code BlockToolOutlineRenderer} 的第二层） */
    private static final float TRANSPARENT_ALPHA_FACTOR = 0.3F;

    @Override
    public Optional<BlockOutlineRenderContext> getContext(Minecraft mc, ClientLevel level, Player player,
                                                          ISkillInstance<BlockOutlineRenderContext> instance) {
        if (level == null || player == null || instance == null) {
            return Optional.empty();
        }
        // TODO 定位后整体删除：聊天栏诊断收集器。
        //  显示统一放在 finally 里，是为了让**中途提前 return Optional.empty()**（例如 canCollect=false）
        //  时用户照样能看到"卡在哪一步"那一句——目的就是让用户不用翻日志、一句话复制回来即可。
        ChatDiag bar = new ChatDiag();
        try {
            return collectContext(mc, level, player, instance, bar);
        } finally {
            bar.show();
        }
    }

    /** {@link #getContext} 的实际计算体（逻辑与重构前逐字一致，只是多了一个诊断出口）。 */
    private Optional<BlockOutlineRenderContext> collectContext(Minecraft mc, ClientLevel level, Player player,
                                                              ISkillInstance<BlockOutlineRenderContext> instance,
                                                              ChatDiag bar) {
        // 不按技能键就不显示预览（旧 SkillsStrategyRenderer 的门）。
        // 新内核的 schedule() 是"把身上所有策略技能都排上"，没有这道门，缺了它预览会常亮。
        if (!AllKeys.SKILL_RELEASE.isPressed()
                && !AllKeys.SKILL_RELEASE_2.isPressed()
                && !AllKeys.SKILL_RELEASE_3.isPressed()) {
            bar.gate = false;
            // TODO 定位后整体删除：松开键时补一条收尾读数（由 getContext 的 finally 统一输出，
            //  即这次提前 return **之前**就打出来）——聊天栏不淡出，会一直留着可复制。
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
        bar.pickType = String.valueOf(hit.getType());
        Vec3 pickPoint = blockHit.getLocation();
        bar.pickPoint = pickPoint;
        bar.playerPos = player.position();
        bar.pickDistance = pickPoint.distanceTo(player.position());
        // ⚠ 坐标系：Sable 的 level.clip（player.pick 的底层）命中物理结构时，返回的
        // BlockHitResult 的位置/方块坐标是**结构局部（plot）坐标**，不是世界坐标——
        // 它内部把射线逆变换到 plot 空间后对结构方块求出命中，全程没有把结果投影回世界。
        // 站在结构上的玩家自身位置/视线同样在局部空间（Sable 的 entities_stick_sublevels）。
        // 所以先判一次"这个拾取点是不是已经在结构本地系里"，是的话直接当局部坐标用，
        // 不再走世界坐标的 query/toLocal（那会把局部点再逆变换一次 → 采样到空气 → 结构命中丢失）。
        SubLevelBridge.Hit localPick = bridge == null ? null : bridge.queryLocalBlock(level, pickPoint);
        SubLevelBridge.Hit playerSub = bridge == null ? null : bridge.locateSubLevel(level, player.position());
        // 兜底第二步要用的"玩家坐标 query"：这里一次算好，诊断与兜底共用（不重复调用桥接）。
        SubLevelBridge.Hit playerQuery = bridge == null ? null : bridge.query(level, player.position());
        bar.qLocal = localPick != null;
        bar.playerInSub = playerSub != null;
        bar.playerQueryHit = playerQuery != null;
        bar.subLevelCount = diagSubLevelCount(level);
        // TODO 定位后整体删除：把"客户端认不认得出这个点属于哪个结构"自证出来——
        //  客户端容器里有几个结构、最近结构的 plot 绝对范围/中心、点与它的距离、
        //  contains 与归属两条判据各自的结论。这一串是下次一行定性的关键。
        bar.plotProbe = bridge == null ? "" : bridge.describeLocalProbe(level, pickPoint);
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
            bar.qWorldSkipped = true;   // 原路 1 已命中 → 原路 2 没试过（读数里标"未试"，不写成"否"误导人）
            diag("local-pick", "拾取结果已在结构局部系 → center(局部)=" + BlockPos.containing(localPoint));
        } else {
            structureHit = bridge == null ? null : bridge.query(level, pickPoint);
            bar.qWorld = structureHit != null;
            localPoint = structureHit == null ? null : bridge.toLocal(structureHit, pickPoint);
            if (structureHit != null) {
                diag("world-hit", "query(世界坐标) 命中结构；toLocal=" + diagVec(localPoint));
            } else {
                boolean air = level.getBlockState(blockHit.getBlockPos()).isAir();
                diag("query-null", "bridge=" + (bridge != null) + " query(世界坐标)=null；主世界该处是空气=" + air
                        + " blockPos=" + blockHit.getBlockPos()
                        + (air ? " ← 方块很可能在结构上、但没被认出来（或结构不存在）" : ""));
                // ===================== 兜底识别（本轮新增） =====================
                // 两条路都没认出结构，但拾取点离玩家**远超正常拾取距离** → 两者不在同一坐标系，
                // 拾取点已在某个结构本地系里。这条判据不依赖 plot.contains 的坐标语义。
                if (bridge != null && bar.pickDistance > FAR_PICK_DISTANCE) {
                    bar.usedFallback = true;
                    // 取"玩家所在的结构"：先 locateSubLevel(局部语义)，再 query(世界语义)。
                    SubLevelBridge.Hit owner = playerSub != null ? playerSub : playerQuery;
                    if (owner != null) {
                        structureHit = owner;
                        // 拾取点直接用，**不再 toLocal**（它本来就是局部坐标；再逆变换一次就废了）
                        localPoint = pickPoint;
                        diag("fallback-hit", "距离异常(" + Math.round(bar.pickDistance) + ">" + (long) FAR_PICK_DISTANCE
                                + ") → 兜底命中玩家所在结构(" + (playerSub != null ? "locateSubLevel" : "query(player)")
                                + ")；localPoint=拾取点=" + diagVec(localPoint)
                                + " center(局部)=" + BlockPos.containing(localPoint));
                    } else {
                        bar.fallbackMissed = true;
                        // 拿不到结构句柄 = 拿不到位姿矩阵 → 这一帧不画（绝不用世界坐标把框画到十几万格外）
                        diag("fallback-miss", "距离异常(" + Math.round(bar.pickDistance)
                                + ") 但 locateSubLevel(player)/query(player) 都拿不到结构 → 放弃本次预览"
                                + " subLevels=" + bar.subLevelCount);
                        return Optional.empty();
                    }
                }
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
            // ================= 结构场景：先"减原点"，再乘位姿（本轮修复的核心） =================
            // 结构局部（plot）坐标在 2.048e7 量级，float 尾数只有 24 bit → 该量级整数 ulp = 2。
            // 顶点若以局部大数写进顶点缓冲，再乘"局部 → 世界"矩阵，就会在 float 里发生
            // 灾难性抵消（结果 = R·v + 位姿平移，两个 ~2e7 的大数相减），框被画到别处/退化不可见。
            //
            // 办法：取"相机在结构局部系的坐标"（double）的整数部分当近处原点 shift，
            // 顶点一律以 pos − shift 写出（只剩几十格量级）；位姿矩阵的平移列则补成
            // shift 的世界坐标 W(shift)——于是 R·(p−shift) + W(shift) = W(p)，几何位置分毫不动，
            // 而链路里每一个 float（顶点、旋转列、平移列）都是小量，精度全程充足。
            //
            // 注：小数部分 frac 不单独 translate——W(shift) 与相机世界坐标之差本来就等于 −R·frac，
            // 它已经由"translate(−camPos) + 平移列 W(shift)"这一对自然产生（都在百格量级，
            // float 相减无抵消）；再补一次 translate(−frac) 反而会让整框多偏 R·frac。
            Vec3 camLocal = bridge.toLocal(structureHit, camPos);
            BlockPos shift = BlockPos.containing(camLocal);
            Vec3 frac = camLocal.subtract(shift.getX(), shift.getY(), shift.getZ());
            poseStack.mulPose(localToWorld(bridge, structureHit, mc, shift));
            // TODO 定位后整体删除：这条日志是"修完还看不见"时的第一分诊口
            //  （区分"根本没画"和"画偏了"），1 秒最多一条。
            diagRenderSub(mc, level, bridge, structureHit, positions, camLocal, shift, frac);

            // 合并/去内部边必须在**局部空间**做完（先转世界坐标再合并，旋转后的 AABB 不再轴对齐、
            // 框会碎），所以这里仍按局部坐标交给 OutlineRenderer；只是把"写顶点"那一步整体减去
            // shift（lookupOffset）——读方块状态仍用原坐标，否则会读到空白格而把方块全过滤掉。
            draw(level, positions, poseStack, buffer, r, g, b, a, shift);

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
        draw(level, positions, poseStack, buffer, r, g, b, a, BlockPos.ZERO);
    }

    /**
     * 同上，但顶点整体平移 {@code -lookupOffset}（见
     * {@link OutlineRenderer#renderOutline(Level, Set, PoseStack, VertexConsumer, float, float, float, float, BlockPos)}）。
     * 主世界路径传 {@link BlockPos#ZERO}，与改动前逐字一致。
     */
    private static void draw(ClientLevel level, Set<BlockPos> positions, PoseStack poseStack,
                             MultiBufferSource buffer, float r, float g, float b, float a,
                             BlockPos lookupOffset) {
        VertexConsumer solid = buffer.getBuffer(RenderType.LINES);
        OutlineRenderer.renderOutline(level, positions, poseStack, solid, r, g, b, a, lookupOffset);

        VertexConsumer transparent = buffer.getBuffer(AllRenderTypes.LINES_TRANSPARENT);
        OutlineRenderer.renderOutline(level, positions, poseStack, transparent, r, g, b,
                a * TRANSPARENT_ALPHA_FACTOR, lookupOffset);
    }

    /**
     * 结构"局部 → 世界"的仿射矩阵（<b>列 = 三个基向量，第 4 列 = 平移</b>）。
     *
     * <p>位姿是仿射的：{@code W(p) = R·p + t}，因此只需问桥接几个问题——
     * 局部原点、局部 (1,0,0)/(0,1,0)/(0,0,1) 的世界位置（基向量 = 与原点之差），
     * 以及 <b>{@code shift} 的世界位置</b>（平移列）。</p>
     *
     * <h2>平移列为什么是 {@code W(shift)} 而不是 {@code W(0)}</h2>
     * <p>顶点已经整体减掉了 {@code shift}，矩阵必须把这份平移补回去，否则整框会偏 {@code shift}
     * （≈2.048e7 格 = 等于看不见）。因为 {@code R·(p − shift) + W(shift) = R·p + t = W(p)}，
     * 几何位置不变；而 {@code W(shift)} 与相机世界坐标同量级（几十~几百格），float 里没有抵消——
     * 平移列、顶点、旋转列于是全是小量，这就是本轮修复的核心。</p>
     *
     * <h2>为什么这里按"每 4 个参数是一列"填写（必读）</h2>
     * <p>JOML 的字段与 16 参构造器是<b>列主序</b>：{@code m00/m01/m02} 是第 0 <b>列</b>，
     * 平移在 {@code m30/m31/m32}（{@code Matrix4f.translation(x,y,z)} 就写这三个；
     * {@code Vector3f.mulPosition} 也只读这三个）。早先这里按"行主序"把
     * {@code (ex.x, ey.x, ez.x, origin.x)} 填进前四个参数，实际得到的是
     * <b>转置的旋转</b> + 落在底行 {@code m03/m13/m23} 的平移——而位置变换根本不读底行，
     * 于是平移恒为 0：顶点被丢到 2.048e7 的纯旋转坐标上，这才是"结构上一个框都看不到"的
     * 第一条硬原因（第二条才是上面说的浮点抵消）。现在四个参数一组、组内就是完整的一列：
     * 第 0/1/2 列 = 三个基向量，第 3 列 = 世界平移，约定写死在注释里，不再依赖记忆。</p>
     */
    private static Matrix4f localToWorld(SubLevelBridge bridge, SubLevelBridge.Hit hit, Minecraft mc,
                                         BlockPos shift) {
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 origin = bridge.toWorld(hit, Vec3.ZERO, partialTick);
        Vec3 ex = bridge.toWorld(hit, new Vec3(1.0D, 0.0D, 0.0D), partialTick).subtract(origin);
        Vec3 ey = bridge.toWorld(hit, new Vec3(0.0D, 1.0D, 0.0D), partialTick).subtract(origin);
        Vec3 ez = bridge.toWorld(hit, new Vec3(0.0D, 0.0D, 1.0D), partialTick).subtract(origin);
        // 顶点已减 shift → 平移列补 W(shift)（= 局部点 shift 的世界坐标；double 算出后转 float，
        // 结果只有几十~几百格，float 精度绰绰有余）
        Vec3 translation = bridge.toWorld(hit, Vec3.atLowerCornerOf(shift), partialTick);
        return new Matrix4f(
                // 第 0 列 = 基向量 ex（第 4 个分量恒 0：方向，不平移）
                (float) ex.x, (float) ex.y, (float) ex.z, 0.0F,
                // 第 1 列 = 基向量 ey
                (float) ey.x, (float) ey.y, (float) ey.z, 0.0F,
                // 第 2 列 = 基向量 ez
                (float) ez.x, (float) ez.y, (float) ez.z, 0.0F,
                // 第 3 列 = 平移（列主序下就是 m30/m31/m32，列向量约定下唯一被位置变换读取的三个分量）
                (float) translation.x, (float) translation.y, (float) translation.z, 1.0F);
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
    // 删除时同步清掉 collectContext 里所有 diag(...) / bar.* 调用与 Util/HashMap/Map 三个 import；
    // 并删掉下面「聊天栏读数」小节（含 getContext 的 finally、bar.* 赋值，
    // 以及 Component / MinecraftServer / ServerLevel 三个 import 与 diagSubLevelCount）。

    /** 同原因最短输出间隔：2 秒最多一条（不逐帧刷屏）。 */
    private static final long DIAG_INTERVAL_MS = 2000L;

    /** 键位门那条特别吵（拿镐站着不动也会每 2 秒来一条），单独放宽到 20 秒。 */
    private static final long GATE_LOG_MS = 20000L;

    /** {@code render-sub} 的节流间隔（用户指定：同一状态 1 秒最多一条）。 */
    private static final long RENDER_SUB_LOG_MS = 1000L;

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

    /**
     * 与 {@link #diag(String, String, long)} 同一张限流表，但<b>消息按需构建</b>：只有这一条
     * 现在真的会被输出时，才调用 {@code message.get()}。给"拼一行要顺带读十几个方块状态"的
     * 诊断用（{@link #diagRenderSub}），避免逐帧做无用的字符串拼接与 Level 查询。
     */
    private static void diagLazy(String reason, long intervalMs, Supplier<String> message) {
        long now = Util.getMillis();
        Long last = DIAG_LAST.get(reason);
        if (last != null && now - last < intervalMs) {
            return;
        }
        DIAG_LAST.put(reason, now);
        CreateOreExpansion.LOGGER.info("[SkillerRender] {} | {}", reason, message.get());
    }

    /**
     * TODO 定位后整体删除：结构场景的"这一帧到底画在哪"读数（{@code render-sub}，1 秒最多一条）。
     *
     * <p>这是"修完还看不见"时的第一分诊口，用来一眼区分三种失败：
     * <ul>
     *   <li>{@code levelAir=n/n}（或 {@code bridgeAir=n/n}，即所有方块都被判成空气）→
     *       {@link OutlineRenderer#mergeBlocks} 的过滤把集合清空了 = <b>根本没画</b>；</li>
     *   <li>{@code shift} 与 {@code camLocal} 差得离谱（例如 shift=0 而 camLocal=2.048e7）→
     *       坐标系取错 = <b>画偏了</b>；</li>
     *   <li>{@code levelAir=0/n} 且 shift/camLocal 合理 → 画是画了，问题在别处（矩阵/深度/冲刷）。</li>
     * </ul>
     * 其中 {@code shiftWorld} 就是矩阵的平移列（{@code W(shift)}）：它应当与相机世界坐标同量级
     * （差一个 frac），若它仍是 2.048e7 量级，说明"减原点"没生效。</p>
     */
    private static void diagRenderSub(Minecraft mc, ClientLevel level, SubLevelBridge bridge,
                                      SubLevelBridge.Hit hit, Set<BlockPos> positions,
                                      Vec3 camLocal, BlockPos shift, Vec3 frac) {
        diagLazy("render-sub", RENDER_SUB_LOG_MS, () -> {
            float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
            Vec3 shiftWorld = bridge.toWorld(hit, Vec3.atLowerCornerOf(shift), partialTick);
            int airAtLevel = 0;
            int airAtBridge = 0;
            for (BlockPos pos : positions) {
                if (level.getBlockState(pos).isAir()) {
                    airAtLevel++;
                }
                if (bridge.getBlockState(hit, pos).isAir()) {
                    airAtBridge++;
                }
            }
            int size = positions.size();
            return "camLocal=" + diagVec(camLocal)
                    + " shift=" + shift.getX() + "," + shift.getY() + "," + shift.getZ()
                    + " frac=" + diagVec(frac)
                    + " set=" + size
                    + " shiftWorld=" + diagVec(shiftWorld)
                    + " levelAir=" + airAtLevel + "/" + size
                    + " bridgeAir=" + airAtBridge + "/" + size;
        });
    }

    /** 坐标格式化（局部坐标是大数，取两位小数足够判读）。 */
    private static String diagVec(Vec3 v) {
        return String.format("%.2f/%.2f/%.2f", v.x, v.y, v.z);
    }

    /** 坐标取整格式化（聊天栏读数用；局部坐标是大数，取整后一眼能看出量级）。 */
    private static String diagPos(Vec3 v) {
        if (v == null) {
            return "?";
        }
        return ((long) Math.floor(v.x)) + "," + ((long) Math.floor(v.y)) + "," + ((long) Math.floor(v.z));
    }

    /**
     * 该维度能枚举到的 sub-level 数量（聊天栏读数用；拿不到就写 {@code -}）。
     *
     * <p>接口层的 {@link SubLevelBridge#subLevels(net.minecraft.server.level.ServerLevel)} 只接受
     * {@code ServerLevel}，而我们在客户端渲染线程上——单机时经整合服务器取该维度的
     * {@code ServerLevel}，专用服务器上客户端拿不到（返回 {@code -}）。</p>
     *
     * <p><b>为什么这个数很重要</b>：若它是 3 而 {@code 玩家在结构/qLocal/qWorld} 全是"否"，
     * 就说明服务端明明有结构、客户端却一条都看不见（客户端容器取不到）——那问题就不在
     * 坐标语义上，而在"客户端能力"/桥接的取容器方式上。</p>
     */
    private static String diagSubLevelCount(ClientLevel level) {
        try {
            SubLevelBridge bridge = SableBridges.get();
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (bridge == null || server == null || level == null) {
                return "-";
            }
            ServerLevel serverLevel = server.getLevel(level.dimension());
            if (serverLevel == null) {
                return "-";
            }
            return String.valueOf(bridge.subLevels(serverLevel).size());
        } catch (Throwable t) {
            return "-";
        }
    }

    // --------------------- 聊天栏读数（给用户看的，TODO 定位后整体删除） ---------------------
    // 只输出**一行**（前缀 [预览]，中文短标签），按阶段优先级取第一个不成立的阶段：
    // 没按键 → 无桥接 → 没对着方块 → 未识别结构 → 结构/不可收集 → 结构/集合0 → 结构+N格
    // （走兜底成功时状态换成"结构兜底+…"，一眼可辨是哪条路认出来的）。
    // 走到"对着方块"之后，行尾一律附上判定所需的数字，共 8 个字段：
    //   pick / 玩家 / 距 / 玩家在结构 / 玩家q / 结构数 / qLocal / qWorld
    // 之后跟一段"结构探针"（describeLocalProbe）：客户端结构数 + 最近结构的 plot 绝对范围/
    //   中心 + 点距该矩形 + contains/归属结论——用来一行定性"判据差在哪"。
    // **桥接为 null（未装 Sable 或没装上）时整份读数只有"无桥接"这一句**，绝不会再出现
    // "未识别结构"（那会误导）——为 null 时也不调用任何桥接方法。
    // 输出方式：displayClientMessage(..., false) → **聊天栏**（可慢慢读、可直接复制），不再是动作栏。
    // 频率：**只在文本变化时输出一次**（静态字段记住上一串），同一串绝不重复刷屏；
    // 另加 500ms 下限，防止"多个挖掘技能实例轮流说话"时逐帧互相刷——被下限压掉的那一串
    // **不写入 CHAT_LAST_LINE**，所以稍后它还会补上（是延后，不是丢弃）。
    // 删除时同步清掉：getContext/collectContext 里的 ChatDiag 与 bar.* 赋值、本小节全部内容，
    // 以及 Component / MinecraftServer / ServerLevel 三个 import 与 diagSubLevelCount。

    /** 聊天栏两次输出之间的最短间隔（防多实例轮流说话；不写 CHAT_LAST_LINE，故只是延后）。 */
    private static final long CHAT_MIN_GAP_MS = 500L;

    /** 上一次输出的整行（只在渲染线程访问）。 */
    private static String CHAT_LAST_LINE = "";

    /** 上一次输出的时刻（{@link System#currentTimeMillis()}）。 */
    private static long CHAT_LAST_LINE_MS;

    /**
     * 一次 getContext 采到的**阶段状态 + 现场数字**。所有 return 路径都填它，由
     * {@link CoeBlockOutlineRenderer#getContext} 的 finally 统一交给 {@link #show()} 输出，
     * 因此"提前 return（没对着方块 / 未识别结构 / canCollect=false / 空集合 / 没策略）"
     * 也不会漏掉断点。
     */
    private static final class ChatDiag {

        /** 阶段 1：技能键门（Shift/R/G 任一按下）。 */
        boolean gate;

        /** 阶段 2：桥接是否存在（{@code SableBridges.get() != null}；为 null 时只显示"无桥接"）。 */
        boolean bridgePresent;

        /** 阶段 3：准星拾取是不是方块命中。 */
        boolean pickIsBlock;

        /** 阶段 3：拾取结果类型（非方块命中时显示，例如 MISS）。 */
        String pickType = "MISS";

        /** 阶段 4：是否识别出结构（两条原路任一命中，或距离异常兜底命中）。 */
        boolean structureFound;

        /** 阶段 4：是否走了"距离异常"兜底分支（含兜底也没拿到结构句柄的那种）。 */
        boolean usedFallback;

        /** 阶段 4：兜底触发但 locateSubLevel(player)/query(player) 都拿不到结构 → 本帧不画。 */
        boolean fallbackMissed;

        /** 阶段 5：策略 canCollect。 */
        boolean canCollect;

        /** 阶段 6：策略 collect 出的方块数（不含 center）；-1 = 还没走到这一步。 */
        int collected = -1;

        // ---------- 行尾数字（只有走到"对着方块"之后才有效） ----------

        /** 准星拾取点（{@code hit.getLocation()}，坐标系待定——这正是要看的东西）。 */
        Vec3 pickPoint;

        /** 玩家位置（{@code player.position()}）。 */
        Vec3 playerPos;

        /** 拾取点与玩家的距离；> {@link #FAR_PICK_DISTANCE} 即"两者不在同一坐标系"。 */
        double pickDistance = -1.0D;

        /** {@code locateSubLevel(level, player.position()) != null}。 */
        boolean playerInSub;

        /** {@code query(level, player.position()) != null}（兜底第二步）。 */
        boolean playerQueryHit;

        /** 该维度能枚举到的 sub-level 数（拿不到写 {@code -}）。 */
        String subLevelCount = "-";

        /**
         * TODO 定位后整体删除：{@code describeLocalProbe(level, pickPoint)} 的读数——
         * 客户端结构数 + 最近结构的 plot 绝对范围/中心 + 点距该矩形 + contains/归属结论。
         */
        String plotProbe = "";

        /** {@code queryLocalBlock(level, pickPoint) != null}（原路 1）。 */
        boolean qLocal;

        /** {@code query(level, pickPoint) != null}（原路 2）。 */
        boolean qWorld;

        /** 原路 1 已命中 → 原路 2 根本没试过（读数里写"未试"，不谎报"否"）。 */
        boolean qWorldSkipped;

        /**
         * 输出：整份读数**只有一行**，按 {@link #line()} 的优先级取第一个不成立的阶段。
         *
         * <p>同一串文本绝不重复输出（这是本轮的硬要求：动作栏读不完，聊天栏也不能被刷屏）；
         * 另外 500ms 内只让最先出现的那个技能实例说话——被压掉的那一串不写
         * {@link #CHAT_LAST_LINE}，所以稍后还会补上。</p>
         */
        void show() {
            String line = line();
            if (line.equals(CHAT_LAST_LINE)) {
                return;   // 同一串：绝不重复（用户要的是"状态变化时输出一次"）
            }
            long now = System.currentTimeMillis();
            if (now - CHAT_LAST_LINE_MS < CHAT_MIN_GAP_MS) {
                return;   // 多实例轮流说话：让位（不更新 CHAT_LAST_LINE，稍后补上）
            }
            CHAT_LAST_LINE = line;
            CHAT_LAST_LINE_MS = now;
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // false = **聊天栏**（可慢慢读、可复制）；true 才是动作栏
                player.displayClientMessage(Component.literal(line), false);
            }
            // 同步留一份进日志：用户复制聊天栏那一行的同时，父会话也能在 latest.log 里核对
            CreateOreExpansion.LOGGER.info("[SkillerRender] chat-line | {}", line);
        }

        /**
         * 阶段优先级（只返回第一个不成立的阶段）；走到"对着方块"之后附上 8 个现场数字。
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
                return "[预览] 没对着方块 type=" + pickType;
            }
            return "[预览] " + status() + detail();
        }

        /** 阶段 4 之后的短状态标签（兜底命中会显式标出"结构兜底"，与两条原路区分开）。 */
        private String status() {
            if (!structureFound) {
                return "未识别结构";
            }
            if (usedFallback) {
                if (!canCollect) {
                    return "结构兜底/不可收集";
                }
                return collected <= 0 ? "结构兜底/集合0" : "结构兜底+" + collected + "格";
            }
            if (!canCollect) {
                return "结构/不可收集";
            }
            return collected <= 0 ? "结构/集合0" : "结构+" + collected + "格";
        }

        /** 行尾的 8 个现场数字（中文短标签，便于用户原样复制回来）。 */
        private String detail() {
            return " pick=" + diagPos(pickPoint)
                    + " 玩家=" + diagPos(playerPos)
                    + " 距=" + Math.round(pickDistance)
                    + " 玩家在结构=" + (playerInSub ? "是" : "否")
                    + " 玩家q=" + (playerQueryHit ? "是" : "否")
                    + " 结构数=" + subLevelCount
                    + " qLocal=" + (qLocal ? "是" : "否")
                    + " qWorld=" + (qWorld ? "是" : qWorldSkipped ? "未试" : "否")
                    + " | " + plotProbe;
        }
    }
    // ===================== 临时诊断结束（TODO 定位后删） =====================
}
