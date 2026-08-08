package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class AllDataComponents {
    public static final Codec<List<String>> LIST_STRING_CODEC = Codec.STRING.listOf();

    private static final DeferredRegister.DataComponents DATA_COMPONENTS
            = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateOreExpansion.MOD_ID);

    /**
     * ItemSkill StreamCodec - 网络同步使用
     * 编码：ItemSkill -> ResourceLocation (写入 ByteBuf)
     * 解码：ByteBuf -> ResourceLocation -> ItemSkill
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillsComponent> ITEM_SKILL_STREAM_CODEC =
        StreamCodec.of(
            (buf, value) -> {
                var skills = value.getAllSkills();
                buf.writeInt(skills.size());
                SkillsComponent.getStrings(skills)
                        .forEach(s -> ByteBufCodecs.STRING_UTF8.encode(buf, s));
            },
            buf -> {
                var siz = buf.readInt();
                List<String> strings = new ArrayList<>();
                for (int i = 0; i < siz; i++) {
                    strings.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                return new SkillsComponent(SkillsComponent.getSkills(strings));
            }
        );

    /**
     * ItemSkill Codec - 持久化保存使用
     * 编码：ItemSkill -> ResourceLocation (通过 AllSkills.getId())
     * 解码：ResourceLocation -> ItemSkill (通过 AllSkills.get())
     */
    public static final Codec<SkillsComponent> ITEM_SKILL_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<Pair<SkillsComponent, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getList(input).map(consumer -> {
                List<String> strings = new ArrayList<>();
                consumer.accept(element ->
                        ops.getStringValue(element).ifSuccess(strings::add));
                return Pair.of(new SkillsComponent(SkillsComponent.getSkills(strings)), ops.empty());
            });
        }

        @Override
        public <T> DataResult<T> encode(SkillsComponent input, DynamicOps<T> ops, T prefix) {
            List<String> strings = SkillsComponent.getStrings(input.getAllSkills());
            return DataResult.success(ops.createList(strings.stream().map(ops::createString)));
        }
    };

    public static final DataComponentType<SkillsComponent> SKILLS = register("skills", builder ->
            builder.persistent(ITEM_SKILL_CODEC).networkSynchronized(ITEM_SKILL_STREAM_CODEC));

    public static final DataComponentType<Integer> ENERGY = register("energy", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> MAX_ENERGY = register("max_energy", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> ENERGY_COLOR = register("energy_color", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentType<Integer> ENERGY_COLOR_DARK = register("energy_color_dark", builder ->
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
