package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.leaf.skiller.content.skill.SkillComponent;
import com.leaf.skiller.foundation.provider.SkillProviders;
import com.leaf.skiller.foundation.skill.SkillBundle;
import com.leaf.skiller.foundation.skill.SkillType;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import com.leaf.skiller.server.PlayerPressedKeys;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.Map;

/**
 * 服务端技能释放路由：把「当前按下的技能键 + 本次触发场景」交给新内核执行。
 *
 * <h2>为什么不直接用 Skiller 的 {@code SkillReleaser}</h2>
 * <p>{@code SkillReleaser} 从 {@code ServerSkillCache} 取技能组件，而后者<b>只在该玩家
 * 按过一次"启用技能"开关后</b>才有内容（{@code SkillTogglePacket} → {@code onToggle}）。
 * 本模组既有玩法是<b>随时可用</b>（没有总开关），因此这里改为每次触发时即时收集
 * （{@link CoeSkillProvider} 只读主手物品的一个数据组件，开销可忽略）。换来的好处是：
 * 玩家换工具、技能变化都立刻生效，不存在"缓存过期"这一类问题。</p>
 *
 * <h2>按键来源</h2>
 * <p>按键状态取自 {@link PlayerPressedKeys}——它由客户端的按键包写入，是
 * <b>服务端权威</b>的。这正好修掉本模组的一个既有缺陷：旧 {@code AllKeys} 是纯客户端
 * 对象，专用服务器上 {@code isPressed()} 恒为 false，技能键永远不触发。</p>
 *
 * @since 1.0.0
 */
public final class CoeSkillRelease {

    private CoeSkillRelease() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 释放该玩家当前按下的按键槽位里、指定类型的所有已迁移技能。
     *
     * @param player 触发者（服务端玩家）
     * @param type   本次触发的技能类型（挖掘 / 受击 / 使用）
     * @param env    上下文环境（事件或 extraData）
     * @return 是否有任一槽位成功释放；没有任何按下/绑定/已迁移技能时返回 false
     */
    public static boolean release(ServerPlayer player, SkillType type, SkillContextEnvironment env) {
        if (player == null || type == null || env == null) {
            return false;
        }
        SkillComponent component = SkillProviders.collectAllSkills(player);
        boolean released = false;
        for (Map.Entry<Integer, SkillBundle> binding : component.bindings().entrySet()) {
            Integer slot = binding.getKey();
            if (slot == null || !PlayerPressedKeys.isPressed(player, slot)) {
                continue;
            }
            if (binding.getValue().releaseSkills(type, env)) {
                released = true;
            }
        }
        return released;
    }

    /**
     * 便捷重载：直接把一个 NeoForge 事件包成环境。
     *
     * <p>注意事件类型要与技能自己的 {@code SkillContextFactory} 对得上；对不上时
     * 工厂会回落到 extraData / 尽力构造（见各工厂的 {@code createDefault}）。</p>
     */
    public static boolean release(ServerPlayer player, SkillType type, Event triggerEvent) {
        if (player == null || triggerEvent == null) {
            return false;
        }
        return release(player, type, SkillContextEnvironment.withEvent(player, player.level(), triggerEvent));
    }
}
