package com.hjmmd_8.createoreexpansion.content.lightning;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.recipe.RecipeAutomation;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.energyfield.ChargeApi;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.hjmmd_8.createoreexpansion.content.lightning.recipe.LightningInput;

@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class LightningEventHandler {

    /** 闪电落地效果（原版火焰）之后才执行的方块转化任务，避免转化结果被火焰覆盖 */
    private static final Map<UUID, PendingBlockTransform> PENDING_BLOCK_TRANSFORMS = new HashMap<>();

    private record PendingBlockTransform(BlockPos pos, BlockState targetState, List<ItemStack> extraItems) {
    }

    private LightningEventHandler() {
    }

    @SubscribeEvent
    public static void onLightningStrike(EntityStruckByLightningEvent event) {
        // 兜底：未被闪电落地统一加工覆盖的单个掉落物（已被加工过的会取消事件，避免被闪电伤害）
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            handleItemEntityStrike(event, itemEntity);
        } else if (event.getEntity() instanceof LivingEntity living) {
            // ★ coe-charge 批 5（需求 §3.2 #1）：**被雷电击中的生物**随机获得一种电荷。
            //   刻意用 else-if 追加：上面那条 ItemEntity 分支（掉落物雷击加工）逐字未动，
            //   而 ItemEntity 本来就不是 LivingEntity ⇒ 两条分支互斥、谁也不挡谁。
            //   「同一次雷击只施加一次」由事件本身保证：EntityStruckByLightningEvent 每次雷击
            //   对每个被击中的实体只发一次（限频不需要额外的计时器）。
            applyLightningCharge(living);
        }
    }

    /**
     * ★ coe-charge 批 5（需求 §3.2 #1）：被雷击 ⇒ <b>随机极性、等级
     * {@link ChargeConfigs#LIGHTNING_LEVEL}、时长 {@link ChargeConfigs#durationTicks(int)}</b>。
     *
     * <p>施加一律走 {@link ChargeApi}（本模组四条获得途径共用同一个施加面）——这里
     * <b>不</b>自己构造 {@code MobEffectInstance}、也不认那两个 effect 类：随机极性的口径
     * （{@code entity.getRandom()}）、同极合并、异极中和全部由门面负责。</p>
     *
     * <p>与 {@code MedallionEffectHandler#onStruckByLightning} 的关系：雷鸣合金系列被雷击时
     * 那个处理器会 {@code setCanceled(true)}（豁免销毁 + 补满能量），但事件取消<b>不会</b>
     * 阻止其它监听器继续收到本事件 ⇒ 雷鸣系列玩家/生物被雷击时<b>照样染电</b>（这是需求
     * §3.2 #1 的字面口径：被雷击就给，与是不是雷鸣系列无关）。</p>
     */
    private static void applyLightningCharge(LivingEntity living) {
        if (living.level().isClientSide) {
            return; // 服务端权威（效果、扣血、爆炸都只在服务端结算）
        }
        ChargeApi.applyRandom(living, ChargeConfigs.LIGHTNING_LEVEL,
            ChargeConfigs.durationTicks(ChargeConfigs.LIGHTNING_LEVEL));
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
        if (bolt.getPersistentData().getBoolean(LightningStrikeProcessor.LIGHTNING_PROCESSED_TAG))
            return;
        bolt.getPersistentData().putBoolean(LightningStrikeProcessor.LIGHTNING_PROCESSED_TAG, true);

        LightningStrikeProcessor.processBoltStrike((ServerLevel) bolt.level(), bolt);
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
            LightningStrikeProcessor.spawnProtectedItem(level, particlePos, extra);
        }
    }

    private static void handleItemEntityStrike(EntityStruckByLightningEvent event, ItemEntity itemEntity) {
        if (itemEntity.level().isClientSide)
            return;

        if (itemEntity.getPersistentData().getBoolean(LightningStrikeProcessor.LIGHTNING_PROCESSED_TAG)) {
            event.setCanceled(true);
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty())
            return;

        ServerLevel level = (ServerLevel) itemEntity.level();

        // 优先本模组 lightning 配方；未命中且 CC&A 已加载时，尝试其充电配方（自动适用雷击转化）
        Optional<RecipeHolder<? extends Recipe<?>>> recipeOpt = CoeRecipeTypes.LIGHTNING
            .find(LightningInput.of(stack.copyWithCount(1)), level)
            .map(holder -> holder);
        if (recipeOpt.isEmpty())
            recipeOpt = findCcaCharging(level, stack);
        if (recipeOpt.isEmpty())
            return;

        event.setCanceled(true);
        itemEntity.discard();

        Vec3 pos = itemEntity.position();
        LightningStrikeProcessor.spawnWhiteBurstParticles(level, pos);

        List<ItemStack> allResults = LightningStrikeProcessor.processBatch(level, stack, recipeOpt.get().value());
        allResults.forEach(result -> {
            ItemEntity outputEntity = new ItemEntity(level, pos.x, pos.y, pos.z, result.copy());
            outputEntity.setDefaultPickUpDelay();
            outputEntity.getPersistentData().putBoolean(LightningStrikeProcessor.LIGHTNING_PROCESSED_TAG, true);
            level.addFreshEntity(outputEntity);
        });
    }

    /**
     * 在 CC&amp;A 充电配方中查找匹配当前物品的配方（雷击转化自动适用 CC&amp;A 充电配方）。
     * <p>匹配语义与本模组 lightning 配方一致：单物品输入，按充电配方的首个 ingredient 测试。
     * CC&amp;A 未安装时返回空。</p>
     *
     * <p><b>2026-09 隔离整改</b>：CC&amp;A 的类/配方类型全部收在 compat 门面里，本类只见
     * {@code RecipeHolder} 这类中立类型——核心层不出现任何 CC&amp;A 硬编码。</p>
     */
    private static Optional<RecipeHolder<? extends Recipe<?>>> findCcaCharging(ServerLevel level, ItemStack stack) {
        return com.hjmmd_8.createoreexpansion.compat.createaddition.coe.CreateAdditionCompat
            .findChargingRecipe(level, stack);
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
        var recipeOpt = CoeRecipeTypes.LIGHTNING_BLOCK.find(RecipeAutomation.wrap(blockAsItem), level);
        if (recipeOpt.isEmpty())
            return null;

        Vec3 particlePos = Vec3.atCenterOf(pos);
        LightningStrikeProcessor.spawnWhiteBurstParticles(level, particlePos);

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

}