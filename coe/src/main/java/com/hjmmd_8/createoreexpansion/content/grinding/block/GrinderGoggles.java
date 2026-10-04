package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * <b>动力角磨床的护目镜 / 悬停文案装配</b>（2026-10-05 行为零变化拆分，从
 * {@code PowerAngleGrinderBlockEntity#addToTooltip} 与 {@code #addToGoggleTooltip} 的
 * <b>方法体</b>逐字搬出；两个 {@code @Override} 仍留在方块实体里，形状一个字未改）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>护目镜与物品悬停栏上那几行字长什么样</b> —— 当前支持的加工类型
 * （随角磨轮等级）、当前装的轮子、轮子的特殊效果、所需转速与实际转速过低提示。行排版一律走
 * {@link GoggleUtil}（与充能器/波闸同一套缩进）；<b>零新增翻译键</b>，全部沿用既有
 * {@code createoreexpansion.tooltip.*} / {@code createoreexpansion.goggles.*}。</p>
 *
 * <p>搬运口径（与回旋镖那次拆分的存档/NBT 半区同形：宿主保留 {@code super} 调用 + 把装配交给本类）：
 * 方法体逐字相同，差异只有三类 —— {@code private/无} → 包级私有、宿主成员改写成 {@code host.} 限定
 * （{@code getWheel()} / {@code getWheelEffect()} / {@code getWheelTier()} / {@code getSpeed()}）、
 * {@code addToTooltip} 那条<b>未使用</b>的局部变量与两处 {@code return true;} 换成
 * {@code appendSupportedTypes} 的无返回值形态（宿主仍然返回 {@code true}，与原来逐字同值）。
 * <b>文案、颜色、条件顺序一个字未动</b>。</p>
 */
final class GrinderGoggles {

	private GrinderGoggles() {
		throw new AssertionError("This class should not be instantiated");
	}

	/** 悬停提示：当前支持的加工类型（随安装的角磨轮等级变化）；行排版统一缩进（GoggleUtil） */
	static void appendSupportedTypes(PowerAngleGrinderBlockEntity host, List<Component> tooltip) {
		GrindingWheelTier tier = host.getWheelTier();
		if (tier == null) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.tooltip.no_wheel_type")
				.withStyle(ChatFormatting.GRAY));
			return;
		}
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.tooltip.supported_types")
			.withStyle(ChatFormatting.GRAY));
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.tooltip.type_grinding")
			.withStyle(ChatFormatting.GRAY));
		if (tier.level >= 2) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.tooltip.type_advanced")
				.withStyle(ChatFormatting.GRAY));
		}
		if (tier.level >= 3) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.tooltip.type_dismantling")
				.withStyle(ChatFormatting.GRAY));
		}
	}

	/** 护目镜信息行：行排版统一缩进（GoggleUtil，同充能器/波闸） */
	static boolean appendGoggleLines(PowerAngleGrinderBlockEntity host, List<Component> tooltip,
									 boolean added) {
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.angle_grinder")
			.withStyle(ChatFormatting.GRAY));
		added = true;

		ResourceLocation wheelId = host.getWheel();
		if (wheelId == null) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.no_wheel")
				.withStyle(ChatFormatting.RED));
			return added;
		}

		Item wheelItem = BuiltInRegistries.ITEM.get(wheelId);
		Component wheelName = wheelItem != null && wheelItem != Items.AIR
			? wheelItem.getDescription()
			: Component.literal(wheelId.toString());
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.installed_wheel", wheelName)
			.withStyle(ChatFormatting.WHITE));

		// 轮子特殊效果（护目镜可见）
		Component effectDescription = host.getWheelEffect().getDescription();
		if (!effectDescription.getString()
			.isEmpty())
			GoggleUtil.forGoggles(tooltip, effectDescription.copy()
				.withStyle(ChatFormatting.AQUA));

		GrindingWheelTier tier = host.getWheelTier();
		if (tier == null)
			return added;

		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.required_speed", tier.getMinRpm())
			.withStyle(ChatFormatting.GOLD));

		float speed = Math.abs(host.getSpeed());
		if (speed < tier.getMinRpm()) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.speed_too_low")
				.withStyle(ChatFormatting.RED));
		} else {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.processing_time",
				String.format(Locale.ROOT, "%.1f", tier.getProcessingTime(speed)))
				.withStyle(ChatFormatting.AQUA));
		}
		return added;
	}
}
