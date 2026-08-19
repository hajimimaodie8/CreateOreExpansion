package com.hjmmd_8.createoreexpansion.content.equipment.medallion;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * 凝能佩与能量工具的绑定合成：
 * 物品栏中把凝能佩拖到工具上（或反过来）合成，工具保留自身数据并写入绑定组件。
 */
public class MedallionBindingRecipe extends CustomRecipe {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateOreExpansion.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MedallionBindingRecipe>> SERIALIZER =
        SERIALIZERS.register("medallion_binding", () -> new Serializer());

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    public MedallionBindingRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * 匹配两种模式：
     * 绑定：恰好 1 个凝能佩 + 1 个能量工具；
     * 解绑：仅 1 个已绑定的能量工具（再次合成移除绑定）。
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack medallion = ItemStack.EMPTY;
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof IMedallion) {
                if (!medallion.isEmpty())
                    return false;
                medallion = stack;
            } else if (ToolEnergy.hasEnergy(stack)) {
                if (!tool.isEmpty())
                    return false;
                tool = stack;
            } else {
                return false;
            }
        }
        if (tool.isEmpty())
            return !medallion.isEmpty();
        // 绑定（佩+工具）或解绑（单个已绑定工具）
        return !medallion.isEmpty() || tool.has(AllDataComponents.BOUND_MEDALLION);
    }

    /**
     * 绑定/解绑切换（toggle）：工具未绑定 → 写入绑定组件；工具已绑定 → 移除绑定组件。
     * 佩在两种情况下都返还（不消耗），工具本身作为输出（不消耗）。
     */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack medallion = ItemStack.EMPTY;
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof IMedallion) {
                medallion = stack;
            } else if (ToolEnergy.hasEnergy(stack)) {
                tool = stack;
            }
        }
        if (tool.isEmpty())
            return ItemStack.EMPTY;

        if (tool.isEmpty()) {
            // 仅佩：解绑该佩对所有工具的绑定（输出佩副本，清空绑定列表）
            if (medallion.isEmpty())
                return ItemStack.EMPTY;
            ItemStack result = medallion.copy();
            result.set(AllDataComponents.BOUND_TOOL, List.of());
            return result;
        }

        ItemStack result = tool.copy();
        if (tool.has(AllDataComponents.BOUND_MEDALLION) && medallion.isEmpty()) {
            // 已绑定 + 无佩 → 解绑（不依赖原佩）
            result.remove(AllDataComponents.BOUND_MEDALLION);
            return result;
        }
        // 有佩：未绑定 → 绑定；已绑定 → 换绑到新佩（一个工具一次只能绑定一枚佩）
        ResourceLocation medallionId = medallion.getItemHolder()
            .unwrapKey()
            .map(key -> key.location())
            .orElse(null);
        if (medallionId != null) {
            result.set(AllDataComponents.BOUND_MEDALLION, medallionId);
        }
        // 充能模式：绑定瞬间工具能量为空 → 佩补满（佩返还，仅扣佩内能量）
        if (medallion.getItem() instanceof IMedallion im && im.isChargeMode(medallion)
            && ToolEnergy.getEnergy(result) == 0) {
            ToolEnergy.setEnergy(result, ToolEnergy.getMaxEnergy(result));
        }
        return result;
    }

    /**
     * 合成不消耗：绑定/换绑时凝能佩留在原合成格（返还，并记录绑定的工具 id），工具作为输出格产物；
     * 解绑模式下工具是输出格产物，无剩余物品。
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (ToolEnergy.hasEnergy(stack)) {
                tool = stack;
            }
        }
        if (tool.isEmpty())
            return remaining; // 仅佩：佩是输出（assemble 返回清绑定佩），无剩余物品
        // 有工具：佩返还并记录本次绑定的工具（去重，一枚佩可绑定多个工具）
        ResourceLocation toolId = tool.getItemHolder()
            .unwrapKey()
            .map(key -> key.location())
            .orElse(null);
        if (toolId != null) {
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (stack.getItem() instanceof IMedallion) {
                    ItemStack returned = stack.copy();
                    List<ResourceLocation> tools = new ArrayList<>(
                        returned.getOrDefault(AllDataComponents.BOUND_TOOL, List.of()));
                    if (!tools.contains(toolId)) {
                        tools.add(toolId);
                    }
                    returned.set(AllDataComponents.BOUND_TOOL, tools);
                    remaining.set(i, returned);
                }
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    /** 无 JSON 字段的固定配方（匹配/合成逻辑全在代码内） */
    public static class Serializer implements RecipeSerializer<MedallionBindingRecipe> {
        @Override
        public MapCodec<MedallionBindingRecipe> codec() {
            return MapCodec.unit(() -> new MedallionBindingRecipe(CraftingBookCategory.MISC));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MedallionBindingRecipe> streamCodec() {
            return StreamCodec.of((buf, recipe) -> {
            }, buf -> new MedallionBindingRecipe(CraftingBookCategory.MISC));
        }
    }
}