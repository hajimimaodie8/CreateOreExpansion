package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.client.render.types.AllRenderTypes;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowAstralBarrageConfigs;
import com.hjmmd_8.createoreexpansion.foundation.util.SkillOutlineColors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * <b>星界弓「星元波置」的圆形技能范围预选框</b>（弓技能批 9，作者 2026-10-05）。
 *
 * <h2>作者原话（逐字，本类存在的唯一理由）</h2>
 * <blockquote>
 * 下优化：当他拉弓时，应该会看到一个圆形技能范围的预选框。<br>
 * 1. 预选框外观：边界是一圈类似于能量波的粉色线条。<br>
 * 2. 移动机制：预选框会一点一点往远离视角的方向移动，边缘最多移动至距离玩家准心 5 格的位置。<br>
 * 3. 释放机制：确定位置后松手，开始释放技能。……技能释放完之后，该预选框才会消失。
 * </blockquote>
 *
 * <h2>它照的是仓里哪一条既有栈（<b>不自创</b>）</h2>
 * <ul>
 *   <li><b>注册形状</b>：纯客户端 {@code @EventBusSubscriber(modid = CoeCore.MOD_ID,
 *       value = Dist.CLIENT)} + {@link ClientTickEvent.Post}（推进状态）+ {@link RenderLevelStageEvent}
 *       的 {@link RenderLevelStageEvent.Stage#AFTER_TRANSLUCENT_BLOCKS} 自绘 —— 与
 *       {@code content/energyfield/EnergyFieldGoggleOutlineRenderer} <b>逐条同形</b>
 *       （那条是"场区域预览框"，跟本类同一件事：把一块区域画成线框）；</li>
 *   <li><b>绘制形状</b>：两层线 —— 不透明 {@link RenderType#LINES} + 穿透
 *       {@link AllRenderTypes#LINES_TRANSPARENT}（alpha 再乘 0.3），顶点写法与
 *       {@code OutlineRenderer#renderEdge} 一字不差（{@code addVertex → setColor → setNormal}）；
 *       冲刷方式照 {@code CoeBlockOutlineRenderer#flush}（<b>必须自己 endBatch</b>：
 *       框架不替我们冲刷，漏了这一步的症状是"算完了却什么都看不到"）；
 *       相机位移也照它（{@code translate(-camPos)}，漏了框会被画到大约两倍距离外）。</li>
 * </ul>
 * <p>⚠ <b>为什么不是 Skiller 的 {@code StrategyRenderer}</b>：那条管线只调度
 * {@code ClientSkillCache} 里<b>实现 {@code StrategySkill} 的技能</b>（
 * {@code CoeBlockOutlineRenderer} / {@code CoeEntityOutlineRenderer} 就是这么被驱动的），
 * 而星界弓这条专属技能是 {@code BowExclusiveShotItemSkill implements
 * ItemSkill<BowShootSkillContext>} —— 它进不了那条桶，所以本类自己订阅游戏总线，
 * 但<b>用同一套渲染类型、同一个阶段、同一个顶点写法</b>。</p>
 *
 * <h2>★ 圆心 / 半径 / 颜色三件都不在本类算</h2>
 * <ul>
 *   <li><b>圆心</b>：{@code BowAstralBarrageConfigs#previewCenter(眼睛, 视线, 拉弓 tick 数)} ——
 *       <b>服务端发射弹幕用的是同一个方法</b>（{@code BowAstralBarrageLauncher#fire}）⇒
 *       "客户端看到的圈"与"实际落点"<b>结构上不可能不一致</b>（关卡
 *       {@code bow9-one-centre}：两个调用点都不许自己 {@code scale(} 视线、
 *       也不许提 {@code ANCHOR_FORWARD_BLOCKS} / {@code PREVIEW_MAX_FORWARD_BLOCKS} /
 *       {@code PREVIEW_FORWARD_BLOCKS_PER_TICK}）；</li>
 *   <li><b>半径</b>：{@code BowAstralBarrageConfigs#radiusFor(该弓档位起始等级)} —— 与发射处同一处
 *       真源（"半径等于技能等级加 1"）；</li>
 *   <li><b>粉色</b>：{@code BowTier#skillOutlineColor()} —— 也就是<b>星界那一档既有的描边色</b>
 *       （{@code SkillOutlineColors.STELLARSTONE_PINK}，仓库里五件星界工具用的同一色）。
 *       本类<b>不写任何 RGB / hex 字面量</b>，也不新造色值（作者："边界是一圈类似于能量波的
 *       粉色线条"——"粉色"取的就是弓自己那一色）；alpha 走既有的 {@link OutlineColors#ALPHA}
 *       （技能预览统一的 0.5）与既有的穿透层系数。</li>
 * </ul>
 *
 * <h2>★ 什么时候出现、什么时候消失（作者第 3 条）</h2>
 * <ol>
 *   <li><b>拉弓中</b>（{@code player.isUsingItem()} 且手里那把弓的档位<b>走这条技能</b>，
 *       判据就是发射处那同一个 {@code BowAstralBarrageConfigs#appliesTo(档位)}）：
 *       每 tick 用"已经拉了多少 tick"重算圆心 ⇒ 圈<b>一点一点往远离视角的方向移动</b>，
 *       到了真源的上限就不动；</li>
 *   <li><b>松手那一刻</b>：圆心<b>冻结</b>（本类不再重算，保留最后一次拉弓时的值）；
 *       服务端此刻用同一个输入算出同一个圆心 ⇒ 落点就是圈所在处；</li>
 *   <li><b>释放期间</b>：圈一直在，直到 {@code BowAstralBarrageConfigs#barrageScheduleTicks()}
 *       （= 条数 × 节拍）数完才消失 —— 作者："技能释放完之后，该预选框才会消失"。
 *       客户端没有"弹幕打完了"的包可收，所以这个时长与发射处的排程用<b>同两个常量</b>算，
 *       改条数/节拍时两边一起动。</li>
 * </ol>
 *
 * <h2>⛔ 零注册、零贴图、零模型、零语言键</h2>
 * <p>整条预选框是<b>代码画线</b>：不加物品/方块/实体/渲染器注册，不动任何
 * {@code textures/**} 或 {@code models/**}（用户资产红线），不写语言键、不跑 {@code runData}。</p>
 *
 * <h2>⚠ 只有进游戏才看得见</h2>
 * <p>按 {@code AGENTS.md} 的"客户端渲染只有进游戏才看得见"：{@code compileJava} / {@code runData}
 * 都<b>不</b>跑渲染，本类的正确性最终只能在游戏里验收（拉弓看到粉色圆圈并向外推进、
 * 远端最多 5 格、松手后箭与波同速落下、放完圈才消失）。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class BowAstralBarragePreviewRenderer {

    /** 圆环的段数（画得足够圆、顶点又少；纯表现参数，不进数值真源）。 */
    private static final int RING_SEGMENTS = 64;

    /**
     * 穿透层的 alpha 系数（= {@code CoeBlockOutlineRenderer#TRANSPARENT_ALPHA_FACTOR} 同值）：
     * 第二层用 {@link AllRenderTypes#LINES_TRANSPARENT}（无深度测试）画同样的圈，
     * 被地形挡住时也还看得见一圈淡淡的边。
     */
    private static final float TRANSPARENT_ALPHA_FACTOR = 0.3F;

    private static final double TWO_PI = 2.0D * Math.PI;

    /** 当前是否该画圈（拉弓中 = true；松手后数排程时长，数完置 false）。 */
    private static boolean active;

    /** 圆心（世界坐标，水平圆环所在的 Y 就是它）。 */
    private static Vec3 center = Vec3.ZERO;

    /** 圆环半径（格）—— 来自真源的"技能等级 + 1"。 */
    private static double radius;

    /** 松手之后还要亮多少 tick（排程时长；数到 0 才让圈消失）。 */
    private static int holdTicks;

    /** 本档的描边色（取色处唯一：{@code BowTier#skillOutlineColor()}，在拉弓那一 tick 记下来）。 */
    private static float red;
    private static float green;
    private static float blue;

    private BowAstralBarragePreviewRenderer() {
    }

    // ========== 状态推进（每 tick） ==========

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            clear();
            return;
        }

        ItemStack using = player.getUseItem();
        BowTier tier = barrageTier(using);
        if (player.isUsingItem() && tier != null) {
            // 拉弓中：圆心跟着"已经拉了多少 tick"一点点往前推。
            // "拉了多少 tick"与"圆心怎么算"两端共用真源同一条（BowAstralBarrageConfigs），
            // 所以这里与服务端发射处喂进去的是同一个数、算出的是同一个点。
            center = BowAstralBarrageConfigs.previewCenter(
                player.getEyePosition(), player.getLookAngle(),
                BowAstralBarrageConfigs.drawnTicks(using, player, player.getUseItemRemainingTicks()));
            radius = BowAstralBarrageConfigs.radiusFor(tier.baseSkillLevel());
            SkillOutlineColors.SkillColor color = tier.skillOutlineColor();
            red = color.r();
            green = color.g();
            blue = color.b();
            holdTicks = BowAstralBarrageConfigs.barrageScheduleTicks();
            active = true;
            return;
        }

        if (!active) {
            return;
        }
        // 已松手：圆心停在松手那一刻（上面最后一次算出来的值），只把排程时长数完。
        holdTicks--;
        if (holdTicks <= 0) {
            clear();
        }
    }

    /** 只在"圈该消失"时统一收尾（三处出口共用，免得留下一半个状态）。 */
    private static void clear() {
        active = false;
        holdTicks = 0;
    }

    /**
     * <b>这把弓是不是走「星元波置」的那一档</b>（唯一判据 = 发射处那同一个
     * {@code BowAstralBarrageConfigs#appliesTo(档位)}）。
     *
     * <p>刻意复用发射闸门而不是另写一份"是不是星界弓"：哪一档有这条技能<b>只有那一处</b>说了算，
     * 将来挪档位/加弓时，预选框与真发射会一起变（不会出现"画了圈但没有技能"或反之）。</p>
     */
    private static BowTier barrageTier(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof JadeTopazBowItem bow)) {
            return null;
        }
        return BowAstralBarrageConfigs.appliesTo(bow.tier()) ? bow.tier() : null;
    }

    // ========== 自绘渲染（每帧） ==========

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera()
            .getPosition();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers()
            .bufferSource();

        // **必须做相机位移**（照 CoeBlockOutlineRenderer）：这一步漏掉的症状是把世界坐标当成
        // 相机相对坐标画 → 圈被画到大约两倍距离的地方，又远又小、几乎看不见。
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer solid = buffer.getBuffer(RenderType.LINES);
        ring(pose, solid, red, green, blue, OutlineColors.ALPHA);

        VertexConsumer transparent = buffer.getBuffer(AllRenderTypes.LINES_TRANSPARENT);
        ring(pose, transparent, red, green, blue, OutlineColors.ALPHA * TRANSPARENT_ALPHA_FACTOR);

        poseStack.popPose();

        // **必须自己冲刷**：接口给的是原版 buffer source，没有人替我们 endBatch
        // （形状照 CoeBlockOutlineRenderer#flush，按 RenderType 立即结算这两批）。
        buffer.endBatch(RenderType.LINES);
        buffer.endBatch(AllRenderTypes.LINES_TRANSPARENT);
    }

    /**
     * 把预选框画成<b>水平面上的一圈线</b>（圆心 {@link #center}、半径 {@link #radius}、
     * 高度取圆心的 Y —— 与弹幕圆盘在 XZ 上同心）。
     *
     * <p>顶点写法照 {@code OutlineRenderer#renderEdge}：{@code addVertex → setColor → setNormal}
     * （法线取该段的方向），两种渲染类型的格式差异（{@code RenderType.LINES} 是
     * POSITION_COLOR_NORMAL、穿透层是 POSITION_COLOR）在这里与既有渲染器处理方式完全一致。</p>
     */
    private static void ring(PoseStack.Pose pose, VertexConsumer consumer, float r, float g, float b, float a) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double from = TWO_PI * i / RING_SEGMENTS;
            double to = TWO_PI * (i + 1) / RING_SEGMENTS;
            double x0 = center.x + Math.cos(from) * radius;
            double z0 = center.z + Math.sin(from) * radius;
            double x1 = center.x + Math.cos(to) * radius;
            double z1 = center.z + Math.sin(to) * radius;
            double dx = x1 - x0;
            double dz = z1 - z0;
            double length = Math.sqrt(dx * dx + dz * dz);
            float nx = length > 1.0E-9D ? (float) (dx / length) : 1.0F;
            float nz = length > 1.0E-9D ? (float) (dz / length) : 0.0F;
            vertex(pose, consumer, x0, z0, nx, nz, r, g, b, a);
            vertex(pose, consumer, x1, z1, nx, nz, r, g, b, a);
        }
    }

    /** 圈上的一个顶点（Y 恒 = 圆心 Y ⇒ 圆环是水平的，与弹幕落下的那块圆盘同一个平面）。 */
    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double z,
                               float nx, float nz, float r, float g, float b, float a) {
        consumer.addVertex(pose, (float) x, (float) center.y, (float) z)
            .setColor(r, g, b, a)
            .setNormal(pose, nx, 0.0F, nz);
    }
}
