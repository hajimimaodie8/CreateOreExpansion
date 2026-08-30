package com.hjmmd_8.createoreexpansion.common;

import com.google.common.collect.Maps;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.BowCurseSkill;
import com.hjmmd_8.createoreexpansion.content.skill.BowDisarmSkill;
import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.content.skill.HoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.PlunderSkill;
import com.hjmmd_8.createoreexpansion.content.skill.SkinSkill;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowCurseConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowDisarmConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.HoeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.PlunderConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.SkillAoeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.SkinConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.FellingStrategy;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.HoeStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.EntityStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 技能注册表 —— 全模组技能的统一注册入口。
 *
 * 技能名称翻译不在此处定义，统一由
 * {@code ChineseLangProvider} / {@code EnglishLangProvider} 管理。
 */
public final class AllSkills {
    private static final Map<ResourceLocation, ItemSkill> SKILLS = Maps.newHashMap();
    private static final Map<ItemSkill, ResourceLocation> SKILL_IDS = Maps.newHashMap();

    /**
     * 已注册技能的完整数据（含配置）。
     * 用于反序列化时恢复技能配置，保证物品从存档/网络加载后仍能正常释放技能。
     */
    private static final Map<ResourceLocation, RegisteredDataSkill> SKILL_DATA = Maps.newHashMap();

    // ========== 伐树（斧类连锁砍树）—— 数值统一在 FellingConfigs 修改 ==========
    /** 伐树（一技能多等级：addSkills(FELL, 等级) 按等级取实际配置，Lv1 翡翠斧/ Lv2 黄玉斧/ Lv3 蓝宝石斧） */
    public static final RegisteredDataSkill FELL =
            skill("fell", FellingSkill.class, FellingStrategy.class)
                    .skill(FellingSkill::new)
                    .strategy(FellingStrategy::new)
                    .config(FellingConfigs.config(FellingConfigs.LEVEL_1))
                    .configsByLevel(level -> FellingConfigs.config(FellingConfigs.level(level)))
                    .register();
    // 伐树 Lv4（范围再扩大）、Lv5（无视范围限制）为预留等级：数值已在 FellingConfigs 定义，
    // 将来启用时 addSkills(FELL, 4/5) 即可，无需新增注册。

