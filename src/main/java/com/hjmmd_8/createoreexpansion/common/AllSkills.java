package com.hjmmd_8.createoreexpansion.common;

import com.google.common.collect.Maps;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.content.skill.SkinSkill;
import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.SkinConfig;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.FellingStrategy;
import com.hjmmd_8.createoreexpansion.data.lang.COELangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.Translator;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifierHolder;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.hjmmd_8.createoreexpansion.common.AllStrategies.RENDERERS;
import static com.hjmmd_8.createoreexpansion.common.AllStrategies.STRATEGIES;

public final class AllSkills {
    private static final Map<ResourceLocation, ItemSkill> SKILLS = Maps.newHashMap();
    private static final Map<ItemSkill, ResourceLocation> SKILL_IDS = Maps.newHashMap();
    private static final Map<ItemSkill, SkillConfig<?, ?>> CONFIGS = Maps.newHashMap();

    // ========== 公共技能实例 ==========
    public static final RegisteredDataSkill FELL =
            skill("fell", FellingSkill.class, FellingStrategy.class)
                    .skill(FellingSkill::new)
                    .strategy(FellingStrategy::new)
                    .translate("伐树", "Fell")
                    .config(new FellingConfig(
                            8, 200,
                            FellingConfig.BlockPredicate.IS_LOG,
                            0, .12f, .12f))
                    .level(1)
                    .register();
    public static final RegisteredDataSkill GREAT_FELL =
        skill("great_fell", FellingSkill.class, FellingStrategy.class)
                .skill(FellingSkill::new)
                .strategy(FellingStrategy::new)
                .config(new FellingConfig(
                        8, FellingConfig.BlockPredicate.IS_TREE,
                        100, .08f, .01f))
                .translate("伐树", "Fell")
                .level(2)
                .register();
    public static final RegisteredDataSkill GRAND_FELL =
        skill("grand_fell", FellingSkill.class, FellingStrategy.class)
                .skill(FellingSkill::new)
                .strategy(FellingStrategy::new)
                .config(new FellingConfig(
                        8, FellingConfig.BlockPredicate.IS_TREE,
                        100, .05f, .005f))
                .translate("伐树", "Fell")
                .level(3)
                .register();

