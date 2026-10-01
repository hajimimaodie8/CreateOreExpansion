package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeArmorMaterials;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * <b>COE 四套盔甲的「套」身份 + 「必须佩戴全套才有效果」的唯一判定入口。</b>
 *
 * <p>本类是<b>纯查询工具</b>：不注册任何东西、不订阅任何事件、不缓存任何状态。
 * 全部套装技能与套装被动效果<b>必须先过这里</b>，不要在各自的处理器里各写一遍槽位遍历
 * （本仓的"唯一取值点"纪律，与 {@code IMedallion#isWearing} 同范式）。</p>
 *
 * <h2>用户 2026-10-01 的裁定（本类的定义来源）</h2>
 * <ol>
 *   <li><b>「全套」= 同套四件</b>：四个<b>原版护甲槽</b>（头 / 胸 / 腿 / 脚）都非空，且四件同属一套。
 *       <b>不是</b>"任意四件本模组护甲"，也<b>不按件数打折</b>——预告原文"必须佩戴全套才能有效果"
 *       是硬门槛，不是按件数加成。</li>
 *   <li><b>判定入口返回等级，不是 boolean</b>：{@link #wornLevel(Player)} 返回该套的生效等级，
 *       <b>0 = 未生效</b>。理由：本模组所有技能都有等级（{@code configsByLevel} + {@code *Configs}），
 *       套装技能将来也要接同一套体系，接口形状一次定死，免得日后改所有调用点。</li>
 *   <li><b>基准等级 = 每套一个固定值</b>：翠玉 {@code 1} / 宝石 {@code 2} / 星界 {@code 3} / 雷鸣 {@code 4}
 *       （按升级阶梯）。将来"技艺提升 / 记忆回溯"附魔在这个基准上加减
 *       （预告："通常来说，技能的最高等级为 5"，所以基准 4 + 技艺提升最高 2 级是留了余量的）。</li>
 *   <li><b>散构聚能附魔例外：本轮只留接口</b>（附魔尚未注册）。预告原文是"附魔在装备上之后可以随心搭配，
 *       打破全套强制限制，但仍需要装备栏中至少有一件本模组的特殊装备部分才能生效"。
 *       本类为此提供 {@link #wearsAnyOurArmor(Player)} 这个<b>门槛查询</b>；
 *       <b>混搭时算几级仍未裁定</b>（见下方"未定项"），所以不要把放松分支写进 {@link #wornLevel(Player)}。</li>
 * </ol>
 *
 * <h2>判定细节（都写死在这里，别在调用点重写）</h2>
 * <ul>
 *   <li><b>只认四个原版护甲槽</b>：不含副手、不含 Curios 饰品槽（凝能佩才走 Curios）。</li>
 *   <li><b>不判耐久、不判附魔</b>：与原版一致——破损的护甲仍算穿着，附魔不同不影响"是不是同一套"。</li>
 *   <li><b>不缓存</b>：一次判定 = 4 次 {@code getItemBySlot}，比维护"装备变更事件 + 失效"便宜得多，
 *       也不会出现"换了装备但缓存没刷新"的隐蔽 bug。</li>
 * </ul>
 *
 * <h2>「散构聚能」放宽带门槛（用户 2026-10-01 第二轮裁定，已落地）</h2>
 * <p>原本的分区判据只有"四件同套"。现在多一条：<b>本套恰好 3 件</b>（只差一件）且穿戴中
 * <b>任意一件护甲</b>带 {@link ArmorEnchantments#LOOSE_CONVERGENCE} ⇒ 视为齐全；<b>2 件无效</b>。
 * 该附魔是<b>宝藏附魔、只有 1 级、可附在任意护甲上</b>（含原版/他模组），判定在
 * {@link #isCompletedByEnchant(Player)}。</p>
 *
 * <h2>仍未定项（别自己发明）</h2>
 * <ul>
 *   <li>各套装技能<b>每一级的具体效果</b>（用户 2026-10-01 明确"等级效果还没定义"）。</li>
 *   <li>装备技能的<b>能量来源</b>（护甲是否也挂 {@code ToolEnergy}）—— 挡住
 *       {@code ArmorSkillProvider} 里的实例构造。</li>
 * </ul>
 *
 * @see CoeArmorMaterials 四套材质与"套名"常量的真源（本类的 {@link #setName} 与它同源）
 */
public enum ArmorSet {

    /** 翠玉套（基准 Lv1；耐久倍率 36，防御略强于铁；单件储能 250 ⇒ 满套 1000）。 */
    JADE(CoeArmorMaterials.JADE_SET, 1, CoeArmorMaterials.JADE, 250),
    /** 宝石套（基准 Lv2；耐久 = 下界合金 1.25 倍；单件储能 1000 ⇒ 满套 4000）。 */
    GEM(CoeArmorMaterials.GEM_SET, 2, CoeArmorMaterials.GEM, 1000),
    /** 星界套（基准 Lv3；耐久 = 下界合金 2 倍，韧性 2；单件储能 2500 ⇒ 满套 10000）。 */
    ASTRAL(CoeArmorMaterials.ASTRAL_SET, 3, CoeArmorMaterials.ASTRAL, 2500),
    /** 雷鸣套（基准 Lv4；与星界同耐久，韧性 3 + 抗击退；单件储能 2500 ⇒ 满套 10000）。 */
    THUNDER(CoeArmorMaterials.THUNDER_SET, 4, CoeArmorMaterials.THUNDER, 2500);

    /**
     * 四个护甲槽（判定顺序固定为头 → 胸 → 腿 → 脚）。
     *
     * <p>刻意<b>不用</b> {@code ArmorItem.Type.values()} 循环：那张表还含 {@code BODY}（狼铠），
     * 将来原版若再加槽位，这里也不会被悄悄带进判定。</p>
     */
    private static final List<EquipmentSlot> ARMOR_SLOTS =
        List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /**
     * 四个护甲槽的<b>唯一来源</b>（顺序 = 头 / 胸 / 腿 / 脚）。
     *
     * <p>判定、能量合计、消耗分摊、凝能佩充能、提示层全都要这份清单 —— 一律问这里，
     * 别在各自类里再写一遍（写两份必然漂移，且"顺序"本身是消耗分摊的语义）。</p>
     */
    public static List<EquipmentSlot> armorSlots() {
        return ARMOR_SLOTS;
    }

    private static final ArmorSet[] VALUES = values();

    /** 套名（{@code jade} / {@code gem} / {@code astral} / {@code thunder}）：贴图名与语言键的统一前缀。 */
    private final String setName;

    /** 全套生效时的基准等级（1~4，按升级阶梯）。 */
    private final int baseLevel;

    /** 该套的盔甲材质；物品归属靠它判（护甲物品就是用它构造的，同一 Holder 实例 ⇒ 身份比较即可）。 */
    private final Holder<ArmorMaterial> material;

    /**
     * <b>单件储能</b>（用户 2026-10-01 定稿）：翠玉 250 / 宝石 1000 / 星界 2500 / 雷鸣 2500。
     * 满套总容量 = 四件之和（1000 / 4000 / 10000 / 10000）。
     *
     * <p>⚠ 这只是"这件护甲本身"的容量；<b>带散构聚能的件一律 2500</b>（用户按字面选定），
     * 那条例外在 {@link ArmorEnergy#maxOf(net.minecraft.world.item.ItemStack)} 里生效 ——
     * 因为容量取决于"有没有那个附魔"，附魔会被铁砧加减，不能写死成物品组件。</p>
     */
    private final int perPieceEnergy;

    ArmorSet(String setName, int baseLevel, Holder<ArmorMaterial> material, int perPieceEnergy) {
        this.setName = setName;
        this.baseLevel = baseLevel;
        this.material = material;
        this.perPieceEnergy = perPieceEnergy;
    }

    /** 单件储能（250 / 1000 / 2500 / 2500）。 */
    public int perPieceEnergy() {
        return this.perPieceEnergy;
    }

    /** 套名（{@code jade} 等），与 {@link CoeArmorMaterials#JADE_SET} 同源。 */
    public String setName() {
        return this.setName;
    }

    /** 全套生效时的基准等级（1~4）。 */
    public int baseLevel() {
        return this.baseLevel;
    }

    /**
     * 该物品属于哪一套。
     *
     * @return 本模组四套护甲之一 ⇒ 对应的枚举值；不是护甲 / 不是本模组的 ⇒ {@code null}
     */
    public static @Nullable ArmorSet of(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
            return null;
        }
        Holder<ArmorMaterial> material = armor.getMaterial();
        for (ArmorSet set : VALUES) {
            if (set.material == material) {
                return set;
            }
        }
        return null;
    }

    /** 是否本模组四套护甲中的任意一件（散构聚能例外的门槛要用，见类注释"未定项"）。 */
    public static boolean isOurArmor(ItemStack stack) {
        return of(stack) != null;
    }

    /**
     * 玩家四个护甲槽里<b>完整穿着</b>的那一套。
     *
     * @return 四槽都非空且同属一套 ⇒ 该套；否则（有空槽 / 混搭 / 穿了他模组护甲）⇒ {@code null}
     */
    public static @Nullable ArmorSet wornSet(Player player) {
        if (player == null) {
            return null;
        }
        ArmorSet found = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ArmorSet set = of(player.getItemBySlot(slot));
            if (set == null) {
                // 任一槽位空着、或穿的不是本模组护甲 ⇒ 不成套（这是"硬门槛"的落点）
                return null;
            }
            if (found == null) {
                found = set;
            } else if (found != set) {
                // 四件不同套 ⇒ 混搭 ⇒ 不成套
                return null;
            }
        }
        return found;
    }

    /**
     * 玩家当前<b>生效</b>的那一套：优先严格全套，其次「散构聚能补齐」的那一套。
     *
     * <p><b>技能来源与提示层都必须问这个</b>（不是 {@link #wornSet(Player)}）：
     * 3 件 + 散构聚能时 {@code wornSet} 为 {@code null}，但那一套是生效的。</p>
     *
     * @return 生效的套；没有（未成套且无补齐）返回 {@code null}
     */
    public static @Nullable ArmorSet effectiveSet(Player player) {
        ArmorSet strict = wornSet(player);
        if (strict != null) {
            return strict;
        }
        for (ArmorSet set : VALUES) {
            if (set.isCompletedByEnchant(player)) {
                return set;
            }
        }
        return null;
    }

    /**
     * <b>「必须佩戴全套才有效果」的唯一判定入口。</b>
     *
     * <p>两条路径（用户 2026-10-01 定义）：</p>
     * <ol>
     *     <li><b>严格全套</b>：四槽同套 ⇒ 基准等级；</li>
     *     <li><b>散构聚能补齐</b>：本套 <b>恰好 3 件</b>（就只差一件）且穿戴中<b>任意一件护甲</b>
     *         带该附魔 ⇒ 也算生效（等级同样取基准等级）；<b>只有 2 件时无效</b>
     *         —— 用户原话："如果身上只有两件配套装备，这个是不起作用的"。</li>
     * </ol>
     *
     * @return 生效等级（1~4）；未生效返回 <b>0</b>
     */
    public int wornLevel(Player player) {
        return effectiveSet(player) == this ? this.baseLevel : 0;
    }

    /**
     * 本套是否靠「散构聚能」补上了缺的那一件（3 件同套 + 穿戴中任意一件带该附魔）。
     *
     * <p>提示层用它区分「全套生效」与「散构聚能补齐」两种来源；两者生效等级相同。</p>
     */
    public boolean isCompletedByEnchant(Player player) {
        if (player == null || countWorn(player) != 3) {
            // 只差一件才算补齐：4 件走严格全套，2 件及以下按用户裁定无效
            return false;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (ArmorEnchantments.looseConvergenceLevel(player.getItemBySlot(slot)) > 0) {
                return true;
            }
        }
        return false;
    }

    /** 玩家四个护甲槽里属于本套的件数（0~4；空槽与他模组护甲都不计）。 */
    public int countWorn(Player player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (of(player.getItemBySlot(slot)) == this) {
                count++;
            }
        }
        return count;
    }

    /**
     * 玩家护甲槽里是否至少有一件本模组护甲（<b>不要求成套</b>）。
     *
     * <p><b>它不是生效门槛</b>（2026-10-01 第二轮裁定后，门槛是"同套 3 件 + 散构聚能"，见
     * {@link #isCompletedByEnchant(Player)}）。它只用来<b>区分提示文案</b>：
     * 一件本模组护甲都没穿 / 穿了但不够 3 件，这两种情况该说的话不一样。</p>
     */
    public static boolean wearsAnyOurArmor(Player player) {
        if (player == null) {
            return false;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (isOurArmor(player.getItemBySlot(slot))) {
                return true;
            }
        }
        return false;
    }
}
