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
 * <h2>未定项（等用户裁定，别自己发明）</h2>
 * <ul>
 *   <li>装了散构聚能附魔、且四件混搭时，{@link #wornLevel(Player)} 该返回几：
 *       取"身上最高那一套的基准等级"？还是"件数最多那一套"？还是"最高等级减去缺的件数"？</li>
 *   <li>散构聚能的<b>等级数 / 获取途径 / 每级作用范围</b>（附魔本身还没注册）。</li>
 *   <li>各套装技能<b>每一级的具体效果</b>（用户 2026-10-01 明确"等级效果还没定义"）。</li>
 * </ul>
 *
 * @see CoeArmorMaterials 四套材质与"套名"常量的真源（本类的 {@link #setName} 与它同源）
 */
public enum ArmorSet {

    /** 翠玉套（基准 Lv1；耐久倍率 36，防御略强于铁）。 */
    JADE(CoeArmorMaterials.JADE_SET, 1, CoeArmorMaterials.JADE),
    /** 宝石套（基准 Lv2；耐久 = 下界合金 1.25 倍）。 */
    GEM(CoeArmorMaterials.GEM_SET, 2, CoeArmorMaterials.GEM),
    /** 星界套（基准 Lv3；耐久 = 下界合金 2 倍，韧性 2）。 */
    ASTRAL(CoeArmorMaterials.ASTRAL_SET, 3, CoeArmorMaterials.ASTRAL),
    /** 雷鸣套（基准 Lv4；与星界同耐久，韧性 3 + 抗击退）。 */
    THUNDER(CoeArmorMaterials.THUNDER_SET, 4, CoeArmorMaterials.THUNDER);

    /**
     * 四个护甲槽（判定顺序固定为头 → 胸 → 腿 → 脚）。
     *
     * <p>刻意<b>不用</b> {@code ArmorItem.Type.values()} 循环：那张表还含 {@code BODY}（狼铠），
     * 将来原版若再加槽位，这里也不会被悄悄带进判定。</p>
     */
    private static final List<EquipmentSlot> ARMOR_SLOTS =
        List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final ArmorSet[] VALUES = values();

    /** 套名（{@code jade} / {@code gem} / {@code astral} / {@code thunder}）：贴图名与语言键的统一前缀。 */
    private final String setName;

    /** 全套生效时的基准等级（1~4，按升级阶梯）。 */
    private final int baseLevel;

    /** 该套的盔甲材质；物品归属靠它判（护甲物品就是用它构造的，同一 Holder 实例 ⇒ 身份比较即可）。 */
    private final Holder<ArmorMaterial> material;

    ArmorSet(String setName, int baseLevel, Holder<ArmorMaterial> material) {
        this.setName = setName;
        this.baseLevel = baseLevel;
        this.material = material;
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
     * <b>「必须佩戴全套才有效果」的唯一判定入口。</b>
     *
     * @return 玩家完整穿着本套 ⇒ 本套基准等级（1~4）；否则 <b>0</b>（= 未生效）
     */
    public int wornLevel(Player player) {
        return wornSet(player) == this ? this.baseLevel : 0;
    }

    /**
     * 玩家护甲槽里是否至少有一件本模组护甲（<b>不要求成套</b>）。
     *
     * <p>这是预告里散构聚能附魔那条例外的<b>门槛</b>："打破全套强制限制，但仍需要装备栏中至少有一件
     * 本模组的特殊装备部分才能生效"。本轮只提供查询，放松后的等级口径等用户裁定后再接。</p>
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