    public static final RegisteredDataSkill SHATTER =
            skill("shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(AreaAoeSkill::new)
                    .strategy(AreaAoeStrategy::new)
                    .config(new AreaAoeConfig(0, BlockTags.MINEABLE_WITH_PICKAXE, 3, 1, 1, DualDirection.From.PLAYER_YAW))
                    .translate("开岩", "Shatter")
                    .level(1)
                    .register();
    public static final RegisteredDataSkill GREAT_SHATTER =
            skill("great_shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(AreaAoeSkill::new)
                    .strategy(AreaAoeStrategy::new)
                    .config(new AreaAoeConfig(100, BlockTags.MINEABLE_WITH_PICKAXE, 3, 3, 1))
                    .translate("开岩", "Shatter")
                    .level(2)
                    .register();
    public static final RegisteredDataSkill GRAND_SHATTER =
            skill("grand_shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(AreaAoeSkill::new)
                    .strategy(AreaAoeStrategy::new)
                    .config(new AreaAoeConfig(100, BlockTags.MINEABLE_WITH_PICKAXE, 5, 5, 1))
                    .translate("开岩", "Shatter")
                    .level(3)
                    .register();

    public static final RegisteredDataSkill CHANNEL =
            skill("channel", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(AreaAoeSkill::new)
                    .strategy(AreaAoeStrategy::new)
                    .config(new AreaAoeConfig(0, BlockTags.MINEABLE_WITH_SHOVEL, 1, 1, 6, DualDirection.From.PLAYER_YAW))
                    .translate("引渠", "Channel")
                    .level(1)
                    .register();
    public static final RegisteredDataSkill GREAT_CHANNEL =
        skill("great_channel", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(AreaAoeSkill::new)
                .strategy(AreaAoeStrategy::new)
                .config(new AreaAoeConfig(50, BlockTags.MINEABLE_WITH_SHOVEL, 1, 1, 8, DualDirection.From.PLAYER_YAW))
                .translate("引渠", "Channel")
                .level(2)
                .register();
    public static final RegisteredDataSkill GRADE =
        skill("grade", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(AreaAoeSkill::new)
                .strategy(AreaAoeStrategy::new)
                .config(new AreaAoeConfig(50, BlockTags.MINEABLE_WITH_SHOVEL, 7, 7, 1))
                .translate("平场", "Grade")
                .level(1)
                .register();

    public static final RegisteredDataSkill SKIN =
            skill("skin", SkinSkill.class, null)
                    .skill(strategy -> new SkinSkill())
                    .config(new SkinConfig(.5f, 0))
                    .translate("剥取", "Skin")
                    .level(1)
                    .register();

    // ========== 工具方法 ==========
    public static <T extends ItemSkill, S extends AreaStrategy> SkillBuilder<T, S> skill(
            ResourceLocation id, Class<T> skillType, Class<S> strategyType) {
        return new SkillBuilder<>(id);
    }

    public static <T extends ItemSkill, S extends AreaStrategy> SkillBuilder<T, S> skill(
            String id, Class<T> skillType, Class<S> strategyType) {
        return new SkillBuilder<>(id);
    }

    public static ItemSkill get(ResourceLocation id) {
        if (id == null) return null;
        if (!SKILLS.containsKey(id)) return null;
        return SKILLS.get(id);
    }

    public static ResourceLocation getId(ItemSkill skill) {
        if (skill == null) return null;
        if (!SKILL_IDS.containsKey(skill)) return null;
        return SKILL_IDS.get(skill);
    }

    public static SkillConfig<?, ?> getConfig(ItemSkill skill) {
        if (skill == null) return null;
        if (!CONFIGS.containsKey(skill)) return null;
        return CONFIGS.get(skill);
    }

    public static <C, V> V modifier(ModifiableAttributeType<C, V> type, SkillAttributeModifierHolder holder, C context) {
        ModifiableAttribute<V> attribute = type.create(context);
        holder.modifier(type, attribute);
        return attribute.getValue();
    }

    public static void register() {}

    private enum SkillsTranslator implements Translator {
        INSTANCE;

        private final List<Consumer<COELangProvider.Builder>> lst = new ArrayList<>();

        public void add(ItemSkill skill,
                        @Nullable String chineseTranslate,
                        @Nullable String englishTranslate) {
            lst.add(builder -> builder.add(skill, chineseTranslate, englishTranslate));
        }

        @Override
        public COELangProvider.Builder translate(COELangProvider.Builder builder) {
            lst.forEach(c -> c.accept(builder));
            return builder;
        }
    }

    public static class SkillBuilder<T extends ItemSkill, S extends AreaStrategy> {
        private final ResourceLocation id;
        private Function<S, T> factory;
        private S strategy;
        private T skill;
        private CompoundTag defaultNbt;
        private SkillConfig<T, S> config;

        public SkillBuilder(ResourceLocation id) {
            this.id = id;
        }

        public SkillBuilder(String id) {
            this.id = CreateOreExpansion.modLoc(id);
        }

        public SkillBuilder<T, S> skill(Function<S, T> factory) {
            this.factory = factory;
            return this;
        }

        public SkillBuilder<T, S> strategy(Supplier<S> strategy) {
            this.strategy = strategy.get();
            return this;
        }

        public SkillBuilder<T, S> config(Consumer<CompoundTag> tag) {
            if (defaultNbt == null) defaultNbt = new CompoundTag();
            tag.accept(defaultNbt);
            return this;
        }

        public SkillBuilder<T, S> config(SkillConfig<T, S> c) {
            this.config = c;
            return config((Consumer<CompoundTag>) c);
        }

        public SkillBuilder<T, S> level(int level) {
            return config(nbt -> nbt.putInt("Level", level));
        }

        public SkillBuilder<T, S> translate(@Nullable String chineseTranslate,
                                            @Nullable String englishTranslate) {
            if (factory == null) throw new NullPointerException("Factory cannot be null");
            // 允许strategy为null，支持没有strategy的技能
            if (skill == null) skill = factory.apply(strategy);
            if (skill == null) throw new NullPointerException("Skill cannot be null");
            SkillsTranslator.INSTANCE.add(skill, chineseTranslate, englishTranslate);
            return this;
        }

        public RegisteredDataSkill register() {
            if (factory == null) throw new NullPointerException("Factory cannot be null");
            // 允许strategy为null，支持没有strategy的技能
            if (skill == null) skill = factory.apply(strategy);
            if (skill == null) throw new NullPointerException("Skill cannot be null");
            SKILLS.put(id, skill);
            SKILL_IDS.put(skill, id);
            // 只有在strategy不为null时才放入STRATEGIES map
            if (strategy != null) {
                STRATEGIES.put(skill, strategy);
                RENDERERS.put(strategy, strategy.getRenderer());
            }
            if (config != null) {
                CONFIGS.put(skill, config);
            }
            RegisteredDataSkill data = (defaultNbt == null)
                    ? new RegisteredDataSkill(skill)
                    : new RegisteredDataSkill(skill, config, defaultNbt, skill.getCost());
            if (config != null) {
                config.load(data, skill, strategy);
            }
            return data;
        }
    }

    public static class RegisteredDataSkill extends DataSkill {
        public RegisteredDataSkill(ItemSkill skill) {
            super(skill, null, new CompoundTag(), skill.getCost());
        }

        public RegisteredDataSkill(ItemSkill skill, SkillConfig<?, ?> config, CompoundTag nbt, int cost) {
            super(skill, config, nbt, cost);
            if (config != null) {
                config.accept(nbt);
            }
        }

        public RegisteredDataSkill addConfig(Consumer<CompoundTag> c) {
            c.accept(nbt);
            return this;
        }

        public RegisteredDataSkill setConfig(Consumer<CompoundTag> c) {
            nbt = new CompoundTag();
            c.accept(nbt);
            return this;
        }
    }

    public static COELangProvider.Builder translate(COELangProvider.Builder builder) {
        return builder
                .add(SkillsTranslator.INSTANCE);
    }
}
