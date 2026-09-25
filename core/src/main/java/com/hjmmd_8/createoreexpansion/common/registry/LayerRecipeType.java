package com.hjmmd_8.createoreexpansion.common.registry;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>单个"本模组配方类型"的载体</b>（P3c：四个多层枢纽文件按层拆分，住 SHARED 层）。
 *
 * <p><b>它从哪来</b>：原先本模组没有"按层分家"的配方类型概念——六个配方类型
 * （{@code transmuting / lightning / lightning_block / grinding / dismantling / charging}）
 * 全部声明在同一个枚举 {@code common/AllRecipeTypes} 里，而那个枚举同时被 COE（雷击、角磨）、
 * CEWS（充能）、TRANS（嬗变）三层引用。P3c 把"每层自己的配方类型"搬进各自的包
 * （{@code common/registry/coe/CoeRecipeTypes}、{@code .../cews/CewsRecipeTypes}、
 * {@code .../transmutation/TransmutationRecipeTypes}），本类就是它们共用的那个载体。</p>
 *
 * <p><b>为什么两个 DeferredRegister 放在这里（SHARED）而不放在各层</b>：这两张注册表里
 * <b>只有一个命名空间</b>（{@link CoeCore#REGISTRY_NAMESPACE}），六个条目原先共用<b>同一对</b>
 * DeferredRegister，注册条目顺序由枚举常量顺序决定。若每层各建一对，同一个注册表就会有三张
 * DeferredRegister 并发写入，条目顺序会从"枚举顺序"变成"三层各自的顺序"——
 * 这是本轮要避免的（{@code DeferredRegister} 内部是 {@code LinkedHashMap}，
 * 条目顺序 = 注册顺序 = 数值注册 id 顺序）。所以注册表留一处，各层只负责<b>声明自己的条目</b>。</p>
 *
 * <p><b>注册 id 与拆分前逐字一致</b>：条目名由常量名经 {@link Lang#asId}（= 全小写）得到，
 * 与拆分前枚举构造器里的 {@code Lang.asId(name())} 是同一条路径；命名空间恒为
 * {@link CoeCore#REGISTRY_NAMESPACE}。所以 id、序列化器类型、配方类型一个都没变。</p>
 */
public final class LayerRecipeType implements IRecipeTypeInfo, StringRepresentable {

    /** 全部本模组配方序列化器的注册表（命名空间 = createoreexpansion）。 */
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CoeCore.REGISTRY_NAMESPACE);

    /** 全部本模组配方类型的注册表（命名空间 = createoreexpansion）。 */
    private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER =
        DeferredRegister.create(Registries.RECIPE_TYPE, CoeCore.REGISTRY_NAMESPACE);

    private final ResourceLocation id;
    private final Supplier<RecipeSerializer<?>> serializerSupplier;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    @Nullable
    private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
    private final Supplier<RecipeType<?>> type;

    /** 语义标记，与拆分前的枚举字段同名同义（当前无读取方，保留以对齐状态位）。 */
    private boolean isProcessingRecipe;

    private LayerRecipeType(String constantName, Supplier<RecipeSerializer<?>> serializerSupplier) {
        String name = Lang.asId(constantName);
        id = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, name);
        this.serializerSupplier = serializerSupplier;
        serializerObject = SERIALIZER_REGISTER.register(name, serializerSupplier);
        typeObject = TYPE_REGISTER.register(name, () -> RecipeType.simple(id));
        type = typeObject;
        isProcessingRecipe = false;
    }

    /**
     * 声明一个走 Create {@code StandardProcessingRecipe} 流水线的配方类型
     * （拆分前 {@code AllRecipeTypes(StandardProcessingRecipe.Factory<?>)} 那条构造器路径）。
     *
     * @param constantName 常量名（大小写与拆分前的枚举常量名一致，id 由它小写得到）
     * @param processingFactory 配方工厂（如 {@code LightningRecipe::new}）
     */
    public static LayerRecipeType processing(String constantName,
                                             StandardProcessingRecipe.Factory<?> processingFactory) {
        LayerRecipeType recipeType = new LayerRecipeType(constantName,
            () -> new StandardProcessingRecipe.Serializer<>(processingFactory));
        recipeType.isProcessingRecipe = true;
        return recipeType;
    }

    /**
     * 声明一个自带序列化器的配方类型
     * （拆分前 {@code AllRecipeTypes(Supplier<RecipeSerializer<?>>)} 那条构造器路径）。
     *
     * @param constantName 常量名（大小写与拆分前的枚举常量名一致，id 由它小写得到）
     * @param serializerSupplier 序列化器工厂（如 {@code () -> new DismantlingRecipe.Serializer()}）
     */
    public static LayerRecipeType serializer(String constantName,
                                             Supplier<RecipeSerializer<?>> serializerSupplier) {
        return new LayerRecipeType(constantName, serializerSupplier);
    }

    /** 把两张注册表挂到 mod 事件总线（拆分前 {@code AllRecipeTypes.register} 的动作，只此一处）。 */
    public static void registerOn(IEventBus modEventBus) {
        SERIALIZER_REGISTER.register(modEventBus);
        TYPE_REGISTER.register(modEventBus);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializerObject.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) type.get();
    }

    public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(I inv, Level world) {
        return world.getRecipeManager()
            .getRecipeFor(getType(), inv, world);
    }

    @Override
    public String getSerializedName() {
        return id.toString();
    }
}
