package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * <b>回旋镖的吸附与乘客过滤</b>（2026-10-03 行为零变化拆分，从 {@code AbstractBoomerangEntity} 逐字搬出）。
 *
 * <p>唯一吸附实现 {@link #pickUpItems(AbstractBoomerangEntity)}（每服务端 tick 一次，去程与回程都经过），
 * 以及它与 {@code canAddPassenger} 共用的乘客判据 {@link #canCarry(Entity)}。</p>
 *
 * <h2>八、批 6（2026-10-03）：吸附只有一处 —— 两种模式都经过它（作者报的 bug）</h2>
 * <p><b>作者原话</b>：「点按直线收回，掉落物什么的，带回自己，但是<b>长按并不能</b>。长按的话，
 * <b>掉落物都存在原地，经验也没有回</b>。」</p>
 *
 * <p><b>真因（几何，不是拾取码本身）</b>：{@link #pickUpItems()} 一直只有一处实现，但它的
 * <b>唯一调用点</b>被关在 {@code DATA_RETURNING} 分支里（回程才吸附）。点按之所以"看起来正常"，
 * 是因为点按的去程是一条直线、回程是<b>原路返回</b>，回程那几 tick 正好一路掠过掉落点；
 * 而长按的花瓣曲线两端都收敛到出手点 P（{@code r(±Δθ/2) = 0}，见 {@link BoomerangCurveConfigs}）
 * ⇒ 花瓣走完那一刻镖已经<b>回到主人身上</b>，回程只剩不到一 tick 就 {@code collect}
 * （距离 &lt; {@link AbstractBoomerangEntity#RETURN_ARRIVE_SQR}）⇒ 花瓣弧上挖出来的掉落物与经验<b>一个都没机会上船</b>。</p>
 *
 * <p><b>修法（唯一实现 + 唯一调用点，两模式共用）</b>：把吸附从"回程专属"抬成
 * <b>每服务端 tick 一次、去程与回程都经过</b>的那一处（见 {@link AbstractBoomerangEntity#tick()} 末尾），
 * 并且去程那一支<b>不再早退</b>（旧的 {@code else if (tickOutbound()) { return; }} 会连吸附一起跳过）。
 * 于是花瓣弧上"镖飞过掉落点"的那些 tick 就能直接吸走，与点按走的是同一条路径、
 * 同一份 {@link #pickUpItems()}／{@link #canCarry(Entity)}（关卡 §29r 钉着"只有一处实现、
 * 两模式都经过它、花瓣段不得绕过"）。</p>
 *
 * <p><b>连带必须补的一道闸门</b>：{@link AbstractBoomerangEntity#canHitEntity(Entity)} —— 乘客的骑乘位就在镖身上，
 * 而 {@code ProjectileUtil} 的候选只排除载具自己、不排除乘客；吸附一旦在去程也跑，
 * "镖带着战利品飞"就是常态，不拦的话镖会在下一个 tick 把自己的掉落物当敌人打死
 * （{@code ItemEntity} 5 点血 vs 镖 6~12 点 {@code indirectMagic}）。</p>
 *
 * <p><b>顺带堵掉的一条旧缝</b>：旧写法在 {@code collect → finishFlight → discard} 之后还会再扫一次
 * 吸附（同一 tick、实体已 {@code discard}），那一窗口里上船的掉落物会挂到一个<b>已被移除、
 * 再也不会 tick</b> 的载具上。现在那一处有 {@code !isRemoved()} 守卫。</p>
 */
final class BoomerangPickup {

	private BoomerangPickup() {
	}

	/**
	 * ★ <b>唯一吸附实现</b>：每服务端 tick 扫一次膨胀 {@value AbstractBoomerangEntity#PICKUP_RADIUS} 格的区域，
	 * 掉落物与经验球上船（{@code startRiding(this)} + 掉落物设拾取延迟 {@value AbstractBoomerangEntity#PICKUP_DELAY}）。
	 *
	 * <p>⚠ <b>批 6 起它不再只属于回程</b>：调用点仍然唯一（{@link AbstractBoomerangEntity#tick()} 末尾），
	 * 但<b>去程（含花瓣段）与回程都经过它</b> —— 那正是「长按的掉落物/经验没被带回」的修复落点
	 * （见类注释第八节）。这里<b>不许</b>出现第二个调用点、也不许有第二份扫描代码。</p>
	 */
	static void pickUpItems(AbstractBoomerangEntity host) {
		AABB area = host.getBoundingBox().inflate(AbstractBoomerangEntity.PICKUP_RADIUS);
		List<Entity> found = host.level().getEntitiesOfClass(Entity.class, area, e -> canCarry(e) && !e.isPassenger());
		for (Entity entity : found) {
			entity.startRiding(host);
			if (entity instanceof ItemEntity item) {
				item.setPickUpDelay(AbstractBoomerangEntity.PICKUP_DELAY);
			}
		}
	}

	/** 只让掉落物与经验球上船（需求 4）。 */
	static boolean canCarry(Entity entity) {
		return entity instanceof ItemEntity || entity instanceof ExperienceOrb;
	}
}
