package com.hjmmd_8.createoreexpansion.content.lightning;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 闪电的<b>落点加工服务</b>：收集落点周围的全部输入来源、循环匹配闪电配方、结算产物并分发。
 *
 * <p>本类只做"加工"，不关心事件生命周期——{@code LightningEventHandler} 负责事件分发与
 * 时序（例如方块转化要推迟到原版火焰生成之后），加工规则全部集中在这里，便于单独推理与验证。</p>
 *
 * <p><b>候选配方</b>：本模组 {@code lightning} 配方 + 其它模组顺带适用的充电配方
 * （经 compat 门面，核心层不出现任何可选模组的类型）。</p>
 */
public final class LightningStrikeProcessor {

    /** 已被闪电加工过的实体/物品标记（避免同一次雷击被重复处理）。 */
    public static final String LIGHTNING_PROCESSED_TAG = "LightningProcessed";

    /** 落点输入来源的扫描范围（格）。 */
    public static final int LIGHTNING_DETECT_RANGE = 1;

    private LightningStrikeProcessor() {
    }

    // ===== 输入来源（掉落物 / 置物台 / 弹射置物台 / 工作盆槽位） =====

    /** 闪电加工的物品输入来源。 */
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

    /**
     * 闪电落地统一加工：收集落点周围所有输入来源（掉落物 / 置物台 / 弹射置物台 / 工作盆），
     * 组装多槽 LightningInput 循环匹配配方（多输入配方优先），逐次消耗材料并产出。
     */
    public static void processBoltStrike(ServerLevel level, LightningBolt bolt) {
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
        // 候选配方：本模组 lightning 配方 + 其它模组顺带适用的充电配方（经 compat 门面，核心层无硬编码）
        List<RecipeHolder<? extends Recipe<?>>> allRecipes = new ArrayList<>(
            level.getRecipeManager().getAllRecipesFor(AllRecipeTypes.LIGHTNING.getType()));
        com.hjmmd_8.createoreexpansion.compat.createaddition.CreateAdditionTransmuterSupport
            .addChargingRecipes(level, allRecipes);

        while (true) {
            LightningInput input =
                new LightningInput(sources.stream().map(LightningSource::getStack).toList());
            if (input.isEmpty())
                break;

            Recipe<?> best = null;
            int bestIngredientCount = -1;
            for (RecipeHolder<? extends Recipe<?>> holder : allRecipes) {
                Recipe<?> recipe = holder.value();
                int ingredientCount = recipe.getIngredients().size();
                if (ingredientCount > bestIngredientCount && matchesLightning(recipe, input, level)) {
                    best = recipe;
                    bestIngredientCount = ingredientCount;
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

            if (best instanceof LightningRecipe lightningRecipe) {
                for (ItemStack result : lightningRecipe.rollResults(level.random)) {
                    mergeIntoList(outputs, result);
                }
            } else {
                // 其余（充电类）配方：产物推导交给 compat 门面（非该类返回空表）
                for (ItemStack result : com.hjmmd_8.createoreexpansion.compat.createaddition.CreateAdditionTransmuterSupport
                    .rollChargingResults(best, level.random)) {
                    mergeIntoList(outputs, result);
                }
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
     * 统一匹配闪电输入：本模组配方走自身 {@code matches}；其余（如 CC&amp;A 充电配方）交给
     * compat 门面按贪心多槽匹配（每个 ingredient 需在输入池中找到可消耗物品）。
     *
     * <p>输入池在此按"逐槽复制"准备——门面匹配时会递减池内物品，不会动到世界里的真实物品。</p>
     */
    private static boolean matchesLightning(Recipe<?> recipe, LightningInput input, Level level) {
        if (recipe instanceof LightningRecipe lightningRecipe)
            return lightningRecipe.matches(input, level);
        if (input.isEmpty())
            return false;
        List<ItemStack> pool = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty())
                pool.add(stack.copy());
        }
        return com.hjmmd_8.createoreexpansion.compat.createaddition.CreateAdditionTransmuterSupport
            .matchesChargingRecipe(recipe, pool);
    }

    /** 按输入堆叠数逐件套用配方并合并产物（掉落物路径的"批量加工"）。 */
    public static List<ItemStack> processBatch(ServerLevel level, ItemStack inputStack,
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

    /** 把一件产物并入结果表：先尝试并入同种同组件的既有堆，放不下再按最大堆拆分新增。 */
    public static void mergeIntoList(List<ItemStack> list, ItemStack toAdd) {
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

    /** 生成一件"已受雷击加工"标记的掉落物（不会被同一次雷击二次加工）。 */
    public static void spawnProtectedItem(ServerLevel level, Vec3 pos, ItemStack stack) {
        ItemEntity outputEntity = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, stack);
        outputEntity.setDefaultPickUpDelay();
        outputEntity.getPersistentData().putBoolean(LIGHTNING_PROCESSED_TAG, true);
        level.addFreshEntity(outputEntity);
    }

    /** 落点白色爆散粒子（烟花 + 末地烛 + 电火花）。 */
    public static void spawnWhiteBurstParticles(ServerLevel level, Vec3 pos) {
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
