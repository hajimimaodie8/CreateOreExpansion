package com.hjmmd_8.createoreexpansion.integration.skiller.resource;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;
import com.leaf.skiller.foundation.SkillResource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * <b>装备（护甲）能量资源</b>——把四套护甲的储能池接成 Skiller 的 {@link SkillResource}。
 *
 * <h2>口径（唯一取值点 = {@link ArmorEnergy}）</h2>
 * <ul>
 *   <li>{@code amountOf} = 穿戴中四件的能量<b>合计</b>（技能的扣能口径就是四件平摊）；</li>
 *   <li>{@code canPay} = {@link ArmorEnergy#canAfford}（合计够不够 = 全有或全无）；</li>
 *   <li>{@code pay} = {@link ArmorEnergy#consume}（四件平摊、余数按 头→胸→腿→脚）。</li>
 * </ul>
 *
 * <p><b>注意</b>：装备技能的<b>实际执行</b>在 {@code ArmorSkillRuntime}（长按语义，内核没有这个模型），
 * 内核的释放路径会跳过装备段槽位（见 {@code CoeSkillRelease}）。本资源因此主要服务于
 * "内核知道这个技能花什么"，扣费口径与运行时<b>同源</b>，不会两套账。</p>
 *
 * <p>骨架（ID/KEY 样板、null 判断、扣款失败的日志与物品栏同步）由
 * {@link AbstractEnergySkillResource} 承担。</p>
 *
 * @since 1.0.0
 */
public class CoeArmorEnergyResource extends AbstractEnergySkillResource {

    /** 注册路径（完整 id = {@code createoreexpansion:armor_energy}）。 */
    public static final String PATH = "armor_energy";

    /** 完整资源 id（实现见基类，保留常量以免改动既有引用点）。 */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(com.hjmmd_8.createoreexpansion.common.CoeCore.REGISTRY_NAMESPACE, PATH);

    public CoeArmorEnergyResource() {
        super(PATH);
    }

    @Override
    protected int amountOf(Player player) {
        return ArmorEnergy.totalEnergy(player);
    }

    @Override
    protected boolean canPay(Player player, int amount) {
        return ArmorEnergy.canAfford(player, amount);
    }

    @Override
    protected boolean pay(Player player, int amount) {
        return ArmorEnergy.consume(player, amount);
    }
}
