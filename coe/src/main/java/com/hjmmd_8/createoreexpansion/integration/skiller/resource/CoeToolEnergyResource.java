package com.hjmmd_8.createoreexpansion.integration.skiller.resource;

import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.leaf.skiller.foundation.SkillResource;

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
 *
 * <p>骨架（ID/KEY 样板、null 判断、扣款失败的日志与物品栏同步）由
 * {@link AbstractEnergySkillResource} 承担 —— 本类只回答"多少 / 够不够 / 怎么扣"。</p>
 *
 * @since 1.0.0
 */
public class CoeToolEnergyResource extends AbstractEnergySkillResource {

    /** 注册路径（完整 id = {@code createoreexpansion:tool_energy}）。 */
    public static final String PATH = "tool_energy";

    /** 完整资源 id（注册与实例 NBT 都用它；实现见基类，这里保留常量以免改动既有引用点）。 */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(com.hjmmd_8.createoreexpansion.common.CoeCore.REGISTRY_NAMESPACE, PATH);

    public CoeToolEnergyResource() {
        super(PATH);
    }

    @Override
    protected int amountOf(Player player) {
        return ToolEnergy.getAvailable(player, player.getMainHandItem());
    }

    @Override
    protected boolean canPay(Player player, int amount) {
        return ToolEnergy.canAfford(player, player.getMainHandItem(), amount);
    }

    @Override
    protected boolean pay(Player player, int amount) {
        return ToolEnergy.consume(player, player.getMainHandItem(), amount);
    }

    /** 工具能源的失败日志多带"是哪件主手物品"，便于定位（基类只打 path 与数量）。 */
    @Override
    protected void logPayFailure(Player player, int amount) {
        ItemStack stack = player.getMainHandItem();
        com.hjmmd_8.createoreexpansion.common.CoeCore.LOGGER.warn(
            "[Skiller] 工具能量扣减失败：player={}, amount={}（主手={}）",
            player.getName().getString(), amount, stack.getHoverName().getString());
    }

    /**
     * 扣款成功后的**附加**反馈：剩余能量读数（护目镜限定；有凝能佩时佩行在上、工具行在下）。
     *
     * <p>覆写的是基类的<b>钩子</b>而不是 {@code consume} 骨架 —— 骨架已经做了
     * "扣成功 ⇒ {@code setChanged()}"，这里只补工具侧特有的读数播报。</p>
     */
    @Override
    protected void afterPaid(Player player, int amount) {
        ItemStack stack = player.getMainHandItem();
        ToolEnergy.sendRemainingEnergyWithMedallion(player, stack,
            IMedallion.findBoundMedallion(player, stack));
    }
}
