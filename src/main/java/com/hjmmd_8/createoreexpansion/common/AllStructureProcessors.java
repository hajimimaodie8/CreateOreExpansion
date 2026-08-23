package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.worldgen.processor.BastionTreasureSapphireProcessor;
import com.hjmmd_8.createoreexpansion.content.worldgen.processor.EndShipStellarstoneProcessor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 自定义结构处理器注册（StructureProcessorType）。
 *
 * <p>结构彩蛋全部依靠 {@link StructureProcessor} 实现，不覆盖原版结构模板 nbt：
 * <ul>
 *     <li>{@link BastionTreasureSapphireProcessor} —— 堡垒宝藏室：50% 概率在金块堆内部
 *         镶嵌 1 块蓝宝石块（单堡垒最多 1 处，通过 finalizeProcessing 统计后只改一块）；</li>
 *     <li>{@link EndShipStellarstoneProcessor} —— 末影船：50% 概率将龙首后方船体方块
 *         替换为星辉石块（隐藏建筑彩蛋）。</li>
 * </ul>
 */
public final class AllStructureProcessors {

	private static StructureProcessorType<BastionTreasureSapphireProcessor> bastionTreasureSapphire;
	private static StructureProcessorType<EndShipStellarstoneProcessor> endShipStellarstone;

	private AllStructureProcessors() {
	}

	/** 在 mod 事件总线注册（vanilla registry，RegisterEvent 触发时注册） */
	public static void register(IEventBus modEventBus) {
		modEventBus.addListener(AllStructureProcessors::onRegister);
	}

	private static void onRegister(RegisterEvent event) {
		if (!event.getRegistryKey().equals(BuiltInRegistries.STRUCTURE_PROCESSOR.key()))
			return;
		bastionTreasureSapphire = register(event, "bastion_treasure_sapphire", BastionTreasureSapphireProcessor.CODEC);
		endShipStellarstone = register(event, "end_ship_stellarstone", EndShipStellarstoneProcessor.CODEC);
	}

	public static StructureProcessorType<BastionTreasureSapphireProcessor> bastionTreasureSapphire() {
		return bastionTreasureSapphire;
	}

	public static StructureProcessorType<EndShipStellarstoneProcessor> endShipStellarstone() {
		return endShipStellarstone;
	}

	@SuppressWarnings("unchecked")
	private static <P extends StructureProcessor> StructureProcessorType<P> register(
		RegisterEvent event, String name, MapCodec<P> codec) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, name);
		StructureProcessorType<P> type = () -> codec;
		event.register(BuiltInRegistries.STRUCTURE_PROCESSOR.key(), id, () -> type);
		return (StructureProcessorType<P>) BuiltInRegistries.STRUCTURE_PROCESSOR.get(id);
	}
}
