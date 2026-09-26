package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.lang.Lang;

/**
 * 蓝宝石应力充能器模式（仿黄铜隧道模式槽：侧面 ValueBox 按住/滚轮切换）。
 *
 * <p>模式经 Create 的 {@code ScrollOptionBehaviour} 持久化（NBT "ScrollValue"）并显示在
 * 侧面模式槽（{@link SapphireChargerModeSlot}）；0 = 普通、1 = 储存（与
 * {@code SapphireStressChargerBlockEntity#isStoringMode()} 对齐）。</p>
 */
public enum SapphireChargerMode implements INamedIconOptions {

	NORMAL(AllIcons.I_ACTIVE),
	STORE(AllIcons.I_PASSIVE),
	;

	private final AllIcons icon;

	SapphireChargerMode(AllIcons icon) {
		this.icon = icon;
		this.translationKey = "createoreexpansion.charger_mode." + Lang.asId(name());
	}

	private final String translationKey;

	@Override
	public AllIcons getIcon() {
		return icon;
	}

	@Override
	public String getTranslationKey() {
		return translationKey;
	}
}
