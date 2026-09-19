package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.context.RightClickItemContext;
import com.hjmmd_8.createoreexpansion.content.skill.context.UseOnBlockContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

@EventBusSubscriber
public class UseItemHandler {
    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent event) {
        if (event.getPlayer() == null) return;
        // UseItemOnBlockEvent 会按交互阶段触发多次（ITEM_BEFORE_BLOCK/BLOCK/ITEM_AFTER_BLOCK），
        // 技能只应在第一次（物品交互前）释放一次，否则一次右键会重复扣费。
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK) return;
        releaseSkills(event.getPlayer(), event.getItemStack(), new UseOnBlockContext(event), event);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        releaseSkills(event.getEntity(), event.getItemStack(), new RightClickItemContext(event), event);
    }

    private static void releaseSkills(Player player, ItemStack stack, UseItemContext<?> context,
                                      net.neoforged.bus.api.Event triggerEvent) {
        if (player.level().isClientSide()) return;
        // 弓类武器技能由弓自身在松手射击（releaseUsing）时释放：右键拉弓瞬间不触发，
        // 否则会在蓄力开始时就消耗能量/进入冷却（USE_SKILL 触发时机不匹配）。
        if (stack.getItem() instanceof com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem)
            return;
        SkillItemStack skillStack = SkillItemStack.of(stack);
        SkillsComponent holder = skillStack.getSkillsHolder();
        // 非技能物品（无 SKILLS 组件）直接忽略，避免 NPE 干扰原版交互（如放置方块）
        if (holder == null || holder.getDataSkills(SkillType.USE_SKILL).isEmpty()) return;

        // 新内核（Skiller）路径：已经迁移过去的右键技能由它执行。
        // 放在旧按键判定之前 —— 旧 AllKeys 是纯客户端对象、专用服务器上恒为"未按下"，
        // 新路径读的是服务端权威按键状态。两条路径不会重复：迁移闸门让旧路径跳过已迁移的技能。
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            CoeSkillRelease.release(serverPlayer, CoeSkillTypes.USE,
                    SkillContextEnvironment.withEvent(serverPlayer, serverPlayer.level(), triggerEvent));
        }

        // 按技能键选槽位释放（与剑类/挖掘类多技能机制一致）：
        // 键一(Shift)→槽位0、键二(R)→槽位1、键三(G)→槽位2；未按技能键不触发
        boolean released = false;
        if (AllKeys.SKILL_RELEASE.isPressed()) {
            released |= holder.releaseSkillAt(skillStack, SkillType.USE_SKILL, 0, context);
        }
        if (AllKeys.SKILL_RELEASE_2.isPressed()) {
            released |= holder.releaseSkillAt(skillStack, SkillType.USE_SKILL, 1, context);
        }
        if (AllKeys.SKILL_RELEASE_3.isPressed()) {
            released |= holder.releaseSkillAt(skillStack, SkillType.USE_SKILL, 2, context);
        }

        // 剩余能量显示统一由 ToolEnergy.tryConsume（消耗时）处理：工具行 + 绑定凝能佩行
    }
}
