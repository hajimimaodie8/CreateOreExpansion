package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * <b>装备技能特效</b>（用户 2026-10-01 要求）—— 服务端发粒子（原版染色粒子，零自定义粒子类型，
 * 与本仓波系统同一套路，见 {@code ChargerWaveFx}）。
 *
 * <ul>
 *   <li><b>迅捷拖尾</b>：{@link #dashTrail} —— 两端色<b>按调用方传进来的那一套</b>取
 *       （翠玉 = 黄 → 绿，宝石 = 蓝 → 红），只在玩家<b>水平移动</b>时发。
 *       ⚠ 入口<b>必须带套参数</b>：这条拖尾在松手之后仍会持续（跟着迅捷 buff 的存续期），
 *       所以色源绝不能是"玩家此刻穿的套"（见 {@link #dashTrail}）。</li>
 *   <li><b>落地特效</b>：{@link #landingImpact} —— 摔落伤害被豁免的瞬间，脚下炸一圈；
 *       <b>按住技能键落地</b>时升级为"两色交加"（两色交替 + 亮闪点缀）。颜色按<b>调用方传进来的那一套</b>走。</li>
 * </ul>
 *
 * <h2>装备技能粒子的<b>唯一颜色源</b>（规格 §8 第 4 层，用户 2026-10-01）</h2>
 *
 * <p>任何装备技能粒子都<b>不许自己定色</b>，一律问 {@link #trailColors(ArmorSet)} /
 * {@link #impactColors(ArmorSet)}：</p>
 * <ol>
 *   <li><b>宝石套</b>（{@link ArmorSet#GEM}）⇒ <b>蓝 → 红</b>，<b>直接取</b>
 *       {@link ArmorEnergyColors#stopsOf(ArmorSet)} 的两端（与宝石套能量条同源，见下方
 *       {@code GEM_SOURCE}）——本文件<b>不抄字面量</b>；</li>
 *   <li><b>翠玉套</b>（{@link ArmorSet#JADE}）⇒ 现有黄→绿，<b>逐字保留</b>（本层观感零变化）；</li>
 *   <li><b>其它套</b>（星界 / 雷鸣，以及未知）⇒ 回落到该套
 *       {@code ArmorEnergyColors.stopsOf(set)} 的<b>首尾两色</b>，不另定色。</li>
 * </ol>
 *
 * <p>⚠ 宝石套继承了虚衡坠护的摔落豁免，落地时曾经走翠玉的黄绿常量
 * （{@code LAND_YELLOW} / {@code LAND_GREEN}）—— 那是用户看到的 bug。关卡
 * {@code tools/check-armor-sets.ps1} 第 24 节钉住了"宝石段不得再出现这两个标识符"。</p>
 *
 * <p>颜色一律用 0~1 的 RGB 三元组（{@link DustParticleOptions} 的口径）。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillFx {

    /** 迅捷拖尾的两端色：尾部偏黄、头部偏绿（用户："黄色到绿色渐变"）。 */
    private static final Vec3 DASH_TAIL_YELLOW = new Vec3(1.00D, 0.82D, 0.12D);
    private static final Vec3 DASH_HEAD_GREEN = new Vec3(0.34D, 0.96D, 0.26D);

    /** 落地特效的两色（按住技能键时两色交替 = "两色交加"）。 */
    private static final Vec3 LAND_YELLOW = new Vec3(1.00D, 0.85D, 0.18D);
    private static final Vec3 LAND_GREEN = new Vec3(0.38D, 0.95D, 0.30D);

    /** 翠玉套的两对端点色：拖尾 / 落地（数值就是上面那四个常量，本层<b>逐字没动</b>）。 */
    private static final Duo JADE_TRAIL = new Duo(DASH_TAIL_YELLOW, DASH_HEAD_GREEN);
    private static final Duo JADE_IMPACT = new Duo(LAND_YELLOW, LAND_GREEN);

    /**
     * <b>一对端点色</b>（0~1 的 RGB）：{@code from} = 尾端 / 第一色，{@code to} = 头端 / 第二色。
     *
     * <p>落地特效的用法是"两者交替"（{@code i % 2}），拖尾的用法是"沿距离插值"。</p>
     */
    public record Duo(Vec3 from, Vec3 to) {
    }

    /**
     * <b>拖尾的端点色</b>（唯一颜色源，按套装解析）—— 翠玉黄→绿逐字保留，宝石蓝红取自能量条，
     * 其它套回落该套能量条色标的首尾两色。
     *
     * @param set 当前生效的套；{@code null} 视为"没有套"⇒ 回落默认色
     */
    public static Duo trailColors(ArmorSet set) {
        return set == ArmorSet.JADE ? JADE_TRAIL : energyDuo(set);
    }

    /**
     * <b>落地特效的两色</b>（唯一颜色源，按套装解析）—— 口径同 {@link #trailColors(ArmorSet)}，
     * 只有"翠玉套落地那对黄绿"与拖尾那对略有差别（今天的观感就是这么定的，本层不改）。
     *
     * @param set 当前生效的套；{@code null} 视为"没有套"⇒ 回落默认色
     */
    public static Duo impactColors(ArmorSet set) {
        return set == ArmorSet.JADE ? JADE_IMPACT : energyDuo(set);
    }

    /** 拖尾每段的间距（格）：越靠后越黄。 */
    private static final double TRAIL_STEP = 0.38D;

    /** 低于这个水平速度平方视为"没在走"，不发拖尾（用户："若玩家在水平面移动"）。 */
    private static final double MOVING_EPSILON = 3.0E-3D;

    private ArmorSkillFx() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 上一 tick 的脚底水平坐标（玩家 UUID → x/z）：用来判定"到底有没有在水平走动"。
     *
     * <p>2026-10-01 用户实测"有 buff 却没有拖尾"：原先读 {@code getDeltaMovement()} 判定移动，
     * 而服务端玩家实体的 {@code deltaMovement} 并不总是反映当帧真实位移（客户端权威位置、
     * 同步时机不一）⇒ 判定偶尔恒为"没动"。改成**自己记上一 tick 的位置**，服务端算差值，
     * 这条判定就与同步时机无关了。</p>
     */
    private static final java.util.Map<java.util.UUID, double[]> LAST_POS = new java.util.HashMap<>();

    /** 水平位移超过这个距离（格/tick）才算"在走"（约等于 1/3 的潜行速度）。 */
    private static final double MIN_STEP = 0.035D;

    /**
     * 迅捷拖尾：按 <b>{@code set}</b> 取色的几段染色粒子铺在玩家身后（+ 少量亮闪点缀）。
     *
     * <p>形态（分段数、左右双列、"只在水平移动时发"、亮闪点缀）<b>不随套变化</b>，
     * 只有两端色按套走 {@link #trailColors(ArmorSet)}：翠玉 = 黄 → 绿（数值就是上面那两个常量，
     * 逐字未动），宝石 = 蓝 → 红。</p>
     *
     * <p><b>为什么套由调用方传进来</b>：这条拖尾跟着<b>迅捷 buff 的存续期</b>走 ——
     * 松手之后 buff 还在（最多 120 秒）的那段时间照样要发，而颜色必须<b>还是长按那一刻那一套的</b>。
     * 调用点 {@code ArmorSkillRuntime#tick} 因此传的是<b>登记拖尾时记住的那一套</b>，
     * 而不是"玩家此刻穿的套"；本方法也<b>不</b>自己去问 {@code ArmorSet.effectiveSet}，
     * 否则 buff 期间换甲会让拖尾颜色当场跳变。</p>
     *
     * <p>调用点已经做过"是否在水平移动"的判定，这里再兜一次底，避免原地站着也喷粒子。</p>
     *
     * @param player 服务端玩家
     * @param set    这次拖尾属于哪一套（{@code null} 视为"没有套"⇒ 回落到该套默认取色）
     */
    public static void dashTrail(ServerPlayer player, ArmorSet set) {
        // 按套取色（唯一颜色源，见 trailColors）：翠玉 = 黄→绿（数值逐字未动）、宝石 = 蓝→红
        Duo colors = trailColors(set);
        trail(player, colors.from(), colors.to());
    }

    /**
     * <b>宝石套（蓝 → 红）的拖尾</b> —— 用户 2026-10-01 规格 §0.3：
     * "第三个技能（临域充力）的拖尾同样用**同一形态的实现**、只换颜色"。
     *
     * <p>形态逐字复用 {@link #trail}（同样的分段数、同样的左右双列、同样的"只在水平移动时发"、
     * 同样的亮闪点缀），两端色同样走唯一颜色源 {@link #trailColors(ArmorSet)} ——
     * 宝石套解析出来就是 {@code ArmorEnergyColors.stopsOf(ArmorSet.GEM)} 的蓝 → 红
     * （见 {@code GEM_SOURCE}），本方法<b>不写死任何套</b>。</p>
     *
     * @param player 服务端玩家
     * @param set    这次拖尾属于哪一套（临域充力只存在于宝石套；由调用方传入本套）
     */
    public static void gemTrail(ServerPlayer player, ArmorSet set) {
        // 同样走唯一颜色源（与 dashTrail 同一条实现，只有颜色不同）
        Duo colors = trailColors(set);
        trail(player, colors.from(), colors.to());
    }

    /**
     * 拖尾的<b>唯一实现</b>（形态由 {@link #dashTrail} / {@link #gemTrail} 共用，只有颜色不同）。
     *
     * @param tailColor 尾端色（越靠后越接近它）
     * @param headColor 头端色（越靠玩家越接近它）
     */
    private static void trail(ServerPlayer player, Vec3 tailColor, Vec3 headColor) {
        if (player == null) {
            return;
        }
        double[] last = LAST_POS.get(player.getUUID());
        double x = player.getX();
        double z = player.getZ();
        LAST_POS.put(player.getUUID(), new double[] { x, z });
        if (last != null) {
            double dx = x - last[0];
            double dz = z - last[1];
            if (dx * dx + dz * dz < MIN_STEP * MIN_STEP) {
                return; // 没在水平移动：不发拖尾（用户要求"水平面移动时才有"）
            }
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        // 侧向单位向量：第一人称下"正后方"看不见，所以拖尾**同时向两侧铺**
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        int segments = 5;
        for (int i = 0; i < segments; i++) {
            double t = (double) i / (segments - 1);
            Vec3 color = tailColor.lerp(headColor, t);
            double back = TRAIL_STEP * (i + 1);
            // 每个断面：左边一颗、右边一颗（越靠后越接近尾端色、越靠前越接近头端色）
            for (int s = -1; s <= 1; s += 2) {
                level.sendParticles(dust(color, 1.15F),
                    player.getX() - look.x * back + side.x * 0.42D * s,
                    player.getY() + 0.25D + 0.06D * i,
                    player.getZ() - look.z * back + side.z * 0.42D * s,
                    2, 0.06D, 0.05D, 0.06D, 0.0D);
            }
        }
        // 点缀：偶尔一颗亮闪，让拖尾"活"一点
        if (player.getRandom().nextFloat() < 0.45F) {
            level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 0.20D, player.getZ(),
                2, 0.22D, 0.08D, 0.22D, 0.01D);
        }
    }

    /**
     * 落地特效（摔落伤害被豁免的瞬间：翠玉套的虚衡坠护 / 宝石套继承来的绝境守护那条豁免）。
     *
     * <p><b>颜色按 {@code set} 走唯一颜色源</b>（规格 §8 第 4 层，用户 2026-10-01）：翠玉套 = 黄绿
     * （与第 3 层之前逐字相同）、宝石套 = 蓝红。形态（圈半径、颗数、上扬、亮闪）<b>不随套变化</b>。</p>
     *
     * @param player  落地的玩家
     * @param holding 落地时是否正按住该套的摔落豁免技能键；为 {@code true} 时升级成"两色交加"版
     * @param set     这次豁免由哪一套提供（{@link ArmorSet#JADE} / {@link ArmorSet#GEM}）——
     *                调用点各传自己的套，别再让宝石套借用翠玉的黄绿
     */
    public static void landingImpact(Player player, boolean holding, ArmorSet set) {
        // 只在服务端发粒子：调用点来自 LivingFallEvent（两侧都会触发），客户端侧直接忽略。
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        // 唯一颜色源：翠玉拿回 LAND_YELLOW/LAND_GREEN（逐字不变），宝石拿回蓝红
        Duo colors = impactColors(set);
        ServerLevel level = server.serverLevel();
        double x = server.getX();
        double y = server.getY();
        double z = server.getZ();
        int ring = holding ? 24 : 16;
        for (int i = 0; i < ring; i++) {
            double angle = Math.PI * 2.0D / ring * i;
            // 用户 2026-10-01 报"免疫摔落的粒子只有黄色，没有绿色"：
            // 现在**两种情况都两色交替**（被动豁免也有一半第二色），按住时再叠亮闪与更密的圈。
            Vec3 color = (i % 2 == 1) ? colors.to() : colors.from();
            level.sendParticles(dust(color, 1.25F),
                x + Math.cos(angle) * 0.9D,
                y + 0.05D,
                z + Math.sin(angle) * 0.9D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // 冲击中心：两色各一半地向上扬（不是清一色第一色）
        for (int i = 0; i < 8; i++) {
            Vec3 color = (i % 2 == 1) ? colors.to() : colors.from();
            level.sendParticles(dust(color, 1.45F), x, y + 0.10D, z, 1, 0.28D, 0.06D, 0.28D, 0.02D);
        }
        if (holding) {
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.20D, z, 12, 0.45D, 0.12D, 0.45D, 0.03D);
        }
        level.sendParticles(ParticleTypes.CRIT, x, y + 0.15D, z, holding ? 12 : 6, 0.35D, 0.05D, 0.35D, 0.08D);
    }

    /** 染色粒子（0~1 RGB）。 */
    private static DustParticleOptions dust(Vec3 color, float scale) {
        return new DustParticleOptions(
            new Vector3f((float) color.x, (float) color.y, (float) color.z), scale);
    }

    // ===== GEM SET COLOURS (blue -> red, straight from ArmorEnergyColors) =====
    // 上面那段是"唯一颜色源"的分发（按套解析）；这一段是宝石侧的取色实现。
    // 关卡 check-armor-sets.ps1 第 24 节按**方法体**判定（注释先被剥掉，提到常量不算数）：
    // 这些取色/发色路径都不得出现翠玉的 LAND_YELLOW / LAND_GREEN，且取色只走
    // ArmorEnergyColors.stopsOf —— 宝石套落地必须是蓝红。

    // ===================== 宝石套 · 绝境守护（用户 2026-10-01，槽位 1） =====================

    /**
     * 宝石套的两个端点色：<b>蓝 → 红</b>（也是"唯一颜色源"里宝石那一支的真源）。
     *
     * <p><b>直接从色源取</b>（用户 2026-10-01 第二次补充 + 规格 §8 第 4 层）：宝石套的蓝红
     * 只有一处定义 —— {@code ArmorEnergyColors.GEM_STOPS} 的 {@code 0x55AAFF} / {@code 0xFF4A4A}
     * （与宝石套能量条同源）。这里用公开查询 {@link ArmorEnergyColors#stopsOf(ArmorSet)}
     * 取回来再转成 0~1 三维色，<b>不再抄一份字面量</b>：以后谁改了宝石套的配色，
     * 技能粒子跟着变（把关卡 {@code check-armor-sets.ps1} 第 14 节也钉住了那对数值）。</p>
     */
    private static final java.util.List<java.awt.Color> GEM_SOURCE =
        ArmorEnergyColors.stopsOf(ArmorSet.GEM);

    private static final Vec3 GEM_BLUE = toVec(GEM_SOURCE.get(0));

    private static final Vec3 GEM_RED = toVec(GEM_SOURCE.get(GEM_SOURCE.size() - 1));

    /** 宝石套的端点色对（拖尾与落地<b>共用同一对</b>：蓝 → 红）。 */
    private static final Duo GEM_DUO = new Duo(GEM_BLUE, GEM_RED);

    /**
     * 非翠玉套的取色：<b>一律取该套能量条色标的首尾两色</b>（宝石 / 星界 / 雷鸣 / 未知），
     * 本文件不另定任何颜色（规格 §8 第 4 层第 3 条）。
     */
    private static Duo energyDuo(ArmorSet set) {
        if (set == ArmorSet.GEM) {
            // 宝石套用已经缓存好的那一对（与能量条同源，见 GEM_SOURCE）
            return GEM_DUO;
        }
        java.util.List<java.awt.Color> stops = ArmorEnergyColors.stopsOf(set);
        return new Duo(toVec(stops.get(0)), toVec(stops.get(stops.size() - 1)));
    }

    /** {@code java.awt.Color}（0~255）→ 粒子用的 0~1 三维色。 */
    private static Vec3 toVec(java.awt.Color color) {
        return new Vec3(color.getRed() / 255.0D, color.getGreen() / 255.0D, color.getBlue() / 255.0D);
    }

    /** 应力注入器环绕粒子：每圈几颗。 */
    private static final int RING_POINTS = 14;

    /** 应力注入器环绕粒子：圈半径（格）。 */
    private static final double RING_RADIUS = 0.80D;

    /**
     * 下圈沿<b>旋转轴</b>的偏移（格，相对方块中心）：轴朝上时 = 方块底面 + 0.20
     * （= 改前那行 {@code y + 0.20D} 的逐值等价形态）。
     */
    private static final double RING_LOW_OFFSET = -0.30D;

    /** 上圈沿<b>旋转轴</b>的偏移（格，相对方块中心）：轴朝上时 = 方块底面 + 0.85。 */
    private static final double RING_HIGH_OFFSET = 0.35D;

    /**
     * <b>被赋能的动力源方块周围：蓝 → 红渐变的圈状环绕粒子</b>（规格 §2.1 第 3 条）。
     *
     * <p>观感 = <b>绕方块转的两圈</b>（层数/颗数/半径/相位差/颜色与用户 2026-10-02 之前<b>逐值相同</b>）：
     * 下圈贴底、细，上圈沿轴抬 0.65 格、更亮、相位错开 180°、半径 ×0.72 ⇒ 看着像"能量在方块周围
     * 打转"。颜色沿<b>角度</b>从宝石套蓝插值到红（与能量条同一对端点色），相位随时间推进 ⇒ 环在转。</p>
     *
     * <h2>★ 2026-10-02：两环落在<b>垂直于角速度矢量</b>的平面里，且旋向与角速度一致</h2>
     * <p>用户原话："控制手摇曲柄时，粒子的效果应该沿着手摇曲柄的转动方向，也就是位于手摇曲柄的
     * 转动角速度矢量垂直的平面，旋转方向与角速度的方向一致。当然还是有两层粒子环，这个效果不变。"</p>
     * <p>所以这里把"世界水平面 + x/z 直算 + 写死 y 高度"换成<b>正交基参数化</b>：</p>
     * <pre>
     * p(θ) = center + cos(θ) · r · u + sin(θ) · r · v ,  θ = 2πt + sign · phase
     * </pre>
     * <p>其中 {@code axis} = 动力源角速度方向的单位向量，{@code u} = {@link #perpendicularUnit}
     * （垂直于 axis 的单位向量），{@code v = axis × u}。<b>定向是这样保证的</b>：
     * {@code (u, v, axis)} 是右手系（{@code u × v = axis}），于是
     * {@code dp/dθ = axis × (p - center)} —— "θ 顺着 +axis 推进"恰好就是"绕 axis 的右手旋转"。
     * 因此把 {@code sign} 取成动力源生成转速的符号，环的旋向就与角速度矢量同向
     * （角速度反向 ⇒ {@code sign} 变号 ⇒ 环反向；符号来源见 {@code HandCrankStressSource#rotationAxis}）。</p>
     * <p>两环的<b>层间距也沿 axis</b>（而不是沿世界 y）：轴朝上时 {@link #RING_LOW_OFFSET} /
     * {@link #RING_HIGH_OFFSET} 正好还原改前的 {@code y + 0.20D} / {@code y + 0.85D}
     * （方块中心的 −0.30 / +0.35）；曲柄躺倒或朝下时，两环就跟着它真实的转轴走。</p>
     *
     * @param level 服务端世界
     * @param pos   被赋能的动力源方块位置
     * @param spin  该动力源此刻的旋转轴（单位向量）+ 转向符号（{@code ±1}，见 {@link RotationAxis}）
     * @param phase 时间相位（调用方给 {@code tickCount × 0.25} 之类的连续值）。
     *              本方法<b>不</b>解释它的量纲，只用 {@code sign} 决定推进方向 ⇒
     *              "旋向跟着曲柄、快慢与改前一样"（改前是每 25 tick 一圈）
     */
    public static void fieldChargeRing(ServerLevel level, BlockPos pos, RotationAxis spin, double phase) {
        if (level == null || pos == null || spin == null) {
            return;
        }
        // 轴 = 角速度方向的单位向量；符号 = 转向（+1 = 绕该轴的右手方向）
        Vec3 axis = spin.axis();
        double sign = spin.sign();
        // 正交基：u ⊥ axis，v = axis × u ⇒ (u, v, axis) 右手系（定向见方法注释）
        Vec3 u = perpendicularUnit(axis);
        Vec3 v = axis.cross(u).normalize();
        // 方块中心；两圈沿 axis 分开（轴朝上时 = 原来的 y + 0.20 / y + 0.85）
        Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        Vec3 low = center.add(axis.scale(RING_LOW_OFFSET));
        Vec3 high = center.add(axis.scale(RING_HIGH_OFFSET));
        for (int i = 0; i < RING_POINTS; i++) {
            double t = (double) i / RING_POINTS;
            // θ 顺着 sign 推进：角速度反向 ⇒ 环反向（快慢仍是调用方给的 phase，观感不变）
            double angle = Math.PI * 2.0D * t + sign * phase;
            Vec3 color = GEM_BLUE.lerp(GEM_RED, t);
            // 平面内的两个垂直分量（"落在垂直于角速度矢量的平面里"的全部含义）
            double cu = Math.cos(angle) * RING_RADIUS;
            double cv = Math.sin(angle) * RING_RADIUS;
            // 下圈（贴底、细）
            Vec3 p0 = low.add(u.scale(cu)).add(v.scale(cv));
            level.sendParticles(dust(color, 0.85F), p0.x, p0.y, p0.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            // 上圈（沿轴抬 0.65 格、更亮、相位差 180°、半径略小 —— 与改前逐值相同）
            Vec3 p1 = high.subtract(u.scale(cu * 0.72D)).subtract(v.scale(cv * 0.72D));
            level.sendParticles(dust(color, 1.20F), p1.x, p1.y, p1.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * <b>垂直于 {@code axis} 的单位向量</b>（两环正交基的第一个分量），退化安全。
     *
     * <p>做法：先取一个与轴<b>最不平行</b>的世界基向量当参考（三个分量里绝对值最小的那个），
     * 再 {@code u = axis × reference} 并归一化。因为 {@code |axis·ref| ≤ 1/√3}，所以
     * {@code |axis × ref| = √(1 - (axis·ref)²) ≥ √(2/3) ≈ 0.816} ——
     * 无论轴指向哪里（正上/正下/东西南北，还是将来别的动力源的任意方向），叉乘都不会退化成零向量，
     * {@code normalize()} 也就不会吐出 NaN。</p>
     */
    private static Vec3 perpendicularUnit(Vec3 axis) {
        double ax = Math.abs(axis.x);
        double ay = Math.abs(axis.y);
        double az = Math.abs(axis.z);
        Vec3 reference;
        if (ax <= ay && ax <= az) {
            reference = new Vec3(1.0D, 0.0D, 0.0D);
        } else if (ay <= az) {
            reference = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            reference = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return axis.cross(reference).normalize();
    }

    /** 长按光环每圈几颗（越大越"流动"，太小会看着像静止的点）。 */
    private static final int AURA_POINTS = 14;

    /** 长按光环的基准半径（格）。 */
    private static final double AURA_RADIUS = 0.78D;

    /**
     * <b>绝境守护长按期间的光环</b>（用户 2026-10-01：这个技能要"带动感"的粒子）。
     *
     * <p>观感 = <b>绕玩家向上流动</b>：两圈错开相位的染色粒子，一圈贴地（y+0.05）、
     * 一圈抬到腰高（y+0.95），相位随时间推进 ⇒ 看着像螺旋往上走；颜色按<b>段位</b>从宝石套蓝
     * 推向红（段位越高越红，与"按得越久越接近满级 buff"一致）。每 2 tick 调一次即可
     * （调用点在 {@code ArmorSkillRuntime#applyLastStand}）。</p>
     *
     * @param player  服务端玩家
     * @param segment 当前段位（1 起）；决定颜色偏向红端多少
     */
    public static void lastStandAura(ServerPlayer player, int segment) {
        if (player == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        // 段位归一：1 段 = 纯蓝，最高段 = 纯红（段数未知时按 3 段兜底，不越界）
        double ratio = Math.max(0.0D, Math.min(1.0D, (segment - 1) / 2.0D));
        Vec3 color = GEM_BLUE.lerp(GEM_RED, ratio);
        double phase = player.tickCount * 0.35D;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int i = 0; i < AURA_POINTS; i++) {
            double angle = Math.PI * 2.0D / AURA_POINTS * i + phase;
            double dx = Math.cos(angle) * AURA_RADIUS;
            double dz = Math.sin(angle) * AURA_RADIUS;
            // 贴地一圈（细）：让"脚边有能量在转"
            level.sendParticles(dust(color, 0.80F), x + dx, y + 0.05D, z + dz,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            // 半高一圈（错开 180°，半径略小、更亮）：与上圈一起看出"向上流动"
            level.sendParticles(dust(color, 1.15F), x - dx * 0.75D, y + 0.95D, z - dz * 0.75D,
                1, 0.0D, 0.02D, 0.0D, 0.01D);
        }
        // 点缀：偶尔一颗亮闪从脚下升起，强化"脉冲"观感
        if (player.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.10D, z,
                2, 0.28D, 0.05D, 0.28D, 0.03D);
        }
    }

    /**
     * <b>绝境守护触发瞬间的爆发</b>（被动高伤豁免 / 主动到段都走它）—— 血色向外扩散 + 图腾闪光。
     *
     * <p>颜色取宝石套红端（与 {@link #lastStandAura} 同源常量）。用原版
     * {@code TOTEM_OF_UNDYING} 粒子类型（服务端发原版粒子，不新增自定义类型）。</p>
     */
    public static void totemBurst(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        // 一圈血色：贴地向外扩散
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2.0D / 20.0D * i;
            level.sendParticles(dust(GEM_RED, 1.35F),
                x + Math.cos(angle) * 1.1D, y + 0.10D, z + Math.sin(angle) * 1.1D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // 原版不死图腾粒子（与原版图腾触发时的观感一致）
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0D, z, 40, 0.4D, 0.6D, 0.4D, 0.35D);
    }
}
