package com.hjmmd_8.createoreexpansion.content.equipment.medallion.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllFluids;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.common.AllModEffects;
import com.hjmmd_8.createoreexpansion.common.AllModItemTags;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * 凝能佩被动效果事件集中处理（与 TransmutationEventHandler 同模式注册）。
 * 黄玉：免摔落；星辉石：免疫嬗乱、虚空上浮不销毁、液体中不销毁（注册名含 stellarstone 全系列）；
 * 雷鸣：雷电吸收。
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class MedallionEffectHandler {

    private MedallionEffectHandler() {
    }

    /** 黄玉：50% 概率全部豁免摔落伤害 */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
            && Math.random() < 0.5
            && IMedallion.isWearing(player, AllItems.TOPAZ_STRESS_MEDALLION.get())) {
            event.setCanceled(true);
        }
    }

    /** 星辉石：免疫嬗乱效果 */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player
            && event.getEffectInstance() != null
            && event.getEffectInstance().getEffect() == AllModEffects.TRANSMUTATION_DISORDER.get()
            && IMedallion.isWearing(player, AllItems.STELLARSTONE_STRESS_MEDALLION.get())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** 注册名含 stellarstone 的物品（星辉石全系列：佩/工具/材料等） */
    public static boolean isStellarstoneItem(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.getPath().contains("stellarstone");
    }

    /** 注册名含 thunderite 的物品（雷鸣合金全系列：佩/工具/材料等） */
    public static boolean isThunderiteItem(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.getPath().contains("thunderite");
    }

    /**
     * 物品实体处理（EntityTickEvent 只对 LivingEntity 触发，故用 ServerTick 遍历）。
     * 照搬 Create NoGravMagicalDohickyItem / ShadowSteelItem：
     * 星辉石全系列物品实体无重力悬浮（掉进虚空即悬浮在该处，不销毁，玩家可下去捡）；
     * 若带 JustCreated 标记（特殊生成），按暗影钢方式一次性上抛（掉落越深抛得越高）。
     * 掉入岩浆或嬗化液：发光（setGlowingTag，方便查看）；嬗化液中不销毁（正常沉底）。
     * 其他物品在嬗化液中销毁。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                if (!(entity instanceof ItemEntity item))
                    continue;
                boolean thunderite = isThunderiteItem(item.getItem());
                boolean stellar = isStellarstoneItem(item.getItem());
                boolean inTransmutationFluid =
                    item.getFluidTypeHeight(AllFluids.TRANSMUTATION_FLUID.get().getFluidType()) > 0.0D;
                if (thunderite) {
                    // 雷鸣系列：岩浆中免疫伤害（不销毁）、不燃烧、发光（仅岩浆）
                    boolean inLava = item.isInLava();
                    item.setInvulnerable(inLava);
                    item.setRemainingFireTicks(0);
                    item.setGlowingTag(inLava);
                } else if (stellar) {
                    boolean inLava = item.isInLava();
                    // 岩浆中免疫伤害（不销毁）、不燃烧；岩浆/嬗化液中发光方便查看
                    item.setInvulnerable(inLava);
                    item.setRemainingFireTicks(0);
                    item.setGlowingTag(inLava || inTransmutationFluid);
                    if (inTransmutationFluid)
                        continue; // 液体中不销毁、不悬浮（正常沉底，但发光）
                    item.setNoGravity(true); // 悬浮（照搬 NoGravMagicalDohickyItem）
                    CompoundTag data = item.getPersistentData();
                    if (data.contains("JustCreated")) {
                        // 照搬暗影钢 onCreated：掉落越深抛得越高
                        float yMotion = (item.fallDistance + 3) / 50f;
                        item.setDeltaMovement(0, yMotion, 0);
                        item.lifespan = 6000;
                        item.setSilent(true);
                        data.remove("JustCreated");
                    }
                } else if (inTransmutationFluid) {
                    item.discard();
                }
            }
        }
    }

    /** 雷鸣：雷电吸收——雷鸣物品实体被雷击不销毁并补满能量；玩家佩戴/手持雷鸣佩/工具同样豁免并补满 */
    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ItemEntity item && isThunderiteItem(item.getItem())) {
            ItemStack stack = item.getItem();
            if (ToolEnergy.hasEnergy(stack)) {
                ToolEnergy.setEnergy(stack, ToolEnergy.getMaxEnergy(stack));
            }
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player player))
            return;
        boolean absorbed = false;
        if (IMedallion.isWearing(player, AllItems.THUNDERITE_STRESS_MEDALLION.get())) {
            CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(AllItems.THUNDERITE_STRESS_MEDALLION.get()))
                .ifPresent(result -> ToolEnergy.setEnergy(result.stack(), ToolEnergy.getMaxEnergy(result.stack())));
            absorbed = true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(AllModItemTags.THUNDERITE_ITEMS) && ToolEnergy.hasEnergy(stack)) {
                ToolEnergy.setEnergy(stack, ToolEnergy.getMaxEnergy(stack));
                absorbed = true;
            }
        }
        if (absorbed) {
            event.setCanceled(true);
        }
    }
}
