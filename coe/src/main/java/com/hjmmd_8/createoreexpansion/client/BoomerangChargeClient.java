package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item.BoomerangItem;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * <b>回旋镖长按蓄力的手持姿态（需求 §3.3"贴图稍微往里收一收"）</b> —— 纯客户端，只有一处注册。
 *
 * <h2>一、为什么是"缩放 PoseStack"而不是改模型/贴图</h2>
 * <p>作者要的是「按住期间给物品/手持模型一个<b>向内收拢</b>的蓄力姿态」，并明确"具体实现由执行会话定"。
 * 本仓另有两条硬口径：<b>不许改贴图</b>（用户 2026-09-14）、<b>不许新造模型</b>（需求 §四：本轮不新增
 * 物品/方块/贴图/模型）。因此这里既不碰 {@code assets}，也不加 {@code CustomModelData} 变体，
 * 而是在 NeoForge 给出的唯一"手部渲染前"钩子上<b>缩放一次 PoseStack</b>：物品整体绕手部锚点收小
 * ⇒ 肉眼就是"往里收"。没有旋转、没有位移（越少的自由维度，越不容易在手感上翻车）。</p>
 *
 * <h2>二、钩子的位置与时机（读 {@code ItemInHandRenderer:463-464} 得到）</h2>
 * <p>{@code IClientItemExtensions#applyForgeHandTransform} 在第一人称手臂渲染的"普通物品"分支
 * <b>最开头</b>被调用（在 {@code getUseAnimation()} 那套位移之前、{@code renderItem} 之前）。
 * 返回 {@code false} = "照旧继续走原版变换"，我们只往 PoseStack 上叠一个 {@code scale}。
 * 不在使用中、或手上不是回旋镖时直接返回 —— 此时 PoseStack 一个字都不动（恒等），
 * 所以别的物品、别的动作逐字不受影响。</p>
 *
 * <p>⚠⚠ <b>必须返回 {@code false}（硬要求，不是风格）</b>：NeoForge 的补丁把调用点写成
 * <pre>
 *   if (!IClientItemExtensions.of(stack).applyForgeHandTransform(poseStack, ...))   // :463
 *   if (player.isUsingItem() &amp;&amp; player.getUseItemRemainingTicks() &gt; 0 &amp;&amp; ...) {    // :464
 *       switch (stack.getUseAnimation()) { ... }                                   // :466
 *   }
 * </pre>
 * （原版 {@code ItemInHandRenderer.java:463-464} 就是这种"悬空 {@code if}"形状）——
 * 返回 {@code true} 会让<b>整个 {@code isUsingItem} 分支被跳过</b>，包括 {@code case BOW}
 * 那套拉弓位移 ⇒ <b>把 {@code BoomerangItem#getUseAnimation} 刚拿到的原版姿态当场抵消掉</b>。
 * 本类只往 PoseStack 上叠一个 {@code scale}，所以恒返回 {@code false}。
 * 关卡 {@code boomerang-use-state} 钉着这一条。</p>
 *
 * <h2>三、进度口径：与"封顶 40 tick"同一个常量</h2>
 * <pre>
 *   按住 tick = BoomerangItem.heldTicks(手上那一份, 玩家, 玩家剩余使用时长的读数)   // = 72000 − 剩余
 *   进度 t    = BoomerangItem.chargeTicks(按住 tick) / HOLD_CHARGE_CAP_TICKS      // 钳在 0..1
 *   缩放      = 1 − {@value #FULL_CHARGE_SHRINK} · t                              // 松开即回 1.0（进度归零）
 * </pre>
 * <p>⚠ {@code chargeTicks} 是<b>唯一</b>的封顶判据（{@link BoomerangItem#HOLD_CHARGE_CAP_TICKS}）：
 * 2 秒之后 {@code t} 恒为 1 ⇒ 姿态停住，继续按着不再变形 —— 需求 §3.3"到 2 秒的时候停止"的
 * 表现面（裁定 D5-A：对轨迹/消耗/耐久零额外影响）。</p>
 *
 * <h2>四、幅度（{@value #FULL_CHARGE_SHRINK}）是我定的，可一句话改</h2>
 * <p>需求 §六 推断值 #8 把这个幅度留给执行会话（"小幅度，能看出在收即可"）。18% 是"一眼能看出在收、
 * 又不至于看不见手上的东西"的量级；改这一个常量即可。⚠ 这是<b>静态关卡验不了</b>的一处
 * （客户端渲染），必须进游戏看 —— 见交付报告里的待作者确认清单。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class BoomerangChargeClient {

	/**
	 * 满蓄力（40 tick）时握手模型收小的比例：{@code 缩放 = 1 − 它} = 0.82。
	 *
	 * <p>需求 §六 推断值 #8 留给执行会话定的幅度；改这一个数即可（0 = 完全不收，
	 * 0.3 以上开始明显影响"还看得清手上是什么"）。</p>
	 */
	public static final float FULL_CHARGE_SHRINK = 0.18F;

	private BoomerangChargeClient() {
	}

	/**
	 * 把姿态实现注册给四把回旋镖（<b>唯一的客户端注册点</b>）。
	 *
	 * <p>用 {@code RegisterClientExtensionsEvent} 而不是已废弃的 {@code Item#initializeClient}：
	 * 后者在本版被标了 {@code @Deprecated(forRemoval = true, since = "1.21")}，而前者是 NeoForge
	 * 现行的注册通道（{@code ClientExtensionsManager.init()} 在客户端构造期把它 post 到 mod 总线；
	 * 本类按 mod id 订阅，注册时机早于任何一次渲染）。</p>
	 *
	 * <p>⚠ 四把镖逐一登记（不写 {@code InstancedList} 之类的遍历技巧）：这份名单与
	 * {@code CoeItems} 里那四行声明一一对应，漏一把就是那一把没有蓄力姿态（无报错、只有眼里能看出来）。</p>
	 */
	@SubscribeEvent
	public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
		ChargePose pose = new ChargePose();
		event.registerItem(pose,
			CoeItems.JADE_TOPAZ_BOOMERANG.get(),
			CoeItems.SAPPHIRE_RUBY_BOOMERANG.get(),
			CoeItems.ASTRAL_BOOMERANG.get(),
			CoeItems.THUNDER_BOOMERANG.get());
	}

	/** 蓄力姿态本体：按住期间把手上的回旋镖按进度收小（其它一切情况恒等）。 */
	private static final class ChargePose implements IClientItemExtensions {

		@Override
		public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
											   ItemStack itemInHand, float partialTick, float equipProcess,
											   float swingProcess) {
			// 先判物品（绝大多数帧在这里就退出了，零额外开销），再判"正在按着的是不是这一份"
			if (!(itemInHand.getItem() instanceof BoomerangItem)) {
				return false;
			}
			if (!player.isUsingItem() || player.getUseItem() != itemInHand
				|| player.getUseItemRemainingTicks() <= 0) {
				return false;
			}
			int held = BoomerangItem.heldTicks(itemInHand, player, player.getUseItemRemainingTicks());
			float progress = (float) BoomerangItem.chargeTicks(held) / BoomerangItem.HOLD_CHARGE_CAP_TICKS;
			float scale = 1.0F - FULL_CHARGE_SHRINK * Mth.clamp(progress, 0.0F, 1.0F);
			poseStack.scale(scale, scale, scale);
			// ⚠ 恒 false：true 会跳过 ItemInHandRenderer:464 起的整个"使用中姿态"分支
			// （含 case BOW 的拉弓位移），等于把 BoomerangItem#getUseAnimation 抵消掉。
			// 我们只把姿态"叠"上去，原版怎么走就怎么走。
			return false;
		}
	}
}
