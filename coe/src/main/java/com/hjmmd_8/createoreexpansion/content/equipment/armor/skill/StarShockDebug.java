package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * <b>星芒嬗震"环绕波调试"开关（临时、可整体移除）</b> —— 作者 2026-10-02 原话：
 * 「我并不能从视觉上直接判断是否有环绕波生成…现在尝试添加<b>调试行</b>，我来进行测试有没有环绕波生成。」
 *
 * <h2>为什么是"命令"而不是"新增配置项"</h2>
 * <p>作者要的是"不用切窗口就知道"，且"一条命令/一个配置项即可"。两条路都能满足，这里选了命令：</p>
 * <ul>
 *   <li><b>不动配置容器</b>：配置容器名 / 键名是 AGENTS 红线（改容器名 = 老玩家设置静默丢失）。
 *       往既有 {@code [wave]} 容器里<b>加</b>新键本身是合法的（NeoForge 会补齐缺失键，老文件不受影响），
 *       但为了一个"测试完就删"的临时提示去动玩家存档里的配置文件，收益不抵风险；</li>
 *   <li><b>作用域天然更细</b>：配置是全局的，命令可以<b>按玩家</b>开 —— 多人服上只有作者自己会收到
 *       "掷骰/命中/生成"的提示，别人不受影响；</li>
 *   <li><b>可整体移除</b>：删掉本类文件 + {@code StarShockRuntime#logSettlement} 里那一行调用即可，
 *       不留配置键、不留语言键、不留注册项（见类尾注释）。</li>
 * </ul>
 *
 * <h2>默认关闭</h2>
 * <p>{@link #DEBUGGING} 初值就是空集 ⇒ <b>没人执行过命令 = 一行提示都不会发</b>（关卡
 * {@code star-orbit-debug-default-off} 守着这一条）。打开方式只有一条命令：
 * <code>/orbitdebug on</code>（或 {@code /orbitdebug}，等价于切换）。</p>
 *
 * <h2>为什么走聊天栏（chat）而不是动作栏（action bar）</h2>
 * <p>作者此前明确讨厌"动作栏字幕盖住 tooltip"（冷却提示那次）。{@link ServerPlayer#sendSystemMessage}
 * 走的是<b>聊天栏</b>（客户端会在聊天记录里留一行），与动作栏互不干扰、也不会盖住任何 tooltip。</p>
 *
 * <h2>为什么只发给发射者本人</h2>
 * <p>提示内容是"<b>你</b>这次发射滚了几次骰"，对别人没有意义；而且波系统的日志出口是全局的
 * {@code WaveDiag}，这条提示若广播就等于把调试信息倒给全服。故一律
 * {@code ServerPlayer#sendSystemMessage}（只到该玩家的连接），不广播。</p>
 *
 * <h2>⛔ 红线</h2>
 * <p>本类<b>不新建任何日志前缀</b>：它一个字符都不写日志文件（要落盘的环绕波行统一在
 * {@code WaveDiag} 里，见 {@code AbstractChargerWaveEntity#logOrbitDiag}）。这里只做
 * <b>游戏内聊天栏提示</b>，且默认关闭。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class StarShockDebug {

    /**
     * 已打开调试提示的玩家（<b>默认空集 = 关闭</b>）。
     *
     * <p>用并发集合：命令注册在服务端主线程，发射结算也在服务端主线程，但读集合的判定
     * （{@link #enabledFor}）与登出清理可能从不同 tick 到达，用并发集合省掉一类时序争论。</p>
     */
    private static final Set<UUID> DEBUGGING = ConcurrentHashMap.newKeySet();

    private StarShockDebug() {
    }

    /** 该玩家是否打开了环绕波调试提示（默认 false = 关闭）。 */
    public static boolean enabledFor(Player player) {
        return player != null && DEBUGGING.contains(player.getUUID());
    }

    /** 翻转该玩家的调试提示；返回翻转后的状态。 */
    public static boolean toggle(Player player) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUUID();
        if (!DEBUGGING.remove(id)) {
            DEBUGGING.add(id);
            return true;
        }
        return false;
    }

    /**
     * <b>一次发射结算后的游戏内提示</b>（只在发射者打开了调试、且真的掷过环绕波骰时发）。
     *
     * <p>内容刻意与 {@code WaveDiag} 的结算行同形（掷骰 / 命中 / 生成三个数），这样"屏幕上看到的数"
     * 与"日志里那一行的数"能直接对上 —— 作者要判的就是"有没有环绕波生成"。</p>
     *
     * <p>{@code rolls <= 0} 时整段跳过：没掷过骰的收尾（例如按下即失败）不该冒出"0/0/0"的噪音。</p>
     *
     * @param player  发射者（提示只发给他本人）
     * @param rolls   本次发射掷了几次骰
     * @param hits    其中命中几次（概率通过）
     * @param spawned 真的生成了几枚环绕波
     */
    public static void reportOrbit(Player player, int rolls, int hits, int spawned) {
        if (!(player instanceof ServerPlayer serverPlayer) || !enabledFor(player) || rolls <= 0) {
            return;
        }
        serverPlayer.sendSystemMessage(Component.literal(
            "[调试] 环绕波：掷骰 " + rolls + " 次 / 命中 " + hits + " 次 / 生成 " + spawned + " 枚"
                + (spawned > 0 ? "（看主波周围：应有另一枚小波在绕圈）" : "")));
    }

    /**
     * 命令注册（走既有注册链：{@code @EventBusSubscriber(modid = CoeCore.MOD_ID)} +
     * {@code RegisterCommandsEvent}，与 {@code EnergyFieldCommandRegistration} 同一条路）。
     *
     * <p>命令名 {@code /orbitdebug}：与既有的 {@code /createoreexpansion} 根命令<b>不冲突</b>
     * （根命令只有 {@code field …} 一棵子树，见 {@code EnergyFieldDebugCommands}）。
     * 权限 {@code hasPermission(0)} = 单人/局域网作者可直接用；不需要 OP。</p>
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher()
            .register(Commands.literal("orbitdebug")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> toggle(ctx.getSource()))
                .then(Commands.literal("on")
                    .executes(ctx -> set(ctx.getSource(), true)))
                .then(Commands.literal("off")
                    .executes(ctx -> set(ctx.getSource(), false)))
                .then(Commands.literal("status")
                    .executes(ctx -> status(ctx.getSource()))));
    }

    /** 执行者登出：清掉他的调试标记（不残留到下一次登录）。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DEBUGGING.remove(event.getEntity()
            .getUUID());
    }

    private static int toggle(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("该命令需由玩家执行（开关按玩家记账）"));
            return 0;
        }
        boolean now = toggle(player);
        src.sendSuccess(() -> Component.literal(now
            ? "环绕波调试提示：已打开（本次登录有效；发射结算后聊天栏会显示 掷骰/命中/生成）"
            : "环绕波调试提示：已关闭"), false);
        return 1;
    }

    private static int set(CommandSourceStack src, boolean on) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("该命令需由玩家执行（开关按玩家记账）"));
            return 0;
        }
        if (on) {
            DEBUGGING.add(player.getUUID());
        } else {
            DEBUGGING.remove(player.getUUID());
        }
        src.sendSuccess(() -> Component.literal("环绕波调试提示：" + (on ? "已打开" : "已关闭")), false);
        return 1;
    }

    private static int status(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("该命令需由玩家执行（开关按玩家记账）"));
            return 0;
        }
        boolean on = enabledFor(player);
        src.sendSuccess(() -> Component.literal("环绕波调试提示：当前" + (on ? "已打开" : "已关闭")
            + "（/orbitdebug on 打开，/orbitdebug off 关闭）"), false);
        return 1;
    }

    // ==================================================================================
    // 怎么整体移除（作者只需"测试完就删"，所以把动作列全，只有三步；每一步都是删行，不改别的）
    //   ① 删掉本文件（StarShockDebug.java）——它的命令注册随官方 @EventBusSubscriber 自动注入，
    //      文件没了命令就没了，不需要去任何注册表里除名；
    //   ② 删掉 StarShockRuntime#logSettlement 里那一行 StarShockDebug.reportOrbit(...)；
    //   ③ 完成。日志侧（WaveDiag 的环绕波出生/心跳/消散三行）是<b>独立</b>的、不属于本开关：
    //      要一并去掉就删 AbstractChargerWaveEntity 的 logOrbitDiag 方法与三处调用
    //      （出生/心跳在 logOrbitDiag 内、消散在 remove 里）——日志走的是既有的
    //      [变体波轨迹] 通道，本来就会持续存在（默认开），不删也不产生任何新前缀。
    //   没有配置键、没有语言键、没有注册项、没有第二套日志前缀 ⇒ 删完即回到改造前。
    // ==================================================================================
}
