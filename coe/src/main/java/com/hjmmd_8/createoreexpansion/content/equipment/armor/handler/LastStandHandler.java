package com.hjmmd_8.createoreexpansion.content.equipment.armor.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillFx;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillRuntime;
import com.hjmmd_8.createoreexpansion.content.skill.config.LastStandConfigs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * <b>绝境守护</b>（宝石套 · 槽位 1）的被动触发 + 不死图腾效果施加点。
 *
 * <h2>被动：高额伤害按概率触发不死图腾（规格 §6.2 逐字照做）</h2>
 * <pre>
 * maxHealth = 玩家最大生命值
 * damage    = 本次将结算的伤害（LivingDamageEvent.Pre#getNewDamage —— 减伤后的最终值）
 * 判定 A：damage &gt;= 0.80 × maxHealth
 * 判定 B：damage &gt;  0.50 × maxHealth 且 (当前生命 − damage) &lt; 0.10 × maxHealth
 * 高额伤害 = A || B  ⇒ 掷 procChance（Lv1 50% / Lv2 60% / Lv3 70%）
 * </pre>
 *
 * <h2>为什么是 {@link LivingDamageEvent.Pre}、以及怎么"取消"伤害</h2>
 * <p>本事件<b>不是</b> {@code ICancellableEvent}（它继承 {@code LivingEvent}，没有 cancel 位），
 * 所以挡伤害的方式是 <b>{@code event.setNewDamage(0)}</b>。等价性有代码级取证（NeoForge 21.1.248
 * 源码，本机 {@code neoforge-21.1.248-sources.jar}）：</p>
 * <ul>
 *   <li>{@code CommonHooks#onLivingDamagePre} 返回 {@code container.getNewDamage()}
 *       —— 它读的就是事件改过之后的值；</li>
 *   <li>{@code LivingEntity#actuallyHurt} / {@code Player#actuallyHurt} 里紧接着
 *       {@code float f1 = this.damageContainers.peek().getNewDamage();}
 *       再 {@code if (f1 != 0.0F) { ... this.setHealth(this.getHealth() - f1); ... }}</p>
 *   ⇒ 置 0 后 {@code f1 == 0}，生命值、战斗记录、伤害统计、吸收（absorption）扣减<b>全部</b>不进分支。</li>
 * </ul>
 * <p>同一处还确认了"取最终将结算的伤害"这条口径：该事件在 ARMOR / 附魔 / 药水减伤<b>之后</b>、
 * absorption 之前触发（事件 javadoc："Absorption modifiers are handled after this event"），
 * 所以 {@code getNewDamage()} 正是玩家真正会失去的生命。</p>
 *
 * <h2>触发后</h2>
 * <p>① 取消这次伤害；② 施加<b>不死图腾</b>那一组效果（{@link #applyTotemEffects}，按段位/等级抬
 * amplifier）；③ 起 30 秒内置冷却（规格 §7 Q3 默认值）；④ 音效 + 粒子。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class LastStandHandler {

    private LastStandHandler() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 被动：受到"高额伤害"时按等级概率触发不死图腾（规格 §6.2）。
     *
     * <p>只服务端、只玩家、只在该玩家生效套的绝境守护等级 &gt; 0 且内置冷却已过时判定。</p>
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int level = ArmorSkillRuntime.levelOf(player, ArmorSkillRuntime.LAST_STAND);
        if (level <= 0) {
            return;
        }
        // 内置冷却（规格 §7 Q3 默认 30 秒）：冷却中连骰子都不掷
        if (!ArmorSkillRuntime.isReady(player, ArmorSkillRuntime.LAST_STAND)) {
            return;
        }
        float damage = event.getNewDamage();
        if (damage <= 0.0F) {
            return;
        }
        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return;
        }
        // 规格 §6.2 的两条判定，比例逐字：0.80 / 0.50 / 0.10
        boolean lethal = damage >= 0.80F * maxHealth;
        boolean nearlyDead = damage > 0.50F * maxHealth
            && (player.getHealth() - damage) < 0.10F * maxHealth;
        if (!lethal && !nearlyDead) {
            return;
        }
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        if (player.getRandom().nextDouble() >= config.procChance()) {
            return;
        }
        // 取消这次伤害（本事件不可 cancel，见类注释的代码级取证：置 0 等于"不进结算分支"）
        event.setNewDamage(0.0F);
        // 被动触发按该级的"最高段"给（等级 N 的段数恰好就是 N ⇒ 最高段 = 该级封顶增益）
        applyTotemEffects(player, config.segments(), level);
        ArmorSkillRuntime.startCooldown(player, ArmorSkillRuntime.LAST_STAND, config.cooldownSeconds());
        playTriggerFx(player);
    }

    /**
     * 施加<b>不死图腾</b>那一组效果（吸收 / 再生 / 抗火），三类都按段位抬升。
     *
     * <p>基准值 = 原版 {@code LivingEntity#checkTotemDeathProtection} 给的那一组
     * （<b>逐字同值</b>）：再生 <b>900 tick / amplifier 1</b>、吸收 <b>100 tick / amplifier 1</b>、
     * 抗火 <b>800 tick / amplifier 0</b>。等级抬升口径是规格 §7 Q5 的默认值
     * （"三类都按段位 +1 级"）⇒ amplifier = 基准 + (段位 − 1)。</p>
     *
     * <p>时长取 {@code segmentSeconds[段位-1]}（被动触发时按"最高段"取）。
     * 三段共用同一个秒数 —— 规格原文"各段持续时间"是<b>每段一个值</b>，不是每个效果一个值。</p>
     *
     * @param player  目标玩家
     * @param segment 段位（1 起）；被钳到 1..该级段数
     * @param level   技能等级（决定读哪一档配置）
     */
    public static void applyTotemEffects(Player player, int segment, int level) {
        if (player == null) {
            return;
        }
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        int clamped = Math.max(1, Math.min(config.segments(), segment));
        int seconds = config.segmentSeconds()[clamped - 1];
        if (seconds <= 0) {
            return;
        }
        int ticks = seconds * 20;
        int lift = clamped - 1;
        // 原版不死图腾那一组（LivingEntity#checkTotemDeathProtection，逐字同 amplifier）：
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1 + lift));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1 + lift));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, lift));
    }

    /** 触发瞬间的音效 + 粒子（原版音效与原版粒子，不新增自定义类型）。 */
    private static void playTriggerFx(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player instanceof ServerPlayer server) {
            // 颜色与长按期间同一处色源（ArmorSkillFx 的宝石套蓝→红）；被动触发按"最高段"取红端
            ArmorSkillFx.totemBurst(server);
        }
    }

    /**
     * 该玩家此刻是否满足"绝境守护继承的摔落豁免"（规格 §1.1：口径同虚衡坠护 —— 被动按概率、
     * 按住 100%）。
     *
     * <p>放在本类而不是 {@code ArmorSkillFx}/{@code ArmorSkillHandler} 里：摔落这条判定与
     * "绝境守护"这条技能绑在一起，行为、数值、判定同住一处便于对照规格。</p>
     *
     * @return 该豁免这次摔落伤害 ⇒ {@code true}
     */
    public static boolean shouldNegateFall(Player player) {
        if (player == null) {
            return false;
        }
        int level = ArmorSkillRuntime.levelOf(player, ArmorSkillRuntime.LAST_STAND);
        if (level <= 0) {
            return false;
        }
        // 主动：长按期间恒 100%（与虚衡坠护同口径，规格 §1.1"按住 100%"）
        if (ArmorSkillRuntime.isHolding(player, ArmorSkillRuntime.LAST_STAND)) {
            return true;
        }
        return player.getRandom().nextDouble() < LastStandConfigs.config(level).passiveFallChance();
    }
}
