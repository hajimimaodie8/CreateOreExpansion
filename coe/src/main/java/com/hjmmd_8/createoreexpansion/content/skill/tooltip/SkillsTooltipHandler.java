package com.hjmmd_8.createoreexpansion.content.skill.tooltip;

import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillEnergySpend;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public class SkillsTooltipHandler {

    private static final String SKILL_TIP_TRANSLATE_KEY = "item.createoreexpansion.tool.skill_tips";

    /**
     * 技能区展示在 tooltip 顶部（能量条之前）：从 index 1 开始插入。
     * 提示行「按住 [Alt] 可查看技能概要」始终保留；按 Alt 时在其下方展示技能列表。
     *
     * @return 插入完成后的下一个可用 index（供能量条继续插入，保证能量条紧跟技能区）
     */
    public static int addSkillsTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        SkillItemStack skillStack = SkillItemStack.of(stack);

        int index = 1;
        if (!skillStack.hasSkill())
            return index;

        // 提示行「按住 [Alt] 可查看技能概要」—— [%s] 方括号保留；按住 Alt 时 Alt 键帽白色高亮
        event.getToolTip().add(index++, Component.translatable(SKILL_TIP_TRANSLATE_KEY, altKeyComponent())
                .withStyle(ChatFormatting.GRAY));

        if (Screen.hasAltDown()) {
            // 攻击类（HIT_SKILL）与使用类（USE_SKILL）技能均按槽位显示对应按键（键一/键二/键三）
            List<DataSkill> hitSkills = skillStack.getSkillsHolder().getDataSkills(SkillType.HIT_SKILL);
            for (int slot = 0; slot < hitSkills.size(); slot++) {
                DataSkill data = hitSkills.get(slot);
                event.getToolTip().add(index++, skillLine(keyComponent(slot), stack, data));
            }
            List<DataSkill> useSkills = skillStack.getSkillsHolder().getDataSkills(SkillType.USE_SKILL);
            for (int slot = 0; slot < useSkills.size(); slot++) {
                DataSkill data = useSkills.get(slot);
                event.getToolTip().add(index++, skillLine(keyComponent(slot), stack, data));
            }
            // 其余类型（如挖掘类）统一键一
            List<DataSkill> all = skillStack.getSkillsHolder().getAllData();
            for (DataSkill data : all) {
                if (data.skill.getType() == SkillType.HIT_SKILL) continue;
                if (data.skill.getType() == SkillType.USE_SKILL) continue;
                event.getToolTip().add(index++, skillLine(keyComponent(0), stack, data));
            }
        }
        return index;
    }

    /**
     * 技能行：`  [按键] 技能名 罗马数字 - 类型`。
     * 整行颜色按技能等级区分：Lv1 白 / Lv2 绿 / Lv3 蓝 / Lv4 紫 / Lv5 金。
     * 等级取有效等级（含技能提升附魔）。
     */
    private static Component skillLine(Component key, ItemStack stack, DataSkill data) {
        int level = SkillEnergySpend.effectiveLevel(stack, data);
        return Component.literal("  [")
                .append(key)
                .append("] ")
                .append(Component.translatable(data.skill.getTranslateKey()))
                .append(" " + toRoman(level))
                .append(" - ")
                .append(Component.translatable(data.skill.getType().translatable.getTranslateKey()))
                .withStyle(style -> style.withColor(TextColor.fromRgb(levelColor(level))));
    }

    /**
     * 技能等级对应的整行颜色（ARGB）：Lv1 白 / Lv2 绿 / Lv3 蓝 / Lv4 紫 / Lv5 橙（超出用橙色）。
     *
     * <p><b>这是"等级 → 颜色"的唯一出处</b>（用户 2026-10-01：工具已经这么写了，装备也要）。
     * 装备 tooltip（{@code ArmorSkillTooltipHandler}）与装备 HUD（{@code EquipmentSkillHud}）
     * 都调它 ⇒ 三处永远一致，不会各写一套色。</p>
     */
    public static int levelColor(int level) {
        return switch (level) {
            case 1 -> 0xFFE8E8E8; // Lv1 白色
            case 2 -> 0xFF49B85C; // Lv2 绿色
            case 3 -> 0xFF3D88D8; // Lv3 蓝色
            case 4 -> 0xFF994FC2; // Lv4 紫色
            default -> 0xFFE68A27; // Lv5 橙色（及超出）
        };
    }

    /**
     * 技能行（<b>给装备 tooltip 复用</b>）：{@code   [按键] 技能名 罗马数字}，整行颜色按等级。
     *
     * <p>装备技能的等级<b>逐技能</b>求值（每条技能行自己的基准 + 技艺提升 − 记忆回溯，见
     * {@code ArmorSkillRuntime#levelOf}），所以等级由调用方传入，不在这里算。</p>
     */
    public static Component skillLine(Component key, Component name, int level) {
        return Component.literal("  [")
                .append(key)
                .append("] ")
                .append(name)
                .append(" " + toRoman(level))
                .withStyle(style -> style.withColor(TextColor.fromRgb(levelColor(level))));
    }

    /** Alt 键帽组件：显示「Alt」（去掉左/右修饰），按住 Alt 时白色高亮，否则灰色（跟随外层样式） */
    private static Component altKeyComponent() {
        Component alt = Component.literal("Alt");
        return Screen.hasAltDown() ? alt.copy().withStyle(ChatFormatting.WHITE) : alt;
    }

    /**
     * 获取槽位对应的技能键帽组件（已翻译的按键名，如 R / G / 左 Shift）。
     * 用 {@code mapping.getTranslatedKeyMessage()} —— 返回翻译后的按键名组件，
     * 不会出现未翻译的原始键值；按住 Alt 查看时按键名白色高亮。
     *
     * @param slot 技能槽位（0=键一、1=键二、2=键三；超出用键一兜底）
     */
    private static Component keyComponent(int slot) {
        AllKeys key = switch (slot) {
            case 1 -> AllKeys.SKILL_RELEASE_2;
            case 2 -> AllKeys.SKILL_RELEASE_3;
            default -> AllKeys.SKILL_RELEASE;
        };
        KeyMapping mapping = key.getKeybind();
        Component comp = mapping != null
                ? mapping.getTranslatedKeyMessage()
                : Component.literal(key.getBoundKey());
        return Screen.hasAltDown() ? comp.copy().withStyle(ChatFormatting.WHITE) : comp;
    }

    /**
     * 将技能等级转换为罗马数字（1=I, 2=II, 3=III, 4=IV, 5=V ...）。
     * 超出常规范围时用重复的 I 兜底。
     */
    private static String toRoman(int level) {
        if (level <= 0) return "I";
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> "I".repeat(level);
        };
    }
}
