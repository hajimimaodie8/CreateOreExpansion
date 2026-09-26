package com.hjmmd_8.createoreexpansion.common.recipe;

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * <b>配方自动化判据 + 配方匹配包装</b>（P3q：从 {@code common/hub/AllRecipeTypes} 下移到 core）。
 *
 * <p><b>为什么要下移</b>：{@code AllRecipeTypes} 是<b>聚合入口</b>——它 import 三层的配方类型
 * （TRANS / COE / CEWS）来确定注册顺序，所以它只能住在根工程，任何一层拆成子模块后就看不见它。
 * 而它身上有三个成员其实与"聚合"无关，是三层共用的<b>纯工具</b>：</p>
 * <ul>
 *   <li>{@link #CAN_BE_AUTOMATED} —— 配方 id 不以 {@code _manual_only} 结尾；</li>
 *   <li>{@link #shouldIgnoreInAutomation(RecipeHolder)} —— 上述约定 ∪ Create 的
 *       {@code AllRecipeTags.AUTOMATION_IGNORE} 序列化器标签；</li>
 *   <li>{@link #wrap(ItemStack)} —— 单物品 → {@link RecipeWrapper}。</li>
 * </ul>
 * <p>三者只依赖 Minecraft / Create 类型（P3m 判据：<b>方法描述符里不出现本模组类型</b>），
 * 因此可以原样住进共享库。调用方从"层 → hub 聚合入口"变成"层 → core"，方向不变、语义不变
 * ——方法体与 {@code AllRecipeTypes} 里那份逐字相同。</p>
 *
 * <p>{@code common/hub/AllRecipeTypes} 当时把同名成员<b>原地保留</b>了一份（P3q 刻意接受的
 * 双份实现代价）；P7a 删除那个聚合入口之后，<b>全仓唯一实现就是本类</b>。</p>
 */
public final class RecipeAutomation {

    /**
     * 该配方是否<b>允许</b>被自动化加工（本模组自有约定：id 不以 {@code _manual_only} 结尾）。
     *
     * <p>与拆分前 {@code AllRecipeTypes.CAN_BE_AUTOMATED} 逐字相同。</p>
     */
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

    /**
     * 单物品 → {@link RecipeWrapper}（配方匹配统一走机械动力的 {@code RecipeWrapper}）。
     *
     * <p>与拆分前 {@code AllRecipeTypes.wrap} 逐字相同。</p>
     */
    public static RecipeWrapper wrap(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack);
        return new RecipeWrapper(handler);
    }

    private RecipeAutomation() {
    }
}
