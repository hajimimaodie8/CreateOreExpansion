package com.hjmmd_8.createoreexpansion.content.equipment.medallion.bridge;

import com.hjmmd_8.createoreexpansion.content.equipment.medallion.BaseStressMedallionItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 凝能佩 ↔ Curios 的桥接契约（<b>本接口绝不 import 任何 Curios 类型</b>，未装 Curios 时也能安全加载）。
 *
 * <p><b>为什么要有它</b>：Curios 已从 required 降为 optional（见 {@code neoforge.mods.toml} 模板）。
 * 只要"引用了 Curios 类的那个类"在未装 Curios 时被加载，就是 {@code NoClassDefFoundError} → 加载阶段崩溃。
 * 因此所有 Curios API 调用都被关进 {@link com.hjmmd_8.createoreexpansion.compat.curios.CurioMedallionBridge}
 * 这一个实现类里；主类仅在 {@code ModList.isLoaded("curios")} 时用 {@code Class.forName} 触发它加载
 * （与 {@code compat.sable.SableSubLevelBridge}、{@code compat.jade.WaveJadePlugin} 的隔离范式一致）。</p>
 *
 * <p><b>未装 Curios 时</b>：{@link MedallionCurios#get()} 恒为 {@code null}，
 * 找佩恒返回空（{@link ItemStack#EMPTY}）、物品工厂走"纯物品支线"。</p>
 */
public interface MedallionCuriosBridge {

    /**
     * 构造<b>饰品支线</b>的凝能佩（{@code implements ICurioItem} 的那一支）。
     *
     * <p>注册 id 与物品显示名两支完全一致 —— 装了 Curios 的存档读进来还是同一个物品。</p>
     *
     * @param kind       {@link MedallionCurios} 里的 {@code KIND_*} 常量
     * @param properties 注册期传入的物品属性（与纯物品支线逐字相同）
     * @return 对应佩种的饰品支线实例
     */
    BaseStressMedallionItem create(String kind, Item.Properties properties);

    /**
     * 玩家 Curios 槽位中的第一件指定物品（<b>返回槽内的那个 ItemStack 引用本身</b>，
     * 因此调用方对返回值的写入会落到饰品槽里 —— 与改前 {@code CuriosApi.getCuriosInventory(...).findFirstCurio(...)} 一致）。
     *
     * @return 未佩戴时返回 {@link ItemStack#EMPTY}
     */
    ItemStack findEquipped(Player player, Item item);
}
