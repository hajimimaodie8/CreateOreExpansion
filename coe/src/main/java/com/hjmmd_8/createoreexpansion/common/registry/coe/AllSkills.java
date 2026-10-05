package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.google.common.collect.Maps;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.skill.ArmorSkillRuntime;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item.BoomerangSkillConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BloodPactConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowCurseConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowDisarmConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.tool.FellingConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.tool.HoeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.PlunderConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.tool.SkillAoeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.SkinConfigs;
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

    // ========== 血契置换（剑类：命中时按百分比双向交换血量）—— 数值统一在 BloodPactConfigs 修改 ==========
    /**
     * 血契置换（coe-pact 批 1 注册 id / 类型 / 配置；<b>批 2 已把执行体填进
     * {@code BloodPactItemSkill}</b>）。
     *
     * <p>语义（需求 §3.1 / §3.4，作者 2026-10-03）：按住技能键攻击 ⇒ <b>先</b>把双方的
     * 当前血量<b>百分比</b>互换（玩家新血 = 上限_p × 对方百分比，对方新血 = 上限_t × 玩家百分比；
     * 作者验收例 20/20 ⇄ 30/40 ⇒ 15/20 与 40/40），<b>再</b>照常造成这次攻击的伤害 ——
     * 因此"未受攻击之前"的四个血量必须在造成伤害<b>之前</b>冻结。</p>
     *
     * <p>范围：</p>
     * <ul>
     *   <li><b>数值</b>在 {@link BloodPactConfigs}（消耗 = 100 × 等级；冷却 600 tick = 30 秒，
     *       1~5 级恒定；<b>交换比例与等级无关</b> ⇒ 比例不进等级表，需求 §六 #4）；</li>
     *   <li><b>执行体</b>（交换 → 照常伤害 → 扣能 → 冷却）在 {@code BloodPactItemSkill}；</li>
     *   <li><b>冷却载体</b> = 按技能记的 {@code PerSkillCooldown}（作者 2026-10-03 裁定 B）——
     *       同一把剑上的剥取 / 夺取不受它影响；</li>
     *   <li><b>两把剑的基准等级绑定</b>（蓝宝石剑 / 星辉石剑，键三）<b>仍未绑</b>，属 coe-pact
     *       批 3 —— 所以现在没有任何物品携带本技能；</li>
     *   <li><b>内核白名单</b>已登记（{@code SkillerIntegration#registerHitSkills}，
     *       实现槽位 = {@code BloodPactItemSkill}）：漏登记 ⇒ 物品上这个技能实例
     *       反序列化返回 null，症状是"看着接好了、其实没绑上"。</li>
     * </ul>
     *
     * <p>{@code maxLevel} 用默认值 5（与工具技能一致；装备技能才是 3）。</p>
     */
    public static final RegisteredDataSkill BLOOD_PACT =
            skill("blood_pact", SkillType.HIT_SKILL)
                    .config(BloodPactConfigs.config(BloodPactConfigs.LEVEL_1))
                    .configsByLevel(level -> BloodPactConfigs.config(BloodPactConfigs.level(level)))
                    .register();

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

    // ========== 三条「专属弓技能」正式条目（2026-10-05 弓技能批 7）==========
    // 它们与上面那对<b>不同</b>：上面那对 id 被四把弓复用（所以等级上限只能住在 BowTier），
    // 这三条<b>各自只被一把弓携带</b>（宝石 / 星界 / 雷鸣），因此等级上限可以、也应该写在这里。
    //
    // 为什么写 maxLevel(3)：作者明令三条技能都是<b>3 级封顶</b>；而三条各自的档位行里
    // BowTier#maxSkillLevel() 也正好是 3（SAPPHIRE_RUBY / ASTRAL / THUNDER），
    // 两处同值 ⇒ <b>不冲突</b>（关卡 §30e-6 把这条一致性逐条钉住）。
    //
    // 为什么<b>不挂</b> .config(...) / .configsByLevel(...)：三张数值表的类型是
    // BowWaveShiftConfigs / BowAstralBarrageConfigs / BowThunderMightConfigs（静态表，
    // 不是 SkillConfig），执行也不在内核里 —— 内核侧 release 只写「这一发要换成 X」的标记，
    // 真正的发射在 JadeTopazBowItem#shoot 读标记之后交给各自的 launcher
    // （与 pierce / orbit / 装备技能「登记只为反序列化」同一形状，见 SkillerIntegration）。
    /** 量波置换（宝石弓专属 · 键三 G） */
    public static final RegisteredDataSkill BOW_WAVE_SHIFT =
            skill("bow_wave_shift", SkillType.USE_SKILL)
                    .maxLevel(3)
                    .register();
    /** 星元波置（星界弓专属 · 键三 G） */
    public static final RegisteredDataSkill BOW_ASTRAL_BARRAGE =
            skill("bow_astral_barrage", SkillType.USE_SKILL)
                    .maxLevel(3)
                    .register();
    /** 雷鸣神力（雷鸣弓专属 · 键三 G） */
    public static final RegisteredDataSkill BOW_THUNDER_MIGHT =
            skill("bow_thunder_might", SkillType.USE_SKILL)
                    .maxLevel(3)
                    .register();

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
                    .maxLevel(ArmorSkillRuntime.MAX_EQUIPMENT_SKILL_LEVEL)
                    .register();

    /**
     * 蓄能疾骋（翠玉套 · 槽位 2；<b>用户 2026-10-01 更正后同时也是宝石套 · 槽位 2</b> ——
     * 从翠玉套移植：同一个技能 id、同一套数值，翠玉套那份保持不动）：
     * 长按蓄力，按住期间按到达段数给对应时长与等级的<b>迅捷</b>。
     *
     * <p>与虚衡坠护同样：执行在 {@code ArmorSkillRuntime}（长按语义）、数值在
     * {@link ChargeDashConfigs}，这里只登记 id / 类型 / 等级上限，不走内核的 config 解析链。
     * 行为按技能 id 分派，与是哪一套无关。</p>
     *
     * <p>⚠ 能量总量（300 / 250 / 200）是<b>我给的默认值</b>；用户确认后只改 {@code ChargeDashConfigs} 一个文件。</p>
     */
    public static final RegisteredDataSkill CHARGE_DASH =
            skill("charge_dash", SkillType.USE_SKILL)
                    .maxLevel(ArmorSkillRuntime.MAX_EQUIPMENT_SKILL_LEVEL)
                    .register();

    /**
     * 绝境守护（宝石套 · 槽位 1，基准等级 1）：被动高额伤害按概率豁免 + 长按分段给不死图腾。
     *
     * <p>与上面两条同样：执行在 {@code ArmorSkillRuntime}（长按语义）与
     * {@code LastStandHandler}（被动触发），数值在 {@link LastStandConfigs}，
     * 这里只登记 id / 类型 / 等级上限，<b>不挂</b> {@code .config(...)}（不走内核的 config 解析链）。</p>
     */
    public static final RegisteredDataSkill LAST_STAND =
            skill("last_stand", SkillType.USE_SKILL)
                    .maxLevel(ArmorSkillRuntime.MAX_EQUIPMENT_SKILL_LEVEL)
                    .register();

    /**
     * 临域充力（宝石套 · 槽位 3，基准等级 1 —— 用户 2026-10-01 更正：原先记成槽位 2 / 基准 2）：
     * 应力注入 / 手摇曲柄判定 / 环绕粒子，数值在 {@link FieldChargeConfigs}，
     * 执行在 {@code ArmorSkillRuntime} + {@code FieldChargeRuntime}。
     */
    public static final RegisteredDataSkill FIELD_CHARGE =
            skill("field_charge", SkillType.USE_SKILL)
                    .maxLevel(ArmorSkillRuntime.MAX_EQUIPMENT_SKILL_LEVEL)
                    .register();

    /**
     * 衡元择势（星界套 · 槽位 1，基准等级 2）：融合蓄能疾骋（移速）+ 绝境守护（图腾），
     * 长按开始时按"选择判断"二选一生效，被动继承绝境守护的高额伤害图腾触发。
     *
     * <p>与上面四条同样：执行在 {@code ArmorSkillRuntime}（长按语义）与
     * {@code LastStandHandler}（被动触发），数值<b>引用</b> {@link ChargeDashConfigs} 与
     * {@link LastStandConfigs}（<b>不新建配置类</b>），这里只登记 id / 类型 / 等级上限，
     * 不挂 {@code .config(...)}（不走内核的 config 解析链）。</p>
     */
    public static final RegisteredDataSkill BALANCE_CHOICE =
            skill("balance_choice", SkillType.USE_SKILL)
                    .maxLevel(ArmorSkillRuntime.MAX_EQUIPMENT_SKILL_LEVEL)
                    .register();

    /**
     * 星芒嬗震（星界套 · 槽位 3，基准等级 1 —— 用户 2026-10-02 当日第二版更正）：
     * 点按/长按向准心发射<b>既有</b>能量波（攻击态），长按按蓄力曲线分叉。
     *
     * <p>数值在 {@code StarShockConfigs}，执行在 {@code ArmorSkillRuntime} +
     * {@code StarShockRuntime}，这里只登记 id / 类型 / 等级上限。</p>
     *
     * <p>{@code maxLevel} 仍是共享的装备技能上限（{@link ArmorSkillRuntime#MAX_EQUIPMENT_SKILL_LEVEL} = 3）：
     * 它是<b>等级上限</b>，与"星界套上的<b>基准</b>等级 1"是两回事（基准在
     * {@code ArmorSkillLevels} 里另记；附魔"技艺提升"可以把 1 抬到 2/3）。</p>
     */
    public static final RegisteredDataSkill STAR_SHOCK =
            skill("star_shock", SkillType.USE_SKILL)
                    .maxLevel(ArmorSkillRuntime.MAX_EQUIPMENT_SKILL_LEVEL)
                    .register();

    // ========== 回旋镖技能（2026-10-02 批 3 穿刺 / 批 4 环绕）—— 数值统一在 BoomerangSkillConfigs ==========
    /**
     * 穿刺（回旋镖四把共用）：投掷出去的镖可以<b>穿过</b>至多 {@code 3×等级} 个生物与
     * {@code 5×等级} 个方块（需求 §3.5），额度用完就掉头；点按与长按都生效，无独立冷却。
     *
     * <p>与上面几条"执行不住在内核"的技能同形：这里只登记 id / 类型 / 等级上限 ——</p>
     * <ul>
     *   <li><b>数值</b>在 {@link BoomerangSkillConfigs}（{@code 3L} / {@code 5L} / {@code 20L} / 上限 5）；</li>
     *   <li><b>执行</b>在 {@code AbstractBoomerangEntity#onHitEntity/onHitBlock}（镖自己的命中判定，
     *       与"碰到就回"那条老规则同一处）——<b>刻意不挂</b> {@code .config(...)} /
     *       {@code .configsByLevel(...)}：内核的 config 解析链是给"内核自己执行"的技能用的，
     *       这里挂了也只会多一份会漂移的数值；</li>
     *   <li><b>内核白名单</b>仍要登记（{@code SkillerIntegration}，与装备技能同一个无操作壳）：
     *       {@code skiller:skill} 是数据驱动白名单，漏登记 ⇒ 物品上这个技能实例反序列化不出来。</li>
     * </ul>
     *
     * <p>等级：基准取档位（{@code BoomerangTier#baseSkillLevel()} = 1/2/3/3），叠技艺提升 /
     * 技艺回溯后钳到 {@code 1..MAX_SKILL_LEVEL}；读取点唯一（{@code BoomerangItem#effectiveSkillLevel}）。</p>
     */
    public static final RegisteredDataSkill PIERCE =
            skill("pierce", SkillType.USE_SKILL)
                    .maxLevel(BoomerangSkillConfigs.MAX_SKILL_LEVEL)
                    .register();

    /**
     * 环绕（回旋镖四把共用；2026-10-02 批 4）：投掷出去的镖身边环着 {@code 等级} 枚小能量波
     * （半径 1.5、垂面、单枚伤害 {@code 2×等级}、撞方块挖掉并入回环镖）——需求 §3.6。
     *
     * <p>与 {@link #PIERCE} <b>逐条同形</b>（同一个"执行不住在内核"的形状）：</p>
     * <ul>
     *   <li><b>数值</b>在 {@link BoomerangSkillConfigs}（数量 = 等级 / 半径 1.5 / 伤害 2L /
     *       消耗 15L / 上限 5）；</li>
     *   <li><b>执行</b>在 {@code AbstractBoomerangEntity#spawnOrbitWaves}（投掷时生成 L 枚
     *       环绕波）+ 环绕波自己的既有命中链（撞生物 2L 伤害、撞方块挖掘）——
     *       这里<b>刻意不挂</b> {@code .config(...)} / {@code .configsByLevel(...)}；</li>
     *   <li><b>内核白名单</b>必须登记（{@code SkillerIntegration#registerUseSkills}，
     *       与穿刺/装备技能共用同一个无操作壳）：漏了 ⇒ 物品上这个技能实例反序列化不出来
     *       （症状是"看着接好了、其实没绑上"）。</li>
     * </ul>
     *
     * <p>⚠ 它与穿刺一样是 {@code USE_SKILL} ⇒ 右键投掷会被 {@code UseItemHandler}
     * 当成一次技能释放，而那里按<b>类型</b>（{@code BoomerangItem}）开的豁免<b>同时覆盖两者</b>
     * （它是类型判据、不是技能 id 白名单）——见该处注释（裁定 D11）。</p>
     */
    public static final RegisteredDataSkill ORBIT =
            skill("orbit", SkillType.USE_SKILL)
                    .maxLevel(BoomerangSkillConfigs.MAX_SKILL_LEVEL)
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
