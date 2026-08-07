package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.tool.RendererConfig;
import com.hjmmd_8.createoreexpansion.client.tool.ToolOutlineRenderer;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.content.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.content.strategy.FellingStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifierHolder;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.hjmmd_8.createoreexpansion.client.tool.RendererConfig.ALPHA;
import static com.hjmmd_8.createoreexpansion.common.AllStrategies.RENDERERS;
import static com.hjmmd_8.createoreexpansion.common.AllStrategies.STRATEGIES;

public final class AllSkills {
    private static final Map<ResourceLocation, ItemSkill> SKILLS = new HashMap<>();
    private static final Map<ItemSkill, ResourceLocation> SKILL_IDS = new HashMap<>();

    // ========== 公共技能实例 ==========
    // 斧头技能
    public static final FellingSkill JADE_AXE_AOE =
        skill("jade_axe_aoe", FellingSkill.class, FellingStrategy.class)
                .skill(strategy -> new FellingSkill(strategy, 0)
                        .breakBlockSpeedCorrection(tree -> Math.max(.1f, 1f / (1f + tree.logs() * .12f))))
                .strategy(new FellingStrategy(8, 200, 100, FellingStrategy.IS_LOG))
                .rendererConfig()
                .color(RendererConfig.JADE_GREEN)
                .build()
                .register();
    public static final FellingSkill SAPPHIRE_AXE_AOE =
        skill("sapphire_axe_aoe", FellingSkill.class, FellingStrategy.class)
            .skill(strategy -> new FellingSkill(strategy, 100)
                    .breakBlockSpeedCorrection(tree ->
                            Math.max(.2f, 1f / (1f + tree.logs() * .08f + tree.leaves() * .01f))))
            .strategy(new FellingStrategy(8, 100, FellingStrategy.IS_LOG))
            .rendererConfig()
            .color(RendererConfig.SAPPHIRE_BLUE)
            .build()
            .register();
    public static final FellingSkill TOPAZ_AXE_AOE =
        skill("topaz_axe_aoe", FellingSkill.class, FellingStrategy.class)
            .skill(strategy -> new FellingSkill(strategy, 100)
                    .breakBlockSpeedCorrection(tree ->
                            Math.max(.35f, 1f / (1f + tree.logs() * .05f + tree.leaves() * .005f))))
            .strategy(new FellingStrategy(12, 100, FellingStrategy.IS_LOG))
            .rendererConfig()
            .color(RendererConfig.TOPAZ_GOLD)
            .build()
            .register();

    // 镐子技能
    public static final AreaAoeSkill JADE_PICKAXE_AOE =
        skill("jade_pickaxe_aoe", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(strategy -> new AreaAoeSkill(strategy, 0, BlockTags.MINEABLE_WITH_PICKAXE))
                .strategy(new AreaAoeStrategy(3, 1, 1, DualDirection.fromPlayerYaw()))
                .rendererConfig()
                .color(RendererConfig.JADE_GREEN)
                .build()
                .register();
    public static final AreaAoeSkill SAPPHIRE_PICKAXE_AOE =
        skill("sapphire_pickaxe_aoe", AreaAoeSkill.class, AreaAoeStrategy.class)
            .skill(strategy -> new AreaAoeSkill(strategy, 100, BlockTags.MINEABLE_WITH_PICKAXE))
            .strategy(new AreaAoeStrategy(5, 5, 1))
            .rendererConfig()
            .color(RendererConfig.SAPPHIRE_BLUE)
            .build()
            .register();
    public static final AreaAoeSkill TOPAZ_PICKAXE_AOE =
        skill("topaz_pickaxe_aoe", AreaAoeSkill.class, AreaAoeStrategy.class)
            .skill(strategy -> new AreaAoeSkill(strategy, 100, BlockTags.MINEABLE_WITH_PICKAXE))
            .strategy(new AreaAoeStrategy(3, 3, 1))
            .rendererConfig()
            .color(RendererConfig.TOPAZ_GOLD)
            .build()
            .register();

    // 铲子技能 - 使用 AreaAoeSkill + AreaAoeStrategy
    public static final AreaAoeSkill JADE_SHOVEL_AOE =
        skill("jade_shovel_aoe", AreaAoeSkill.class, AreaAoeStrategy.class)
            .skill(strategy -> new AreaAoeSkill(strategy, 0, BlockTags.MINEABLE_WITH_SHOVEL))
            .strategy(new AreaAoeStrategy(1, 1, 6, DualDirection.fromPlayerYaw()))
            .rendererConfig()
            .color(RendererConfig.JADE_GREEN)
            .build()
            .register();
    public static final AreaAoeSkill SAPPHIRE_SHOVEL_AOE =
        skill("sapphire_shovel_aoe", AreaAoeSkill.class, AreaAoeStrategy.class)
            .skill(strategy -> new AreaAoeSkill(strategy, 50, BlockTags.MINEABLE_WITH_SHOVEL))
            .strategy(new AreaAoeStrategy(7, 1, 1, DualDirection.fromPlayerYaw()))
            .rendererConfig()
            .color(RendererConfig.SAPPHIRE_BLUE)
            .build()
            .register();
    public static final AreaAoeSkill TOPAZ_SHOVEL_AOE =
        skill("topaz_shovel_aoe", AreaAoeSkill.class, AreaAoeStrategy.class)
            .skill(strategy -> new AreaAoeSkill(strategy, 50, BlockTags.MINEABLE_WITH_SHOVEL))
            .strategy(new AreaAoeStrategy(1, 1, 8, DualDirection.fromPlayerYaw()))
            .rendererConfig()
            .color(RendererConfig.TOPAZ_GOLD)
            .build()
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
        return SKILLS.get(id);
    }

