package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.energy.ToolDataComponents;
import com.hjmmd_8.createoreexpansion.common.registry.DataComponentRegistrar;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 数据组件注册入口（COE 层）。
 *
 * <p><b>P3p：这个类被拆成了两半。</b>它是「工具能量环」上唯一的承重块——它 import
 * {@link SkillsComponent}（根侧共享层），而 {@code SkillsComponent} 又反向 import
 * {@code common.energy} 的工具能量类，于是 {@code common.energy} 永远进不了共享库（core）。
 * 现在把<b>纯数据</b>的那 9 个组件（只依赖 MC 的 {@code int}/{@code long}/
 * {@code ResourceLocation}/{@code List}）搬进了 core 的 {@link ToolDataComponents}，
 * 这里只留技能线专属的 {@code skills}，并对那 9 个组件保留<b>同名别名</b>
 * （全仓 68 处 {@code AllDataComponents.XXX} 调用点一行未改）。</p>
 *
 * <p><b>注册顺序与注册 id 逐字不变</b>：两半共用
 * {@link DataComponentRegistrar#DATA_COMPONENTS} 这唯一一个
 * {@code DeferredRegister}（内部 {@code LinkedHashMap}，顺序 = {@code register(...)} 调用顺序）。
 * 触发点就是下面字段的文本顺序：</p>
 * <ol>
 *   <li>{@code skills} —— 本类自己的第一个注册（第 1 条）；</li>
 *   <li>读 {@link ToolDataComponents} 的任一常量会触发它的类初始化，
 *       于是 {@code energy … skill_cooldown_until} 紧跟着连续登记（第 2~10 条）。</li>
 * </ol>
 * <p>实测顺序（{@code runData} 探针，拆分前后逐字相同）：
 * {@code skills, energy, max_energy, energy_color, energy_color_dark, bound_tool,
 * bound_medallion, medallion_mode, bind_color, skill_cooldown_until}。</p>
 *
 * <p><b>别把 {@link DataComponentRegistrar} 的字段搬进 {@link ToolDataComponents}</b>：
 * 那个类本身绝不能持有 {@code DeferredRegister}，否则它的类初始化会在 {@code skills}
 * 登记之前发生，顺序就变了（原因见 {@code DataComponentRegistrar} 的类注释）。</p>
 */
public class AllDataComponents {
    public static final Codec<List<String>> LIST_STRING_CODEC = Codec.STRING.listOf();

    /** 与 core 共用的唯一注册器（{@code skills} 与那 9 个通用组件都登记在它上面）。 */
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DataComponentRegistrar.DATA_COMPONENTS;

    /**
     * ItemSkill StreamCodec - 网络同步使用
     * 编码：ItemSkill -> ResourceLocation (写入 ByteBuf)
     * 解码：ByteBuf -> ResourceLocation -> ItemSkill
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillsComponent> ITEM_SKILL_STREAM_CODEC =
        StreamCodec.of(
            (buf, value) -> {
                var skills = value.getAllData();
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
                return SkillsComponent.fromStrings(strings);
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
                return Pair.of(SkillsComponent.fromStrings(strings), ops.empty());
            });
        }

        @Override
        public <T> DataResult<T> encode(SkillsComponent input, DynamicOps<T> ops, T prefix) {
            List<String> strings = SkillsComponent.getStrings(input.getAllData());
            return DataResult.success(ops.createList(strings.stream().map(ops::createString)));
        }
    };

    /** 技能组件（技能线专属，类型是根侧共享层的 {@link SkillsComponent}，所以留在这里）。 */
    public static final DataComponentType<SkillsComponent> SKILLS = register("skills", builder ->
            builder.persistent(ITEM_SKILL_CODEC).networkSynchronized(ITEM_SKILL_STREAM_CODEC));

    // ── 下面 9 个是 core（ToolDataComponents）里的同名别名（P3p）──────────────────────
    // 声明顺序 = 拆分前的注册顺序；读到第一个（ENERGY）就会触发 ToolDataComponents 的
    // 类初始化，把 energy(2) … skill_cooldown_until(10) 连续登记完。别调换顺序、别删。

    public static final DataComponentType<Integer> ENERGY = ToolDataComponents.ENERGY;

    public static final DataComponentType<Integer> MAX_ENERGY = ToolDataComponents.MAX_ENERGY;

    public static final DataComponentType<Integer> ENERGY_COLOR = ToolDataComponents.ENERGY_COLOR;

    public static final DataComponentType<Integer> ENERGY_COLOR_DARK = ToolDataComponents.ENERGY_COLOR_DARK;

    /** 凝能佩：绑定的工具物品 id 列表（一枚佩可绑定多个工具） */
    public static final DataComponentType<List<ResourceLocation>> BOUND_TOOL = ToolDataComponents.BOUND_TOOL;

    /** 能量工具：绑定的凝能佩物品 id（工具上存储） */
    public static final DataComponentType<ResourceLocation> BOUND_MEDALLION = ToolDataComponents.BOUND_MEDALLION;

    /** 凝能佩模式：true=充能模式，false=供应模式 */
    public static final DataComponentType<Boolean> MEDALLION_MODE = ToolDataComponents.MEDALLION_MODE;

    /** 绑定信息行颜色（工具 tooltip「绑定：xxx」行的颜色，在物品注册时自定义） */
    public static final DataComponentType<Integer> BIND_COLOR = ToolDataComponents.BIND_COLOR;

    /** 技能冷却截止游戏时刻（tick，per-stack 独立冷却；无此组件=无冷却） */
    public static final DataComponentType<Long> SKILL_COOLDOWN_UNTIL = ToolDataComponents.SKILL_COOLDOWN_UNTIL;

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
