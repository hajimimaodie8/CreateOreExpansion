package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>COE 四套盔甲的材质注册</b>：翠玉 / 宝石 / 星界 / 雷鸣。
 *
 * <p><b>这类文件干什么</b>：{@code Registries.ARMOR_MATERIAL} 是<b>原版注册表</b>（不是 Registrate
 * 管的 Item/Block），所以盔甲材质用 {@link DeferredRegister} 注册——与 {@code AllDataComponents} /
 * {@code AllModPotions} / {@code TransmutationEffects} 同一手法。物品本体仍走 {@code CoeRegistrate}
 * （见 {@link CoeItems} —— 2026-10-01 起四套盔甲的 16 件物品也统一声明在那里）。</p>
 *
 * <p><b>接线两处，缺一不可</b>：① 本类静态字段声明材质；② {@link #register(IEventBus)} 由
 * {@code CreateOreExpansion} 构造器调用（与 {@code AllDataComponents.register} 同处，最早一批）；
 * 物品注册处直接引用 {@link #JADE_TOPAZ} 这类 {@code DeferredHolder}。</p>
 *
 * <h2>耐久度口径（用户 2026-09-30 定稿）</h2>
 * 以<b>原版下界合金</b>为基准（头盔 407 / 胸甲 592 / 护腿 555 / 靴子 481）：
 * <ul>
 *   <li><b>翠玉</b> = 钻石与下界合金的 <b>3/4 分界点</b>。钻石的耐久倍率 33、下界合金 37，
 *       分界点取 <b>36</b>（= 33 + (37−33)×3/4）⇒ 396 / 576 / 540 / 468。</li>
 *   <li><b>宝石</b> = 下界合金的 <b>1.25 倍</b>（倍率 37 × 1.25 ≈ 46.25，取 <b>47</b>，
 *       保证每一件都不低于 1.25 倍）⇒ 517 / 752 / 705 / 611。</li>
 *   <li><b>星界</b> 与 <b>雷鸣</b> = 下界合金的 <b>2 倍</b>（倍率 74）⇒ 814 / 1184 / 1110 / 962。</li>
 * </ul>
 * 原版换算关系（{@code ArmorItem} 用 baseDurability × 倍率）：头盔 ×11、胸甲 ×16、护腿 ×15、靴子 ×13；
 * 下界合金 baseDurability = <b>37</b>。所以上面四组数字都能被 37 整除验证：
 * 36×11=396 / 36×16=576 / 36×15=540 / 36×13=468（翠玉），以此类推。</p>
 *
 * <p><b>贴图位置是硬约定</b>：{@link ArmorMaterial.Layer} 只拿到一个"名字"
 * （这里传 {@code createoreexpansion:<set>_armor}），真正的贴图路径由物品类的
 * {@code getArmorTexture} 覆写决定——本模组沿用 Create 在同一环境实测可用的 <b>legacy 路径</b>：
 * {@code assets/createoreexpansion/textures/models/armor/<set>_armor_layer_{1,2}.png}
 * （层 2 只给护腿用）。见 {@code content/equipment/armor/CoeArmorItem}。</p>
 */
public final class CoeArmorMaterials {

    /** 盔甲材质注册表（命名空间恒为 {@link CoeCore#REGISTRY_NAMESPACE}，与全仓一致）。 */
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, CoeCore.REGISTRY_NAMESPACE);

    // ==================== 四套材质 ====================
    // 参数顺序（register 方法）：防御力 4 件 / 附魔能力 / 音效 / 韧性 / 抗击退 / 修复材料
    // 护甲点数与两条系数口径（作者 2026-10-02 定稿。原版锚点：钻石点数 3/6/8/3 = 合计 20、
    // 韧性 2.0、抗击退 0.0；下界合金点数同为 3/6/8/3 = 合计 20、韧性 3.0、抗击退 0.1）：
    //   翠玉 点数 3/6/8/3 (20) + 韧性 2.5 + 抗击退 0.05 —— 点数与钻石持平，靠韧性 2.5 胜出
    //   宝石 点数 4/7/8/3 (22) + 韧性 4.5 + 抗击退 0.15 —— 韧性 / 抗击退 = 下界合金的 1.5 倍
    //   星界 点数 4/8/9/4 (25) + 韧性 6.0 + 抗击退 0.20 —— 韧性 / 抗击退 = 下界合金的 2 倍
    //   雷鸣 点数 4/8/9/4 (25) + 韧性 6.0 + 抗击退 0.20 —— 与星界同档（2 倍）
    // 点数 20 上限（原版机制，作者 2026-10-02 据此定档）：CombatRules#getDamageAfterAbsorb 参与减伤
    // 的是 clamp(armor - damage/f, armor*0.2F, 20.0F)（f = 2 + 韧性/4；armor = Attributes.ARMOR 合计
    // 值，原版上限 30，并不会先被截到 20）⇒ 有效点数最多 20（减伤上限 80%）；对一次 d 点伤害，
    // 超过 20 + d/f 的点数才完全没有收益、且不会更差，所以超出 20 的部分收益递减，而不是按
    // 1.5×/2× 直接乘上去；点数因此按"逐档不降、且不低于钻石"排，强度由韧性 + 抗击退承担。

    /** <b>翠玉盔甲</b>：耐久 = 钻石与下界合金的 3/4 分界点（倍率 36）。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JADE_TOPAZ = register(
        "jade_topaz",
        new int[] { 3, 6, 8, 3 },
        12,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.5F,
        0.05F,
        () -> Ingredient.of(CoeItems.JADE_INGOT.get()));

    /** <b>宝石盔甲</b>：耐久 = 下界合金的 1.25 倍（倍率 47）。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SAPPHIRE_RUBY = register(
        "sapphire_ruby",
        new int[] { 4, 7, 8, 3 },
        16,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        4.5F,
        0.15F,
        () -> Ingredient.of(CoeItems.TOPAZ_INGOT.get()));

    /** <b>星界盔甲</b>：耐久 = 下界合金的 2 倍（倍率 74），韧性 6.0、抗击退 0.20。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ASTRAL = register(
        "astral",
        new int[] { 4, 8, 9, 4 },
        10,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        6.0F,
        0.20F,
        () -> Ingredient.of(CoeItems.STELLARSTONE_INGOT.get()));

    /** <b>雷鸣盔甲</b>：耐久 = 下界合金的 2 倍（倍率 74），韧性 6.0、抗击退 0.20。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> THUNDER = register(
        "thunder",
        new int[] { 4, 8, 9, 4 },
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        6.0F,
        0.20F,
        () -> Ingredient.of(CoeItems.THUNDERITE_INGOT.get()));

    /** 四套共同遵守的"套装名"常量（用于拼贴图名 / 语言键前缀，避免各处散写字符串）。 */
    public static final String JADE_TOPAZ_SET = "jade_topaz";
    public static final String SAPPHIRE_RUBY_SET = "sapphire_ruby";
    public static final String ASTRAL_SET = "astral";
    public static final String THUNDER_SET = "thunder";

    /**
     * 盔甲层的"名字"——决定 {@code getArmorTexture} 拼出的贴图文件名。
     *
     * @param setName 套名（{@code jade_topaz} / {@code sapphire_ruby} / {@code astral} / {@code thunder}）
     * @return {@code createoreexpansion:<setName>_armor}
     */
    public static ResourceLocation layerName(String setName) {
        return CoeCore.modLoc(setName + "_armor");
    }

    /**
     * 注册一套盔甲材质。
     *
     * @param name                 材质名（同时是注册 id 与贴图名前缀）
     * @param defense              4 个槽位的防御力，顺序必须是 {@link ArmorItem.Type} 的枚举声明序
     *                             （HELMET / CHESTPLATE / LEGGINGS / BOOTS）
     * @param enchantmentValue     附魔能力
     * @param equipSound           穿戴音效
     * @param toughness            韧性
     * @param knockbackResistance  抗击退
     * @param repairIngredient     铁砧修复材料（延迟取值，避免与本类字段初始化顺序打结）
     */
    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(
            String name,
            int[] defense,
            int enchantmentValue,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient) {
        EnumMap<ArmorItem.Type, Integer> defenseMap = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            if (type.ordinal() < defense.length) {
                defenseMap.put(type, defense[type.ordinal()]);
            }
        }
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(layerName(name)));
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
            defenseMap,
            enchantmentValue,
            equipSound,
            repairIngredient,
            layers,
            toughness,
            knockbackResistance));
    }

    /**
     * 把本类挂到 mod 事件总线。
     *
     * <p><b>必须且只能调一次</b>（同一个 {@code DeferredRegister} 挂两次总线会在 RegisterEvent 上
     * 重复注册）——调用点见 {@code CreateOreExpansion} 构造器，与其它 DeferredRegister 同处。</p>
     */
    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }

    private CoeArmorMaterials() {}
}
