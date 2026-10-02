package com.hjmmd_8.createoreexpansion.content.equipment.armor.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillFx;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillRuntime;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.handler.LastStandHandler;
import com.hjmmd_8.createoreexpansion.content.skill.config.FallGuardConfigs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * <b>装备技能的服务端事件处理</b>（照 {@code MedallionEffectHandler} 的模式：
 * {@code @EventBusSubscriber} + 静态 {@code @SubscribeEvent}）。</p>
 *
 * <h2>它做三件事</h2>
 * <ol>
 *     <li><b>驱动长按运行时</b>：每个服务端 tick 给每个在线玩家调一次
 *         {@link ArmorSkillRuntime#tick(ServerPlayer)}（键位状态读服务端权威的
 *         {@code PlayerPressedKeys}）。</li>
 *     <li><b>虚衡坠护的被动</b>：摔落时按该等级的被动概率豁免（{@link LivingFallEvent}）。
 *         与凝能佩的"黄玉 50% 免摔落"同一挂点，两者互不冲突（谁先取消谁生效）。</li>
 *     <li><b>虚衡坠护的主动</b>：正在长按该技能期间，摔落<b>恒豁免</b>（预告："按住技能键则 100%
 *         可豁免摔落伤害"）。</li>
 * </ol>
 *
 * <p>离场/复活时清掉长按状态（{@link ArmorSkillRuntime#forget(Player)}）—— 不清会留下
 * "幽灵长按"，让玩家下次进服白挨一次扣能。</p>
 *
 * <h2>只判"生效的那一套"</h2>
 * <p>等级一律问 {@link ArmorSkillRuntime#levelOf(Player, String)}：它内部走
 * {@code ArmorSet.effectiveSet}（严格全套 <b>或</b> 散构聚能补齐）再叠加技艺提升/记忆回溯。
 * 因此"散构聚能补齐的套"也照样生效，无需在这里再写一遍判断。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class ArmorSkillHandler {

    private ArmorSkillHandler() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 长按运行时的心跳（每个服务端 tick）。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ArmorSkillRuntime.tick(player);
            // 宝石套 · 临域充力（规格 §8 第 3 层）的会话心跳兜底：长按状态是"按槽位"的，
            // 而技能 id 按**当前生效的那一套**解析 —— 玩家中途脱甲/换套时这个槽位就不再是
            // field_charge 了，那条收尾路径也就没人走。这里给每个在线玩家兜一次：
            // 会话超过 5 tick 没被续期 ⇒ 强制"移除注入器 + 曲柄静止"。
            // （用户最强调的一条：漏一处退出路径就会留下一个永远在转的曲柄。）
            com.hjmmd_8.createoreexpansion.content.equipment.armor.field.FieldChargeRuntime.watchdog(player);
        }
    }

    /**
     * 摔落：虚衡坠护的被动（按等级概率）+ 主动（长按期间 100%）。
     *
     * <p>顺序刻意"先主动后被动"：主动是确定性的 100%，先短路掉就不必掷骰子（也避免
     * 主动期间因为随机数失败而掉血这种明显不合理的表现）。</p>
     *
     * <p><b>落地粒子按"这次豁免由哪一套提供"上色</b>（规格 §8 第 4 层，用户 2026-10-01）：
     * ① 段传 {@link ArmorSet#JADE_TOPAZ}（黄绿，与本层之前逐字相同）；② 段传 {@link ArmorSet#SAPPHIRE_RUBY}
     * （蓝红）——宝石套继承虚衡坠护的摔落豁免，落地<b>不再</b>显示翠玉的黄绿。</p>
     */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // ① 翠玉套 · 虚衡坠护（**保持原样，一行判定都不动**；只有粒子多带一个"我是翠玉"的套参）
        int level = ArmorSkillRuntime.levelOf(player, ArmorSkillRuntime.FALL_GUARD);
        if (level > 0) {
            if (ArmorSkillRuntime.isHolding(player, ArmorSkillRuntime.FALL_GUARD)) {
                event.setCanceled(true);
                // 落地特效（用户 2026-10-01）：按住技能键落地 ⇒ "两色交加"加强版
                ArmorSkillFx.landingImpact(player, true, ArmorSet.JADE_TOPAZ);
                return;
            }
            FallGuardConfigs.Config config = FallGuardConfigs.config(level);
            if (player.getRandom().nextDouble() < config.passiveChance()) {
                event.setCanceled(true);
                // 基础落地特效（用户 2026-10-01）：被动豁免摔落伤害时也有
                ArmorSkillFx.landingImpact(player, false, ArmorSet.JADE_TOPAZ);
                return;
            }
        }
        // ② 宝石套 · 绝境守护：继承虚衡坠护的摔落豁免（规格 §1.1"继承虚衡坠护的摔落豁免"，
        // 口径同 §一 虚衡坠护：被动按概率、按住 100%）。判定与概率源都住在 LastStandHandler
        // （概率沿用虚衡坠护同级那一档，见 LastStandConfigs.passiveFallChance）。
        // ⚠ **不**判 LastStand 的冷却：摔落豁免是"继承来的被动"，与主动长按的内置冷却无关，
        // 冷却中按住技能键仍然该 100% 豁免（与虚衡坠护一致）。
        // 走到这里说明生效的那一套提供了 LAST_STAND ⇒ 那一套就是宝石套（levelOf 内部按
        // ArmorSet.effectiveSet 解析），所以粒子按宝石套蓝红上色。
        if (LastStandHandler.shouldNegateFall(player)) {
            event.setCanceled(true);
            boolean holding = ArmorSkillRuntime.isHolding(player, ArmorSkillRuntime.LAST_STAND);
            // 宝石套：蓝红落地（不再借翠玉的黄绿 —— 规格 §8 第 4 层）
            ArmorSkillFx.landingImpact(player, holding, ArmorSet.SAPPHIRE_RUBY);
        }
    }

    /** 离场：清长按状态（冷却留在玩家持久数据里，下次进来仍然是冷的）。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ArmorSkillRuntime.forget(event.getEntity());
    }

    /** 死亡/重生：同上（重生会换实体，旧实体的长按状态必须清）。 */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        ArmorSkillRuntime.forget(event.getOriginal());
    }

    /**
     * 服务器关闭：把临域充力还活着的会话全部收尾（移除注入器 + 曲柄归零）。
     *
     * <p>{@code ServerStoppingEvent} 在存档写盘<b>之前</b>触发 ⇒ 干净关服的存档里不会留下
     * 孤儿注入器，也不会留下一个 inUse 仍为 10 的曲柄（硬崩溃才走"注入器闲置自愈 +
     * inUse 自减到 0"那条兜底路径）。</p>
     */
    @SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        com.hjmmd_8.createoreexpansion.content.equipment.armor.field.FieldChargeRuntime
            .finishAll(event.getServer());
    }
}
