package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class AllTags {

	public enum AllItemTags {
		RODS("rods"),
		RODS_ALL_METAL("rods/all_metal"),
		WIRES("wires"),
		WIRES_ALL_METAL("wires/all_metal"),
		/** 有技能的工具（减耗/技能提升附魔可附） */
		SKILL_TOOLS(CreateOreExpansion.modLoc("skill_tools")),
		/** 释放技能有冷却的工具（迅启附魔可附） */
		COOLDOWN_TOOLS(CreateOreExpansion.modLoc("cooldown_tools"));

		public final TagKey<Item> tag;

		AllItemTags(String path) {
			this.tag = TagKey.create(Registries.ITEM,
				ResourceLocation.fromNamespaceAndPath("c", path));
		}

		AllItemTags(ResourceLocation id) {
			this.tag = TagKey.create(Registries.ITEM, id);
		}
	}

	public enum AllFluidTags {
		FAN_PROCESSING_CATALYSTS_TRANSMUTING("fan_processing_catalysts/transmuting");

		public final TagKey<Fluid> tag;

		AllFluidTags(String path) {
			this.tag = TagKey.create(Registries.FLUID,
				ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, path));
		}

		@SuppressWarnings("deprecation")
		public boolean matches(Fluid fluid) {
			return fluid.is(tag);
		}

		public boolean matches(FluidState state) {
			return state.is(tag);
		}
	}

}
