package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;

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
}
