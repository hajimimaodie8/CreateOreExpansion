package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.CoeArmorItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * <b>COE 四套盔甲的物品注册（4 套 × 4 件 = 16 件）</b>：翠玉 / 宝石 / 星界 / 雷鸣。
 *
 * <p><b>为什么单独一个文件、而不是塞进 {@code CoeItems}</b>：{@code CoeItems} 已经 1400 行、
 * 装着 80 多件物品与两个 builder 内类。盔甲是"一个完整子系统"（材质 + 物品 + 套装效果 + 技能），
 * 单独成文件后：① 用户逐文件标注用途时一目了然；② 后续的套装效果层只改这一个文件与它自己的
 * handler；③ 16 件物品集中成 4 组，看得见"每套 4 件"的结构。</p>
 *
 * <p><b>触发方式</b>：本类靠类初始化注册（Registrate 的注册动作就是字段初始化），
 * {@link #register()} 方法体为空。它由 {@link CoeItems#register()} <b>显式调用</b>——
 * 这样"物品注册的先后顺序"仍然只有一条线索（{@code CreateOreExpansion} 构造器里那句
 * {@code CoeItems.register()}）。</p>
 *
 * <h2>注册 id（16 个；命名空间恒 {@code createoreexpansion}）</h2>
 * <pre>
 * jade_helmet    jade_chestplate    jade_leggings    jade_boots      翠玉
 * gem_helmet     gem_chestplate     gem_leggings     gem_boots       宝石
 * astral_helmet  astral_chestplate  astral_leggings  astral_boots    星界
 * thunder_helmet thunder_chestplate thunder_leggings thunder_boots   雷鸣
 * </pre>
 *
 * <h2>耐久度口径（用户 2026-09-30 定稿，逐件写死）</h2>
 * <p><b>为什么写死在物品属性上</b>：1.21.1 的 {@code ArmorMaterial} 记录<b>已经不含耐久倍率</b>
 * （字段只有 defense / enchantmentValue / equipSound / repairIngredient / layers / toughness /
 * knockbackResistance）；原版是把「倍率 × {@code ArmorItem.Type.getDurability(int)}」直接写进
 * {@code Item.Properties.durability(...)}（实测字节码：{@code NETHERITE_HELMET} 用 {@code bipush 37}
 * + {@code Type.getDurability}，槽位倍率 = 头盔 11 / 胸甲 16 / 护腿 15 / 靴子 13）。
 * 所以本模组也走同一条路：{@link Durability} 里算好四种基准值，逐件传给物品属性。</p>
 * <ul>
 *   <li><b>翠玉</b>：钻石(33) 与下界合金(37) 的 3/4 分界点 ⇒ 基准 <b>36</b> ⇒ 396 / 576 / 540 / 468</li>
 *   <li><b>宝石</b>：下界合金 1.25 倍 ⇒ 基准 <b>47</b>（≥1.25×）⇒ 517 / 752 / 705 / 611</li>
 *   <li><b>星界</b>：下界合金 2 倍 ⇒ 基准 <b>74</b> ⇒ 814 / 1184 / 1110 / 962</li>
 *   <li><b>雷鸣</b>：与星界相同 ⇒ 基准 <b>74</b> ⇒ 814 / 1184 / 1110 / 962</li>
 * </ul>
 *
 * <p><b>贴图</b>（用户提供素材，未改一像素）：物品图标
 * {@code assets/createoreexpansion/textures/item/<id>.png}；装备层
 * {@code assets/createoreexpansion/textures/models/armor/<set>_armor_layer_{1,2}.png}。</p>
 */
public final class CoeArmorItems {

    // ==================== 翠玉盔甲（jade）====================
    // 素材：矿物扩展双色盔甲.zip 组1（亮绿 + 金）
    // 物品 1790667044966 / 1790667048920 / 1790667053458 / 1790667058085
    // 装备层 1790668020120(layer_1) / 1790670229300(layer_2)

    public static final ItemEntry<CoeArmorItem> JADE_HELMET = armor(
        "jade_helmet", CoeArmorMaterials.JADE, ArmorItem.Type.HELMET, "jade_armor", Durability.JADE_HELMET);

    public static final ItemEntry<CoeArmorItem> JADE_CHESTPLATE = armor(
        "jade_chestplate", CoeArmorMaterials.JADE, ArmorItem.Type.CHESTPLATE, "jade_armor", Durability.JADE_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> JADE_LEGGINGS = armor(
        "jade_leggings", CoeArmorMaterials.JADE, ArmorItem.Type.LEGGINGS, "jade_armor", Durability.JADE_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> JADE_BOOTS = armor(
        "jade_boots", CoeArmorMaterials.JADE, ArmorItem.Type.BOOTS, "jade_armor", Durability.JADE_BOOTS);

    // ==================== 宝石盔甲（gem）====================
    // 素材：组2（深蓝紫 + 红，饱和度最高）
    // 物品 1790670583422 / 1790670587464 / 1790670592296 / 1790670596333
    // 装备层 1790670637200(layer_1) / 1790670820040(layer_2)

    public static final ItemEntry<CoeArmorItem> GEM_HELMET = armor(
        "gem_helmet", CoeArmorMaterials.GEM, ArmorItem.Type.HELMET, "gem_armor", Durability.GEM_HELMET);

    public static final ItemEntry<CoeArmorItem> GEM_CHESTPLATE = armor(
        "gem_chestplate", CoeArmorMaterials.GEM, ArmorItem.Type.CHESTPLATE, "gem_armor", Durability.GEM_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> GEM_LEGGINGS = armor(
        "gem_leggings", CoeArmorMaterials.GEM, ArmorItem.Type.LEGGINGS, "gem_armor", Durability.GEM_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> GEM_BOOTS = armor(
        "gem_boots", CoeArmorMaterials.GEM, ArmorItem.Type.BOOTS, "gem_armor", Durability.GEM_BOOTS);

    // ==================== 星界盔甲（astral）====================
    // 素材：组3（淡蓝灰 + 粉/青，低饱和）
    // 物品 1790674037442 / 1790674049296 / 1790674056157 / 1790674088692
    // 装备层 1790676421592(layer_1) / 1790676959906(layer_2)

    public static final ItemEntry<CoeArmorItem> ASTRAL_HELMET = armor(
        "astral_helmet", CoeArmorMaterials.ASTRAL, ArmorItem.Type.HELMET, "astral_armor", Durability.ASTRAL_HELMET);

    public static final ItemEntry<CoeArmorItem> ASTRAL_CHESTPLATE = armor(
        "astral_chestplate", CoeArmorMaterials.ASTRAL, ArmorItem.Type.CHESTPLATE, "astral_armor", Durability.ASTRAL_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> ASTRAL_LEGGINGS = armor(
        "astral_leggings", CoeArmorMaterials.ASTRAL, ArmorItem.Type.LEGGINGS, "astral_armor", Durability.ASTRAL_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> ASTRAL_BOOTS = armor(
        "astral_boots", CoeArmorMaterials.ASTRAL, ArmorItem.Type.BOOTS, "astral_armor", Durability.ASTRAL_BOOTS);

    // ==================== 雷鸣盔甲（thunder）====================
    // 素材：E:\mc\mc资料\雷鸣合金系列和玉牌（light periwinkle，avgB 175）
    // 物品 thunderite_helmet / thunderite_chestplate / thunderite_leggings / thunderite_boots
    // 装备层 1786950423536(layer_1) / 1786950487011(layer_2)

    public static final ItemEntry<CoeArmorItem> THUNDER_HELMET = armor(
        "thunder_helmet", CoeArmorMaterials.THUNDER, ArmorItem.Type.HELMET, "thunder_armor", Durability.THUNDER_HELMET);

    public static final ItemEntry<CoeArmorItem> THUNDER_CHESTPLATE = armor(
        "thunder_chestplate", CoeArmorMaterials.THUNDER, ArmorItem.Type.CHESTPLATE, "thunder_armor", Durability.THUNDER_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> THUNDER_LEGGINGS = armor(
        "thunder_leggings", CoeArmorMaterials.THUNDER, ArmorItem.Type.LEGGINGS, "thunder_armor", Durability.THUNDER_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> THUNDER_BOOTS = armor(
        "thunder_boots", CoeArmorMaterials.THUNDER, ArmorItem.Type.BOOTS, "thunder_armor", Durability.THUNDER_BOOTS);

    /**
     * <b>四套盔甲的逐件耐久度</b>——数值集中在这里，改平衡只改这一段。
     *
     * <p>换算依据（1.21.1 实测字节码）：原版把「基准 × 槽位倍率」写进 {@code Item.Properties.durability}，
     * 基准取自 {@code ArmorItem.Type.getDurability(int)}，<b>下界合金基准 = 37</b>，
     * 槽位倍率 = 头盔 11 / 胸甲 16 / 护腿 15 / 靴子 13。于是：</p>
     * <pre>
     * 材质   基准  头盔(×11)  胸甲(×16)  护腿(×15)  靴子(×13)   与下界合金之比
     * 钻石    33      363       528       495       429        0.89x（原版对照）
     * 下界合金 37      407       592       555       481        1.00x（基准）
     * 翠玉    36      396       576       540       468        0.97x（= 钻/合金 3/4 分界点）
     * 宝石    47      517       752       705       611        1.27x（≥1.25x）
     * 星界    74      814      1184      1110       962        2.00x（恰为 2 倍）
     * 雷鸣    74      814      1184      1110       962        2.00x（与星界同）
     * </pre>
     */
    public static final class Durability {
        /** 原版钻石盔甲的耐久基准（对照用，不参与注册）。 */
        public static final int VANILLA_DIAMOND_BASE = 33;
        /** 原版下界合金盔甲的耐久基准（本模组四套的换算基准）。 */
        public static final int VANILLA_NETHERITE_BASE = 37;

        // 翠玉：钻石与下界合金的 3/4 分界点 = 33 + (37-33)*3/4 = 36
        public static final int JADE_HELMET = 36 * 11;
        public static final int JADE_CHESTPLATE = 36 * 16;
        public static final int JADE_LEGGINGS = 36 * 15;
        public static final int JADE_BOOTS = 36 * 13;

        // 宝石：下界合金 1.25 倍（37*1.25 = 46.25 → 47，保证不低于 1.25 倍）
        public static final int GEM_HELMET = 47 * 11;
        public static final int GEM_CHESTPLATE = 47 * 16;
        public static final int GEM_LEGGINGS = 47 * 15;
        public static final int GEM_BOOTS = 47 * 13;

        // 星界：下界合金 2 倍
        public static final int ASTRAL_HELMET = 74 * 11;
        public static final int ASTRAL_CHESTPLATE = 74 * 16;
        public static final int ASTRAL_LEGGINGS = 74 * 15;
        public static final int ASTRAL_BOOTS = 74 * 13;

        // 雷鸣：与星界相同（下界合金 2 倍）
        public static final int THUNDER_HELMET = 74 * 11;
        public static final int THUNDER_CHESTPLATE = 74 * 16;
        public static final int THUNDER_LEGGINGS = 74 * 15;
        public static final int THUNDER_BOOTS = 74 * 13;

        private Durability() {}
    }

    /**
     * 注册一件盔甲。
     *
     * <p>四件事集中在这一处：① 材质（{@code Holder<ArmorMaterial>}）；
     * ② 槽位与贴图基名（{@link CoeArmorItem} 用后者拼 {@code _layer_N} 路径）；
     * ③ 逐件耐久度；④ 护甲附魔标签（原版按槽位分 4 个 tag）+ 盔甲纹饰标签；
     * ⑤ 物品图标模型（{@code item/generated}，贴图 {@code item/<id>}）。</p>
     *
     * @param id           注册 id（同时是图标贴图名）
     * @param material     盔甲材质 holder（见 {@link CoeArmorMaterials}）
     * @param type         槽位
     * @param textureBase  装备层贴图基名（不带命名空间，不带 {@code _layer_N} 后缀）
     * @param durability   该件的耐久度（见上面 {@link Durability} 的换算表）
     */
    private static ItemEntry<CoeArmorItem> armor(String id, Holder<ArmorMaterial> material,
                                                 ArmorItem.Type type, String textureBase, int durability) {
        ResourceLocation textureLoc = CoeCore.modLoc(textureBase);
        // ⑥ 能量：<b>初始即满</b>（用户 2026-10-01："所有东西制造出来之后、或者在创造模式物品栏
        // 里面，能量都是满的"）。做法与工具一致（CoeItems 的 EnergyItemBuilder 也是
        // defaultEnergy == maxEnergy）：ENERGY 与 MAX_ENERGY 写同一个值。
        // 容量真源 = ArmorSet.perPieceEnergy()（翠玉 250 / 宝石 1000 / 星界·雷鸣 2500），
        // 按材质查套 —— 注册期还没有 ItemStack，所以走 ArmorSet.byMaterial。
        // 注意：组件是逐堆的，老存档里已存在的那件保留它自己存的值（不回溯、不需要迁移）。
        ArmorSet set = ArmorSet.byMaterial(material);
        int perPiece = set == null ? 0 : set.perPieceEnergy();
        return CoeRegistrate.REGISTRATE
            .item(id, p -> new CoeArmorItem(material, type, p, textureLoc))
            .properties(p -> p.durability(durability)
                .component(AllDataComponents.ENERGY, perPiece)
                .component(AllDataComponents.MAX_ENERGY, perPiece))
            .tag(armorTag(type))
            .tag(ItemTags.TRIMMABLE_ARMOR)
            .model((ctx, provider) -> provider.generated(ctx::get, CoeCore.modLoc("item/" + id)))
            .register();
    }

    /** 槽位 → 原版护甲标签（决定护甲类附魔能否附上）。 */
    private static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> armorTag(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> ItemTags.HEAD_ARMOR;
            case CHESTPLATE -> ItemTags.CHEST_ARMOR;
            case LEGGINGS -> ItemTags.LEG_ARMOR;
            case BOOTS -> ItemTags.FOOT_ARMOR;
            default -> ItemTags.HEAD_ARMOR; // BODY（狼/马甲）本模组不使用
        };
    }

    /** 触发本类注册（Registrate 的注册动作就是字段初始化，因此方法体为空）。 */
    public static void register() {
    }

    private CoeArmorItems() {}
}
