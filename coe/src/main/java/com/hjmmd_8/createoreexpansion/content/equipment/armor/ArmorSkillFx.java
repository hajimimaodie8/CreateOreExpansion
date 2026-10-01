package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * <b>装备技能特效</b>（用户 2026-10-01 要求）—— 服务端发粒子（原版染色粒子，零自定义粒子类型，
 * 与本仓波系统同一套路，见 {@code ChargerWaveFx}）。
 *
 * <ul>
 *   <li><b>迅捷拖尾</b>：{@link #dashTrail} —— <b>黄 → 绿渐变</b>的拖尾，只在玩家<b>水平移动</b>时发。</li>
 *   <li><b>落地特效</b>：{@link #landingImpact} —— 虚衡坠护豁免一次摔落伤害的瞬间，脚下炸一圈黄绿；
 *       <b>按住技能键落地</b>时升级为"黄绿交加"（黄绿交替 + 亮闪点缀）。</li>
 * </ul>
 *
 * <p>颜色一律用 0~1 的 RGB 三元组（{@link DustParticleOptions} 的口径），改色只改下面几个常量。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillFx {

    /** 迅捷拖尾的两端色：尾部偏黄、头部偏绿（用户："黄色到绿色渐变"）。 */
    private static final Vec3 DASH_TAIL_YELLOW = new Vec3(1.00D, 0.82D, 0.12D);
    private static final Vec3 DASH_HEAD_GREEN = new Vec3(0.34D, 0.96D, 0.26D);

    /** 落地特效的两色（按住技能键时黄绿交替 = "黄绿交加"）。 */
    private static final Vec3 LAND_YELLOW = new Vec3(1.00D, 0.85D, 0.18D);
    private static final Vec3 LAND_GREEN = new Vec3(0.38D, 0.95D, 0.30D);

    /** 拖尾每段的间距（格）：越靠后越黄。 */
    private static final double TRAIL_STEP = 0.38D;

    /** 低于这个水平速度平方视为"没在走"，不发拖尾（用户："若玩家在水平面移动"）。 */
    private static final double MOVING_EPSILON = 3.0E-3D;

    private ArmorSkillFx() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 迅捷拖尾：黄→绿渐变的几段染色粒子铺在玩家身后（+ 少量亮闪点缀）。
     *
     * <p>调用点已经做过"是否在水平移动"的判定，这里再兜一次底，避免原地站着也喷粒子。</p>
     */
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

    public static void dashTrail(ServerPlayer player) {
        trail(player, DASH_TAIL_YELLOW, DASH_HEAD_GREEN);
    }

    /**
     * <b>宝石套（蓝 → 红）的拖尾</b> —— 用户 2026-10-01 规格 §0.3：
     * "第二个技能（临域充力）的拖尾与『加移速那条』**完全一致**（用户：第二个不需要你复效）"。
     *
     * <p>因此形态逐字复用 {@link #dashTrail} 的私有实现 {@link #trail}（同样的分段数、同样的
     * 左右双列、同样的"只在水平移动时发"、同样的亮闪点缀），<b>只换两端色</b>为宝石套的
     * {@link #GEM_BLUE} → {@link #GEM_RED}（同源于 {@code ArmorEnergyColors.GEM_STOPS}）。</p>
     */
    public static void gemTrail(ServerPlayer player) {
        trail(player, GEM_BLUE, GEM_RED);
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
     * 落地特效（虚衡坠护豁免摔落伤害的瞬间）。
     *
     * @param holding 落地时是否正按住虚衡坠护技能键；为 {@code true} 时升级成"黄绿交加"版
     */
    public static void landingImpact(net.minecraft.world.entity.player.Player player, boolean holding) {
        // 只在服务端发粒子：调用点来自 LivingFallEvent（两侧都会触发），客户端侧直接忽略。
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        ServerLevel level = server.serverLevel();
        double x = server.getX();
        double y = server.getY();
        double z = server.getZ();
        int ring = holding ? 24 : 16;
        for (int i = 0; i < ring; i++) {
            double angle = Math.PI * 2.0D / ring * i;
            // 用户 2026-10-01 报"免疫摔落的粒子只有黄色，没有绿色"：
            // 现在**两种情况都黄绿交替**（被动豁免也有一半绿），按住时再叠亮闪与更密的圈。
            Vec3 color = (i % 2 == 1) ? LAND_GREEN : LAND_YELLOW;
            level.sendParticles(dust(color, 1.25F),
                x + Math.cos(angle) * 0.9D,
                y + 0.05D,
                z + Math.sin(angle) * 0.9D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // 冲击中心：黄绿各一半地向上扬（不是清一色黄）
        for (int i = 0; i < 8; i++) {
            Vec3 color = (i % 2 == 1) ? LAND_GREEN : LAND_YELLOW;
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

    // ===================== 宝石套 · 绝境守护（用户 2026-10-01，槽位 1） =====================

    /**
     * 宝石套的两个端点色：<b>蓝 → 红</b>。
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

    /** {@code java.awt.Color}（0~255）→ 粒子用的 0~1 三维色。 */
    private static Vec3 toVec(java.awt.Color color) {
        return new Vec3(color.getRed() / 255.0D, color.getGreen() / 255.0D, color.getBlue() / 255.0D);
    }

    /** 应力注入器环绕粒子：每圈几颗。 */
    private static final int RING_POINTS = 14;

    /** 应力注入器环绕粒子：圈半径（格）。 */
    private static final double RING_RADIUS = 0.80D;

    /**
     * <b>被赋能的动力源方块周围：蓝 → 红渐变的圈状环绕粒子</b>（规格 §2.1 第 3 条）。
     *
     * <p>观感 = <b>绕方块转的两圈</b>：下圈贴方块底部、上圈抬到中高，两圈相位错开 180°、
     * 半径略小 ⇒ 看着像"能量在方块周围打转"。颜色沿<b>角度</b>从宝石套蓝插值到红
     * （与能量条的左蓝右红同一对端点色），相位随时间推进 ⇒ 环在转。</p>
     *
     * @param level 服务端世界
     * @param pos   被赋能的动力源方块位置
     * @param phase 时间相位（调用方给 {@code tickCount × 0.25} 之类的连续值）
     */
    public static void fieldChargeRing(ServerLevel level, BlockPos pos, double phase) {
        if (level == null || pos == null) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        for (int i = 0; i < RING_POINTS; i++) {
            double t = (double) i / RING_POINTS;
            double angle = Math.PI * 2.0D * t + phase;
            Vec3 color = GEM_BLUE.lerp(GEM_RED, t);
            double dx = Math.cos(angle) * RING_RADIUS;
            double dz = Math.sin(angle) * RING_RADIUS;
            // 下圈（贴底、细）
            level.sendParticles(dust(color, 0.85F), x + dx, y + 0.20D, z + dz,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            // 上圈（半高、更亮、相位差 180°、半径略小）
            level.sendParticles(dust(color, 1.20F), x - dx * 0.72D, y + 0.85D, z - dz * 0.72D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
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
