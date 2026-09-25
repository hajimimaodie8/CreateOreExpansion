package com.hjmmd_8.createoreexpansion.common.hub;

import java.util.function.Predicate;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsRecipeTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRecipeTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * <b>配方类型的协调入口</b>（P3c：四个"多层枢纽文件"按层拆分后保留的同名入口，住 SHARED 层）。
 *
 * <p><b>拆分前</b>：本类是一个枚举，六个配方类型（TRANS 的 {@code transmuting}、COE 的
 * {@code lightning}/{@code lightning_block}/{@code grinding}/{@code dismantling}、CEWS 的
 * {@code charging}）与它们的注册动作 <b>混在一处</b>——既 import 三层的配方类，又自己持有
 * 两张 DeferredRegister。</p>
 *
 * <p><b>拆分后</b>：每个配方类型的<b>声明</b>搬进各自层的类
 * （{@link TransmutationRecipeTypes} / {@link CoeRecipeTypes} / {@link CewsRecipeTypes}），
 * 本类只剩"按固定顺序把它们串起来"这一件事：</p>
 * <ul>
 *   <li>下面的六个字段按<b>拆分前枚举常量的声明顺序逐字排列</b>。Java 的静态字段初始化按文本顺序
 *       执行，所以访问本类的那一刻，三层类就按同样的顺序被初始化、条目也按同样的顺序进注册表
 *       （{@code DeferredRegister} 内部是 {@code LinkedHashMap}，条目顺序 = 注册顺序）。</li>
 *   <li>{@link #register(IEventBus)} 把两张注册表挂到事件总线（调用点、时机、行为与拆分前一致）。</li>
 *   <li>配方无关的公共物（{@link #CAN_BE_AUTOMATED}、{@link #shouldIgnoreInAutomation}、
 *       {@link #wrap}）留在本入口，三层共用。</li>
 * </ul>
 *
 * <p><b>顺序说明（本轮唯一的取舍，务必先读）</b>：拆分前枚举常量的整体顺序是</p>
 * <pre>TRANSMUTING(TRANS) → LIGHTNING(COE) → LIGHTNING_BLOCK(COE) → GRINDING(COE)
 * → DISMANTLING(COE) → CHARGING(CEWS)</pre>
 * <p>即层的交错顺序是 <b>TRANS → COE → CEWS</b>，<b>不是</b>任务书里写的约定顺序
 * COE → CEWS → TRANS。本轮的硬指标是"注册顺序与拆分前逐字相同"，所以这里按<b>原顺序</b>排列：
 * 若改成按层分组（COE → CEWS → TRANS），配方序列化器/配方类型进注册表的顺序会变
 * （产物层面无影响——这两张注册表不产生任何数据生成产物，数值 id 也不进存档、不参与网络同步，
 * 但"逐字相同"这条就不再成立）。要翻转只需调换下面六行的顺序。</p>
 *
 * <p><b>为什么本类不再是枚举</b>：枚举的常量必须与"注册动作"绑定在同一个类里，做不到
 * "声明住各层、顺序住入口"。改成 {@code final class} + 静态常量后，所有既有调用点
 * （{@code AllRecipeTypes.GRINDING} / {@code .LIGHTNING.getType()} / {@code .TRANSMUTING.find(...)}
 * 等）类型仍是 {@link LayerRecipeType}（实现了 Create 的 {@code IRecipeTypeInfo} 与
 * {@code StringRepresentable}），因此一行都不用改。</p>
 */
public final class AllRecipeTypes {

    // ================= 六个配方类型：声明在各层，这里只定顺序 =================
    //
    // 顺序 = 拆分前枚举常量顺序（逐字相同）：TRANS → COE ×4 → CEWS。
    // 访问任一个都会按下面的文本顺序把三层的类初始化、并把条目按同样顺序登记进注册表。

    /** 嬗变加工（TRANS 层：{@link TransmutationRecipeTypes#TRANSMUTING}）。 */
    public static final LayerRecipeType TRANSMUTING = TransmutationRecipeTypes.TRANSMUTING;

    /** 雷击加工（COE 层：{@link CoeRecipeTypes#LIGHTNING}）。 */
    public static final LayerRecipeType LIGHTNING = CoeRecipeTypes.LIGHTNING;

    /** 方块雷击加工（COE 层：{@link CoeRecipeTypes#LIGHTNING_BLOCK}）。 */
    public static final LayerRecipeType LIGHTNING_BLOCK = CoeRecipeTypes.LIGHTNING_BLOCK;

    /** 角磨（COE 层：{@link CoeRecipeTypes#GRINDING}）。 */
    public static final LayerRecipeType GRINDING = CoeRecipeTypes.GRINDING;

    /** 拆解（COE 层：{@link CoeRecipeTypes#DISMANTLING}）。 */
    public static final LayerRecipeType DISMANTLING = CoeRecipeTypes.DISMANTLING;

    /** 充能（CEWS 层：{@link CewsRecipeTypes#CHARGING}）。 */
    public static final LayerRecipeType CHARGING = CewsRecipeTypes.CHARGING;

    // ================= 与配方无关的公共物（三层共用，留在入口） =================

    public static final Predicate<RecipeHolder<?>> CAN_BE_AUTOMATED = r -> !r.id()
        .getPath()
        .endsWith("_manual_only");

    /**
     * 该配方是否应被"自动化加工"忽略（波的全库候选池按此过滤，见设计文档 §1.1 第 5 条）。
     *
     * <p>判据有两半（2026-09 审计修复）：</p>
     * <ol>
     *   <li>Create 的 <b>serializer tag 分支</b>：{@code AllTags.AllRecipeSerializerTags.AUTOMATION_IGNORE}
     *       （原版 Create 只标了 occultism 那两条）——旧实现漏了这一半，导致带该标签的配方仍会进波的全库池；</li>
     *   <li>本模组自有约定：配方 id 以 {@code _manual_only} 结尾 = 只能手动。</li>
     * </ol>
     *
     * <p>Create 侧读取失败（版本差异等）时退化为本模组约定，不影响主流程。</p>
     */
    public static boolean shouldIgnoreInAutomation(RecipeHolder<?> recipe) {
        if (!CAN_BE_AUTOMATED.test(recipe))
            return true;
        try {
            return com.simibubi.create.AllRecipeTypes.shouldIgnoreInAutomation(recipe);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 把两张注册表（序列化器 / 配方类型）挂到 mod 事件总线（唯一注册动作，只此一处）。 */
    public static void register(IEventBus modEventBus) {
        LayerRecipeType.registerOn(modEventBus);
    }

    /** 单物品 → RecipeWrapper（配方匹配统一走机械动力的 RecipeWrapper）。 */
    public static RecipeWrapper wrap(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack);
        return new RecipeWrapper(handler);
    }

    private AllRecipeTypes() {}
}
