package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeEffects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * <b>电荷 debuff 的「解不掉」闸门</b>（coe-charge 批 2）—— 只挡
 * {@code createoreexpansion:charged_positive} / {@code createoreexpansion:charged_negative}
 * 这两个 id 的 {@link MobEffectEvent.Remove}。
 *
 * <p>需求 §3.1「能否被牛奶解除：<b>不能</b>」+ §5.3 陷阱 1/2。挡住
 * {@code Remove} 之后，所有「清除效果」的路径都解不掉这两个 debuff，而
 * <b>唯一</b>能让它们消失的路径是自然到期（见下「为什么不订 {@code Expired}」）。</p>
 *
 * <p><b>为什么读 {@code getEffect()} 而不是 {@code getEffectInstance()}</b>：{@code Remove}
 * 的 {@code getEffectInstance()} 被 {@code @Nullable} 标注，且它自己的注释写明
 * 「can be null if the entity does not have a MobEffect of the right type active」
 * （反编译实测：{@code MobEffectEvent.Remove} 的 {@code Holder} 字段来自
 * {@code effectInstance.getEffect()} 或直接的 {@code Holder} 参数，两条构造器都可能拿到
 * {@code null} 的实例）⇒ <b>本类一个字都不读实例</b>，只比 {@code Holder}。</p>
 *
 * <p><b>Holder 身份比较（本仓的既有教训）</b>：1.21 起 {@code MobEffectInstance#getEffect()}
 * 返回 {@code Holder<MobEffect>}，拿 {@code MobEffect} 实例与 {@code Holder} 做 {@code ==}
 * <b>恒为 false</b>（编译能过，只因非 final 类可转型成接口）——
 * {@code MedallionEffectHandler} 的 {@code Applicable} 兜底就因此从来没拦下过任何一次嬗乱
 * （P3u 修复，见该类的类注释）。本类照修后的形状写：<b>同一种东西比同一种东西</b>。</p>
 *
 * <p><b>两条判定都要，不是重复</b>（执行会话 2026-10-03 反编译实测后定，原先只打算写第一条）：</p>
 * <ul>
 *   <li><b>① 身份比较</b>（{@code event.getEffect() == CoeEffects.CHARGED_POSITIVE}）：效果实例
 *       里塞的是我们的 {@code DeferredHolder} 时命中。牛奶与无参 {@code /effect clear} 都从
 *       「拿实体身上那个 {@code MobEffectInstance}」的入口进来
 *       （{@code LivingEntity#removeEffectsCuredBy} / {@code #removeAllEffects}，
 *       两者都转成 {@code EventHooks.onEffectRemoved(entity, effectInstance, cure)}），
 *       事件里的 Holder 就是实例自己那个 ⇒ 这一条命中。</li>
 *   <li><b>② {@code Holder#is(ResourceKey)} 兜底</b>：{@code /effect clear <目标> <效果id>}
 *       走的是 {@code LivingEntity#removeEffect(Holder)}（反编译实测
 *       {@code EffectCommands#clearEffect} → 第 239 行 {@code removeEffect(effect)}），
 *       它把<b>命令参数解析出来的注册表 {@code Holder.Reference}</b> 直接交给事件 ——
 *       <b>不是</b>我们的 {@code DeferredHolder}。{@code DeferredHolder} 只是<b>包着注册表
 *       Holder 的一层壳</b>（{@code DeferredHolder#holder} 字段），它跟注册表 Holder
 *       <b>不是同一个对象</b> ⇒ 身份比较会漏掉这一条形态，而 {@code Holder#is(ResourceKey)}
 *       两边 Holder 都认（{@code DeferredHolder#is} 与 {@code Holder.Reference#is} 都是
 *       {@code key ==}，{@code ResourceKey.create} 走 {@code VALUES} 缓存 ⇒ 同一个 key 是
 *       同一个实例）。</li>
 * </ul>
 *
 * <p><b>为什么不订 {@code MobEffectEvent.Expired}</b>（需求 §3.4 / §5.3 陷阱 1）：
 * {@code Expired} 与 {@code Remove} 是<b>两个不同的事件</b>（{@code LivingEntity} 里到期那条
 * 路径自己 post {@code MobEffectEvent.Expired}），到期必须<b>放行</b>——一旦顺手把它也 cancel，
 * 这两个 debuff 就<b>永不过期</b>（批 3 接上扣血之后就是永久掉血）。本类因此<b>一个
 * {@code Expired} 订阅都不加</b>，全仓 {@code Expired} 命中数为 0，关卡里有负向断言守着。</p>
 *
 * <p><b>死亡/重生不受影响</b>（需求 §3.4 那一行）：死亡清效果走
 * {@code LivingEntity#triggerOnDeathMobEffects}，它对 {@code activeEffects} 直接
 * {@code clear()}、<b>不 post 任何 {@code Remove}</b> ⇒ 本类天生拦不到死亡清除，
 * 不需要（也不该）在这里写「死亡豁免」的分支。</p>
 *
 * <p><b>作用范围只有一个 id 集合</b>：本类只认这两个 {@code DeferredHolder}，别的效果
 * （嬗乱、凝能佩那条 {@code Applicable} 免疫通道…）一律不碰。{@code @EventBusSubscriber}
 * 的 {@code modid} 与<b>本类所在 mod 文件</b>一致（{@code :coe} ⇒ {@code createoreexpansion}）——
 * 这条错配会<b>静默不注入</b>（无警告、无报错、编译全绿），关卡里有正/反两形态的断言。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class ChargeEffectRemovalHandler {

	private ChargeEffectRemovalHandler() {
	}

	/**
	 * 拦下这两个电荷效果的移除（{@code Remove} 本质是 {@code ICancellableEvent}；
	 * cancel 之后调用方（{@code EventHooks.onEffectRemoved} 的返回值为 true）会跳过
	 * {@code onEffectRemoved} 与 {@code iterator.remove()}，效果因此留在实体身上）。
	 */
	@SubscribeEvent
	public static void onRemove(MobEffectEvent.Remove event) {
		if (isChargedEffect(event.getEffect())) {
			event.setCanceled(true);
		}
	}

	/**
	 * 这个 {@code Holder} 是不是我们要挡的两个电荷效果之一。
	 *
	 * <p>① 身份比较 + ② {@code Holder#is(ResourceKey)} 兜底，两者都只覆盖这两个 id；
	 * 为什么不合并成一条、以及为什么不能只留 ①，见类注释。</p>
	 */
	private static boolean isChargedEffect(Holder<MobEffect> effect) {
		if (effect == CoeEffects.CHARGED_POSITIVE || effect == CoeEffects.CHARGED_NEGATIVE) {
			return true;
		}
		return effect.is(CoeEffects.CHARGED_POSITIVE.getKey())
			|| effect.is(CoeEffects.CHARGED_NEGATIVE.getKey());
	}

}
