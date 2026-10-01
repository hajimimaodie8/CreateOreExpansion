package com.hjmmd_8.createoreexpansion.content.series;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.SeriesTraits;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>系列掉落物保护</b>（用户 2026-10-01 定稿）——放在这里的是"掉在地上/被雷劈"这一层的特性，
 * 与 {@link SeriesTraits}（"这件东西属不属于某系列"的<b>唯一判定入口</b>）配套。
 *
 * <h2>三档待遇（用户原话）</h2>
 * <ul>
 *   <li><b>星辉石系列</b>（含 <b>星芒石</b>：两者按用户裁定<b>同待遇</b> ⇒ 同挂
 *       {@code stellarstone_items} 标签）：① 丢进<b>岩浆</b>不销毁；② 丢进<b>善化液</b>
 *       （{@code transmutation_fluid}）不销毁；③ 掉进<b>虚空会自动浮回来</b>。</li>
 *   <li><b>雷鸣合金系列</b>：丢进<b>岩浆</b>不销毁（虚空不浮回、善化液待定 ⇒ 本类只做岩浆）。</li>
 * </ul>
 *
 * <h2>为什么"善化液那一档"现在不需要写代码</h2>
 * <p>查过全链：{@code TransmutationFluidBlock} 只做 {@code animateTick}（不在 {@code entityInside} 里销毁实体），
 * {@code TransmutationEventHandler} 只统计<b>玩家</b>接触时长以施加嬗乱效果。
 * ⇒ <b>掉落物在善化液里本来就不会被销毁</b>（无一处在销毁它）。所以那一档当前是"天然满足"，
 * 一旦将来给善化液加了销毁掉落物的逻辑，<b>在那处判定里调 {@link SeriesTraits#isStellarstone} 即可</b>，
 * 不必在本类里预先写一段永远不会被触发的代码。</p>
 *
 * <h2>为什么用"仅岩浆期间不可摧毁"而不是常驻无敌</h2>
 * <p>{@code ItemEntity} 在岩浆里是<b>每 tick 掉血</b>（原版物品 5 点血、岩浆 4 点/次 ⇒ 两 tick 就没了），
 * 而 NeoForge 没有"物品受伤可拦截"的事件（{@code LivingHurtEvent} 只覆盖生物）。
 * 因此这里用<b>作用域最小</b>的手法：<b>只在"处于岩浆/着火"时</b>把该实体设为不可摧毁，
 * 一旦脱离就立刻恢复 —— 而不是让它整条命都免疫爆炸、仙人掌、铁砧。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class SeriesDropProtectionHandler {

    /** 虚空兜底：低于世界底面这么多格就直接抬回来（原版 {@code outOfWorld} 阈值是 −64，留足余量）。 */
    private static final int VOID_RESCUE_MARGIN = 16;

    /** 浮回速度（格/tick）：比重力加速度大一个量级，表现为"缓慢稳定地上浮"。 */
    private static final double VOID_FLOAT_SPEED = 0.4D;

    private SeriesDropProtectionHandler() {
        throw new AssertionError("This class should not be instantiated");
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        Level level = item.level();
        if (level.isClientSide()) {
            return;
        }
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) {
            return;
        }
        // 唯一判定入口复用 SeriesTraits（标签 ∪ 方块标签 ∪ 注册名约定），本类不自己认物品
        boolean stellar = SeriesTraits.isStellarstone(stack);
        boolean thunder = SeriesTraits.isThunderite(stack);
        if (!stellar && !thunder) {
            return;
        }

        // ① 岩浆/着火：**仅此期间**不可摧毁（脱离后立刻恢复），顺手把身上的火清掉
        boolean inLava = item.isInLava() || item.isOnFire();
        if (inLava) {
            item.clearFire();
            if (!item.isInvulnerable()) {
                item.setInvulnerable(true);
            }
        } else if (item.isInvulnerable()) {
            item.setInvulnerable(false);
        }

        // ② 虚空浮回：**只有星辉石系列（含星芒石）**有这一档；雷鸣不享受
        if (stellar && item.getY() < level.getMinBuildHeight()) {
            item.setDeltaMovement(item.getDeltaMovement().x, VOID_FLOAT_SPEED, item.getDeltaMovement().z);
            if (item.getY() < level.getMinBuildHeight() - VOID_RESCUE_MARGIN) {
                // 兜底：万一被什么东西极快压下去，直接抬回底面之上，避免触发原版的 outOfWorld 销毁
                item.setPos(item.getX(), level.getMinBuildHeight() + 1.0D, item.getZ());
            }
        }
    }
}
