package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

import net.minecraft.resources.ResourceLocation;

public class AllSpriteShifts {
    public static final CTSpriteShiftEntry JADE_CASING = omni("jade_casing");

    private static CTSpriteShiftEntry omni(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "block/" + name),
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "block/" + name + "_connected"));
    }

    public static void register() {}
}
