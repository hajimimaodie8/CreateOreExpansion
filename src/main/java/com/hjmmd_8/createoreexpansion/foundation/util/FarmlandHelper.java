package com.hjmmd_8.createoreexpansion.foundation.util;

import com.hjmmd_8.createoreexpansion.content.skill.config.HoeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 农田操作工具 —— 锄头技能的服务端逻辑统一入口。
 *
 * 包含：范围收集、一键收割重种、背包播种、批量施肥、犁地。
 * 所有方法仅应在服务端调用（调用方负责客户端/服务端分流）。
 */
public final class FarmlandHelper {

    private FarmlandHelper() {
    }

    // ========== 范围收集 ==========

    /**
     * 以中心方块为基准，收集技能作用范围内的全部方块位置。
     *
     * @param center 点击的方块
     * @param width  范围宽度（X 轴）
     * @param depth  范围深度（Z 轴）
     */
    public static List<BlockPos> collectArea(BlockPos center, int width, int depth) {
        int halfWidth = width / 2;
        int halfDepth = depth / 2;
        List<BlockPos> result = new ArrayList<>(width * depth);
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfDepth; dz <= halfDepth; dz++) {
                result.add(center.offset(dx, 0, dz));
            }
        }
        return result;
    }

    // ========== 目标判定（优先级解析） ==========

    /**
     * 判断目标方块属于哪个优先级分支。
     *
     * @return 优先级：1=成熟作物收割，2=未成熟作物施肥，3=耕地播种，4=可犁地，0=不可操作
     */
    public static int resolvePriority(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? 1 : 2;
        }
        if (state.is(Blocks.FARMLAND)) {
            return 3;
        }
        if (isTillable(state)) {
            return 4;
        }
        return 0;
    }

    // ========== 效果预览（扣费前确认，保证"有实际效果才扣费"） ==========

    /** 范围内是否存在至少一个成熟作物 */
    public static boolean hasHarvestable(Level level, List<BlockPos> area) {
        for (BlockPos pos : area) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                return true;
            }
        }
        return false;
    }

    /** 范围内是否存在至少一个可播种的耕地空位（耕地 + 上方空气 + 背包有种子） */
    public static boolean hasPlantableSpot(Level level, List<BlockPos> area, Player player) {
        for (BlockPos pos : area) {
            if (!level.getBlockState(pos).is(Blocks.FARMLAND)) continue;
            if (!level.getBlockState(pos.above()).isAir()) continue;
            if (findSeed(player) != null) return true;
        }
        return false;
    }

    /** 范围内是否存在至少一个可犁方块 */
    public static boolean hasTillable(Level level, List<BlockPos> area) {
        for (BlockPos pos : area) {
            if (isTillable(level.getBlockState(pos))) return true;
        }
        return false;
    }

    /** 范围内是否存在至少一株未成熟作物（可施肥目标） */
    public static boolean hasImmatureCrop(Level level, List<BlockPos> area) {
        for (BlockPos pos : area) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                return true;
            }
        }
        return false;
    }

    /** 背包中是否拥有骨粉 */
    public static boolean hasBonemeal(Player player) {
        return findBonemeal(player) != null;
    }

    // ========== 批量施肥（优先级 2） ==========

    /**
     * 对范围内<b>全部</b>未成熟作物批量施肥（消耗背包骨粉）。
     *
     * 逐株使用骨粉催熟，背包骨粉用完即停止。
     *
     * @return 实际施肥的作物数量
     */
    public static int fertilizeAll(Level level, List<BlockPos> area, Player player) {
        int fertilized = 0;
        for (BlockPos pos : area) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) continue;
            if (crop.isMaxAge(state)) continue; // 只施肥未成熟作物

            ItemStack bonemeal = findBonemeal(player);
            if (bonemeal == null || bonemeal.isEmpty()) break; // 骨粉用完停止

            if (fertilizeSingle(level, pos, player, bonemeal)) {
                if (!player.isCreative()) {
                    bonemeal.shrink(1); // 消耗一份骨粉
                }
                fertilized++;
            }
        }
        return fertilized;
    }

    /**
     * 对单株作物施骨粉。
     *
     * 原版骨粉催熟对作物按概率生长 1~2 个阶段；若目标已达到最大阶段或
     * 方块不可被催熟则返回 false（本方法调用前已过滤未成熟作物，正常恒为 true）。
     */
    private static boolean fertilizeSingle(Level level, BlockPos pos, Player player, ItemStack bonemeal) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) return false;
        if (!bonemealable.isValidBonemealTarget(level, pos, state)) return false;
        if (!bonemealable.isBonemealSuccess(level, player.getRandom(), pos, state)) {
            // 原版骨粉有小概率不生长（isBonemealSuccess 为 false 时无效果）
            return false;
        }
        bonemealable.performBonemeal((ServerLevel) level, player.getRandom(), pos, state);
        level.levelEvent(2005, pos, 0); // 骨粉粒子效果
        return true;
    }

    /**
     * 按背包 UI 顺序（0→35 格）查找骨粉。
     *
     * @return 骨粉物品，无骨粉时返回 null
     */
    private static ItemStack findBonemeal(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.BONE_MEAL)) return stack;
        }
        return null;
    }

    // ========== 一键收割 + 自动种植（优先级 1） ==========

    /**
     * 一键收割范围内<b>全部</b>成熟作物并自动种植。
     *
     * 步骤：
     * <ol>
     *     <li>收割范围内所有成熟作物：掉落中可种植的种子<b>收集起来用于补种</b>，
     *     其余掉落生成掉落物实体；</li>
     *     <li>原地重种：方块<b>必定重置</b>为 0 阶段（按配置概率催熟，但永不超过
     *     {@code maxAge-1}，杜绝甜菜根等作物无限产出）；</li>
     *     <li>范围内所有空耕地（耕地 + 上方空气）自动补种：<b>优先用本次收割收集的种子</b>，
     *     不够再取背包种子。</li>
     * </ol>
     *
     * @return 实际收割的作物数量
     */
    public static int harvestAndReplant(Level level, List<BlockPos> area, Player player, HoeConfig config) {
        // 1. 收割全部成熟作物，收集可种植种子用于自动补种
        List<ItemStack> harvestedSeeds = new ArrayList<>();
        int harvested = 0;
        for (BlockPos pos : area) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) continue;
            if (!crop.isMaxAge(state)) continue;

            // 收割：掉落中种子留下用于补种，其余生成掉落物
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos,
                    null, player, player.getMainHandItem());
            for (ItemStack drop : drops) {
                if (isPlantableSeed(drop)) {
                    harvestedSeeds.add(drop.copy());
                } else {
                    spawnItemEntity(level, pos, drop);
                }
            }

            // 额外掉落（按配置概率）
            if (config.extraDropChance > 0 && level.getRandom().nextFloat() < config.extraDropChance) {
                List<ItemStack> extra = Block.getDrops(state, (ServerLevel) level, pos,
                        null, player, player.getMainHandItem());
                if (!extra.isEmpty()) {
                    spawnItemEntity(level, pos, extra.getFirst().copy());
                }
            }

            // 原地重种：必定重置为 0 阶段（杜绝无限获取）；按配置概率催熟但不超过 maxAge-1
            // 注意：不能用硬编码 CropBlock.AGE——甜菜根等作物重定义了 AGE 属性（值域不同），
            // 写死会抛异常导致重置失败、无限获取。这里按属性名 "age" 从状态中查找，
            // 对小麦/甜菜根/任何 mod 作物均通用。
            IntegerProperty ageProperty = findAgeProperty(state);
            if (ageProperty == null) continue; // 无 age 属性（异常状态），跳过本次
            BlockState newState = state.setValue(ageProperty, 0);
            if (level.getRandom().nextFloat() < config.matureChance) {
                newState = state.setValue(ageProperty,
                        config.rollStage(level.getRandom(), crop.getMaxAge()));
            }
            level.setBlock(pos, newState, 3);
            harvested++;
        }

        // 2. 自动补种空耕地：优先用本次收割收集的种子，不够再取背包种子
        for (BlockPos pos : area) {
            if (!level.getBlockState(pos).is(Blocks.FARMLAND)) continue;
            if (!level.getBlockState(pos.above()).isAir()) continue;

            ItemStack seed = harvestedSeeds.isEmpty() ? findSeed(player) : harvestedSeeds.removeFirst();
            if (seed == null || seed.isEmpty()) continue;

            plantAt(level, pos, player, seed);
        }

        return harvested;
    }

    /** 在指定位置生成掉落物实体 */
    private static void spawnItemEntity(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /**
     * 从方块状态中按属性名 "age" 查找生长阶段属性。
     *
     * 不能用硬编码 {@code CropBlock.AGE}：甜菜根等作物重定义了同名属性
     * （值域不同），写死会在 setValue 时抛异常。按名字查找对任何作物均通用。
     *
     * @return 名为 "age" 的整型属性；不存在时返回 null
     */
    private static IntegerProperty findAgeProperty(BlockState state) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals("age") && property instanceof IntegerProperty age) {
                return age;
            }
        }
        return null;
    }

    // ========== 播种（优先级 1 补种 / 优先级 2） ==========

    /**
     * 在范围内所有耕地空位上，按背包顺序取可种植物品种植。
     *
     * @return 实际种下的作物数量
     */
    public static int plantAll(Level level, List<BlockPos> area, Player player) {
        int planted = 0;
        for (BlockPos pos : area) {
            if (!level.getBlockState(pos).is(Blocks.FARMLAND)) continue;
            BlockPos above = pos.above();
            if (!level.getBlockState(above).isAir()) continue;

            ItemStack seed = findSeed(player);
            if (seed == null) break;

            if (plantAt(level, pos, player, seed)) {
                planted++;
            }
        }
        return planted;
    }

    /** 在指定耕地上方种植一株作物，成功则消耗种子 */
    private static boolean plantAt(Level level, BlockPos farmlandPos, Player player, ItemStack seedItem) {
        BlockPos plantPos = farmlandPos.above();
        Vec3 hitVec = new Vec3(plantPos.getX() + 0.5, plantPos.getY(), plantPos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, net.minecraft.core.Direction.UP, farmlandPos, false);
        // 必须把种子物品显式传入 UseOnContext：3 参数构造器会取主手物品（锄头），导致种子放置逻辑失效
        UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, seedItem, hitResult);
        InteractionResult result = seedItem.useOn(context);
        return result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME;
    }

    /**
     * 按背包 UI 顺序（0→35 格）查找第一个可种植的种子。
     * 仅识别原版种子 tag 与作物方块物品，避免把泥土块等"种"到耕地上。
     *
     * @return 种子物品，无可用种子时返回 null
     */
    private static ItemStack findSeed(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isPlantableSeed(stack)) return stack;
        }
        return null;
    }

    /** 判断物品是否是可种植的种子（原版种子 tag 或作物方块物品） */
    private static boolean isPlantableSeed(ItemStack stack) {
        if (stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
            return true;
        }
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof CropBlock;
    }

    // ========== 犁地（优先级 4） ==========

    /**
     * 将范围内所有可犁方块（泥土/草地/土径等）转换为耕地。
     *
     * 直接设置耕地方块状态：避免逐格调用锄头 useOn 导致音效/粒子重复播放 25 次。
     *
     * @return 实际犁地的数量
     */
    public static int tillAll(Level level, List<BlockPos> area) {
        int tilled = 0;
        for (BlockPos pos : area) {
            if (isTillable(level.getBlockState(pos))) {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
                tilled++;
            }
        }
        return tilled;
    }

    /** 判断方块是否可被犁成耕地 */
    private static boolean isTillable(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.COARSE_DIRT);
    }
}
