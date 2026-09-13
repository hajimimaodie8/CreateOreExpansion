package com.hjmmd_8.createoreexpansion.content.wave.gauge;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 波情查询仪：手持它的玩家右键，查询<b>最近的</b>能量波并把波情四要素报到动作栏。
 *
 * <p><b>手持静态 / 右键动画</b>：物品贴图是 8 帧竖排条（{@code wave_query_gauge.png} + 同名 .mcmeta），
 * 默认模型用静态第 0 帧（{@code wave_query_gauge_idle.png}）；查询后物品进入
 * {@link #SCAN_TICKS} tick 冷却，冷却期内客户端把模型切到动画贴图
 * （原版 item property {@code createoreexpansion:scanning} + 模型 overrides，
 * 见 {@code client.WaveQueryGaugeModelRegistration}）。</p>
 *
 * <p><b>本类只做三件事</b>：找最近的波（{@link #findNearestWave}）、把读数显示出去、
 * 挂冷却。"波情长什么样"由 {@link WaveReadout} / {@link WavePayloadReadout} 负责，
 * 本类不碰任何取值与文案。</p>
 *
 * <p><b>不依赖 Jade</b>：Jade 那套读数在 {@code compat/jade}，本物品在 {@code content} 层，
 * 两者互不引用（不 import 可选模组类）。</p>
 */
public class WaveQueryGaugeItem extends Item {

	/** 本物品全部词条前缀（中英词条见 data/lang 的两个 LangProvider）。 */
	private static final String LANG = "createoreexpansion.wave_gauge.";

	/**
	 * 搜索半径（格）：以玩家碰撞箱外扩这么远取搜索区（正方体，半边长为 16 格，
	 * 即斜对角最远约 27.7 格）。16 格 ≈ 两级充能器（及其中继机器）之间的常见距离，
	 * 够覆盖"玩家站在机器旁边查刚发射的波"，又不至于把远处另一条产线的波也捞进来。
	 */
	public static final int SEARCH_RADIUS = 16;

	/**
	 * 查询冷却（tick）：= 动画贴图<b>一个完整循环</b>的时长（{@code .mcmeta} 里 frametime 4 × 8 帧 = 32 tick）。
	 * 取 32 而非更短的值，是为了让"一次右键 = 一次完整扫描动画"读得顺；
	 * 想更短的手感就改这一个常量（例如 10 = 只播动画的前 3 帧）。
	 */
	public static final int SCAN_TICKS = 32;

	public WaveQueryGaugeItem(Properties properties) {
		super(properties);
	}

	/**
	 * 右键：服务端查最近的能量波 → 动作栏（聊天栏会刷屏，查询是高频操作）报四要素；
	 * 找不到就给一条"附近没有能量波"。随后给自己挂 {@link #SCAN_TICKS} tick 冷却。
	 *
	 * <p>冷却中再右键直接不做任何事：既不重复查询，也不把冷却推后
	 * （保证"一次右键 = 一段动画"读得完整）。</p>
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns()
			.isOnCooldown(this))
			return InteractionResultHolder.pass(stack);

		// 全程只在服务端执行：查询与消息都不需要客户端发包
		// （冷却由服务端加，原版 ServerItemCooldowns 会自动把它同步给客户端，供客户端切动画模型）
		if (!level.isClientSide) {
			AbstractChargerWaveEntity nearest = findNearestWave(level, player);
			player.displayClientMessage(nearest == null ? Component.translatable(LANG + "no_wave")
				: WaveReadout.of(nearest)
					.line(), true);
			player.getCooldowns()
				.addCooldown(this, SCAN_TICKS);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(LANG + "tooltip")
			.withStyle(ChatFormatting.DARK_GRAY));
	}

	/**
	 * 取搜索区内<b>最近</b>的一只能量波（无则 null）：比较到玩家的距离平方，最近的胜出
	 * （距离平方比较省一次开方，且与本模组其它"取最近"点位的口径一致）。
	 */
	private static AbstractChargerWaveEntity findNearestWave(Level level, Player player) {
		List<AbstractChargerWaveEntity> waves = level.getEntitiesOfClass(AbstractChargerWaveEntity.class,
			player.getBoundingBox()
				.inflate(SEARCH_RADIUS),
			AbstractChargerWaveEntity::isAlive);
		AbstractChargerWaveEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (AbstractChargerWaveEntity wave : waves) {
			double distance = wave.distanceToSqr(player);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = wave;
			}
		}
		return nearest;
	}
}
