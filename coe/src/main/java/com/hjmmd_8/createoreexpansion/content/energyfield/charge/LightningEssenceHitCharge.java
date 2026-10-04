package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.content.energyfield.ChargeApi;

import net.minecraft.world.entity.LivingEntity;

/**
 * <b>雷魔素命中生物 ⇒ 随机附着一种电荷</b>（作者 2026-10-04 统一标准，于「弓技能批 3 · 元矢自生」
 * 期间下达；本条口径<b>覆盖</b>了此前"元矢自生抽到雷就按波级给电荷"的默认裁定）。
 *
 * <h2>作者原话与逐条落地</h2>
 * <p>「雷魔素攻击生物之后，会随机给生物附着一种电荷，附着时间为 5 秒，附着的那个 buff 等级从
 * 1~3 随机。」⇒ 四条，逐条对应一处代码：</p>
 * <ol>
 *   <li><b>触发 = 雷魔素命中生物</b>：判据在调用点
 *       （{@code WaveEssenceEffects} 的生物那一支只有 {@code case LIGHTNING} 会走到这里），
 *       本类不认魔素、也不认波 —— 它只负责"给这个生物一笔随机电荷"这一件事；</li>
 *   <li><b>极性 = 随机</b>：走 {@link ChargeApi#applyRandom}（本仓"施加一律经 ChargeApi"的既有
 *       口径，同极合并 / 异极中和都在门面里，本类不重写任何一条）；</li>
 *   <li><b>附着时长 = 固定 5 秒</b>：{@link ChargeConfigs#LIGHTNING_ESSENCE_CHARGE_TICKS}
 *       （⚠ <b>不是</b> {@link ChargeConfigs#durationTicks(int)} 那套"按等级给时长"的公式）；</li>
 *   <li><b>电荷等级 = 1~3 纯随机</b>：
 *       {@link ChargeConfigs#randomLightningEssenceLevel(net.minecraft.util.RandomSource)}
 *       （⚠ 既不继承波级、也不按弓的档位等级；随机源用<b>被命中的那个生物自己的</b>
 *       {@code RandomSource}，与 {@code applyRandom} 掷硬币用的是同一个源）。</li>
 * </ol>
 *
 * <h2>为什么单独一个类，而不是写在 {@code WaveEssenceEffects} 里</h2>
 * <p>魔素层有一条<b>既有断言</b>钉着「{@code WaveEssenceEffects} 的代码里不许出现
 * {@code ChargeApi}」（那条断言的由来见 {@code tools/check-armor-sets.ps1} 的
 * {@code charge-route-essence-arm}）：魔素层的 {@code case LIGHTNING} 当初是<b>刻意留空</b>的，
 * 电荷的施加面被收口在 {@code ChargeApi} 这一处。作者这次下的是<b>统一标准</b>（凡"带雷魔素的
 * 攻击波命中生物"都算），于是"哪一支魔素 ⇒ 施加电荷"留在魔素层、"怎么施加"落在本类
 * ⇒ 两条既有断言（魔素层的 {@code case LIGHTNING} 正文仍是 {@code return;}、代码里没有
 * {@code ChargeApi}）都保持原样，一条都不用放宽。</p>
 *
 * <p><b>数字</b>：本文件一个数字字符都不写 —— 时长与等级上下界全部按名取自
 * {@link ChargeConfigs}（关卡里有一条负向断言扫这个调用点）。</p>
 *
 * <p><b>日志</b>：本类刻意不打日志。这次命中已经由魔素层那条 {@code WaveDiag} 事件流
 * （"魔素命中生物：…"）记下来了，而波相关日志的唯一出口是 {@code WaveDiag} —— 再在这里补一条
 * {@code CoeCore.LOGGER} 就是同一次命中两条日志、两个前缀。</p>
 *
 * @since 1.0.0
 */
public final class LightningEssenceHitCharge {

	private LightningEssenceHitCharge() {
	}

	/**
	 * 给这个生物随机附着一种电荷：<b>极性随机、等级 1~3 随机、时长固定 5 秒</b>。
	 *
	 * <p>唯一调用点两处、<b>同一件事</b>：① 波实体命中生物那一处（带雷魔素的波，作者统一标准）；
	 * ② "元矢自生"打出的魔法箭命中生物那一处（箭上带的魔素恰好是雷）。两处都经由魔素层
	 * {@code WaveEssenceEffects} 的 {@code case LIGHTNING} 汇到这里 ⇒ 只有一份实现。</p>
	 *
	 * @param target 被雷魔素打中的生物（{@code null} ⇒ 直接返回 <b>false</b>，不抛）
	 * @return 这次调用之后，该生物身上是否带着这一极的电荷（免疫拦下时为 false，与
	 *         {@link ChargeApi#applyRandom} 同义）
	 */
	public static boolean applyOnHit(LivingEntity target) {
		if (target == null) {
			return false;
		}
		return ChargeApi.applyRandom(target,
			ChargeConfigs.randomLightningEssenceLevel(target.getRandom()),
			ChargeConfigs.LIGHTNING_ESSENCE_CHARGE_TICKS);
	}
}
