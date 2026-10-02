package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * <b>COE 盔甲物品基类</b>（四套双色盔甲 16 件物品共用）。
 *
 * <p><b>它为什么存在</b>：1.21.1 的 {@link ArmorMaterial} 只描述"材料"，<b>贴图路径</b>要么走
 * 数据驱动的 {@code EquipmentClientInfo}（{@code assets/<ns>/equipment/**.json}），要么由物品覆写
 * {@link ArmorItem#getArmorTexture}。本模组选择<b>覆写</b>这一支——理由：</p>
 * <ol>
 *   <li>与 Create 在<b>同一套运行环境</b>实测可用的写法一致
 *       （{@code com.simibubi.create...BaseArmorItem} 就是这么覆写的），不用赌数据格式；</li>
 *   <li>贴图直接落 {@code assets/createoreexpansion/textures/models/armor/}，路径一眼可读；</li>
 *   <li>少一层数据文件，用户后续"逐文件标注用途"时更容易交代。</li>
 * </ol>
 *
 * <p><b>贴图路径约定</b>：{@code assets/<namespace>/textures/models/armor/<name>_layer_1.png}
 * （头盔 / 胸甲 / 靴子）与 {@code ..._layer_2.png}（<b>只有护腿</b>）。
 * 传给构造器的 {@code textureLoc} 是<b>去掉 {@code _layer_N} 后缀</b>的名字，
 * 例如 {@code createoreexpansion:jade_topaz_armor} → {@code jade_topaz_armor_layer_1.png}。</p>
 *
 * <p><b>注意</b>：{@code Properties.stacksTo(1)} 是盔甲类的惯例（继承自 Create 的写法），
 * 但真正决定"能否堆叠"的是物品属性本身；此处保持与 Create 一致。</p>
 */
public class CoeArmorItem extends ArmorItem {

    /** 该套盔甲的贴图基名（不含 {@code _layer_N} 后缀）。 */
    protected final ResourceLocation textureLoc;

    public CoeArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, ResourceLocation textureLoc) {
        super(material, type, properties.stacksTo(1));
        this.textureLoc = textureLoc;
    }

    /**
     * 返回该槽位该用哪张贴图。
     *
     * @param slot        穿戴部位（{@code LEGS} 用第 2 层，其余用第 1 层）
     * @param innerModel  是否内层模型（1.21.1 起该参数已被层文件取代，此处忽略，保留给签名）
     */
    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                                     ArmorMaterial.Layer layer, boolean innerModel) {
        int layerIndex = slot == EquipmentSlot.LEGS ? 2 : 1;
        return ResourceLocation.fromNamespaceAndPath(
            this.textureLoc.getNamespace(),
            String.format(Locale.ROOT, "textures/models/armor/%s_layer_%d.png", this.textureLoc.getPath(), layerIndex));
    }
}