    public static ResourceLocation getId(ItemSkill skill) {
        if (skill == null) return null;
        return SKILL_IDS.get(skill);
    }

    public static <C, V> V modifier(ModifiableAttributeType<C, V> type, SkillAttributeModifierHolder holder, C context) {
        ModifiableAttribute<V> attribute = type.create(context);
        holder.modifier(type, attribute);
        return attribute.getValue();
    }

    /**
     * 获取技能对应的渲染器
     * @param skill 技能
     * @return 渲染器实例，如果不存在返回null
     */
    public static ToolOutlineRenderer getRenderer(ItemSkill skill) {
        return RENDERERS.get(skill);
    }

    /**
     * 获取所有渲染器
     * @return 渲染器数组
     */
    public static ToolOutlineRenderer[] getAllRenderers() {
        return RENDERERS.values().toArray(new ToolOutlineRenderer[0]);
    }

    public static void register() {}

    public static class SkillBuilder<T extends ItemSkill, S extends AreaStrategy> {
        private final ResourceLocation id;
        private Function<S, T> factory;
        private S strategy;
        private T skill;
        private RendererConfig rendererConfig;

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

        public RendererConfigBuilder<T, S> rendererConfig() {
            if (skill == null) skill = factory.apply(strategy);
            return new RendererConfigBuilder<>(this, skill);
        }

        public T register() {
            if (factory == null) throw new NullPointerException("Factory cannot be null");
            if (strategy == null) throw new NullPointerException("Strategy cannot be null");
            if (skill == null) skill = factory.apply(strategy);
            if (skill == null) throw new NullPointerException("Skill cannot be null");
            if (rendererConfig == null) throw new NullPointerException("RendererConfig cannot be null");
            SKILLS.put(id, skill);
            SKILL_IDS.put(skill, id);
            STRATEGIES.put(skill, strategy);

            // 自动注册渲染器
            if (skill instanceof AbstractStrategySkill<?> strategySkill) {
                ToolOutlineRenderer renderer = new ToolOutlineRenderer(
                        rendererConfig,
                        strategySkill
                );

                RENDERERS.put(skill, renderer);
            }
            return skill;
        }

        public static class RendererConfigBuilder<T extends ItemSkill, S extends AreaStrategy> {
            private final SkillBuilder<T, S> builder;
            private final ItemSkill skill;
            private float r, g, b, a;

            public RendererConfigBuilder(SkillBuilder<T, S> builder, ItemSkill skill) {
                this.builder = builder;
                this.skill = skill;
            }

            public RendererConfigBuilder<T, S> color(float r, float g, float b, float a) {
                this.r = r;
                this.g = g;
                this.b = b;
                this.a = a;
                return this;
            }

            public RendererConfigBuilder<T, S> color(float[] color) {
                this.r = color[0];
                this.g = color[1];
                this.b = color[2];
                this.a = ALPHA;
                return this;
            }

            public SkillBuilder<T, S> build() {
                builder.rendererConfig = new RendererConfig(skill, r, g, b, a);
                return builder;
            }
        }

    }
}
