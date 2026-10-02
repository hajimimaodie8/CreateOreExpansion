package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * <b>回旋镖四档材质的数值真源</b>（唯一出处：物品注册、实体伤害、挖掘判定、能量扣减全读这里）。
 *
 * <p>四档与既有工具线同一顺序：<b>翠玉（jade_topaz）→ 宝石（sapphire_ruby）→ 星界（astral）→ 雷鸣（thunder）</b>。
 * 数值不是我凭空拍的，而是<b>逐条挂到既有梯度上</b>：</p>
 * <ul>
 *   <li>{@link #energy()} —— <b>对应装备全套能量的一半</b>（500 / 2000 / 5000 / 5000，作者 2026-10-02 定稿）。
 *       盔甲全套能量 = 单件 × 4（{@code ArmorSet} 的 {@code perPieceEnergy} = 250 / 1000 / 2500 / 2500）
 *       ⇒ 一半 = 500 / 2000 / 5000 / 5000。
 *       <br>⚠ <b>旧口径已被推翻</b>：本行原文写的是「抄 {@code AllTiers} 同名材质的工具能量档
 *       （600 / 3000 / 4500 / 5000）」——那是按<b>工具</b>挂梯度，本轮按<b>同档盔甲全套的一半</b>重挂。
 *       依据：开工需求 2026-10-02 §3.1 数值表 + §3.9「{@code BoomerangTier.energy()} = 600/3000/4500/5000
 *       ⇒ 作废 ⇒ 改 500/2000/5000/5000」。</li>
 *   <li>{@link #durability()} —— <b>本轮（2026-10-02 批 1）新增</b>：1000 / 2000 / 3500 / 3500（需求 §3.1 给死）。
 *       它由物品注册走原版 {@code Item.Properties#durability}（{@code BoomerangItem} 构造器），
 *       但<b>扣减一律不走 {@code hurtAndBreak}</b>——那会把栈当场打成空，做不到需求 §3.8 的
 *       「不立即归零、不当场销毁，等回程批量结算」；读写走 {@code BoomerangItem} 自己的
 *       {@code getDurability / setDurability / addWear}（批 1 只搭接口，扣减时机在批 2）。
 *       <br>⚠ 同一处旧口径（{@code mineCost} 上的「耐久 → 能量：这就是"耐久"的替代品」）也被本轮推翻，
 *       逐字标注见 {@link #mineCost()}。</li>
 *   <li>{@link #miningLevel()} / {@link #incorrectBlocks()} —— 与 {@code AllTiers} <b>同一条原版标签</b>：
 *       翠玉用 {@code INCORRECT_FOR_DIAMOND_TOOL}（= 钻石级，与 {@code AllTiers.JADE/TOPAZ} 一致），
 *       其余三档用 {@code INCORRECT_FOR_NETHERITE_TOOL}（= 下界合金级，与 {@code AllTiers.SAPPHIRE/
 *       STELLARSTONE/THUNDERITE} 一致）。</li>
 *   <li>{@link #damage()} —— 按四档拉开（6 / 8 / 10 / 12），落在既有工具的"材质加成 3.5 / 4.5 / 5 / 5"
 *       与剑的"加成 +4"这两条线之间（镖是投掷物，既不叠手部攻击、也不吃蓄力）。</li>
 *   <li>{@link #throwCost()} / {@link #mineCost()} / {@link #cooldownTicks()} —— 我定的（作者可一句话改）。
 *       <br>⚠ 作者 2026-10-02（需求 §3.2 / §3.8 / §3.9）已把这三条的<b>口径</b>拆掉：投掷费与冷却拆成
 *       「点按 / 长按」两套、挖方块不再扣能量而改扣耐久。这三个字段的数值<b>在批 1 未动</b>
 *       （批 1 只做数值面 + 物品侧基础），拆分与调用点改道排在批 2；关卡 §29h-1 逐值钉着它们的现值。</li>
 *   <li>{@link #returnDistance()} —— 作者 2026-10-02 报的 bug（"扔远了会自动消失"）的<b>主判据</b>：
 *       单位<b>格</b>，翠玉 5 / 宝石 10 / 星界 15 / 雷鸣 20。去程一旦离主人超过这个距离就<b>立刻掉头</b>
 *       （不是消失、也不是掉地上）——执行处在 {@code AbstractBoomerangEntity#outboundRangeExceeded}。</li>
 * </ul>
 *
 * <h2>为什么 {@link #digSpeed()} 长这个样子</h2>
 * <p>挖掘判定整段照搬原版的口径（见 {@code AbstractBoomerangEntity#mineBlock}）：
 * {@code 进度 = digSpeed / (hardness * i)}，{@code i = doPlayerHarvestCheck ? 30 : 100}，
 * 原版要求<b>一个 tick 内进度 ≥ 1</b> 才算挖开。把 {@code digSpeed} 取成
 * {@code 30 * maxHardness} 之后，<b>i=30 的分支退化成 {@code hardness <= maxHardness}</b>
 * ——正是需求里"与该把的 maxHardness 比"那句话；i=100 的"用错工具"分支则自动变成
 * {@code hardness <= 0.3 * maxHardness}（原版对用错工具的惩罚，不是新规则）。</p>
 *
 * <p>⚠ {@code Item#getDestroySpeed} 恒 0（需求 10：防止被当普通工具用），所以这里的
 * {@code digSpeed} <b>不是</b>从物品上读的——它是这一档自己的数字，只给实体挖掘判定用。</p>
 */
public enum BoomerangTier {

    /** 翠玉镖：钻石级（与 {@code AllTiers.JADE/TOPAZ} 同标签）、硬度上限 10、能量 500、耐久 1000、收回距离 5 格。 */
    JADE_TOPAZ(6.0F, 500, 1000, 10.0F, 3, 25, 5, 20, 5, ToolEnergyColorConfig.JADE),

    /** 宝石镖：下界合金级（与 {@code AllTiers.SAPPHIRE} 同标签）、硬度上限 20（= Quark 默认值）、能量 2000、耐久 2000、收回距离 10 格。 */
    SAPPHIRE_RUBY(8.0F, 2000, 2000, 20.0F, 4, 50, 10, 15, 10, ToolEnergyColorConfig.SAPPHIRE),

    /** 星界镖：下界合金级、硬度上限 30（够到远古残骸的 30）、能量 5000、耐久 3500、收回距离 15 格。 */
    ASTRAL(10.0F, 5000, 3500, 30.0F, 4, 75, 15, 10, 15, ToolEnergyColorConfig.STELLARSTONE),

    /** 雷鸣镖：下界合金级、硬度上限 40（仍够不到黑曜石的 50）、能量 5000、耐久 3500、收回距离 20 格。 */
    THUNDER(12.0F, 5000, 3500, 40.0F, 4, 100, 20, 10, 20, ToolEnergyColorConfig.THUNDERITE);

    /** 命中生物的伤害（{@code hurt} 的原始值，不叠手部攻击/附魔）。 */
    private final float damage;
    /** 能量上限（初始即满，与工具同形：ENERGY == MAX_ENERGY）—— 本档盔甲全套能量的一半。 */
    private final int energy;
    /**
     * 耐久上限（2026-10-02 批 1 新增）。
     *
     * <p>它是<b>数值真源</b>：物品注册读它写进原版 {@code MAX_DAMAGE} 组件，实体的回程批量结算
     * （需求 §3.8，批 2）也读它。<b>不</b>走 {@code hurtAndBreak}。</p>
     */
    private final int durability;
    /** 能挖动的最大方块硬度（挖方块那条判定的唯一门槛）。 */
    private final float maxHardness;
    /** 挖掘等级 1~4 = 原版石/铁/钻/合金（决定 {@link #incorrectBlocks()}，也用于报告里的对照表）。 */
    private final int miningLevel;
    /**
     * 一次投掷的能量消耗（25 / 50 / 75 / 100）。
     *
     * <p>⚠ 作者 2026-10-02 已把投掷费拆成「点按 / 长按」两套（需求 §3.2：点按 10/9/8/8、长按 50/45/40/40），
     * 本字段在批 2 作废；<b>批 1 数值不动</b>（关卡逐值钉着现值）。</p>
     */
    private final int throwCost;
    /**
     * 成功挖掉一个方块的能量消耗（5 / 10 / 15 / 20）。
     *
     * <p>⚠ <b>本字段的口径已被作者 2026-10-02 推翻（批 1 只标注、不改数值与调用点）</b>。
     * 旧注释原文是「<b>耐久 → 能量：这就是"耐久"的替代品</b>」——本轮给镖加了<b>真正的耐久</b>
     * （{@link #durability()}）之后那句话不再成立。依据：开工需求 2026-10-02 §3.8
     * （投掷 −2/−5、每命中一个生物/挖掉一个方块各额外 −1，耐久见底<b>不当场归零</b>，回到玩家身上才批量结算）
     * + §3.9（「{@code mineCost()} = 5/10/15/20 ⇒ 作废 ⇒ 挖方块<b>不再扣能量</b>，改扣耐久」）。
     * 挖方块改扣耐久与 {@code mineCost} 的删除都排在<b>批 2</b>。</p>
     */
    private final int mineCost;
    /**
     * 投掷冷却（20 / 15 / 10 / 10 tick）。
     *
     * <p>⚠ 作者 2026-10-02 已把冷却拆成「点按 / 长按」两套（需求 §3.2：点按 100/90/80/80、
     * 长按 200/180/160/160 tick），本字段在批 2 作废；<b>批 1 数值不动</b>。</p>
     */
    private final int cooldownTicks;
    /**
     * 收回距离（格）：去程离主人超过这个距离就立刻掉头（作者 2026-10-02）。
     *
     * <p>它是<b>主判据</b>；去程的时间上限（{@code AbstractBoomerangEntity#MAX_OUTBOUND_TICKS}）
     * 只是"距离判据万一失效"时的兜底。</p>
     */
    private final int returnDistance;
    /** 能量条配色（沿用同材质工具的配色）。 */
    private final ToolEnergyColorConfig color;

    BoomerangTier(float damage, int energy, int durability, float maxHardness, int miningLevel,
                  int throwCost, int mineCost, int cooldownTicks, int returnDistance,
                  ToolEnergyColorConfig color) {
        this.damage = damage;
        this.energy = energy;
        this.durability = durability;
        this.maxHardness = maxHardness;
        this.miningLevel = miningLevel;
        this.throwCost = throwCost;
        this.mineCost = mineCost;
        this.cooldownTicks = cooldownTicks;
        this.returnDistance = returnDistance;
        this.color = color;
    }

    public float damage() {
        return damage;
    }

    public int energy() {
        return energy;
    }

    /**
     * 这一档的耐久上限（2026-10-02 批 1 新增）。
     *
     * <p>唯一消费点是物品注册（{@code BoomerangItem} 构造器把它交给
     * {@code Item.Properties#durability}）；实体侧的写入走
     * {@code BoomerangItem#getDurability/setDurability/addWear}，<b>不是</b> {@code hurtAndBreak}。</p>
     */
    public int durability() {
        return durability;
    }

    public float maxHardness() {
        return maxHardness;
    }

    public int miningLevel() {
        return miningLevel;
    }

    public int throwCost() {
        return throwCost;
    }

    public int mineCost() {
        return mineCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    /**
     * 这一档的收回距离（格）—— <b>实体侧唯一能拿到它的地方</b>（实体里不许出现距离字面量）。
     *
     * <p>去程"离主人超过它 ⇒ {@code setReturning(true)}"由
     * {@code AbstractBoomerangEntity#outboundRangeExceeded} 执行。</p>
     */
    public int returnDistance() {
        return returnDistance;
    }

    public ToolEnergyColorConfig color() {
        return color;
    }

    /**
     * 原版挖掘进度里的 {@code digSpeed}（见类注释的推导：{@code 30 * maxHardness}）。
     *
     * <p>取 30 而不是 100：{@code i} 的两个分支里，<b>30 是"算正确工具/不需要正确工具"的那一支</b>
     * （草地上手挖、镐挖石头都是它），也就是 {@code maxHardness} 那句话想表达的那一支。</p>
     */
    public float digSpeed() {
        return 30.0F * maxHardness;
    }

    /**
     * 这一档挖不动的方块标签——<b>与原版 {@code Tier#getIncorrectBlocksForDrops} 同一判据</b>
     * （{@code AllTiers} 用的就是这两个标签，所以镖与镐的挖掘等级天然一致）。
     */
    public TagKey<Block> incorrectBlocks() {
        return switch (miningLevel) {
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }
}
