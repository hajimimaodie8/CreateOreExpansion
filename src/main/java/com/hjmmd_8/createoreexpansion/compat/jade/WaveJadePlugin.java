package com.hjmmd_8.createoreexpansion.compat.jade;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 集成：在准星指向能量波实体时显示波特点（等级/速度/寿命）。
 *
 * <p><b>可选依赖（重要）</b>：本类<b>只在 Jade 存在时才会被加载</b>——
 * 入口 {@code WaveJadeCompat} 用 {@code ModList.isLoaded("jade")} + 反射
 * {@code Class.forName} 触发本类加载；未安装 Jade 时本类永不被 JVM 加载，
 * 不会出现"标注 optional 仍硬编码调用导致崩溃"的问题（农夫乐事教训）。</p>
 *
 * <p><b>显示内容</b>：</p>
 * <ul>
 *   <li>等级：低/高/伽马，文字颜色随波种类（{@link AbstractChargerWaveEntity#getWaveRenderColor()}）；</li>
 *   <li>运行速度（格/秒，含波速调节器修正）；</li>
 *   <li>剩余寿命（秒）。</li>
 * </ul>
 */
@WailaPlugin
public class WaveJadePlugin implements IWailaPlugin, IEntityComponentProvider {

	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("createoreexpansion", "wave");

	@Override
	public void register(IWailaCommonRegistration registration) {
		// 波实体无服务端同步数据需求（等级/速度/寿命均在实体实例上可读），无需 serverDataProvider
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerEntityComponent(this, AbstractChargerWaveEntity.class);
	}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		if (!(accessor.getEntity() instanceof AbstractChargerWaveEntity wave))
			return;

		// 等级：颜色随波种类（渲染颜色 RGB 转十六进制）
		String levelName = switch (wave.getWaveLevel()) {
			case 2 -> "高";
			case 3 -> "伽马";
			default -> "低";
		};
		int color = colorOf(wave.getWaveRenderColor());
		tooltip.add(Component.translatable("createoreexpansion.jade.wave_level", levelName)
			.withStyle(ChatFormatting.WHITE)
			.withStyle(style -> style.withColor(color)));

		// 运行速度（格/秒）
		tooltip.add(Component.translatable("createoreexpansion.jade.wave_speed",
			String.format("%.1f", wave.getWaveSpeed()))
			.withStyle(ChatFormatting.GRAY));

		// 剩余寿命（秒）
		double remaining = wave.getRemainingLifetime() / 20.0d;
		tooltip.add(Component.translatable("createoreexpansion.jade.wave_lifetime",
			String.format("%.1f", remaining))
			.withStyle(ChatFormatting.GRAY));
	}

	/** RGB(0-1) → 十六进制颜色（用于等级文字着色）。 */
	private static int colorOf(Vec3 rgb) {
		if (rgb == null)
			return 0xFFFFFF;
		int r = (int) (rgb.x * 255) & 0xFF;
		int g = (int) (rgb.y * 255) & 0xFF;
		int b = (int) (rgb.z * 255) & 0xFF;
		return (r << 16) | (g << 8) | b;
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}
}
