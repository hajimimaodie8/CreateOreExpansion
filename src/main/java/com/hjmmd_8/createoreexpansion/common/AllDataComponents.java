package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.UnaryOperator;

public class AllDataComponents {
    public static final Codec<List<String>> LIST_STRING_CODEC = Codec.STRING.listOf();

    private static final DeferredRegister.DataComponents DATA_COMPONENTS
            = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateOreExpansion.MOD_ID);

    public static final DataComponentType<List<String>> SKILLS = register("skills", builder ->
            builder.persistent(LIST_STRING_CODEC)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    public static final DataComponentType<Integer> ENERGY = register("energy", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> MAX_ENERGY = register("max_energy", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> ENERGY_COLOR = register("energy_color", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
