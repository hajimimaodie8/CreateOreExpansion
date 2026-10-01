package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorEnergy;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.SkillResource;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * <b>装备（护甲）能量资源</b>（新内核版）——把四套护甲的储能池接成 Skiller 的
 * {@link SkillResource}（照 {@link CoeToolEnergyResource} 的写法）。
 *
 * <h2>为什么必须有它（不是可选的装饰）</h2>
 * <p>装备技能实例的 NBT 里要写 {@code "resource"}（内核据此知道"这个技能消耗什么"）。
 * 若沿用工具能源：语义错（护甲技能花的是护甲的池子，不是主手的），
 * 一旦以后有人给装备技能加渲染器/门槛，读数会静默取错来源。</p>
 *
 * <h2>口径（唯一取值点 = {@link ArmorEnergy}）</h2>
 * <ul>
 *   <li>{@code getAmount} = 穿戴中四件的能量<b>合计</b>（技能的扣能口径就是四件平摊）；</li>
 *   <li>{@code canConsume} = {@link ArmorEnergy#canAfford}（合计够不够 = 全有或全无）；</li>
 *   <li>{@code consume} = {@link ArmorEnergy#consume}（四件平摊、余数按 头→胸→腿→脚）。</li>
 * </ul>
 *
 * <p><b>注意</b>：装备技能的<b>实际执行</b>在 {@code ArmorSkillRuntime}（长按语义，内核没有这个模型），
 * 内核的释放路径会跳过装备段槽位（见 {@code CoeSkillRelease}）。本资源因此主要服务于
 * "内核知道这个技能花什么"以及未来可能的读数/门槛，扣费口径与运行时<b>同源</b>，不会两套账。</p>
 *
 * @since 1.0.0
 */
public class CoeArmorEnergyResource implements SkillResource {

    /** 注册路径（完整 id = {@code createoreexpansion:armor_energy}） */
    public static final String PATH = "armor_energy";

    /** 完整资源 id */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, PATH);

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
        return ArmorEnergy.totalEnergy(player);
    }

    @Override
    public boolean canConsume(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        return player != null && ArmorEnergy.canAfford(player, amount);
    }

    /** 与工具能源同规矩：失败状态由 {@link #canConsume} 前置把关，这里只做扣减与反馈。 */
    @Override
    public void consume(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        if (!ArmorEnergy.consume(player, amount)) {
            CoeCore.LOGGER.warn("[Skiller] 护甲能量扣减失败：player={}, amount={}",
                    player.getName().getString(), amount);
            return;
        }
        // 强制物品栏同步：客户端立刻看到四件护甲的能量变化（工具能源那条注释同理）
        player.getInventory().setChanged();
    }
}
