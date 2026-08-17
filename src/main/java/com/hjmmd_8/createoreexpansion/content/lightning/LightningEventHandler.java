package com.hjmmd_8.createoreexpansion.content.lightning;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class LightningEventHandler {
    
    private static final String LIGHTNING_PROCESSED_TAG = "LightningProcessed";
    private static final int DEPOT_MAX_ITEMS = 4;
    private static final int LIGHTNING_DETECT_RANGE = 1;

    private LightningEventHandler() {
    }

    @SubscribeEvent
    public static void onLightningStrike(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            handleItemEntityStrike(event, itemEntity);
        } else if (event.getEntity() instanceof LightningBolt bolt) {
            handleLightningBoltStrike(event, bolt);
        }
    }

    private static void handleItemEntityStrike(EntityStruckByLightningEvent event, ItemEntity itemEntity) {
        if (itemEntity.level().isClientSide) return;
        
        if (itemEntity.getPersistentData().getBoolean(LIGHTNING_PROCESSED_TAG)) {
            event.setCanceled(true);
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;

        ServerLevel level = (ServerLevel) itemEntity.level();
        
        ItemStack singleItem = stack.copyWithCount(1);
        var recipeOpt = AllRecipeTypes.LIGHTNING.find(new SingleRecipeInput(singleItem), level);
        if (recipeOpt.isEmpty()) return;
        
        event.setCanceled(true);
        itemEntity.discard();

        Vec3 pos = itemEntity.position();
        spawnWhiteBurstParticles(level, pos);

        List<ItemStack> allResults = processBatch(level, stack, recipeOpt.get().value());
        allResults.forEach(result -> {
            ItemEntity outputEntity = new ItemEntity(level, pos.x, pos.y, pos.z, result.copy());
            outputEntity.setDefaultPickUpDelay();
            outputEntity.getPersistentData().putBoolean(LIGHTNING_PROCESSED_TAG, true);
            level.addFreshEntity(outputEntity);
        });
    }

    private static void handleLightningBoltStrike(EntityStruckByLightningEvent event, LightningBolt bolt) {
        if (bolt.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) bolt.level();
        BlockPos centerPos = bolt.blockPosition();
        
        boolean depotHandled = false;
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset(-LIGHTNING_DETECT_RANGE, -LIGHTNING_DETECT_RANGE, -LIGHTNING_DETECT_RANGE),
                centerPos.offset(LIGHTNING_DETECT_RANGE, LIGHTNING_DETECT_RANGE, LIGHTNING_DETECT_RANGE))) {
            if (level.getBlockEntity(pos) instanceof DepotBlockEntity depot) {
                handleDepotStrike(event, level, pos, depot);
                depotHandled = true;
            }
        }
        
        if (!depotHandled) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    centerPos.offset(-LIGHTNING_DETECT_RANGE, -LIGHTNING_DETECT_RANGE, -LIGHTNING_DETECT_RANGE),
                    centerPos.offset(LIGHTNING_DETECT_RANGE, LIGHTNING_DETECT_RANGE, LIGHTNING_DETECT_RANGE))) {
                if (handleBlockStrike(event, level, pos)) {
                    break;
                }
            }
        }
    }

    private static void handleDepotStrike(EntityStruckByLightningEvent event, ServerLevel level, BlockPos pos, DepotBlockEntity depot) {
        ItemStack heldItem = depot.getHeldItem();
        if (heldItem.isEmpty()) return;

        ItemStack singleItem = heldItem.copyWithCount(1);
        var recipeOpt = AllRecipeTypes.LIGHTNING.find(new SingleRecipeInput(singleItem), level);
        if (recipeOpt.isEmpty()) return;

        event.setCanceled(true);

        Vec3 particlePos = Vec3.atCenterOf(pos);
        spawnWhiteBurstParticles(level, particlePos);

        List<ItemStack> allResults = processBatch(level, heldItem, recipeOpt.get().value());
        
        depot.setHeldItem(ItemStack.EMPTY);
        
        if (!allResults.isEmpty()) {
            depot.setHeldItem(allResults.get(0).copy());
            
            for (int i = 1; i < allResults.size(); i++) {
                spawnProtectedItem(level, particlePos, allResults.get(i).copy());
            }
        }
    }

    private static boolean handleBlockStrike(EntityStruckByLightningEvent event, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        ItemStack blockAsItem = state.getBlock().asItem().getDefaultInstance();
        var recipeOpt = AllRecipeTypes.LIGHTNING_BLOCK.find(new SingleRecipeInput(blockAsItem), level);
        if (recipeOpt.isEmpty()) return false;

        event.setCanceled(true);

        Vec3 particlePos = Vec3.atCenterOf(pos);
        spawnWhiteBurstParticles(level, particlePos);

        List<ItemStack> results = RecipeApplier.applyRecipeOn(level, blockAsItem, recipeOpt.get().value(), true);
        
        boolean blockPlaced = false;
        
        if (!results.isEmpty()) {
            ItemStack firstResult = results.get(0);
            Block resultBlock = Block.byItem(firstResult.getItem());
            
            if (resultBlock != net.minecraft.world.level.block.Blocks.AIR) {
                level.setBlock(pos, resultBlock.defaultBlockState(), 3);
                blockPlaced = true;
                
                if (firstResult.getCount() > 1) {
                    ItemStack remainder = firstResult.copy();
                    remainder.shrink(1);
                    spawnProtectedItem(level, particlePos, remainder);
                }
            } else {
                spawnProtectedItem(level, particlePos, firstResult.copy());
            }
            
            for (int i = 1; i < results.size(); i++) {
                spawnProtectedItem(level, particlePos, results.get(i).copy());
            }
        }
        
        if (!blockPlaced) {
            level.destroyBlock(pos, false);
        }
        
        return true;
    }

    private static List<ItemStack> processBatch(ServerLevel level, ItemStack inputStack, Recipe<? extends RecipeInput> recipe) {
        int inputCount = inputStack.getCount();
        List<ItemStack> combinedResults = new ArrayList<>();
        
        for (int i = 0; i < inputCount; i++) {
            ItemStack singleInput = inputStack.copyWithCount(1);
            List<ItemStack> batchResults = RecipeApplier.applyRecipeOn(level, singleInput, recipe, true);
            
            for (ItemStack result : batchResults) {
                mergeIntoList(combinedResults, result);
            }
        }
        
        return combinedResults;
    }

    private static void mergeIntoList(List<ItemStack> list, ItemStack toAdd) {
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, toAdd)) {
                int maxStack = existing.getMaxStackSize();
                int canAdd = Math.min(toAdd.getCount(), maxStack - existing.getCount());
                
                if (canAdd > 0) {
                    existing.grow(canAdd);
                    toAdd.shrink(canAdd);
                }
                
                if (toAdd.isEmpty()) return;
            }
        }
        
        while (!toAdd.isEmpty()) {
            int splitSize = Math.min(toAdd.getCount(), toAdd.getMaxStackSize());
            list.add(toAdd.copyWithCount(splitSize));
            toAdd.shrink(splitSize);
        }
    }

    private static void spawnProtectedItem(ServerLevel level, Vec3 pos, ItemStack stack) {
        ItemEntity outputEntity = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, stack);
        outputEntity.setDefaultPickUpDelay();
        outputEntity.getPersistentData().putBoolean(LIGHTNING_PROCESSED_TAG, true);
        level.addFreshEntity(outputEntity);
    }

    private static void spawnWhiteBurstParticles(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 30; i++) {
            double angle = Math.random() * Math.PI * 2;
            double pitch = Math.random() * Math.PI - Math.PI / 2;
            
            double speed = 0.2 + Math.random() * 0.3;
            double vx = Math.cos(angle) * Math.cos(pitch) * speed;
            double vy = Math.sin(pitch) * speed;
            double vz = Math.sin(angle) * Math.cos(pitch) * speed;

            level.sendParticles(ParticleTypes.FIREWORK,
                pos.x, pos.y + 0.5, pos.z,
                1, 0, 0, 0, 0);
            
            level.sendParticles(ParticleTypes.END_ROD,
                pos.x, pos.y + 0.5, pos.z,
                1, vx, vy, vz, 0.1);
        }
        
        for (int i = 0; i < 10; i++) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x + (Math.random() - 0.5) * 0.5,
                pos.y + 0.5,
                pos.z + (Math.random() - 0.5) * 0.5,
                1, 0, 0.2, 0, 0);
        }
    }

}