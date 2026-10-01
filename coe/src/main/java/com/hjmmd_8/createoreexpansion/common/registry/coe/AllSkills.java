package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.google.common.collect.Maps;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowCurseConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowDisarmConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.FallGuardConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.HoeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.PlunderConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.SkillAoeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.SkinConfigs;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.MetadataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
            skill("fell", SkillType.EXCAVATION_SKILL)
                    .config(FellingConfigs.config(FellingConfigs.LEVEL_1))
                    .configsByLevel(level -> FellingConfigs.config(FellingConfigs.level(level)))
                    .register();
    // 伐树 Lv4（范围再扩大）、Lv5（无视范围限制）为预留等级：数值已在 FellingConfigs 定义，
    // 将来启用时 addSkills(FELL, 4/5) 即可，无需新增注册。

    // ========== 开岩（稿类范围挖掘）—— 数值统一在 SkillAoeConfigs 修改 ==========
    /** 开岩（一技能多等级：addSkills(SHATTER, 等级) 按等级取实际配置，Lv1 翡翠稿/ Lv2 黄玉稿/ Lv3 蓝宝石稿） */
    public static final RegisteredDataSkill SHATTER =
            skill("shatter", SkillType.EXCAVATION_SKILL)
                    .config(SkillAoeConfigs.aoeConfig(SkillAoeConfigs.BREAK_ROCK_TAG, SkillAoeConfigs.BREAK_ROCK_1))
                    .configsByLevel(level -> SkillAoeConfigs.aoeConfig(
                            SkillAoeConfigs.BREAK_ROCK_TAG, SkillAoeConfigs.breakRockLevel(level)))
                    .register();
    // 开岩 Lv4（5×5）、Lv5（5×5×2）为预留等级：数值已在 SkillAoeConfigs 定义，
    // 将来启用时 addSkills(SHATTER, 4/5) 即可，无需新增注册。

    // ========== 引渠 / 平场（铲类范围挖掘）—— 数值统一在 SkillAoeConfigs 修改 ==========
    /** 引渠（一技能多等级：addSkills(CHANNEL, 等级) 按等级取实际配置，Lv1 翡翠铲/ Lv2 黄玉铲/ Lv3 蓝宝石铲） */
    public static final RegisteredDataSkill CHANNEL =
            skill("channel", SkillType.EXCAVATION_SKILL)
                    .config(SkillAoeConfigs.aoeConfig(SkillAoeConfigs.CHANNEL_TAG, SkillAoeConfigs.CHANNEL_1))
                    .configsByLevel(level -> SkillAoeConfigs.aoeConfig(
                            SkillAoeConfigs.CHANNEL_TAG, SkillAoeConfigs.channelLevel(level)))
                    .register();
    // 引渠 Lv4（8 格）、Lv5（10 格）为预留等级：数值已在 SkillAoeConfigs 定义，
    // 将来启用时 addSkills(CHANNEL, 4/5) 即可，无需新增注册。
    /** 平场（一技能多等级：addSkills(GRADE, 等级) 按等级取实际配置，Lv1 蓝宝石铲/ Lv2/ Lv3） */
    public static final RegisteredDataSkill GRADE =
        skill("grade", SkillType.EXCAVATION_SKILL)
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
            skill("skin", SkillType.HIT_SKILL)
                    .config(SkinConfigs.config(SkinConfigs.LEVEL_1))
                    .configsByLevel(level -> SkinConfigs.config(SkinConfigs.level(level)))
                    .register();
    // 剥取 Lv4（87%，25/25/50 掉0/1/2）、Lv5（95%，25/50/25 掉1/2/3）为预留等级：
    // 数值已在 SkinConfigs 定义，将来启用时 addSkills(SKIN, 4/5) 即可，无需新增注册。

    // ========== 夺取（剑类夺取装备 + 吸血）—— 数值统一在 PlunderConfigs 修改 ==========
    /** 夺取（一技能多等级：addSkills(PLUNDER, 等级) 按等级取实际配置，Lv1 黄玉剑键二/ Lv2 蓝宝石剑键二） */
    public static final RegisteredDataSkill PLUNDER =
            skill("plunder", SkillType.HIT_SKILL)
                    .config(PlunderConfigs.config(PlunderConfigs.LEVEL_1))
                    .configsByLevel(level -> PlunderConfigs.config(PlunderConfigs.level(level)))
                    .register();
    // 夺取 Lv3~Lv5 为预留等级：数值已在 PlunderConfigs 定义，
    // 将来启用时 addSkills(PLUNDER, 3/4/5) 即可，无需新增注册。

    // ========== 耕作（锄头）—— 数值统一在 HoeConfigs 修改 ==========
    /** 耕作（一技能多等级：addSkills(HOE, 等级) 按等级取实际配置，Lv1 翡翠锄/ Lv2 黄玉锄/ Lv3 蓝宝石锄） */
    public static final RegisteredDataSkill HOE =
            skill("hoe", SkillType.USE_SKILL)
                    .config(HoeConfigs.config(HoeConfigs.LEVEL_1))
                    .configsByLevel(level -> HoeConfigs.config(HoeConfigs.level(level)))
                    .register();
    // 耕作 Lv4（5×7）、Lv5（7×7）为预留等级：数值已在 HoeConfigs 定义，
    // 将来启用时 addSkills(HOE, 4/5) 即可，无需新增注册。

    // ========== 翠玉之弓技能（传说武器：能量上限 2000，一技能多等级，数值统一在 BowCurseConfigs/BowDisarmConfigs 修改） ==========
    /** 凋零诅咒（弓技能一：命中附加凋零+缓慢+药水云） */
    public static final RegisteredDataSkill BOW_CURSE =
            skill("bow_curse", SkillType.USE_SKILL)
                    .config(BowCurseConfigs.config(BowCurseConfigs.LEVEL_1))
                    .configsByLevel(level -> BowCurseConfigs.config(BowCurseConfigs.level(level)))
                    .register();
    // 凋零诅咒 Lv4/Lv5 为预留等级：数值已在 BowCurseConfigs 定义，
    // 将来启用时 addSkills(BOW_CURSE, 4/5) 即可，无需新增注册。
    /** 缴械风暴（弓技能二：范围缴械+怪物扒装备） */
    public static final RegisteredDataSkill BOW_DISARM =
            skill("bow_disarm", SkillType.USE_SKILL)
                    .config(BowDisarmConfigs.config(BowDisarmConfigs.LEVEL_1))
                    .configsByLevel(level -> BowDisarmConfigs.config(BowDisarmConfigs.level(level)))
                    .register();
    // 缴械风暴 Lv4/Lv5 为预留等级：数值已在 BowDisarmConfigs 定义，
    // 将来启用时 addSkills(BOW_DISARM, 4/5) 即可，无需新增注册。

    // ========== 装备（护甲）技能 —— 数值统一在各自的 *Configs 里改 ==========
    /**
     * 虚衡坠护（翠玉套 · 槽位 1）：被动摔落豁免 + 长按 100% 豁免。
     *
     * <p><b>装备技能 3 级封顶</b>（用户 2026-10-01："和工具的五级封顶不一样"）。数值见
     * {@link FallGuardConfigs}；执行由 {@code ArmorSkillRuntime} + {@code ArmorSkillHandler}
     * 承担（长按语义，内核没有"按住期间持续生效"的模型）。</p>
     */
    public static final RegisteredDataSkill FALL_GUARD =
            skill("fall_guard", SkillType.USE_SKILL)
                    // 刻意<b>不</b>挂 .config(...)/.configsByLevel(...)：护甲技能的执行走
                    // ArmorSkillRuntime（长按语义），数值由 FallGuardConfigs 按等级直接取，
                    // 不经内核的 config 解析链。这里只登记 id / 类型 / 等级上限。
                    .maxLevel(FallGuardConfigs.MAX_LEVEL)
                    .register();

    /**
     * 蓄能疾骋（翠玉套 · 槽位 2）：长按蓄力，松手按到达段数给对应时长与等级的<b>迅捷</b>。
     *
     * <p>与虚衡坠护同样：执行在 {@code ArmorSkillRuntime}（长按语义）、数值在
     * {@link ChargeDashConfigs}，这里只登记 id / 类型 / 等级上限，不走内核的 config 解析链。</p>
     *
     * <p>⚠ 能量总量（300 / 250 / 200）是<b>我给的默认值</b>；用户确认后只改 {@code ChargeDashConfigs} 一个文件。</p>
     */
    public static final RegisteredDataSkill CHARGE_DASH =
            skill("charge_dash", SkillType.USE_SKILL)
                    .maxLevel(ChargeDashConfigs.MAX_LEVEL)
                    .register();

    // ========== 工具方法 ==========
    /**
     * 创建技能构建器（<b>唯一的注册入口</b>，2026-09-30 换核后只剩这一种形态）。
     *
     * <p>技能执行已整体迁到 Skiller 新内核，旧自研实现类（{@code FellingSkill} 等）与其策略类
     * 已全部删除，因此注册<b>不再需要传入技能类 / 策略类 / 工厂</b>：每条条目由
     * {@link MetadataSkill} 承载，只提供 id、类型、配置与等级映射。
     * <b>存档格式一字不变</b>（{@code skillId + NBT}，见 {@link DataSkill#toString()}）。</p>
     *
     * @param id   技能注册 id（{@code createoreexpansion:xxx}，必须与旧注册一字不差）
     * @param type 技能类型（决定触发器 / 上下文族）
     */
    public static SkillBuilder<ItemSkill, Object> skill(String id, SkillType type) {
        return new SkillBuilder<ItemSkill, Object>(CoeCore.modLoc(id), ItemSkill.class, Object.class)
                .metadata(type);
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

    public static class SkillBuilder<T extends ItemSkill, S> {
        private final ResourceLocation id;
        private final Class<T> skillType;
        /**
         * 元数据壳（2026-09-30 起<b>唯一</b>模式）。
         *
         * <p>{@link #createSkill()} 直接返回它，不再实例化任何旧技能实现类 ——
         * 技能执行已迁到 Skiller 新内核，本类只负责「注册 id + 类型 + 配置」。</p>
         */
        private MetadataSkill metadata;
        private CompoundTag defaultNbt;
        private SkillConfig config;
        /** 等级 → 配置 映射（一技能多等级：addSkills(技能, 等级) 时按等级取实际配置） */
        private Function<Integer, SkillConfig> configsByLevel;
        /** 技能满级（技能提升附魔提升等级的上限；未声明默认 5） */
        private int maxLevel = 5;

        /**
         * @param id            技能注册 id
         * @param skillType     技能实例类型（恒为 {@link ItemSkill}，保留作注册期自检）
         * @param ignoredType   历史遗留的策略类型参数，已无用途（换核后策略归新内核）
         */
        public SkillBuilder(ResourceLocation id, Class<T> skillType, Class<S> ignoredType) {
            this.id = id;
            this.skillType = skillType;
        }

        /**
         * 元数据壳模式：注册条目只承载 id + 类型，不实例化任何旧技能实现类。
         *
         * @param type 技能类型（决定触发器，与旧实现在新内核里声明的类型一致）
         */
        public SkillBuilder<T, S> metadata(SkillType type) {
            this.metadata = new MetadataSkill(this.id, type);
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
         * <p>{@code CoeItems} 注册链上的 {@code addSkills(技能, 等级)} 时，等级参数会同时决定
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
            SKILLS.put(id, built);
            SKILL_IDS.put(built, id);
            RegisteredDataSkill data = (defaultNbt == null)
                    ? new RegisteredDataSkill(built, configsByLevel)
                    : new RegisteredDataSkill(built, config, configsByLevel, defaultNbt);
            data.maxLevel = this.maxLevel;
            if (config != null) {
                config.load(data);
                // 2026-09-30 第 4 阶段：不再往技能实例里灌配置（旧式 ConfigSkill 通路
                // 已随旧执行层删除）。真正生效的消耗与冷却由新内核从同一份 SkillConfig 读取。
            }
            SKILL_DATA.put(id, data);
            return data;
        }

        /**
         * 产出该条目的技能实例 —— 换核后<b>只有元数据壳一条路径</b>。
         *
         * <p>旧的「工厂 + 策略」分支已随之删除：所有条目都走 {@link #metadata(SkillType)}。</p>
         */
        private T createSkill() {
            if (metadata == null) {
                throw new IllegalStateException("Skill " + id + " was registered without metadata(type)");
            }
            if (!skillType.isInstance(metadata)) {
                throw new IllegalArgumentException(
                        "Skill type mismatch for " + id + ": expected "
                                + skillType.getSimpleName() + " but got "
                                + metadata.getClass().getSimpleName());
            }
            @SuppressWarnings("unchecked")
            T asMeta = (T) metadata;
            return asMeta;
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
