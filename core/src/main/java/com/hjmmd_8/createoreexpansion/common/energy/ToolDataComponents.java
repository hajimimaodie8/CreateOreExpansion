package com.hjmmd_8.createoreexpansion.common.energy;

import com.hjmmd_8.createoreexpansion.common.registry.DataComponentRegistrar;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * <b>工具 / 凝能佩的通用数据组件</b>（P3p 从 COE 的
 * {@code common.registry.coe.AllDataComponents} 拆出来的「纯数据」那一半）。
 *
 * <p>这些组件的类型只依赖 Minecraft（{@code Integer} / {@code Long} / {@code ResourceLocation} /
 * {@code List}），没有任何技能线的类型，所以它们属于共享库（core）——
 * {@code common.energy} 的能量门面（P12 起包名还原为
 * {@code content.equipment.tool.energy}）读的是这里的常量，不再反向依赖 COE 的注册类。</p>
 *
 * <p><b>注册顺序不变</b>：本类字段的文本顺序就是它们进注册表的顺序
 * （{@code energy}(2) → … → {@code skill_cooldown_until}(10)），
 * 且第 1 个 {@code skills} 由 {@code AllDataComponents} 先登记——
 * 触发点见 {@link DataComponentRegistrar} 的类注释。注册 id 与拆分前逐字相同。</p>
 */
public final class ToolDataComponents {

    public static final DataComponentType<Integer> ENERGY = register("energy", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> MAX_ENERGY = register("max_energy", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> ENERGY_COLOR = register("energy_color", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> ENERGY_COLOR_DARK = register("energy_color_dark", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    /** 凝能佩：绑定的工具物品 id 列表（一枚佩可绑定多个工具） */
    public static final DataComponentType<List<ResourceLocation>> BOUND_TOOL = register("bound_tool", builder ->
            builder.persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** 能量工具：绑定的凝能佩物品 id（工具上存储） */
    public static final DataComponentType<ResourceLocation> BOUND_MEDALLION = register("bound_medallion", builder ->
            builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** 凝能佩模式：true=充能模式，false=供应模式 */
    public static final DataComponentType<Boolean> MEDALLION_MODE = register("medallion_mode", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** 绑定信息行颜色（工具 tooltip「绑定：xxx」行的颜色，在物品注册时自定义） */
    public static final DataComponentType<Integer> BIND_COLOR = register("bind_color", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    /** 技能冷却截止游戏时刻（tick，per-stack 独立冷却；无此组件=无冷却） */
    public static final DataComponentType<Long> SKILL_COOLDOWN_UNTIL = register("skill_cooldown_until", builder ->
            builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    private ToolDataComponents() {
    }

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DataComponentRegistrar.DATA_COMPONENTS.register(name, () -> type);
        return type;
    }
}
