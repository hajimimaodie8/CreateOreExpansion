package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeEffects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * <b>电荷 debuff 的「解不掉」闸门</b>（coe-charge 批 2）—— 只挡
 * {@code createoreexpansion:charged_positive} / {@code createoreexpansion:charged_negative}
 * 这两个 id 的 {@link MobEffectEvent.Remove}。
 *
 * <p>需求 §3.1「能否被牛奶解除：<b>不能</b>」+ §5.3 陷阱 1/2。挡住
 * {@code Remove} 之后，<b>外部</b>「清除效果」的路径（牛奶 / {@code /effect clear} /
 * 别的模组的净化）全都解不掉这两个 debuff；能让它们消失的路径有<b>两条</b>：
 * ① 自然到期（见下「为什么不订 {@code Expired}」）；
 * ② ★ <b>电荷中和爆炸</b>（批 6，需求 §3.5「中和 = 两种效果都消失」）—— 它是本模组
 * <b>自己发起</b>的移除，走 {@link #removeForNeutralization} 这唯一一道放行。
 * ⚠ 这道放行不是可选项：{@code LivingEntity#removeEffect} 的<b>第一句</b> post 的正是本类
 * 拦的那个事件 ⇒ 不放行的话中和会<b>连一个效果都去不掉、而且什么都不报</b>
 * （理由与实测出处见该方法的 javadoc）。</p>
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
	 * 「这次移除是本模组的中和爆炸发起的」标记（只在 {@link #removeForNeutralization} 的调用栈
	 * 内为 true，{@code finally} 复位）。
	 *
	 * <p><b>为什么不是 {@code ThreadLocal}、也不加 {@code volatile}</b>：① 本仓关卡
	 * {@code charge-remove-guard-holder-identity} 对<b>本文件</b>做负向扫描
	 * （{@code .get()} 命中数必须为 0 —— 那是「拿 Holder 与效果实例比」那类历史错误的哨兵）
	 * ⇒ 读 {@code ThreadLocal#get()} 会踩到它；② 事件的派发<b>同线程同步</b>
	 * （{@code EventHooks.onEffectRemoved} 直接 post），标记只在一次调用内为 true，
	 * 而不加 {@code volatile} 恰好是最安全的方向：别的线程即使读到陈旧值也只会读到 false
	 * ⇒ 照旧拦下它自己那次移除（那本来就不该放行）。</p>
	 */
	private static boolean neutralizationRemoval;

	/**
	 * 拦下这两个电荷效果的移除（{@code Remove} 本质是 {@code ICancellableEvent}；
	 * cancel 之后调用方（{@code EventHooks.onEffectRemoved} 的返回值为 true）会跳过
	 * {@code onEffectRemoved} 与 {@code iterator.remove()}，效果因此留在实体身上）。
	 */
	@SubscribeEvent
	public static void onRemove(MobEffectEvent.Remove event) {
		if (neutralizationRemoval) {
			return; // 中和爆炸自己发起的移除：放行（唯一出口，见 removeForNeutralization）
		}
		if (isChargedEffect(event.getEffect())) {
			event.setCanceled(true);
		}
	}

	/**
	 * ★ <b>中和爆炸专用的一次性放行</b>：把某个电荷效果<b>真的</b>从实体身上移除
	 * （coe-charge 批 6；需求 §3.5「中和 = 两种效果都消失」）。
	 *
	 * <p><b>为什么不能直接调 {@code LivingEntity#removeEffect}</b>（本批最容易写成
	 * 「静默无效」的一处）：那个方法的<b>第一句</b>就是
	 * {@code EventHooks.onEffectRemoved(this, effect, null)}，而它 post 的正是本类拦下的
	 * {@link MobEffectEvent.Remove} ⇒ 对本类认的这两个效果，{@code removeEffect} 会
	 * <b>直接返回 false 并且什么都不做</b>（效果留在身上、扣血照旧、连一行日志都没有）。
	 * 实机口径的来源：{@code build/patch/mcsrc-all} 的 {@code LivingEntity.java}
	 * 与 {@code EventHooks.java}（{@code removeEffect} 与 {@code removeEffectsCuredBy}
	 * 两条路都被本类拦下）。</p>
	 *
	 * <p><b>放行的是哪一半</b>：本方法只跳过本类自己那道判断，事件<b>照常派发</b>
	 * （别的订阅者仍然看得到），{@code removeEffect} 因此走完它正常的后半段——
	 * {@code onEffectRemoved} 清属性修饰、把移除包发给玩家、并把 {@code effectsDirty} 置位
	 * （客户端 HUD 图标与效果粒子随之更新）。这些记账<b>一个都不能跳</b>：所以这里选
	 * 「放行 + 走原版移除」，而不是「绕过事件直接删 map」（后者效果确实会消失，
	 * 但玩家的 HUD 图标会一直留到自然到期，是另一种静默不一致）。</p>
	 *
	 * @return 同 {@code LivingEntity#removeEffect}：真的移除了才是 true
	 */
	public static boolean removeForNeutralization(LivingEntity entity, Holder<MobEffect> effect) {
		neutralizationRemoval = true;
		try {
			return entity.removeEffect(effect);
		} finally {
			neutralizationRemoval = false;
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
