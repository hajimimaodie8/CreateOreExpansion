package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.helper.*;
import com.hjmmd_8.createoreexpansion.foundation.item.ItemSkill;
import com.hjmmd_8.createoreexpansion.common.AllMyItems;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class SkillsHandler {
    private static final Map<Item, ItemSkill> skills = new HashMap<>();

    public static void registerSkill(Item item, ItemSkill skill) {
        skills.put(item, skill);
    }

    public static void registerSkill(ItemEntry<?> item, ItemSkill skill) {
        registerSkill(item.get(), skill);
    }

    public static void loadSkills() {
        registerSkill(AllMyItems.JADE_AXE, JadeAxeAoeHelper::causeAoe);
        registerSkill(AllMyItems.JADE_PICKAXE, JadePickaxeAoeHelper::causeAoe);
        registerSkill(AllMyItems.JADE_SHOVEL, JadeShovelAoeHelper::causeAoe);

        registerSkill(AllMyItems.TOPAZ_AXE, TopazAxeAoeHelper::causeAoe);
        registerSkill(AllMyItems.TOPAZ_PICKAXE, TopazPickaxeAoeHelper::causeAoe);
        registerSkill(AllMyItems.TOPAZ_SHOVEL, TopazShovelAoeHelper::causeAoe);

        registerSkill(AllMyItems.SAPPHIRE_AXE, SapphireAxeAoeHelper::causeAoe);
        registerSkill(AllMyItems.SAPPHIRE_PICKAXE, SapphirePickaxeAoeHelper::causeAoe);
        registerSkill(AllMyItems.SAPPHIRE_SHOVEL, SapphireShovelAoeHelper::causeAoe);
    }

    public static boolean hasSkill(Item item) {
        return skills.containsKey(item);
    }

    public static ItemSkill getSkill(Item item) {
        return skills.get(item);
    }

    public static boolean hasSkill(ItemStack stack) {
        return hasSkill(stack.getItem());
    }

    public static ItemSkill getSkill(ItemStack stack) {
        return getSkill(stack.getItem());
    }

    @SubscribeEvent
    public static void onSetup(final FMLCommonSetupEvent event) {
        loadSkills();
    }
}
