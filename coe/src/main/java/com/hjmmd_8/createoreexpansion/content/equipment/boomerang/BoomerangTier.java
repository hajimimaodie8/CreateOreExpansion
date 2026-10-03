package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * <b>回旋镖四档材质的数值真源</b>（唯一出处：物品注册、实体伤害、挖掘判定、投掷/命中/挖掘的耐久扣减全读这里）。
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
 *   <li>{@link #durability()} —— <b>2026-10-02 批 1 新增</b>：1000 / 2000 / 3500 / 3500（需求 §3.1 给死）。
 *       它由物品注册走原版 {@code Item.Properties#durability}（{@code BoomerangItem} 构造器），
 *       但<b>扣减一律不走 {@code hurtAndBreak}</b>——那会把栈当场打成空，做不到需求 §3.8 的
 *       「不立即归零、不当场销毁，等回程批量结算」；读写走 {@code BoomerangItem} 自己的
 *       {@code getDurability / setDurability / addWear}。
 *       <br>⚠ 同一处旧口径（{@code mineCost} 上的「耐久 → 能量：这就是"耐久"的替代品」）也被本轮推翻。</li>
 *   <li>{@link #miningLevel()} / {@link #incorrectBlocks()} —— 与 {@code AllTiers} <b>同一条原版标签</b>：
 *       翠玉用 {@code INCORRECT_FOR_DIAMOND_TOOL}（= 钻石级，与 {@code AllTiers.JADE/TOPAZ} 一致），
 *       其余三档用 {@code INCORRECT_FOR_NETHERITE_TOOL}（= 下界合金级，与 {@code AllTiers.SAPPHIRE/
 *       STELLARSTONE/THUNDERITE} 一致）。</li>
 *   <li>{@link #damage()} —— 按四档拉开（6 / 8 / 10 / 12），落在既有工具的"材质加成 3.5 / 4.5 / 5 / 5"
 *       与剑的"加成 +4"这两条线之间（镖是投掷物，既不叠手部攻击、也不吃蓄力）。</li>
 *   <li>{@link #returnDistance()} —— 作者 2026-10-02 报的 bug（"扔远了会自动消失"）的<b>主判据</b>：
 *       单位<b>格</b>，翠玉 5 / 宝石 10 / 星界 15 / 雷鸣 20。点按去程一旦离主人超过这个距离就<b>立刻掉头</b>
 *       （不是消失、也不是掉地上）——执行处在 {@code AbstractBoomerangEntity#outboundRangeExceeded}；
 *       长按（花瓣）不用它当结束条件，但花瓣的<b>最远距离</b>就是它
 *       （{@link BoomerangCurveConfigs} 的 {@code R}）。</li>
 *   <li>{@link #baseSkillLevel()} —— <b>2026-10-02 批 3 新增</b>：技能基准等级
 *       翠玉 1 / 宝石 2 / 星界 3 / 雷鸣 3（需求 §3.1 给死）。
 *       它是"穿刺额度 3L/5L"与"穿刺消耗 20L"里的那个 L 的<b>基准</b>；
 *       有效等级还要叠附魔（技艺提升 / 技艺回溯 ±2）再钳到 5，读取点唯一：
 *       {@code BoomerangItem#effectiveSkillLevel(ItemStack, int)}。
 *       ⚠ 与护甲的"雷鸣 = 4"<b>不同</b>，别照抄 {@code ArmorSkillLevels}（见该方法的注释）。</li>
 * </ul>
 *
 * <h2>两种投掷模式（2026-10-02 批 2：需求 §3.2 逐值给死）</h2>
 * <table border="1">
 *   <caption>点按 / 长按 的消耗与冷却（每一档都从这张表进代码）</caption>
 *   <tr><th>档</th><th>点按消耗</th><th>点按冷却</th><th>长按消耗</th><th>长按冷却</th></tr>
 *   <tr><td>翠玉</td><td>10 点</td><td>5.0 s = 100 tick</td><td>50 点</td><td>10.0 s = 200 tick</td></tr>
 *   <tr><td>宝石</td><td>9 点</td><td>4.5 s = 90 tick</td><td>45 点</td><td>9.0 s = 180 tick</td></tr>
 *   <tr><td>星界</td><td>8 点</td><td>4.0 s = 80 tick</td><td>40 点</td><td>8.0 s = 160 tick</td></tr>
 *   <tr><td>雷鸣</td><td>8 点</td><td>4.0 s = 80 tick</td><td>40 点</td><td>8.0 s = 160 tick</td></tr>
 * </table>
 * <p>递减口径（作者原话）：翠玉为基准、宝石降 10%、星界与雷鸣降 20%。
 * <b>秒一律先换成 tick 再判整</b>（{@code mcmod_experience.md} §1.12）：{@code 5 × 0.9 = 4.5 s = 90 tick}、
 * {@code 5 × 0.8 = 4.0 s = 80 tick}，两者都是整 tick，<b>零取整歧义</b>——所以代码里存的就是 tick，
 * 秒只在注释与需求文档里出现。</p>
 *
 * <h2>⚠ 被推翻并删除的三个旧字段（需求 §3.9；作者 2026-10-02）</h2>
 * <p>本枚举原来有 {@code throwCost} / {@code mineCost} / {@code cooldownTicks} 三个 {@code int} 字段，
 * 批 2 已<b>整体删除</b>（不是改值、也不是留着不用）。被删掉的现值与推翻依据逐条留档在这里，
 * 免得下一个人按旧口径理解：</p>
 * <table border="1">
 *   <caption>已删除的字段（旧值 → 现行口径）</caption>
 *   <tr><th>旧字段</th><th>旧值（批 1 时仍在）</th><th>推翻依据</th><th>取代者</th></tr>
 *   <tr><td>{@code throwCost()}</td><td>25 / 50 / 75 / 100</td>
 *       <td>需求 §3.2 + §3.9：「拆成"点按消耗 / 长按消耗"两组」</td>
 *       <td>{@link #tapCost()} / {@link #holdCost()}</td></tr>
 *   <tr><td>{@code cooldownTicks()}</td><td>20 / 15 / 10 / 10</td>
 *       <td>需求 §3.2 + §3.9：「拆成两组」</td>
 *       <td>{@link #tapCooldownTicks()} / {@link #holdCooldownTicks()}</td></tr>
 *   <tr><td>{@code mineCost()}</td><td>5 / 10 / 15 / 20</td>
 *       <td>需求 §3.8 + §3.9：「挖方块<b>不再扣能量</b>，改扣耐久」</td>
 *       <td>{@link #WEAR_PER_HIT}（每挖一个方块 −1 耐久）</td></tr>
 * </table>
 * <p>同一处旧注释「<b>耐久 → 能量：这就是"耐久"的替代品</b>"也随之作废：本轮给镖加了
 * <b>真正的耐久</b>（{@link #durability()}），挖方块与命中生物都扣<b>耐久</b>而不是能量。</p>
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

    /** 翠玉镖：钻石级（与 {@code AllTiers.JADE/TOPAZ} 同标签）、硬度上限 10、能量 500、耐久 1000、收回距离 5 格；点按 10 点/100 tick、长按 50 点/200 tick。 */
    JADE_TOPAZ(6.0F, 500, 1000, 10.0F, 3, 10, 50, 100, 200, 5, ToolEnergyColorConfig.JADE),

    /** 宝石镖：下界合金级（与 {@code AllTiers.SAPPHIRE} 同标签）、硬度上限 20（= Quark 默认值）、能量 2000、耐久 2000、收回距离 10 格；点按 9 点/90 tick、长按 45 点/180 tick。 */
    SAPPHIRE_RUBY(8.0F, 2000, 2000, 20.0F, 4, 9, 45, 90, 180, 10, ToolEnergyColorConfig.SAPPHIRE),

    /** 星界镖：下界合金级、硬度上限 30（够到远古残骸的 30）、能量 5000、耐久 3500、收回距离 15 格；点按 8 点/80 tick、长按 40 点/160 tick。 */
    ASTRAL(10.0F, 5000, 3500, 30.0F, 4, 8, 40, 80, 160, 15, ToolEnergyColorConfig.STELLARSTONE),

    /** 雷鸣镖：下界合金级、硬度上限 40（仍够不到黑曜石的 50）、能量 5000、耐久 3500、收回距离 20 格；点按 8 点/80 tick、长按 40 点/160 tick。 */
    THUNDER(12.0F, 5000, 3500, 40.0F, 4, 8, 40, 80, 160, 20, ToolEnergyColorConfig.THUNDERITE);

    /**
     * <b>点按投掷的耐久损耗</b>（需求 §3.8：点按抛出 −2）。
     *
     * <p>它<b>与档位无关</b>（四档同值），所以是常量而不是构造参数；但它是本族自己的数值，
     * 所以<b>只在这里写一遍</b>——{@code AbstractBoomerangEntity} 与 {@code BoomerangItem}
     * 都不许出现 {@code 2} 这个字面量。</p>
     */
    public static final int TAP_THROW_WEAR = 2;
    /** <b>长按（花瓣）投掷的耐久损耗</b>（需求 §3.8：长按抛出 −5）。同 {@link #TAP_THROW_WEAR}，与档位无关。 */
    public static final int HOLD_THROW_WEAR = 5;
    /**
     * <b>每命中一个生物 / 每挖掉一个方块的额外耐久损耗</b>（需求 §3.8：各额外追加 −1）。
     *
     * <p>两条计数共用同一个常量：作者原话是"期间每攻击一次生物或挖一个方块，额外追加 1 点耐久扣除"，
     * 并没有把两者分开（需求 §六 推断值 #4 也只说"穿刺的穿透也照扣"）⇒ 一个常量、两个调用点。</p>
     */
    public static final int WEAR_PER_HIT = 1;

    /** 命中生物的伤害（{@code hurt} 的原始值，不叠手部攻击/附魔）。 */
    private final float damage;
    /** 能量上限（初始即满，与工具同形：ENERGY == MAX_ENERGY）—— 本档盔甲全套能量的一半。 */
    private final int energy;
    /**
     * 耐久上限（2026-10-02 批 1 新增）。
     *
     * <p>它是<b>数值真源</b>：物品注册读它写进原版 {@code MAX_DAMAGE} 组件，实体的回程批量结算
     * （需求 §3.8）也读它。<b>不</b>走 {@code hurtAndBreak}。</p>
     */
    private final int durability;
    /** 能挖动的最大方块硬度（挖方块那条判定的唯一门槛）。 */
    private final float maxHardness;
    /** 挖掘等级 1~4 = 原版石/铁/钻/合金（决定 {@link #incorrectBlocks()}，也用于报告里的对照表）。 */
    private final int miningLevel;
    /** <b>点按</b>一次投掷的能量消耗（10 / 9 / 8 / 8；需求 §3.2）。 */
    private final int tapCost;
    /** <b>长按</b>一次投掷的能量消耗（50 / 45 / 40 / 40；需求 §3.2）。 */
    private final int holdCost;
    /** <b>点按</b>投掷冷却（100 / 90 / 80 / 80 tick = 5.0 / 4.5 / 4.0 / 4.0 秒；需求 §3.2）。 */
    private final int tapCooldownTicks;
    /** <b>长按</b>投掷冷却（200 / 180 / 160 / 160 tick = 10.0 / 9.0 / 8.0 / 8.0 秒；需求 §3.2）。 */
    private final int holdCooldownTicks;
    /**
     * 收回距离（格）：点按去程离主人超过这个距离就立刻掉头（作者 2026-10-02）；
     * 长按的花瓣曲线也把它当最远距离 {@code R}（{@link BoomerangCurveConfigs}）。
     *
     * <p>点按侧它是<b>主判据</b>；去程的时间上限（{@code AbstractBoomerangEntity#MAX_OUTBOUND_TICKS}）
     * 只是"距离判据万一失效"时的兜底。</p>
     */
    private final int returnDistance;
    /**
     * 本档注册进物品的<b>单一能量色</b>（写进 {@code ToolDataComponents.ENERGY_COLOR}，供
     * 文字/绑定行这类不画渐变的场合取用；注册点在 {@code CoeItems#boomerang} 的 {@code .color(...)}）。
     *
     * <p>⚠ <b>它不是能量条的色标</b>：条的色标自 2026-10-03 小修起一律问护甲那张表
     * （{@link #armorSet()} → {@code ArmorEnergyColors#stopsOf}），本字段只剩文字色这一个用途。</p>
     */
    private final ToolEnergyColorConfig color;

    BoomerangTier(float damage, int energy, int durability, float maxHardness, int miningLevel,
                  int tapCost, int holdCost, int tapCooldownTicks, int holdCooldownTicks,
                  int returnDistance, ToolEnergyColorConfig color) {
        this.damage = damage;
        this.energy = energy;
        this.durability = durability;
        this.maxHardness = maxHardness;
        this.miningLevel = miningLevel;
        this.tapCost = tapCost;
        this.holdCost = holdCost;
        this.tapCooldownTicks = tapCooldownTicks;
        this.holdCooldownTicks = holdCooldownTicks;
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

    /**
     * <b>点按</b>投掷的能量消耗（10 / 9 / 8 / 8）。
     *
     * <p>批 2 起取代已删除的 {@code throwCost()}（25 / 50 / 75 / 100，见类注释的作废表）。
     * 模式由 {@code BoomerangItem} 的按键时长判定（&lt; 10 tick = 点按、≥ 10 tick = 长按）。</p>
     */
    public int tapCost() {
        return tapCost;
    }

    /** <b>长按</b>投掷的能量消耗（50 / 45 / 40 / 40）。取代已删除的 {@code throwCost()}。 */
    public int holdCost() {
        return holdCost;
    }

    /**
     * 按模式取投掷能量消耗（<b>唯一一处"模式 → 消耗"的映射</b>）。
     *
     * @param longHold {@code true} = 长按（花瓣曲线），{@code false} = 点按（直线）
     */
    public int throwCost(boolean longHold) {
        return longHold ? holdCost : tapCost;
    }

    /** <b>点按</b>投掷冷却（100 / 90 / 80 / 80 tick = 5.0 / 4.5 / 4.0 / 4.0 秒）。取代已删除的 {@code cooldownTicks()}。 */
    public int tapCooldownTicks() {
        return tapCooldownTicks;
    }

    /** <b>长按</b>投掷冷却（200 / 180 / 160 / 160 tick = 10.0 / 9.0 / 8.0 / 8.0 秒）。取代已删除的 {@code cooldownTicks()}。 */
    public int holdCooldownTicks() {
        return holdCooldownTicks;
    }

    /**
     * 按模式取投掷冷却（tick；<b>唯一一处"模式 → 冷却"的映射</b>）。
     *
     * <p>⚠ 点按与长按<b>共用同一条</b> {@code ItemCooldowns} 键（原版按物品计）⇒ 两组冷却
     * <b>互相覆盖</b>：点按之后 5 秒内连长按也投不出去（谁后投谁把剩余冷却改成自己那一档）。
     * 这是<b>预期行为</b>（作者口径："直接右键操作"只有一条操作路径，不按模式分键）。</p>
     */
    public int cooldownTicks(boolean longHold) {
        return longHold ? holdCooldownTicks : tapCooldownTicks;
    }

    /**
     * 按模式取<b>投掷一次的耐久损耗</b>（点按 −2 / 长按 −5；需求 §3.8）。
     *
     * <p>与能量消耗<b>不是</b>同一件事的两半：能量在投掷那一瞬间进 {@code ToolEnergy} 的账，
     * 耐久则只累计在实体上、等回到玩家手里才结算（{@code AbstractBoomerangEntity#settleWear}）。</p>
     */
    public int throwWear(boolean longHold) {
        return longHold ? HOLD_THROW_WEAR : TAP_THROW_WEAR;
    }

    /**
     * 这一档的收回距离（格）—— <b>实体侧唯一能拿到它的地方</b>（实体里不许出现距离字面量）。
     *
     * <p>点按：去程"离主人超过它 ⇒ {@code setReturning(true)}"由
     * {@code AbstractBoomerangEntity#outboundRangeExceeded} 执行。
     * 长按：它同时是花瓣曲线的 {@code R}（最远距离 = 收回距离，需求 §3.4.2）。</p>
     */
    public int returnDistance() {
        return returnDistance;
    }

    /**
     * <b>花瓣段的"能力范围"</b>（格；作者 2026-10-02 第三次裁定第 3 条）。
     *
     * <p>裁定口径：长按（花瓣）在<b>近程无限制</b>——沿轨迹上的方块全破坏、生物全伤害、不吃额度；
     * 一旦它"到了一个比较远的地方"，就<b>回到上限规则</b>：那时遇到<b>超出本档能力</b>的方块
     * （挖不动 / 超过 {@code maxHardness} / 挖掘等级不够）就「既不破坏也不伤害，直接返回」。</p>
     *
     * <p>⚠⚠ <b>本方法返回的阈值目前是暂定值，正在向作者确认</b>：现在取
     * {@code = returnDistance()}（翠玉 5 / 宝石 10 / 星界 15 / 雷鸣 20 格）。
     * <b>它就是那一处"唯一真源"</b>——作者回话后只改这一行的方法体
     * （例如改成固定 10 格或 {@code returnDistance() * 2}），
     * {@code AbstractBoomerangEntity#withinCapabilityRange()} 与关卡都不用动。</p>
     *
     * <p>为什么不做成第 12 个构造参数：与 {@link #baseSkillLevel()} 同理——构造参数表（11 项）
     * 被关卡 §29h-1 / §29k 逐位钉住，而本值可以从既有字段派生。</p>
     */
    public int capabilityRange() {
        return returnDistance();
    }

    public ToolEnergyColorConfig color() {
        return color;
    }

    /**
     * <b>这一档对应的护甲套</b>（作者 2026-10-03 小修：「能量条的样式和颜色应该与那个装备一样，
     * 而不是你自己造出一个新的渐变」）。
     *
     * <p>四档与四套<b>一一对应</b>：翠玉镖 ↔ 翠玉套、宝石镖 ↔ 宝石套、星界镖 ↔ 星界套、
     * 雷鸣镖 ↔ 雷鸣套。唯一消费点是 {@code BoomerangItem#energyGradientStops}，它拿本方法的结果
     * 去问护甲那套取色源（{@code ArmorEnergyColors#stopsOf(ArmorSet)}）——<b>同一个方法、一行调用</b>，
     * 回旋镖侧既不复写色值、也不自己拼渐变。因此色标条数也跟着护甲走
     * （翠玉 2 / 宝石 2 / <b>星界 4</b> / 雷鸣 2），四把镖与四套护甲逐格一致。</p>
     *
     * <p>⚠ 用 {@code switch} 而不是 {@code ArmorSet.valueOf(name())}：枚举一改名，后者要到
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

    /**
     * <b>这一档的技能基准等级</b>（需求 §3.1 给死）：翠玉 <b>1</b> / 宝石 <b>2</b> / 星界 <b>3</b> /
     * 雷鸣 <b>3</b>。
     *
     * <p>⚠ <b>与护甲不同</b>：{@code ArmorSkillLevels} 里雷鸣是 <b>4</b>，回旋镖的雷鸣是 <b>3</b>
     * ——作者在本轮需求里逐档写死过（§3.1 的表 + §六 推断值 #6 的引用），
     * 所以这里是<b>独立的一份</b>，绝不去引用护甲那张表（引用就会把 4 带进来）。</p>
     *
     * <p>为什么写成 {@code switch (this)} 而不是第 12 个构造参数：本枚举的构造参数表
     * （11 项）在关卡 §29h-1 与 §29k 里<b>逐位钉住</b>，加参数会让那两处一起动；
     * 而基准等级<b>不影响任何其他档位字段的语义</b>，用 {@code switch} 表达"四档 → 四个数"
     * 同样只有一处真源。派生量（额度 / 消耗）读的是
     * {@code SkillEnergyCost.effectiveLevel(stack, 本方法, }{@code BoomerangSkillConfigs.MAX_SKILL_LEVEL}{@code )}
     * ——读取点只有 {@code BoomerangItem#effectiveSkillLevel(ItemStack, int)} 一处。</p>
     *
     * <p>⚠ 正提升量上限是 +2 ⇒ <b>翠玉实际最高 3 级、宝石最高 4 级</b>（星界/雷鸣基准 3 才够到 5），
     * 这是"基准 + 附魔 ±2"的必然结果，见 {@code BoomerangSkillConfigs} 第二节。</p>
     */
    public int baseSkillLevel() {
        return switch (this) {
            case JADE_TOPAZ -> 1;
            case SAPPHIRE_RUBY -> 2;
            case ASTRAL, THUNDER -> 3;
        };
    }

    /**
     * <b>这一档有没有"十字挖掘"这个固有特性</b>（需求 2026-10-03 第二轮 §3.3；
     * 作者裁定：<b>星界 / 雷鸣固有、不占技能槽</b> ⇒ "回旋镖只有两个技能"仍然成立）。
     *
     * <p>翠玉 / 宝石 <b>没有</b>：{@code ASTRAL, THUNDER -> true}、
     * {@code JADE_TOPAZ, SAPPHIRE_RUBY -> false}。</p>
     *
     * <p><b>它是"哪几档开出十字"的唯一判据处</b>：实体侧只有
     * {@code AbstractBoomerangEntity#onHitBlock} 普通支里那一句
     * {@code tier().crossMine() ? mineCross(pos) : mineBlock(pos)} 问过它
     * （容器支一个字没动）；物品注册 / 技能表 / 按键都不许再写第二份"星界雷鸣"的名单。</p>
     *
     * <p>⚠ 与 {@link #armorSet()} / {@link #baseSkillLevel()} / {@link #capabilityRange()}
     * <b>同形</b>：派生量用 {@code switch (this)} 表达，<b>绝不</b>加成第 12 个构造参数 ——
     * 构造参数表（11 项）被关卡 §29h-1 与 §29k <b>逐位钉住</b>，加一个参数那两处一起红。</p>
     *
     * <p><b>穷尽、不写 {@code default}</b>（与 {@link #armorSet()} 同理）：枚举一旦改名、
     * 或将来加成五档，少一个分支<b>编译就不过</b> —— 而不是到运行期悄悄少挖一格。</p>
     */
    public boolean crossMine() {
        return switch (this) {
            case ASTRAL, THUNDER -> true;
            case JADE_TOPAZ, SAPPHIRE_RUBY -> false;
        };
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
