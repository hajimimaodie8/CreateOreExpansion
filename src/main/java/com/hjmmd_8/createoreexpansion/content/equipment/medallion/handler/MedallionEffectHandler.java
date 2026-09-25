package com.hjmmd_8.createoreexpansion.content.equipment.medallion.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.transmutation.TransmutationLink;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.AllModItemTags;
import com.hjmmd_8.createoreexpansion.common.SeriesTraits;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.common.energy.ToolEnergy;

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

/**
 * 凝能佩被动效果事件集中处理（与 TransmutationEventHandler 同模式注册）。
 * 黄玉：免摔落；星辉石：免疫嬗乱、虚空上浮不销毁、液体中不销毁（注册名含 stellarstone 全系列）；
 * 雷鸣：雷电吸收。
 *
 * <p><b>Curios optional</b>：本类不含任何 Curios 引用 —— "玩家是否佩戴某佩"一律走
 * {@link IMedallion#isWearing} / {@link IMedallion#findEquipped}（内部经
 * {@code compat.curios} 桥接）。未装 Curios 时这些查询恒为假，于是佩的被动效果全部静默失效，
 * 但物品实体侧（星辉/雷鸣系列）的行为与 Curios 无关，照旧生效。</p>
 *
 * <p><b>P3t：嬗变液与嬗乱改走 core 契约</b>。本类要读的两个声明都属于 TRANS 层
 * （{@code common/registry/transmutation/TransmutationFluids} 的嬗变液、{@code TransmutationEffects}
 * 的嬗乱），而 {@code COE -> TRANS} 是禁止方向，所以改读 core 的窄契约
 * {@link TransmutationLink}（实现由 TRANS 在声明初始化时注入）。两处判定与旧写法逐字相同：
 * 液体侧还是 {@code getFluidTypeHeight(嬗变液的 FluidType) > 0}，效果侧见下条；
 * 差别只是"谁去拿那两个对象"。层文件因此不再 import 集成层的
 * {@code common/hub/AllFluids} / {@code AllModEffects}。</p>
 *
 * <p><b>P3u：修掉了"判据恒假"的既有缺陷</b>。{@link #onEffectApplicable} 里那句判定原本是
 * {@code event.getEffectInstance().getEffect() == AllModEffects.TRANSMUTATION_DISORDER.get()}，
 * 而 1.21 起 {@code MobEffectInstance#getEffect()} 返回 {@code Holder<MobEffect>}——于是它是
 * "Holder 与 MobEffect 的身份比较"，<b>恒为 false</b>（编译能过只因非 final 类可转型成接口）。
 * 也就是说这条 {@code MobEffectEvent.Applicable} 兜底<b>从来没拦下过任何一次嬗乱</b>；
 * 真正生效的只有 {@code TransmutationEventHandler} 里"接触嬗变液且佩戴星辉石佩就早退"那条主路径
 * ——流体接触这一路是好的，只有<b>非流体源</b>（雷鸣合金工具命中、黄玉弓的转化紊乱等）没被拦。
 * 现已按用户裁定去掉右侧的 {@code .get()}（改成比较同一个 {@code Holder}，实测本模组施放嬗乱的
 * 三个现场塞进 {@code MobEffectInstance} 的就是那个 {@code DeferredHolder} 本身）；
 * 判定实现住在 {@link TransmutationLink} 的 TRANS 侧实现里。
 * 修后星辉石佩开始豁免<b>所有来源</b>的嬗乱——这是本次唯一的玩法改动。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class MedallionEffectHandler {

    private MedallionEffectHandler() {
    }

    /** 黄玉：50% 概率全部豁免摔落伤害 */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
            && Math.random() < 0.5
            && IMedallion.isWearing(player, CoeItems.TOPAZ_STRESS_MEDALLION.get())) {
            event.setCanceled(true);
        }
    }

    /** 星辉石：免疫嬗乱效果 */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player
            && event.getEffectInstance() != null
            && TransmutationLink.get().isTransmutationDisorder(event.getEffectInstance().getEffect())
            && IMedallion.isWearing(player, CoeItems.STELLARSTONE_STRESS_MEDALLION.get())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * 是否星辉石系列物品（佩/工具/材料/方块物品…）。
     *
     * <p><b>系列归属判定统一收口到 {@link SeriesTraits}</b>（2026-09-15）：以系列物品标签为准
     * （注册链上的 {@code .addStellarstoneTraits()} 会把条目写进标签），注册名含 {@code stellarstone}
     * 作为兜底；不再在本类里各写一份字符串匹配。本方法保留旧名，避免改动全部调用点。</p>
     */
    public static boolean isStellarstoneItem(ItemStack stack) {
        return SeriesTraits.isStellarstone(stack);
    }

    /**
     * 是否雷鸣合金系列物品（佩/工具/材料/方块物品…）。判定口径见
     * {@link #isStellarstoneItem(ItemStack)}（标签优先 + 命名兜底）。
     */
    public static boolean isThunderiteItem(ItemStack stack) {
        return SeriesTraits.isThunderite(stack);
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
                boolean inTransmutationFluid = TransmutationLink.get().isInTransmutationFluid(item);
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

    /**
     * 雷鸣：雷电吸收——雷鸣<b>物品实体</b>被雷击不销毁，且"<b>能充能的</b>那种"顺手补满能量；
     * 玩家佩戴雷鸣佩、或手持雷鸣系列<b>带充能条的</b>武器工具时同样豁免并补满。
     *
     * <p><b>"能充能"与"属于雷鸣系列"是两件事</b>（用户 2026-09-15 明确要求区分）：系列标签里既有
     * 带充能条的工具/武器/佩，也有<b>没有充能条的材料与方块</b>（锭/碎块/板/杆/线/雷鸣块）。
     * 所以充能分支一律先过 {@link ToolEnergy#canCharge(ItemStack)}：材料被雷击照样豁免销毁（系列特性），
     * 但<b>不会、也不可能被写入能量</b>（{@code setEnergy} 对没有充能条的物品直接返回）。</p>
     */
    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ItemEntity item && isThunderiteItem(item.getItem())) {
            ItemStack stack = item.getItem();
            if (ToolEnergy.canCharge(stack)) {
                ToolEnergy.setEnergy(stack, ToolEnergy.getMaxEnergy(stack));
            }
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player player))
            return;
        boolean absorbed = false;
        // 佩戴雷鸣佩：豁免销毁并把佩补满（未装 Curios 时 findEquipped 恒空 → 本分支不进）
        ItemStack worn = IMedallion.findEquipped(player, CoeItems.THUNDERITE_STRESS_MEDALLION.get());
        if (!worn.isEmpty()) {
            ToolEnergy.setEnergy(worn, ToolEnergy.getMaxEnergy(worn));
            absorbed = true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(AllModItemTags.THUNDERITE_ITEMS) && ToolEnergy.canCharge(stack)) {
                ToolEnergy.setEnergy(stack, ToolEnergy.getMaxEnergy(stack));
                absorbed = true;
            }
        }
        if (absorbed) {
            event.setCanceled(true);
        }
    }
}
