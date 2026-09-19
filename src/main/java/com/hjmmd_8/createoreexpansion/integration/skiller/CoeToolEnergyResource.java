package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.SkillResource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 工具能量资源（新内核版）——把模组既有的「主手工具 FE + 凝能佩兜底」接成 Skiller 的 {@link SkillResource}。
 *
 * <p>口径：</p>
 * <ul>
 *     <li><b>一切走主手物品</b>：旧系统所有触发点（挖掘/攻击/使用/弓）都是主手工具，这里保持一致；</li>
 *     <li><b>凝能佩兜底</b>：数值/扣减全部委托 {@link ToolEnergy} 新抽出的三个静态方法
 *         （{@code getAvailable} / {@code canAfford} / {@code consume}），语义与旧
 *         {@code ToolEnergy.tryConsume(Player, ItemStack, ItemSkill)} 一致；</li>
 *     <li>不使用 {@code skiller:empty}：能量是本模组技能的真实成本，必须由内核的资源门槛统一校验。</li>
 * </ul>
 *
 * <p>注册键为 {@code createoreexpansion:tool_energy}，注册进 {@code skiller:skill_resource}；
 * 技能实例的 NBT 里用键 {@code "resource"} 写入 {@link #ID} 的字符串形式，
 * Skiller 的 {@code NbtSkillInstanceFactory.createFromData} 据此把实例恢复成消耗本资源。</p>
 */
public class CoeToolEnergyResource implements SkillResource {

    /** 注册路径（完整 id = {@code createoreexpansion:tool_energy}） */
    public static final String PATH = "tool_energy";

    /** 完整资源 id */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, PATH);

    /** 该资源在 {@code skiller:skill_resource} 注册表中的键 */
    public static final ResourceKey<SkillResource> KEY =
            ResourceKey.create(SkillerRegistries.SKILL_RESOURCE, ID);

    @Override
    public ResourceKey<SkillResource> key() {
        return KEY;
    }

    @Override
    public int getAmount(Player player) {
        if (player == null) {
            return 0;
        }
        return ToolEnergy.getAvailable(player, player.getMainHandItem());
    }

    @Override
    public boolean canConsume(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        return player != null && ToolEnergy.canAfford(player, player.getMainHandItem(), amount);
    }

    /** 注意：Skiller 的 {@link SkillResource#consume(Player, int)} 返回 void，失败状态由 {@link #canConsume} 前置把关。 */
    @Override
    public void consume(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ToolEnergy.consume(player, stack, amount)) {
            // 理论上不该发生（canConsume 已通过）：留一条日志，避免"扣费静默失败"难以定位
            CreateOreExpansion.LOGGER.warn("[Skiller] 工具能量扣减失败：player={}, amount={}（主手={}）",
                    player.getName().getString(), amount, stack.getHoverName().getString());
            return;
        }
        // 下面两步是旧 ToolEnergy.tryConsume 扣完后一定会做的，属于"玩家能看见的反馈"，
        // 漏掉它们的症状就是：能量条不刷新、护目镜的"剩余能量"读数整片消失。
        // 1) 强制物品栏同步，客户端立刻看到能量变化
        player.getInventory().setChanged();
        // 2) 剩余能量读数（护目镜限定；有凝能佩时佩行在上、工具行在下）
        ToolEnergy.sendRemainingEnergyWithMedallion(player, stack, IMedallion.findBoundMedallion(player, stack));
    }

}
