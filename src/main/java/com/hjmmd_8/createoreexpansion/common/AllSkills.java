package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.content.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.content.strategy.FellingStrategy;
import com.hjmmd_8.createoreexpansion.data.lang.COELangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.Translator;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifierHolder;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.hjmmd_8.createoreexpansion.common.AllStrategies.STRATEGIES;

public final class AllSkills {
    private static final Map<ResourceLocation, ItemSkill> SKILLS = new HashMap<>();
    private static final Map<ItemSkill, ResourceLocation> SKILL_IDS = new HashMap<>();

    // ========== 公共技能实例 ==========
    public static final FellingSkill FELL =
        skill("fell", FellingSkill.class, FellingStrategy.class)
                .skill(strategy -> new FellingSkill(strategy, 0)
                        .breakBlockSpeedCorrection(tree -> Math.max(.1f, 1f / (1f + tree.logs() * .12f))))
                .strategy(new FellingStrategy(8, 200, 100, FellingStrategy.IS_LOG))
                .translate("伐树 I", "Fell I")
                .register();
    public static final FellingSkill GREAT_FELL =
        skill("great_fell", FellingSkill.class, FellingStrategy.class)
                .skill(strategy -> new FellingSkill(strategy, 100)
                        .breakBlockSpeedCorrection(tree ->
                                Math.max(.35f, 1f / (1f + tree.logs() * .05f + tree.leaves() * .005f))))
                .strategy(new FellingStrategy(12, 100, FellingStrategy.IS_TREE))
                .translate("伐树 II", "Fell II")
                .register();
    public static final FellingSkill GRAND_FELL =
            skill("grand_fell", FellingSkill.class, FellingStrategy.class)
                .skill(strategy -> new FellingSkill(strategy, 100)
                        .breakBlockSpeedCorrection(tree ->
                                Math.max(.2f, 1f / (1f + tree.logs() * .08f + tree.leaves() * .01f))))
                .strategy(new FellingStrategy(8, 100, FellingStrategy.IS_TREE))
                .translate("伐树 III", "Fell III")
                .register();

    public static final AreaAoeSkill SHATTER =
        skill("shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(strategy -> new AreaAoeSkill(strategy, 0, BlockTags.MINEABLE_WITH_PICKAXE))
                .strategy(new AreaAoeStrategy(3, 1, 1, DualDirection.fromPlayerYaw()))
                .translate("开岩 I", "Shatter I")
                .register();
    public static final AreaAoeSkill GREAT_SHATTER =
        skill("great_shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(strategy -> new AreaAoeSkill(strategy, 100, BlockTags.MINEABLE_WITH_PICKAXE))
                .strategy(new AreaAoeStrategy(3, 3, 1))
                .translate("开岩 II", "Shatter II")
                .register();
    public static final AreaAoeSkill GRAND_SHATTER =
            skill("grand_shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(strategy -> new AreaAoeSkill(strategy, 100, BlockTags.MINEABLE_WITH_PICKAXE))
                    .strategy(new AreaAoeStrategy(5, 5, 1))
                    .translate("开岩 III", "Shatter III")
                    .register();

    public static final AreaAoeSkill CHANNEL =
        skill("channel", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(strategy -> new AreaAoeSkill(strategy, 0, BlockTags.MINEABLE_WITH_SHOVEL))
                .strategy(new AreaAoeStrategy(1, 1, 6, DualDirection.fromPlayerYaw()))
                .translate("引渠 I", "Channel I")
                .register();
    public static final AreaAoeSkill GREAT_CHANNEL =
        skill("great_channel", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(strategy -> new AreaAoeSkill(strategy, 50, BlockTags.MINEABLE_WITH_SHOVEL))
                .strategy(new AreaAoeStrategy(1, 1, 8, DualDirection.fromPlayerYaw()))
                .translate("引渠 II", "Channel II")
                .register();
    public static final AreaAoeSkill GRADE =
            skill("grade", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(strategy -> new AreaAoeSkill(strategy, 50, BlockTags.MINEABLE_WITH_SHOVEL))
                    .strategy(new AreaAoeStrategy(7, 7, 1))
                    .translate("平场 I", "Grade I")
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

        public SkillBuilder<T, S> strategy(S strategy) {
            this.strategy = strategy;
            return this;
        }

        public SkillBuilder<T, S> translate(@Nullable String chineseTranslate,
                                            @Nullable String englishTranslate) {
            if (factory == null) throw new NullPointerException("Factory cannot be null");
            if (strategy == null) throw new NullPointerException("Strategy cannot be null");
            if (skill == null) skill = factory.apply(strategy);
            if (skill == null) throw new NullPointerException("Skill cannot be null");
            SkillsTranslator.INSTANCE.add(skill, chineseTranslate, englishTranslate);
            return this;
        }

        public T register() {
            if (factory == null) throw new NullPointerException("Factory cannot be null");
            if (strategy == null) throw new NullPointerException("Strategy cannot be null");
            if (skill == null) skill = factory.apply(strategy);
            if (skill == null) throw new NullPointerException("Skill cannot be null");
            SKILLS.put(id, skill);
            SKILL_IDS.put(skill, id);
            STRATEGIES.put(skill, strategy);
            return skill;
        }
    }

    public static COELangProvider.Builder translate(COELangProvider.Builder builder) {
        return builder
                .add(SkillsTranslator.INSTANCE);
    }
}