    // ========== 开岩（稿类范围挖掘）—— 数值统一在 SkillAoeConfigs 修改 ==========
    /** 开岩（一技能多等级：addSkills(SHATTER, 等级) 按等级取实际配置，Lv1 翡翠稿/ Lv2 黄玉稿/ Lv3 蓝宝石稿） */
    public static final RegisteredDataSkill SHATTER =
            skill("shatter", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(AreaAoeSkill::new)
                    .strategy(AreaAoeStrategy::new)
                    .config(SkillAoeConfigs.aoeConfig(SkillAoeConfigs.BREAK_ROCK_TAG, SkillAoeConfigs.BREAK_ROCK_1))
                    .configsByLevel(level -> SkillAoeConfigs.aoeConfig(
                            SkillAoeConfigs.BREAK_ROCK_TAG, SkillAoeConfigs.breakRockLevel(level)))
                    .register();
    // 开岩 Lv4（5×5）、Lv5（5×5×2）为预留等级：数值已在 SkillAoeConfigs 定义，
    // 将来启用时 addSkills(SHATTER, 4/5) 即可，无需新增注册。

    // ========== 引渠 / 平场（铲类范围挖掘）—— 数值统一在 SkillAoeConfigs 修改 ==========
    /** 引渠（一技能多等级：addSkills(CHANNEL, 等级) 按等级取实际配置，Lv1 翡翠铲/ Lv2 黄玉铲/ Lv3 蓝宝石铲） */
    public static final RegisteredDataSkill CHANNEL =
            skill("channel", AreaAoeSkill.class, AreaAoeStrategy.class)
                    .skill(AreaAoeSkill::new)
                    .strategy(AreaAoeStrategy::new)
                    .config(SkillAoeConfigs.aoeConfig(SkillAoeConfigs.CHANNEL_TAG, SkillAoeConfigs.CHANNEL_1))
                    .configsByLevel(level -> SkillAoeConfigs.aoeConfig(
                            SkillAoeConfigs.CHANNEL_TAG, SkillAoeConfigs.channelLevel(level)))
                    .register();
    // 引渠 Lv4（8 格）、Lv5（10 格）为预留等级：数值已在 SkillAoeConfigs 定义，
    // 将来启用时 addSkills(CHANNEL, 4/5) 即可，无需新增注册。
    /** 平场（一技能多等级：addSkills(GRADE, 等级) 按等级取实际配置，Lv1 蓝宝石铲/ Lv2/ Lv3） */
    public static final RegisteredDataSkill GRADE =
        skill("grade", AreaAoeSkill.class, AreaAoeStrategy.class)
                .skill(AreaAoeSkill::new)
                .strategy(AreaAoeStrategy::new)
                .config(SkillAoeConfigs.aoeConfig(SkillAoeConfigs.GRADE_TAG, SkillAoeConfigs.GRADE_1))
                .configsByLevel(level -> SkillAoeConfigs.aoeConfig(
                        SkillAoeConfigs.GRADE_TAG, SkillAoeConfigs.gradeLevel(level)))
                .maxLevel(3)
                .register();
    // 平场 Lv2（5×7）、Lv3（7×7）为预留等级：数值已在 SkillAoeConfigs 定义（平场仅 3 级），
    // 将来启用时 addSkills(GRADE, 2/3) 即可，无需新增注册。

    // ========== 剥取（剑类额外掉落）—— 数值统一在 SkinConfigs 修改 ==========
    /** 剥取（一技能多等级：addSkills(SKIN, 等级) 按等级取实际配置，Lv1 翡翠剑/ Lv2 黄玉剑/ Lv3 蓝宝石剑） */
    public static final RegisteredDataSkill SKIN =
            skill("skin", SkinSkill.class, EntityStrategy.class)
                    .skill(SkinSkill::new)
                    .strategy(EntityStrategy::new)
                    .config(SkinConfigs.config(SkinConfigs.LEVEL_1))
                    .configsByLevel(level -> SkinConfigs.config(SkinConfigs.level(level)))
                    .register();
    // 剥取 Lv4（87%，25/25/50 掉0/1/2）、Lv5（95%，25/50/25 掉1/2/3）为预留等级：
    // 数值已在 SkinConfigs 定义，将来启用时 addSkills(SKIN, 4/5) 即可，无需新增注册。

    // ========== 夺取（剑类夺取装备 + 吸血）—— 数值统一在 PlunderConfigs 修改 ==========
    /** 夺取（一技能多等级：addSkills(PLUNDER, 等级) 按等级取实际配置，Lv1 黄玉剑键二/ Lv2 蓝宝石剑键二） */
    public static final RegisteredDataSkill PLUNDER =
            skill("plunder", PlunderSkill.class, EntityStrategy.class)
                    .skill(PlunderSkill::new)
                    .strategy(EntityStrategy::new)
                    .config(PlunderConfigs.config(PlunderConfigs.LEVEL_1))
                    .configsByLevel(level -> PlunderConfigs.config(PlunderConfigs.level(level)))
                    .register();
    // 夺取 Lv3~Lv5 为预留等级：数值已在 PlunderConfigs 定义，
    // 将来启用时 addSkills(PLUNDER, 3/4/5) 即可，无需新增注册。

    // ========== 耕作（锄头）—— 数值统一在 HoeConfigs 修改 ==========
    /** 耕作（一技能多等级：addSkills(HOE, 等级) 按等级取实际配置，Lv1 翡翠锄/ Lv2 黄玉锄/ Lv3 蓝宝石锄） */
    public static final RegisteredDataSkill HOE =
            skill("hoe", HoeSkill.class, HoeStrategy.class)
                    .skill(HoeSkill::new)
                    .strategy(HoeStrategy::new)
                    .config(HoeConfigs.config(HoeConfigs.LEVEL_1))
                    .configsByLevel(level -> HoeConfigs.config(HoeConfigs.level(level)))
                    .register();
    // 耕作 Lv4（5×7）、Lv5（7×7）为预留等级：数值已在 HoeConfigs 定义，
    // 将来启用时 addSkills(HOE, 4/5) 即可，无需新增注册。

    // ========== 翠玉之弓技能（传说武器：能量上限 2000，一技能多等级，数值统一在 BowCurseConfigs/BowDisarmConfigs 修改） ==========
    /** 凋零诅咒（弓技能一：命中附加凋零+缓慢+药水云） */
    public static final RegisteredDataSkill BOW_CURSE =
            skill("bow_curse", BowCurseSkill.class, SkillStrategy.class)
                    .skill(s -> new BowCurseSkill())
                    .config(BowCurseConfigs.config(BowCurseConfigs.LEVEL_1))
                    .configsByLevel(level -> BowCurseConfigs.config(BowCurseConfigs.level(level)))
                    .register();
    // 凋零诅咒 Lv4/Lv5 为预留等级：数值已在 BowCurseConfigs 定义，
    // 将来启用时 addSkills(BOW_CURSE, 4/5) 即可，无需新增注册。
    /** 缴械风暴（弓技能二：范围缴械+怪物扒装备） */
    public static final RegisteredDataSkill BOW_DISARM =
            skill("bow_disarm", BowDisarmSkill.class, SkillStrategy.class)
                    .skill(s -> new BowDisarmSkill())
                    .config(BowDisarmConfigs.config(BowDisarmConfigs.LEVEL_1))
                    .configsByLevel(level -> BowDisarmConfigs.config(BowDisarmConfigs.level(level)))
                    .register();
    // 缴械风暴 Lv4/Lv5 为预留等级：数值已在 BowDisarmConfigs 定义，
    // 将来启用时 addSkills(BOW_DISARM, 4/5) 即可，无需新增注册。

    // ========== 工具方法 ==========
    /**
     * 创建技能构建器。
     *
     * @param skillType    技能类（用于泛型推断与注册校验）
     * @param strategyType 策略类（用于泛型推断与注册校验）
     */
    public static <T extends ItemSkill, S extends SkillStrategy<?>> SkillBuilder<T, S> skill(
            ResourceLocation id, Class<T> skillType, Class<S> strategyType) {
        return new SkillBuilder<>(id, skillType, strategyType);
    }

    public static <T extends ItemSkill, S extends SkillStrategy<?>> SkillBuilder<T, S> skill(
            String id, Class<T> skillType, Class<S> strategyType) {
        return new SkillBuilder<>(id, skillType, strategyType);
    }

    public static ItemSkill get(ResourceLocation id) {
        return id == null ? null : SKILLS.getOrDefault(id, null);
    }

    /**
     * 获取已注册技能的完整数据（含配置），用于反序列化恢复。
     */
    public static RegisteredDataSkill getData(ResourceLocation id) {
        return id == null ? null : SKILL_DATA.get(id);
    }

    public static ResourceLocation getId(ItemSkill skill) {
        return skill == null ? null : SKILL_IDS.get(skill);
    }

    public static class SkillBuilder<T extends ItemSkill, S extends SkillStrategy<?>> {
        private final ResourceLocation id;
        private final Class<T> skillType;
        private final Class<S> strategyType;
        private Function<S, T> factory;
        private S strategy;
        private T skill;
        private CompoundTag defaultNbt;
        private SkillConfig config;
        /** 等级 → 配置 映射（一技能多等级：addSkills(技能, 等级) 时按等级取实际配置） */
        private Function<Integer, SkillConfig> configsByLevel;
        /** 技能满级（技能提升附魔提升等级的上限；未声明默认 5） */
        private int maxLevel = 5;

        public SkillBuilder(ResourceLocation id, Class<T> skillType, Class<S> strategyType) {
            this.id = id;
            this.skillType = skillType;
            this.strategyType = strategyType;
        }

        public SkillBuilder(String id, Class<T> skillType, Class<S> strategyType) {
            this(CreateOreExpansion.modLoc(id), skillType, strategyType);
        }

        public SkillBuilder<T, S> skill(Function<S, T> factory) {
            this.factory = factory;
            return this;
        }

        public SkillBuilder<T, S> strategy(Supplier<S> strategy) {
            this.strategy = strategy.get();
            return this;
        }

        public SkillBuilder<T, S> setTag(Consumer<CompoundTag> tag) {
            if (defaultNbt == null) defaultNbt = new CompoundTag();
            tag.accept(defaultNbt);
            return this;
        }

        public SkillBuilder<T, S> config(SkillConfig c) {
            this.config = c;
            return setTag(c);
        }

        /**
         * 注册「等级 → 配置」映射（一技能多等级）。
         *
         * <p>{@code AllItems.addSkills(技能, 等级)} 时，等级参数会同时决定
         * 显示等级与实际数值等级（从映射取对应配置）。未设置映射时等级仅用于显示。</p>
         *
         * @param configsByLevel 输入等级（1 起）返回该等级的实际配置
         */
        public SkillBuilder<T, S> configsByLevel(Function<Integer, SkillConfig> configsByLevel) {
            this.configsByLevel = configsByLevel;
            return this;
        }

        /**
         * 声明技能满级（技能提升附魔提升等级的上限，默认 5）。
         * 例如平场仅有 Lv1~3 配置，应声明 {@code maxLevel(3)}。
         */
        public SkillBuilder<T, S> maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public SkillBuilder<T, S> level(int level) {
            return setTag(nbt -> nbt.putInt("Level", level));
        }

        public RegisteredDataSkill register() {
            T built = createSkill();
            // 注册时校验策略类型，防止配置错误（与声明类型不符）
            if (strategy != null && !strategyType.isInstance(strategy)) {
                throw new IllegalArgumentException(
                        "Strategy type mismatch for skill " + id + ": expected "
                                + strategyType.getSimpleName() + " but got "
                                + strategy.getClass().getSimpleName());
            }
            SKILLS.put(id, built);
            SKILL_IDS.put(built, id);
            RegisteredDataSkill data = (defaultNbt == null)
                    ? new RegisteredDataSkill(built, configsByLevel)
                    : new RegisteredDataSkill(built, config, configsByLevel, defaultNbt);
            data.maxLevel = this.maxLevel;
            if (config != null) {
                config.load(data);
                // 将配置载入技能实例，保证 getCost() 等字段返回注册时的真实值
                ConfigSkill.loadConfig(built, config, data);
            }
            SKILL_DATA.put(id, data);
            return data;
        }

        private T createSkill() {
            if (factory == null) throw new NullPointerException("Factory cannot be null");
            // 允许strategy为null，支持没有strategy的技能
            if (skill == null) skill = factory.apply(strategy);
            if (skill == null) throw new NullPointerException("Skill cannot be null");
            if (!skillType.isInstance(skill)) {
                throw new IllegalArgumentException(
                        "Skill type mismatch for " + id + ": expected "
                                + skillType.getSimpleName() + " but got "
                                + skill.getClass().getSimpleName());
            }
            return skill;
        }
    }

    public static class RegisteredDataSkill extends DataSkill {
        /** 等级 → 配置 映射（一技能多等级；null 表示无映射） */
        private final Function<Integer, SkillConfig> configsByLevel;
        /** 技能满级（技能提升附魔提升等级的上限） */
        private int maxLevel = 5;

        public RegisteredDataSkill(ItemSkill skill) {
            super(skill, null, new CompoundTag());
            this.configsByLevel = null;
        }

        public RegisteredDataSkill(ItemSkill skill, SkillConfig config, CompoundTag nbt) {
            super(skill, config, nbt);
            this.configsByLevel = null;
            if (config != null) {
                config.accept(nbt);
            }
        }

        public RegisteredDataSkill(ItemSkill skill, Function<Integer, SkillConfig> configsByLevel) {
            super(skill, null, new CompoundTag());
            this.configsByLevel = configsByLevel;
        }

        public RegisteredDataSkill(ItemSkill skill, SkillConfig config,
                                   Function<Integer, SkillConfig> configsByLevel, CompoundTag nbt) {
            super(skill, config, nbt);
            this.configsByLevel = configsByLevel;
            if (config != null) {
                config.accept(nbt);
            }
        }

        /**
         * 按等级取该技能的实际配置（一技能多等级）。
         *
         * @param level 技能等级（1 起）
         * @return 对应等级的配置；无映射时返回注册默认配置
         */
        public SkillConfig configForLevel(int level) {
            if (configsByLevel == null) return config;
            SkillConfig levelConfig = configsByLevel.apply(Math.max(1, level));
            return levelConfig != null ? levelConfig : config;
        }

        /** 技能满级（技能提升附魔提升等级的上限） */
        public int maxLevel() {
            return maxLevel;
        }
    }
}
