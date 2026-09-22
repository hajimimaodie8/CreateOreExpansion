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
        return collectContext(level, player, instance);
    }

    /** {@link #getContext} 的实际计算体。 */
    private Optional<BlockOutlineRenderContext> collectContext(ClientLevel level, Player player,
                                                              ISkillInstance<BlockOutlineRenderContext> instance) {
        // 不按技能键就不显示预览（旧 SkillsStrategyRenderer 的门）。
        // 新内核的 schedule() 是"把身上所有策略技能都排上"，没有这道门，缺了它预览会常亮。
        if (!AllKeys.SKILL_RELEASE.isPressed()
                && !AllKeys.SKILL_RELEASE_2.isPressed()
                && !AllKeys.SKILL_RELEASE_3.isPressed()) {
            return Optional.empty();
        }
        SubLevelBridge bridge = SableBridges.get();
        // 准星拾取：旧调度器就是从玩家的 BlockHitResult 拿中心方块的
        HitResult hit = player.pick(PICK_DISTANCE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return Optional.empty();
        }
        Vec3 pickPoint = blockHit.getLocation();
        double pickDistance = pickPoint.distanceTo(player.position());
        // ⚠ 坐标系：Sable 的 level.clip（player.pick 的底层）命中物理结构时，返回的
        // BlockHitResult 的位置/方块坐标是**结构局部（plot）坐标**，不是世界坐标——
        // 它内部把射线逆变换到 plot 空间后对结构方块求出命中，全程没有把结果投影回世界。
        // 站在结构上的玩家自身位置/视线同样在局部空间（Sable 的 entities_stick_sublevels）。
        // 所以先判一次"这个拾取点是不是已经在结构本地系里"，是的话直接当局部坐标用，
        // 不再走世界坐标的 query/toLocal（那会把局部点再逆变换一次 → 采样到空气 → 结构命中丢失）。
        SubLevelBridge.Hit localPick = bridge == null ? null : bridge.queryLocalBlock(level, pickPoint);
        SubLevelBridge.Hit playerSub = bridge == null ? null : bridge.locateSubLevel(level, player.position());
        // 兜底第二步要用的"玩家坐标 query"：这里一次算好（不重复调用桥接）。
        SubLevelBridge.Hit playerQuery = bridge == null ? null : bridge.query(level, player.position());
        SubLevelBridge.Hit structureHit;
        Vec3 localPoint;
        if (localPick != null) {
            structureHit = localPick;
            localPoint = pickPoint;
        } else {
            structureHit = bridge == null ? null : bridge.query(level, pickPoint);
            localPoint = structureHit == null ? null : bridge.toLocal(structureHit, pickPoint);
            if (structureHit == null) {
                // ===================== 兜底识别 =====================
                // 两条路都没认出结构，但拾取点离玩家**远超正常拾取距离** → 两者不在同一坐标系，
                // 拾取点已在某个结构本地系里。这条判据不依赖 plot.contains 的坐标语义。
                if (bridge != null && pickDistance > FAR_PICK_DISTANCE) {
                    // 取"玩家所在的结构"：先 locateSubLevel(局部语义)，再 query(世界语义)。
                    SubLevelBridge.Hit owner = playerSub != null ? playerSub : playerQuery;
                    if (owner != null) {
                        structureHit = owner;
                        // 拾取点直接用，**不再 toLocal**（它本来就是局部坐标；再逆变换一次就废了）
                        localPoint = pickPoint;
                    } else {
                        // 拿不到结构句柄 = 拿不到位姿矩阵 → 这一帧不画（绝不用世界坐标把框画到十几万格外）
                        return Optional.empty();
                    }
                }
            }
        }
        BlockPos center = structureHit != null
                ? resolveLocalCenter(bridge, structureHit, localPoint)
                : blockHit.getBlockPos();
        ExcavationSkillContext probe = new ExcavationSkillContext(
                level, center, player.getMainHandItem(), player, structureHit);

        if (!(instance.skill().getSkill() instanceof StrategySkill<?, ?> strategySkill)) {
            return Optional.empty();
        }
        SkillStrategy<?, ?> raw = strategySkill.strategy();
        if (raw == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        SkillStrategy<BlockPos, ExcavationSkillContext> strategy =
                (SkillStrategy<BlockPos, ExcavationSkillContext>) raw;
        if (!strategy.canCollect(probe, castInstance(instance))) {
            return Optional.empty();
        }
        Set<BlockPos> positions = new HashSet<>();
        strategy.collect(positions, probe, castInstance(instance));
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
            // ================= 结构场景：先"减原点"，再乘位姿 =================
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
            poseStack.mulPose(localToWorld(bridge, structureHit, mc, shift));

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
}
