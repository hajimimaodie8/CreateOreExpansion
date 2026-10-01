package com.hjmmd_8.createoreexpansion.content.equipment.armor.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillFx;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillRuntime;
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
        }
    }

    /**
     * 摔落：虚衡坠护的被动（按等级概率）+ 主动（长按期间 100%）。
     *
     * <p>顺序刻意"先主动后被动"：主动是确定性的 100%，先短路掉就不必掷骰子（也避免
     * 主动期间因为随机数失败而掉血这种明显不合理的表现）。</p>
     */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int level = ArmorSkillRuntime.levelOf(player, ArmorSkillRuntime.FALL_GUARD);
        if (level <= 0) {
            return;
        }
        if (ArmorSkillRuntime.isHolding(player, ArmorSkillRuntime.FALL_GUARD)) {
            event.setCanceled(true);
            // 落地特效（用户 2026-10-01）：按住技能键落地 ⇒ "黄绿交加"加强版
            ArmorSkillFx.landingImpact(player, true);
            return;
        }
        FallGuardConfigs.Config config = FallGuardConfigs.config(level);
        if (player.getRandom().nextDouble() < config.passiveChance()) {
            event.setCanceled(true);
            // 基础落地特效（用户 2026-10-01）：被动豁免摔落伤害时也有
            ArmorSkillFx.landingImpact(player, false);
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
}
