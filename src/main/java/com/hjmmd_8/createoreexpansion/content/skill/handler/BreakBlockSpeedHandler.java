package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllModifiableAttributes;
import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifierHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * 挖掘速度修饰入口 —— 只对当前按技能键选中的槽位技能应用属性修饰器。
 *
 * 多技能工具（如蓝宝石铲绑引渠+平场）只对当前释放的技能生效减速，
 * 避免叠加多个技能的修饰器。
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class BreakBlockSpeedHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        SkillItemStack skillStack = SkillItemStack.of(held);
        SkillsComponent holder = skillStack.getSkillsHolder();
        // 非技能物品（无 SKILLS 组件）直接忽略，避免 NPE
        if (holder == null) return;
        List<DataSkill> skills = holder.getDataSkills(SkillType.EXCAVATION_SKILL);
        if (skills.isEmpty()) return;

        // 确定当前按下的技能键对应的槽位（键一→0、键二→1、键三→2）
        int slot = -1;
        if (AllKeys.SKILL_RELEASE.isPressed()) slot = 0;
        else if (AllKeys.SKILL_RELEASE_2.isPressed()) slot = 1;
        else if (AllKeys.SKILL_RELEASE_3.isPressed()) slot = 2;
        if (slot < 0 || slot >= skills.size()) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;

        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) return;

        Level level = player.level();
        ModifiableAttribute<Float> attribute = AllModifiableAttributes.BREAK_BLOCK_SPEED.create(
                BreakBlockSpeedModifiableAttribute.ctx(level, pos, event.getOriginalSpeed()));

        DataSkill data = skills.get(slot);
        if (data.skill instanceof SkillAttributeModifierHolder holder2) {
            holder2.modifier(AllModifiableAttributes.BREAK_BLOCK_SPEED, attribute);
            event.setNewSpeed(attribute.getValue());
        }
    }
}
