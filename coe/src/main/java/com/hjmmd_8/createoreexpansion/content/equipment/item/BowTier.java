package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.SkillOutlineColors;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * <b>弓四档材质的数值真源</b>（唯一出处：物品注册读它写能量/耐久/配色）。
 *
 * <p>四档与既有装备线同一顺序：<b>翠玉（jade_topaz）→ 宝石（sapphire_ruby）→ 星界（astral）→
 * 雷鸣（thunder）</b>，形态照 {@code BoomerangTier}（回旋镖那轮的族级档位枚举）——同一件事只有一份口径。</p>
 *
 * <h2>本表含什么、不含什么</h2>
 * <ul>
 *   <li>{@link #energy()} —— <b>能量上限</b>。口径（作者 2026-10-03 指派、沿用回旋镖那轮）：
 *       <b>该套护甲全套能量的一半</b>。盔甲单件 = {@code ArmorSet.perPieceEnergy()}
 *       = 250 / 1000 / 2500 / 2500，全套 = 单件 × 4 ⇒ 一半 = 单件 × 2
 *       ⇒ <b>口径表 = 500 / 2000 / 5000 / 5000</b>。</li>
 *   <li>{@link #durability()} —— <b>耐久上限</b>。口径表 = <b>1000 / 2000 / 3500 / 3500</b>
 *       （与回旋镖同一张表）。它由物品注册经构造器交给原版
 *       {@code Item.Properties#durability(...)}（形态照 {@code JadeTopazBowItem} 原来的
 *       {@code 384 * 4}），上限随档走、出生即满耐久。</li>
 *   <li>{@link #color()} —— <b>本档注册进物品的单一能量色</b>（写进 {@code ToolDataComponents.ENERGY_COLOR}
 *       / {@code ENERGY_COLOR_DARK}，供文字/绑定行这类不画渐变的场合取用）。
 *       它<b>不是</b>能量条的色标：条的色标一律问护甲那张表
 *       （{@link #armorSet()} → {@code ArmorEnergyColors#stopsOf}，与回旋镖同一形态）。</li>
 *   <li>{@link #repairIngredient()} —— <b>本档的修复材料</b>（2026-10-06 批 13 新增，作者裁定）：
 *       三套融合套 = 同档护甲那一对锭（<b>任一即可</b>）、雷鸣 = 单一雷鸣合金锭（非融合）。
 *       唯一消费点是 {@code JadeTopazBowItem#isValidRepairItem}（覆写 + 回落 {@code super}）。</li>
 *   <li><b>技能列（2026-10-03 弓技能批 1；2026-10-06 批 12 重排 / 批 14 补"有效等级"读数）</b> ——
 *       {@link #baseSkillLevel()}（<b>元矢自生</b>那个被动用的档位等级 1/2/3/3）、
 *       {@link #skillLevel()}（槽 0/1 的绑定等级 1/1/2/2）、
 *       {@link #thirdSkillLevel()}（槽 2 的绑定等级 1/1/1/1）、
 *       {@link #thirdSkillEffectiveLevel(ItemStack)}（槽 2 那条技能的<b>有效</b>等级
 *       1..{@link #maxSkillLevel()}，批 14 起效果侧的唯一读数）、
 *       {@link #maxSkillLevel()}（上限 5/3/3/3）、{@link #skillOutlineColor()}（描边发光色）、
 *       {@link #perSkillCooldown()}（冷却载体：翠玉按物品记、三把继承弓按技能记）。
 *       全部都是 {@code switch (this)} 的派生量，<b>不进构造参数表</b>（那三项已被关卡 §30b 逐位钉住）。</li>
 *   <li><b>不含</b>伤害 / 拉弓时间 / 初速 —— 那三项是 {@code JadeTopazBowItem} 里<b>写死的单档常量</b>
 *       （{@code DAMAGE_MULTIPLIER} / {@code MAX_PULL_TIME} / 发射初速 {@code power * 3.0F}），
 *       作者本轮没有给档位差异，因此<b>四把弓目前完全相同</b>。要按档拉开就往这里加列，
 *       别在注册处逐把写数字（见本轮报告的「数值表」一节）。</li>
 * </ul>
 *
 * <h2>⚠ 翠玉档是本表唯一一处「不等于口径」的行（作者待裁）</h2>
 * <p>本轮需求给的档位口径是 {@code 500 / 2000 / 5000 / 5000} 与 {@code 1000 / 2000 / 3500 / 3500}，
 * 配色 {@code JADE / SAPPHIRE / STELLARSTONE / THUNDERITE}。但<b>翠玉之弓是已经存在的那把</b>，
 * 它的现行注册值是<b>能量 2000 / 耐久 1536（= 384 × 4）/ 取色 TOPAZ</b>，与口径行不一致。</p>
 * <p>本轮任务书同时给了两条互斥的要求：③「能量/耐久/配色沿用档位口径」与
 * ⑤「⚠ <b>不许动翠玉之弓的既有行为</b>」；验收条款②又只要求把<b>三把新弓</b>的能量/耐久/配色
 * 逐值硬钉（翠玉那把不在钉的范围内）。把 2000 改成 500 会改动老存档里既存弓的能量上限，
 * 属于"动既有行为"；因此本轮<b>按⑤办</b>：翠玉档照旧，三把新弓按口径表。</p>
 * <p><b>这不是漏做</b>：口径值就写在本注释上面，切换只需改下面 {@code JADE_TOPAZ} 那一行的
 * 三个字面量（{@code 2000 → 500}、{@code 1536 → 1000}、{@code TOPAZ → JADE}）——
 * 它是全仓唯一一处。关卡 {@code tools/check-armor-sets.ps1} 的弓段也钉着这一行，
 * 改了会一起红，正是为了让人看见这个决定。</p>
 */
public enum BowTier {

    /**
     * 翠玉之弓（第一档）。
     *
     * <p>⚠ 本行 = <b>翠玉之弓的现行注册值</b>（能量 2000 / 耐久 1536 = 384 × 4 / 取色 TOPAZ），
     * <b>刻意不等于</b>本轮需求给的档位口径（500 / 1000 / JADE）——见类注释那一节
     * 「翠玉档是本表唯一一处不等于口径的行」：任务书红线⑤明令不许动翠玉之弓的既有行为，
     * 而它已有存档与实装技能（两个技能各耗 100 点，2000 上限是它"传说级"的设计值）。</p>
     */
    JADE_TOPAZ(2000, 1536, ToolEnergyColorConfig.TOPAZ),

    /** 宝石之弓（第二档）：能量 2000（= 宝石套单件 1000 × 2）、耐久 2000、取色 {@code SAPPHIRE}。 */
    SAPPHIRE_RUBY(2000, 2000, ToolEnergyColorConfig.SAPPHIRE),

    /** 星界之弓（第三档）：能量 5000（= 星界套单件 2500 × 2）、耐久 3500、取色 {@code STELLARSTONE}。 */
    ASTRAL(5000, 3500, ToolEnergyColorConfig.STELLARSTONE),

    /** 雷鸣之弓（第四档）：能量 5000（= 雷鸣套单件 2500 × 2）、耐久 3500、取色 {@code THUNDERITE}。 */
    THUNDER(5000, 3500, ToolEnergyColorConfig.THUNDERITE);

    /** 能量上限（初始即满：注册链上 {@code defaultEnergy == maxEnergy}，与工具同形）。 */
    private final int energy;
    /** 耐久上限（交给原版 {@code Item.Properties#durability}）。 */
    private final int durability;
    /** 本档注册进物品的单一能量色（文字/绑定行用；能量条色标走 {@link #armorSet()}）。 */
    private final ToolEnergyColorConfig color;

    // ══════════════════════════════════════════════════════════════════════════════════
    // 技能等级上限：两个数各只有一处（2026-10-06 批 15 真源收敛）
    // ──────────────────────────────────────────────────────────────────────────────────
    // 为什么这两个常量住在<b>枚举常量之后</b>：JLS 8.9.3 要求枚举常量必须是类体里的
    // 第一批评分 —— 写在它们上面编译不过（批 15 实测报"此处需要枚举常量"）。
    //
    // 为什么需要它们：共享弓技能（bow_curse / bow_disarm）被四把弓共用 ⇒ 它们的
    // maxLevel 只能按技能注册一次（AllSkills 那两条），而"哪把弓的上限是几"住在本表。
    // 批 15 之前 AllSkills 那两条<b>不写</b> maxLevel，吃构建器默认 5 —— 于是同一个数
    // 在两个文件里各存一份（默认值一份、本表翠玉行一份），改一处另一处不跟着动。
    // 现在两边都引用同一个 static final int ⇒ JLS 编译期常量内联，不会因为 AllSkills
    // 引用它而触发本枚举的初始化（与 BoomerangSkillConfigs.MAX_SKILL_LEVEL 同一机制）。
    // ══════════════════════════════════════════════════════════════════════════════════

    /**
     * <b>共享弓技能（{@code bow_curse} / {@code bow_disarm}）的等级上限 = 5</b>。
     *
     * <p>两处消费点：{@code AllSkills.BOW_CURSE} / {@code BOW_DISARM} 的
     * {@code .maxLevel(...)}（同一个数，不再是"隐式默认值"）与 {@link #maxSkillLevel()}
     * 的 {@code JADE_TOPAZ} 臂。</p>
     */
    public static final int SHARED_SKILL_CAP = 5;

    /**
     * <b>三把继承弓（宝石 / 星界 / 雷鸣）的档位技能上限 = 3</b>（作者 2026-10-03 给死
     * 「新绑的两条技能上限 3 级」）。
     *
     * <p>两处消费点：{@link #maxSkillLevel()} 的三档臂，以及 {@code AllSkills} 三条
     * <b>专属</b>弓技能条目（{@code bow_wave_shift} / {@code bow_astral_barrage} /
     * {@code bow_thunder_might}）的 {@code .maxLevel(...)} ⇒「登记值 == 运行时档位值」
     * 不再靠两处各写一个 3 来维持。</p>
     */
    public static final int INHERITED_TIER_SKILL_CAP = 3;

    BowTier(int energy, int durability, ToolEnergyColorConfig color) {
        this.energy = energy;
        this.durability = durability;
        this.color = color;
    }

    /** 这一档的能量上限（口径 = 同档护甲全套能量的一半；见类注释）。 */
    public int energy() {
        return energy;
    }

    /** 这一档的耐久上限（口径 = 1000 / 2000 / 3500 / 3500；见类注释）。 */
    public int durability() {
        return durability;
    }

    /**
     * <b>本档弓的修复材料</b>（作者 2026-10-06 批 13 裁定）—— 铁砧里「用材料修」的那一档，
     * 唯一消费点是 {@code JadeTopazBowItem#isValidRepairItem}（覆写 + 回落 {@code super}）。
     *
     * <p>口径 = <b>同档护甲那一对</b>（融合规则，与 {@code CoeArmorMaterials} 逐条同源）：</p>
     * <ul>
     *   <li>翠玉 = {@code jade_ingot} + {@code topaz_ingot}；</li>
     *   <li>宝石 = {@code sapphire_ingot} + {@code ruby_ingot}；</li>
     *   <li>星界 = {@code stellarstone_ingot} + {@code sanctstone_ingot}；</li>
     *   <li>雷鸣 = <b>只认</b> {@code thunderite_ingot}（作者原话「雷鸣不是融合」⇔ 单一材料，
     *       与雷鸣套那行逐字同一条口径）。</li>
     * </ul>
     *
     * <p>多材料用原版 {@code Ingredient#of(ItemLike...)} 表达
     * ⇒ <b>任一即可</b>（原版 {@code ArmorItem#isValidRepairItem} 用的就是同一个
     * {@code Ingredient#test}），因此<b>不自造合并类</b>；这与
     * {@code AllTiers#getRepairIngredient()}（工具走原版 {@code Tier} 接口）是同一件事的两种载体。</p>
     *
     * <p>⚠ 与 {@link #armorSet()} / {@link #skillLevel()} 那类派生量<b>同形</b>：用 {@code switch (this)}
     * 表达「四档 → 四种材料」，<b>绝不</b>加成构造参数 —— 本枚举的三项构造参数表被关卡 §30b 逐位钉住。</p>
     *
     * <p>⚠ 它<b>不</b>影响"两把同名弓合耐久"那条原版路（{@code AnvilMenu} 里是另一个分支）：
     * 本批补的是<b>缺失的</b>「用材料修」，不是替换任何既有修复途径。</p>
     */
    public Ingredient repairIngredient() {
        return switch (this) {
            case JADE_TOPAZ -> Ingredient.of(CoeItems.JADE_INGOT.get(), CoeItems.TOPAZ_INGOT.get());
            case SAPPHIRE_RUBY -> Ingredient.of(CoeItems.SAPPHIRE_INGOT.get(), CoeItems.RUBY_INGOT.get());
            case ASTRAL -> Ingredient.of(CoeItems.STELLARSTONE_INGOT.get(), CoeItems.SANCTSTONE_INGOT.get());
            case THUNDER -> Ingredient.of(CoeItems.THUNDERITE_INGOT.get());
        };
    }

    /** 这一档的单一能量色（注册期写进 {@code ENERGY_COLOR} / {@code ENERGY_COLOR_DARK}）。 */
    public ToolEnergyColorConfig color() {
        return color;
    }

    /**
     * <b>这一档对应的护甲套</b>（四档与四套一一对应：翠玉弓 ↔ 翠玉套、宝石弓 ↔ 宝石套、
     * 星界弓 ↔ 星界套、雷鸣弓 ↔ 雷鸣套）。
     *
     * <p>唯一消费点是 {@code JadeTopazBowItem#energyGradientStops}，它拿本方法的结果去问护甲那套
     * 取色源（{@code ArmorEnergyColors#stopsOf(ArmorSet)}）——<b>同一个方法、一行调用</b>，
     * 弓侧既不复写色值、也不自己拼渐变，因此色标条数也跟着护甲走
     * （翠玉 2 / 宝石 2 / <b>星界 4</b> / 雷鸣 2）。形态与回旋镖四把逐字相同。</p>
     *
     * <p>⚠ 翠玉档返回的色标（绿 {@code 0x55FF55} → 黄 {@code 0xFFFF55}）与
     * {@code EnergyTooltipHandler} 里弓那条<b>历史默认</b>逐字同值 ⇒
     * 翠玉之弓的能量条渲染结果<b>零变化</b>（只是走了同一条"问护甲取色源"的路）。</p>
     *
     * <p>用 {@code switch} 而不是 {@code ArmorSet.valueOf(name())}：枚举一改名，后者要到
     * <b>运行期</b>才炸；而 {@code switch} 少一个分支<b>编译就不过</b>。</p>
     */
    public ArmorSet armorSet() {
        return switch (this) {
            case JADE_TOPAZ -> ArmorSet.JADE_TOPAZ;
            case SAPPHIRE_RUBY -> ArmorSet.SAPPHIRE_RUBY;
            case ASTRAL -> ArmorSet.ASTRAL;
            case THUNDER -> ArmorSet.THUNDER;
        };
    }

    // ========== 技能列（2026-10-03 弓技能批 1）—— 四个 switch，不加构造参数 ==========
    // 与 BoomerangTier 的 baseSkillLevel()/crossMine() 同一路数：这些是「四档 → 四个值」的
    // 派生量，不影响任何既有档位字段的语义，用 switch(this) 表达同样只有一处真源；
    // 而构造参数表（energy, durability, color）已被关卡 §30b 逐位钉住，加参数会让那里一起动。

    /**
     * <b>本档的「档位起始等级」</b>（作者 2026-10-03 弓技能批 1 给死）：
     * 翠玉 <b>1</b> / 宝石 <b>2</b> / 星界 <b>3</b> / 雷鸣 <b>3</b>。
     *
     * <p>⚠ <b>批 12 起它不再是「绑定技能的等级」</b>：技能的绑定等级搬到了
     * {@link #skillLevel()}（槽 0/1）与 {@link #thirdSkillLevel()}（槽 2）两列，
     * 于是本方法<b>只剩一个消费者</b> —— 被动「元矢自生」的中签概率
     * （{@code JadeTopazBowItem#rollMetaArrowEssence} → {@code BowMetaArrowTrait#procs(int, ..)}，
     * 表 = Lv1/2/3 ⇒ 10%/20%/30%）。改这条列就是改那个被动，与技能等级无关。</p>
     *
     * <p><b>为什么保留它而不合并进新列</b>：作者批 3 给元矢自生的口径是"按该弓的<b>档位起始
     * 等级</b>取概率"，而批 12 给技能的是另一张表（1/1/2/2 与 1/1/1/1）——两者在宝石弓
     * （2 vs 1）与雷鸣弓（3 vs 2）上<b>不同值</b>，合成一列就会静默改掉那个被硬边界保护住的
     * 被动（"元矢自生的表现数值不许改"）。关卡 {@code §30e-3} 把本方法的三条臂逐字钉住。</p>
     */
    public int baseSkillLevel() {
        return switch (this) {
            case JADE_TOPAZ -> 1;
            case SAPPHIRE_RUBY -> 2;
            case ASTRAL, THUNDER -> 3;
        };
    }

    /**
     * <b>槽 0 / 槽 1 的绑定等级</b>（弓技能批 12，作者 2026-10-06 给死）：
     * 翠玉 <b>1</b> / 宝石 <b>1</b> / 星界 <b>2</b> / 雷鸣 <b>2</b>。
     *
     * <p>它服务的三条技能是"这一族共用"的那些 —— <b>槽 0 = 凋零诅咒</b>（四把弓），
     * <b>槽 1 = 缴械风暴（翠玉 / 宝石）或量波置换（星界 / 雷鸣）</b>。三条都是
     * {@code createoreexpansion:bow_curse} / {@code bow_disarm} / {@code bow_wave_shift} 这三个
     * <b>被两把以上弓复用的 id</b>，所以它们的等级不能写在自己的 {@code AllSkills} 条目上
     * （那会连别的弓一起改）——只能按弓落在本表这一列。</p>
     *
     * <p>三处消费点，都是"一处真源"：</p>
     * <ol>
     *   <li><b>注册链</b>：{@code CoeItems#threeSkillBow} 的槽 0 与槽 1 两行
     *       （{@code .addSkills(.., tier.skillLevel())}）；</li>
     *   <li><b>运行期</b>：{@link JadeTopazBowItem#effectiveSkillLevel(ItemStack)}
     *       = {@code SkillEnergyCost.effectiveLevel(stack, tier.skillLevel(), tier.maxSkillLevel())}
     *       —— 凋零诅咒 / 缴械风暴 / 量波置换三条<b>同值</b>（每把弓上这三条要么同为 1、
     *       要么同为 2，见本类注释的表），所以一条读数就够；</li>
     *   <li><b>关卡</b>：{@code check-armor-sets.ps1} 的 {@code bow12-skill-table} 逐把逐槽读它。</li>
     * </ol>
     *
     * <p>⚠ 翠玉之弓那一行的 {@code 1} 同样要与它注册处写死的两个字面量 {@code 1}
     * 逐字一致（红线：那两行一个字不许动）；它运行期的读数也走本方法。</p>
     */
    public int skillLevel() {
        return switch (this) {
            case JADE_TOPAZ, SAPPHIRE_RUBY -> 1;
            case ASTRAL, THUNDER -> 2;
        };
    }

    /**
     * <b>槽 2 的「绑定（基准）等级」</b>（弓技能批 12，作者 2026-10-06 给死）：三把带槽 2 的弓
     * <b>都是 1</b> —— 宝石「量波置换」① / 星界「星元波置」① / 雷鸣「雷鸣神力」①；
     * 翠玉之弓<b>没有槽 2</b>。</p>
     *
     * <p>它是"<b>本把弓槽 2 那条技能</b>的等级"（不是"某条技能的等级"）：槽 2 上放哪条技能由
     * 注册处那一行决定（{@code CoeItems} 的声明处），而"放在槽 2 的那条"的<b>绑定等级</b>由本列
     * 决定 —— 于是宝石的槽 2 是共用族的量波置换、星界/雷鸣的槽 2 是各自的专属技能，三条同读本列
     * 却互不牵连。</p>
     *
     * <p>⚠ <b>批 14（2026-10-06）厘清了"绑定等级"与"实际等级"两件事</b>：本列是<b>基准</b>
     * （= 物品出生时写进技能组件的 {@code Level}），<b>实际生效的等级</b>是
     * {@link #thirdSkillEffectiveLevel(ItemStack)} ——
     * 基准 + 技艺提升 − 技艺回溯，按 {@link #maxSkillLevel()}（三把继承弓 = <b>3</b>）钳位，
     * 与全仓其余每一条弓技能<b>同一条口径</b>（{@code SkillEnergyCost#effectiveLevel}）。
     * ⇒ 槽 2 的可达范围是 <b>1..3</b>：不附魔 = ①（作者批 12 的表），附魔拉满 = ③
     * （"所有的弓的技能等级最高也是 3 级"）。</p>
     *
     * <p>消费点<b>只剩一处</b>：注册链（{@code CoeItems#threeSkillBow} 的第三行
     * {@code .addSkills(thirdSkill, tier.thirdSkillLevel())}），以及批 14 新增的那个读数方法
     * 自己（{@link #thirdSkillEffectiveLevel} 拿它当基准）。⚠ <b>批 4~13 的效果侧读的就是本列</b>
     * （星界弹幕闸门 {@code JadeTopazBowItem#fireAstralBarrageInsteadOfArrow}、
     * 雷鸣神力闸门 {@code #fireThunderMightInsteadOfArrow}、客户端预选框
     * {@code BowAstralBarragePreviewRenderer}）—— 那是"星元波置 / 雷鸣神力恒 ①"的<b>根因</b>：
     * 一个注册期常量被当成等级读数，于是费用随有效等级走（1..3）、效果却冻在 ①。
     * 批 14 把这三处全部改读 {@link #thirdSkillEffectiveLevel}（效果与费用同源），
     * <b>本列本身不动</b>（作者批 12 的表就是 ①，它是基准、不是上限）。</p>
     *
     * <p>⚠ 三条技能<b>不再读</b> {@link #baseSkillLevel()}（批 4~11 读的就是它：宝石 2 / 星界 3 /
     * 雷鸣 3）⇒ 本列是批 12 唯一一处改了这三条技能<b>等级口径</b>的地方。翠玉那一行的 {@code 1}
     * 不被任何调用点消费（它没有槽 2），留着是为了让 {@code switch (this)} 保持穷尽、并让关卡能把
     * "三把带槽 2 的弓的绑定等级都是 1"当成一条直读事实（形状照 {@link #skillOutlineColor()} 的翠玉行）。</p>
     */
    public int thirdSkillLevel() {
        return switch (this) {
            case JADE_TOPAZ, SAPPHIRE_RUBY, ASTRAL, THUNDER -> 1;
        };
    }

    /**
     * <b>槽 2 那条技能的「有效等级」—— 唯一读取点</b>（2026-10-06 批 14 新增，作者裁定）。
     *
     * <h2>它在修什么</h2>
     * <p>作者原话（批 14 的触发点）：</p>
     * <blockquote>「这两个玩意儿只能一级？他们不是封顶三级吗」</blockquote>
     * <p>症状：星元波置 / 雷鸣神力的<b>费用</b>随有效等级走（150 × 1/2/3 = 450 封顶），
     * 而<b>效果</b>（半径 / 滞留 / 波级 / 范围 / 真雷概率）读的是注册期常量
     * {@link #thirdSkillLevel()} = ① ⇒ "付 450 拿 ① 效果"，且那两张三行等级表的
     * Lv2 / Lv3 行<b>永远够不到</b>。根因就是"等级读数取错了来源"：常量 ≠ 等级。
     * 批 14 把效果侧改成读本方法，与费用侧（{@code CoeSkillSupport#effectiveLevel} 那条既有口径）
     * <b>同源</b>。</p>
     *
     * <h2>口径（没有任何新算式）</h2>
     * <p>= {@code SkillEnergyCost.effectiveLevel(stack, }{@link #thirdSkillLevel()}{@code ,
     * }{@link #maxSkillLevel()}{@code )}：基准取本表的槽 2 列、上限取本表的技能上限列，
     * 附魔（技艺提升 / 技艺回溯）由 {@code SkillEnergyCost} 从物品读，结果恒夹在
     * {@code [1, maxSkillLevel()]} 内。三把带槽 2 的弓的上限都是 {@code 3}
     * ⇒ 本方法在这三把弓上恒返回 <b>1 / 2 / 3</b>（"封顶三级"）。</p>
     *
     * <h2>消费点（三处，都是"效果"）</h2>
     * <ol>
     *   <li>星界弹幕闸门 {@code JadeTopazBowItem#fireAstralBarrageInsteadOfArrow}
     *       ⇒ 半径 = 等级 + 1（2/3/4）、滞留 4/5/6 秒、落下的波级 α/β/γ；</li>
     *   <li>雷鸣神力闸门 {@code JadeTopazBowItem#fireThunderMightInsteadOfArrow}
     *       ⇒ 水平方形 2×2/3×3/4×4、真雷概率 20%/40%/60%；</li>
     *   <li>客户端预选框 {@code BowAstralBarragePreviewRenderer} 的半径读数 ——
     *       <b>必须与服务端同一个数</b>（红线"客户端与服务端同源"：圈大小 ≠ 实际半径
     *       就是画一个圈、落另一个盘）。</li>
     * </ol>
     * <p>⚠ <b>宝石弓槽 2 的「量波置换」不读本方法</b>（它的效果本来就随等级：走
     * {@code JadeTopazBowItem#effectiveSkillLevel} → {@link #skillLevel()} 那一列），
     * 批 14 一个字节都没动它。</p>
     *
     * <p>⚠ 本方法<b>不参与注册</b>：物品出生时写进技能组件的仍是 {@link #thirdSkillLevel()}
     * （基准等级，作者批 12 的表）。把基准调高只会让"冻住的那个值"从 ① 挪到 ②，
     * 永远到不了 ③ —— 所以修的是"读哪一列"，不是"那一列写几"。</p>
     */
    public int thirdSkillEffectiveLevel(ItemStack stack) {
        return SkillEnergyCost.effectiveLevel(stack, thirdSkillLevel(), maxSkillLevel());
    }

    /**
     * <b>本档技能等级上限</b>（作者 2026-10-03 弓技能批 1 第 7 条：新绑的两条技能上限 3 级）：
     * 宝石 / 星界 / 雷鸣 = <b>3</b>；翠玉 = <b>5</b>。
     *
     * <p>⚠ 翠玉那一行的 {@code 5} 是<b>刻意</b>的：作者同时写明「翠玉之弓那两条的 maxLevel 不动
     * （默认 5）」⇒ 翠玉弓的读数必须仍旧是 {@code 5}（{@code AllSkills} 的注册默认值，
     * 那一段属红线、一个字不许改）。它实际到不了 4/5（基准 1 + 技艺提升封顶 +2 ⇒ ≤3），
     * 所以本行与「上限 3」在翠玉弓上的<b>可观测结果逐值相同</b>——这正是作者写的那个括号。</p>
     *
     * <p>为什么上限住在<b>档位表</b>而不是 {@code AllSkills} 的 {@code .maxLevel(3)}：
     * 那个 {@code maxLevel} 是<b>按技能注册</b>的（{@code AllSkills.RegisteredDataSkill#maxLevel()}），
     * 本模组两条弓技能是<b>复用同一对 id</b>的（作者第 3 条：不新建 id），改成 3 会连翠玉弓一起改
     * ——与红线冲突。等级的上限本来就是「哪把弓」的属性 ⇒ 落在档位表这一处真源，
     * 读取点只有 {@link JadeTopazBowItem#effectiveSkillLevel} 一个。</p>
     */
    public int maxSkillLevel() {
        return switch (this) {
            // 翠玉弓：保持注册默认 5（批 15 起那个 5 = 本表的 SHARED_SKILL_CAP，
            // AllSkills 那两条共享技能条目也引用它 ⇒ 一个数只有一处）。
            case JADE_TOPAZ -> SHARED_SKILL_CAP;
            // 三把继承弓：作者给死 3（配置表仍保留 5 档，Lv4/Lv5 只是够不到）。
            case SAPPHIRE_RUBY, ASTRAL, THUNDER -> INHERITED_TIER_SKILL_CAP;
        };
    }

    /**
     * <b>本档技能描边的发光色</b>（{@code SkillOutlineColors}，供三把继承弓的注册链取用）。
     *
     * <p>口径 = <b>同档工具已经在用的那一色</b>（{@code CoeItems} 里同档工具逐件都是这一色）：
     * 翠玉 {@code TOPAZ_GOLD} / 宝石 {@code SAPPHIRE_BLUE} / 星界 {@code STELLARSTONE_PINK} /
     * 雷鸣 {@code THUNDER_PURPLE}。弓侧不新造色值。</p>
     *
     * <p>⚠ 翠玉那一行 = 翠玉之弓注册处写死的那一字面量（{@code SkillOutlineColors.TOPAZ_GOLD}），
     * 但它<b>不被消费</b>：翠玉之弓的绑定链不许改（红线），所以它走自己那一行；
     * 本行留着是让关卡能把「表里的色 == 那一行的字面量」当一致性断言钉住。</p>
     */
    public SkillOutlineColors.SkillColor skillOutlineColor() {
        return switch (this) {
            case JADE_TOPAZ -> SkillOutlineColors.TOPAZ_GOLD;
            case SAPPHIRE_RUBY -> SkillOutlineColors.SAPPHIRE_BLUE;
            case ASTRAL -> SkillOutlineColors.STELLARSTONE_PINK;
            case THUNDER -> SkillOutlineColors.THUNDER_PURPLE;
        };
    }

    /**
     * <b>本档的技能冷却走哪个载体</b>（作者 2026-10-03 弓技能批 1 第 6 条）：
     * 三把继承弓 = {@code true}（<b>按技能记</b>，{@code PerSkillCooldown}）；
     * 翠玉之弓 = {@code false}（<b>按物品记</b>，{@code ToolSkillCooldown} —— 它的既有行为，
     * 红线：不许动）。</p>
     *
     * <p>两条并存是作者明确允许的（"允许两条并存"），边界就是本方法：
     * 同一把弓上的两条技能互不连坐（各自 5s / 7s 的秒数来自
     * {@code BowCurseConfigs} / {@code BowDisarmConfigs}，与等级同源）只在 {@code true} 这一侧成立；
     * 翠玉那一侧仍是"同一把弓共用一个冷却格"，与它升级前逐字相同。</p>
     *
     * <p>消费点唯一：{@code BowShootItemSkill} 的 {@code onCooldown} / {@code startCooldown}。</p>
     */
    public boolean perSkillCooldown() {
        return switch (this) {
            case JADE_TOPAZ -> false;
            case SAPPHIRE_RUBY, ASTRAL, THUNDER -> true;
        };
    }
}
