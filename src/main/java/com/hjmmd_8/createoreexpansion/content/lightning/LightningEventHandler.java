package com.hjmmd_8.createoreexpansion.content.lightning;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class LightningEventHandler {

    private static final String LIGHTNING_PROCESSED_TAG = "LightningProcessed";
    private static final int DEPOT_MAX_ITEMS = 4;
    private static final int LIGHTNING_DETECT_RANGE = 1;

    /** 闪电落地效果（原版火焰）之后才执行的方块转化任务，避免转化结果被火焰覆盖 */
    private static final Map<UUID, PendingBlockTransform> PENDING_BLOCK_TRANSFORMS = new HashMap<>();

    private record PendingBlockTransform(BlockPos pos, BlockState targetState, List<ItemStack> extraItems) {
    }

    /** 闪电加工的物品输入来源（掉落物 / 置物台 / 弹射置物台 / 工作盆槽位） */
    private interface LightningSource {
        ItemStack getStack();

        void consume(int count);
    }

    private record EntitySource(ItemEntity entity) implements LightningSource {
        @Override
        public ItemStack getStack() {
            return entity.getItem();
        }

        @Override
        public void consume(int count) {
            ItemStack stack = entity.getItem();
            stack.shrink(count);
            if (stack.isEmpty())
                entity.discard();
        }
    }

    private record DepotSource(DepotBehaviour depot) implements LightningSource {
        @Override
        public ItemStack getStack() {
            return depot.getHeldItemStack();
        }

        @Override
        public void consume(int count) {
            depot.removeHeldItem();
            depot.blockEntity.notifyUpdate();
        }
    }

    private record BasinSource(BasinBlockEntity basin, int slot) implements LightningSource {
        @Override
        public ItemStack getStack() {
            return basin.getInputInventory().getStackInSlot(slot);
        }

        @Override
        public void consume(int count) {
            basin.getInputInventory().extractItem(slot, count, false);
            basin.notifyUpdate();
        }
    }

    private LightningEventHandler() {
    }

    @SubscribeEvent
    public static void onLightningStrike(EntityStruckByLightningEvent event) {
        // 兜底：未被闪电落地统一加工覆盖的单个掉落物（已被加工过的会取消事件，避免被闪电伤害）
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            handleItemEntityStrike(event, itemEntity);
        }
    }

    @SubscribeEvent
    public static void onLightningBoltTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LightningBolt bolt))
            return;
        if (entity.level().isClientSide)
            return;
        // LightningBolt 生成时即定位到落点，首个 tick 即原版落地效果 tick（thunderHit / 火焰所在 tick）。
        // 1.21.1 中 bolt.life 为 private、无 isVisualOnly() 公共方法，故用实体持久数据标记去重。
        if (bolt.getPersistentData().getBoolean(LIGHTNING_PROCESSED_TAG))
            return;
        bolt.getPersistentData().putBoolean(LIGHTNING_PROCESSED_TAG, true);

        handleLightningBoltStrike((ServerLevel) bolt.level(), bolt);
    }

    @SubscribeEvent
    public static void onLightningBoltPostTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LightningBolt bolt))
            return;
        if (entity.level().isClientSide)
            return;

        PendingBlockTransform pending = PENDING_BLOCK_TRANSFORMS.remove(bolt.getUUID());
        if (pending == null)
            return;
        // 此刻原版火焰已生成于落点（若可见天空），直接 setBlock 覆盖火焰完成转化
        ServerLevel level = (ServerLevel) bolt.level();

        if (pending.targetState() != null) {
            level.setBlock(pending.pos(), pending.targetState(), 3);
        } else {
            level.destroyBlock(pending.pos(), false);
        }

        Vec3 particlePos = Vec3.atCenterOf(pending.pos());
        for (ItemStack extra : pending.extraItems()) {
            spawnProtectedItem(level, particlePos, extra);
        }
    }

    private static void handleItemEntityStrike(EntityStruckByLightningEvent event, ItemEntity itemEntity) {
        if (itemEntity.level().isClientSide)
            return;

        if (itemEntity.getPersistentData().getBoolean(LIGHTNING_PROCESSED_TAG)) {
            event.setCanceled(true);
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty())
            return;

        ServerLevel level = (ServerLevel) itemEntity.level();

        var recipeOpt = AllRecipeTypes.LIGHTNING.find(LightningInput.of(stack.copyWithCount(1)), level);
        if (recipeOpt.isEmpty())
            return;

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

    /**
     * 闪电落地统一加工：收集落点周围所有输入来源（掉落物 / 置物台 / 弹射置物台 / 工作盆），
     * 组装多槽 LightningInput 循环匹配配方（多输入配方优先），逐次消耗材料并产出。
     */
    private static void handleLightningBoltStrike(ServerLevel level, LightningBolt bolt) {
        BlockPos centerPos = bolt.blockPosition();
        Vec3 centerVec = Vec3.atCenterOf(centerPos);
        int range = LIGHTNING_DETECT_RANGE;

        // ---- 收集输入来源 ----
        List<LightningSource> sources = new ArrayList<>();
        List<DepotBehaviour> depots = new ArrayList<>();
        List<BasinBlockEntity> basins = new ArrayList<>();

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(centerPos).inflate(range))) {
            if (itemEntity.getPersistentData().getBoolean(LIGHTNING_PROCESSED_TAG))
                continue;
            if (itemEntity.getItem().isEmpty())
                continue;
            sources.add(new EntitySource(itemEntity));
        }

        for (BlockPos pos : BlockPos.betweenClosed(centerPos.offset(-range, -range, -range),
            centerPos.offset(range, range, range))) {
            DepotBehaviour depot = BlockEntityBehaviour.get(level, pos, DepotBehaviour.TYPE);
            if (depot != null && !depot.getHeldItemStack().isEmpty()) {
                sources.add(new DepotSource(depot));
                depots.add(depot);
            }
            if (level.getBlockEntity(pos) instanceof BasinBlockEntity basin) {
                basins.add(basin);
                var inputInv = basin.getInputInventory();
                for (int i = 0; i < inputInv.getSlots(); i++) {
                    if (!inputInv.getStackInSlot(i).isEmpty())
                        sources.add(new BasinSource(basin, i));
                }
            }
        }

        if (sources.isEmpty())
            return;

        // ---- 循环匹配加工（多输入配方优先），直到无配方可匹配 ----
        List<ItemStack> outputs = new ArrayList<>();
        boolean processed = false;
        List<RecipeHolder<LightningRecipe>> allRecipes =
            level.getRecipeManager().getAllRecipesFor(AllRecipeTypes.LIGHTNING.getType());

        while (true) {
            LightningInput input =
                new LightningInput(sources.stream().map(LightningSource::getStack).toList());
            if (input.isEmpty())
                break;

            LightningRecipe best = null;
            int bestIngredientCount = -1;
            for (RecipeHolder<LightningRecipe> holder : allRecipes) {
                LightningRecipe recipe = holder.value();
                if (recipe.matches(input, level) && recipe.getIngredients().size() > bestIngredientCount) {
                    best = recipe;
                    bestIngredientCount = recipe.getIngredients().size();
                }
            }
            if (best == null)
                break;

            // 消耗：每个 ingredient 从来源中扣除 1 个匹配物品
            for (Ingredient ingredient : best.getIngredients()) {
                for (LightningSource source : sources) {
                    ItemStack stack = source.getStack();
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        source.consume(1);
                        break;
                    }
                }
            }

            for (ItemStack result : best.rollResults(level.random)) {
                mergeIntoList(outputs, result);
            }
            processed = true;
        }

        if (!processed)
            return;

        spawnWhiteBurstParticles(level, centerVec);

        // ---- 产出分发 ----
        // 单个置物台：首个结果写回置物台（其余结果掉落）
        if (depots.size() == 1 && depots.get(0).blockEntity instanceof DepotBlockEntity depotBE
            && !outputs.isEmpty()) {
            depotBE.setHeldItem(outputs.get(0).copy());
            depotBE.notifyUpdate();
            for (int i = 1; i < outputs.size(); i++) {
                spawnProtectedItem(level, centerVec, outputs.get(i).copy());
            }
            return;
        }
        // 有工作盆：输出交给工作盆（acceptOutputs 内部处理 allowInsertion/溢出导出，渲染正常），放不下则掉落
        if (!basins.isEmpty()) {
            BasinBlockEntity basin = basins.get(0);
            if (basin.acceptOutputs(outputs, List.of(), true)) {
                basin.acceptOutputs(outputs, List.of(), false);
            } else {
                for (ItemStack output : outputs) {
                    spawnProtectedItem(level, centerVec, output.copy());
                }
            }
            basin.notifyUpdate();
            return;
        }
        // 其余：全部掉落
        for (ItemStack output : outputs) {
            spawnProtectedItem(level, centerVec, output.copy());
        }
    }

    /**
     * 方块闪电配方匹配与结果计算。只做匹配与计算、不立即 setBlock：
     * 原版闪电会在本 tick 稍后于落点生成火焰，立即转化会被火焰覆盖，
     * 因此把结果暂存，由 onLightningBoltPostTick（火焰生成后）执行。
     */
    private static PendingBlockTransform prepareBlockTransform(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir())
            return null;

        ItemStack blockAsItem = state.getBlock().asItem().getDefaultInstance();
        var recipeOpt = AllRecipeTypes.LIGHTNING_BLOCK.find(new SingleRecipeInput(blockAsItem), level);
        if (recipeOpt.isEmpty())
            return null;

        Vec3 particlePos = Vec3.atCenterOf(pos);
        spawnWhiteBurstParticles(level, particlePos);

        List<ItemStack> results = RecipeApplier.applyRecipeOn(level, blockAsItem, recipeOpt.get().value(), true);
        if (results.isEmpty())
            return null;

        List<ItemStack> extras = new ArrayList<>();
        ItemStack firstResult = results.get(0);
        Block resultBlock = Block.byItem(firstResult.getItem());
        BlockState targetState = resultBlock != net.minecraft.world.level.block.Blocks.AIR
            ? resultBlock.defaultBlockState()
            : null;

        if (resultBlock != net.minecraft.world.level.block.Blocks.AIR) {
            if (firstResult.getCount() > 1) {
                ItemStack remainder = firstResult.copy();
                remainder.shrink(1);
                extras.add(remainder);
            }
        } else {
            extras.add(firstResult.copy());
        }

        for (int i = 1; i < results.size(); i++) {
            extras.add(results.get(i).copy());
        }

        return new PendingBlockTransform(pos, targetState, extras);
    }

    private static List<ItemStack> processBatch(ServerLevel level, ItemStack inputStack,
        Recipe<? extends RecipeInput> recipe) {
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

                if (toAdd.isEmpty())
                    return;
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
