package com.hjmmd_8.createoreexpansion.mixin;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.worldgen.processor.EndShipStellarstoneProcessor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 末影船彩蛋挂载（TemplateStructurePiece 层，目标类自身字段/方法均可 @Shadow）。
 *
 * <ul>
 *     <li><b>龙首后方船体彩蛋</b>：{@code postProcess} HEAD 时若模板是末影船
 *         （templateName = "ship"），向 placeSettings 追加 {@link EndShipStellarstoneProcessor}
 *         （50% 概率将龙首后方船体方块替换为星辉石块）——赶在 placeInWorld 前生效；</li>
 *     <li><b>鞘翅房箱子保底</b>：{@code postProcess} RETURN 时（原版 handleDataMarker 已设完
 *         箱子表）重新扫描结构方块，将船上<b>第一个</b> Chest 标记的箱子改为专属子表
 *         （2-4 星辉石锭）；两个箱子只改一个，不双刷。</li>
 * </ul>
 *
 * <p>不覆盖原版结构模板 nbt：只给放置设置追加自定义 StructureProcessor + 改箱子战利品表。</p>
 */
@Mixin(TemplateStructurePiece.class)
public abstract class TemplateStructurePieceMixin {

	@Shadow
	protected String templateName;

	@Shadow
	protected StructurePlaceSettings placeSettings;

	@Shadow
	protected StructureTemplate template;

	@Shadow
	protected BlockPos templatePosition;

	/** 是否已把船上的某个箱子改为星辉石专属表（船上两个箱子只改一个，不双刷） */
	@Unique
	private boolean createoreexpansion$shipChestLootSet;

	/** 放置前：追加末影船彩蛋处理器（postProcess 开头即 placeInWorld，HEAD 注入赶在前） */
	@Inject(method = "postProcess", at = @At("HEAD"))
	private void createoreexpansion$attachEndShipProcessor(WorldGenLevel level,
		StructureManager structureManager, ChunkGenerator generator, RandomSource random,
		BoundingBox box, ChunkPos chunkPos, BlockPos pos, CallbackInfo ci) {
		if (placeSettings == null || !"ship".equals(templateName))
			return;
		// 幂等：避免重复加载时多次添加
		for (StructureProcessor p : placeSettings.getProcessors())
			if (p instanceof EndShipStellarstoneProcessor)
				return;
		placeSettings.addProcessor(new EndShipStellarstoneProcessor());
	}

	/**
	 * 末影船鞘翅房箱子：postProcess RETURN 时原版已把所有 Chest 标记设为 end_city_treasure；
	 * 此处重新扫描，将船上第一个箱子覆盖为星辉石专属子表（2-4 保底，不双刷）。
	 * 普通末地城塔楼箱子不受影响（仍走原版表 + w=7 注入）。
	 */
	@Inject(method = "postProcess", at = @At("RETURN"))
	private void createoreexpansion$endShipChestLoot(WorldGenLevel level,
		StructureManager structureManager, ChunkGenerator generator, RandomSource random,
		BoundingBox box, ChunkPos chunkPos, BlockPos pos, CallbackInfo ci) {
		if (!"ship".equals(templateName) || createoreexpansion$shipChestLootSet)
			return;
		List<StructureTemplate.StructureBlockInfo> blocks =
			template.filterBlocks(templatePosition, placeSettings, Blocks.STRUCTURE_BLOCK);
		for (StructureTemplate.StructureBlockInfo info : blocks) {
			if (info.nbt() == null || !info.nbt().getString("metadata").startsWith("Chest"))
				continue;
			BlockPos chestPos = info.pos().below();
			if (!box.isInside(chestPos))
				continue;
			ResourceKey<LootTable> table = ResourceKey.create(
				net.minecraft.core.registries.Registries.LOOT_TABLE,
				ResourceLocation.fromNamespaceAndPath("createoreexpansion",
					"inject/chests/end_ship_stellarstone"));
			RandomizableContainer.setBlockEntityLootTable(level, random, chestPos, table);
			createoreexpansion$shipChestLootSet = true;
			return; // 只改一个箱子
		}
	}
}
