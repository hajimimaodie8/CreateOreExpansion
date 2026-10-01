package com.hjmmd_8.createoreexpansion.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.integration.skiller.client.CoeSkillClient;
import com.leaf.skiller.client.ClientSkillCache;
import com.leaf.skiller.content.skill.SkillComponent;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.SkillBundle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import org.jetbrains.annotations.Nullable;

/**
 * <b>装备技能提示层</b>（用户 2026-10-01 要求的功能②）：按住装备辅助按键时，在快捷栏上方显示
 * <ul>
 *     <li>当前套装状态（是否成套、生效等级），</li>
 *     <li>当前套装提供了哪些技能、各自绑哪个技能键，</li>
 *     <li>能否释放。</li>
 * </ul>
 *
 * <h2>为什么是"按住才显示"</h2>
 * <p>平时不占屏幕（玩家不需要一直知道套装状态）；按住修饰键本身就是"我要用装备技能"的意图声明，
 * 此时才需要知道"我现在这套能放什么、放不放得出"。</p>
 *
 * <h2>数据来源（都不新增状态）</h2>
 * <ul>
 *     <li>套装状态：{@link ArmorSet#wornSet(Player)} —— 与技能释放<b>同一个判定入口</b>，
 *         所以"提示能放"与"实际能放"不可能不一致。</li>
 *     <li>技能列表：{@link ClientSkillCache#skills}（内核在客户端收集到的合并组件），
 *         只看装备段槽位（见 {@link ArmorSkillProvider}）。</li>
 *     <li>键名：{@link AllKeys}，玩家改键后提示跟着变。</li>
 * </ul>
 *
 * <h2>位置与配色</h2>
 * <p>基线在快捷栏上方 {@link #MARGIN_ABOVE_HOTBAR} 像素处往上堆叠、水平居中 ——
 * 那个高度避开了原版的血条/饥饿条/经验条与"切换物品"的名称弹窗。多行时向上生长
 * （最后一行固定贴住基线），这样行数变化时不会跳动。</p>
 *
 * <h2>本轮边界（技能还没实现）</h2>
 * <p>7 个套装技能尚未落地 ⇒ {@link ArmorSkillProvider} 现在不产出任何技能，
 * 因此第三行会显示"暂无套装技能"。"能否释放"目前等于"是否成套"（前提条件）；
 * <b>能量是否够、是否在冷却</b>要等第一个技能落地后接它的配置 —— 那时在这里加判定，
 * 位置就在 {@link #skillLines} 里。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class EquipmentSkillHud {

    /** 行高（与原版聊天/提示行一致，10 像素足够容纳中文）。 */
    private static final int LINE_HEIGHT = 10;

    /** 基线距屏幕底部的像素数：快捷栏上方、血条/经验条与物品名弹窗之上。 */
    private static final int MARGIN_ABOVE_HOTBAR = 74;

    /** 标题行（含修饰键名）。 */
    private static final String TITLE_KEY = "createoreexpansion.hud.equipment.title";
    /** 成套生效行（套名 + 等级）。 */
    private static final String SET_ACTIVE_KEY = "createoreexpansion.hud.equipment.set_active";
    /** 散构聚能补齐行（套名 + 等级）。 */
    private static final String SET_BY_ENCHANT_KEY = "createoreexpansion.hud.equipment.set_by_enchant";
    /** 穿了本模组护甲但不成套。 */
    private static final String SET_INCOMPLETE_KEY = "createoreexpansion.hud.equipment.set_incomplete";
    /** 一件本模组护甲都没穿。 */
    private static final String SET_NONE_KEY = "createoreexpansion.hud.equipment.set_none";
    /** 成套但没有套装技能（本轮状态）。 */
    private static final String NO_SKILL_KEY = "createoreexpansion.hud.equipment.no_skill";
    /** 单条技能行：技能序号 / 技能名 / 按键。 */
    private static final String SKILL_LINE_KEY = "createoreexpansion.hud.equipment.skill_line";
    /** 能量合计行：当前 / 上限。 */
    private static final String ENERGY_LINE_KEY = "createoreexpansion.hud.equipment.energy";

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_ACTIVE = 0xFF6BE06B;
    private static final int COLOR_INACTIVE = 0xFFFF6B6B;
    private static final int COLOR_SKILL = 0xFFD8D8D8;

    private EquipmentSkillHud() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 注册到快捷栏<b>之上</b>。
     *
     * <p>与 {@code AllKeys} 用同一种 {@code @EventBusSubscriber} 写法（该注解对该模组默认覆盖
     * 两条总线，游戏总线事件与 mod 总线事件都能收到）。</p>
     */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, CoeCore.modLoc("equipment_skill_hint"),
            EquipmentSkillHud::render);
    }

    /** 图层本体：按住装备修饰键时才画。 */
    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        // 与键源同源：能"按下"的时候才提示（CoeSkillClient 的判定）
        if (!CoeSkillClient.isEquipmentModifierDown()) {
            return;
        }

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable(TITLE_KEY, modifierName()), COLOR_TITLE));

        ArmorSet worn = ArmorSet.wornSet(player);
        if (worn != null) {
            lines.add(new Line(Component.translatable(SET_ACTIVE_KEY, wornName(worn), worn.wornLevel(player)),
                COLOR_ACTIVE));
        } else {
            // 严格全套之外还有一条：3 件同套 + 散构聚能补齐（生效等级与全套相同）
            ArmorSet assembled = ArmorSet.effectiveSet(player);
            if (assembled != null) {
                lines.add(new Line(
                    Component.translatable(SET_BY_ENCHANT_KEY, wornName(assembled), assembled.wornLevel(player)),
                    COLOR_ACTIVE));
            } else {
                lines.add(new Line(Component.translatable(
                    ArmorSet.wearsAnyOurArmor(player) ? SET_INCOMPLETE_KEY : SET_NONE_KEY), COLOR_INACTIVE));
            }
        }
        lines.addAll(skillLines());
        // 能量行（2026-10-01）：技能扣能是"四件平摊"，所以玩家真正要知道的是合计
        int totalMax = ArmorEnergy.totalMax(player);
        if (totalMax > 0) {
            lines.add(new Line(Component.translatable(ENERGY_LINE_KEY,
                ArmorEnergy.totalEnergy(player), totalMax), COLOR_SKILL));
        }

        drawCentered(graphics, minecraft, lines);
    }

    /**
     * 装备段每个槽位一行：技能序号 + 技能名 + 按键。
     *
     * <p>槽位号来自内核在客户端收集到的组件（{@link ClientSkillCache#skills}）；
     * 名字走 {@link ItemSkill#getTranslateKey()}（与技能 tooltip 同一条语言键）。</p>
     */
    private static List<Line> skillLines() {
        List<Line> lines = new ArrayList<>();
        SkillComponent component = ClientSkillCache.skills;
        Map<Integer, SkillBundle> bindings = component == null ? Map.of() : component.bindings();
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            SkillBundle bundle = bindings.get(ArmorSkillProvider.SLOT_BASE + index);
            if (bundle == null) {
                continue;
            }
            Component name = skillName(bundle);
            if (name == null) {
                continue;
            }
            AllKeys key = CoeSkillClient.skillKeyForEquipmentSlot(ArmorSkillProvider.SLOT_BASE + index);
            Component keyName = key == null ? Component.empty() : key.getKeybind().getTranslatedKeyMessage();
            lines.add(new Line(Component.translatable(SKILL_LINE_KEY, index + 1, name, keyName), COLOR_SKILL));
        }
        if (lines.isEmpty()) {
            lines.add(new Line(Component.translatable(NO_SKILL_KEY), COLOR_INACTIVE));
        }
        return lines;
    }

    /**
     * 技能包里的显示名。
     *
     * <p>取名字有两条口径，这里必须走<b>第二条</b>：</p>
     * <ol>
     *     <li>旧框架的 {@code ItemSkill#getTranslateKey()} —— 只在"技能实例仍归旧框架"时才有；
     *         内核的 {@code ItemSkillRegistration} <b>不是</b>那个接口，对它做 {@code instanceof}
     *         恒为 false（2026-10-01 实测：已经有绑定了却一行技能都不显示，就是踩了这条）；</li>
     *     <li><b>技能 id → 语言键</b>：本模组技能语言键的格式恒为
     *         {@code skill.<namespace>.<path>}（12 条键见两个 LangProvider），
     *         而内核 {@code ItemSkillRegistration#getId()} 给的就是那个 id。</li>
     * </ol>
     */
    private static @Nullable Component skillName(SkillBundle bundle) {
        for (ISkillInstance<?> instance : bundle.getAllData()) {
            if (instance == null || instance.skill() == null) {
                continue;
            }
            ResourceLocation id = instance.skill().getId();
            if (id != null) {
                return Component.translatable("skill." + id.getNamespace() + "." + id.getPath());
            }
        }
        return null;
    }

    /** 修饰键的显示名（玩家改键后跟着变）。 */
    private static Component modifierName() {
        return AllKeys.EQUIPMENT_MODIFIER.getKeybind() == null
            ? Component.empty()
            : AllKeys.EQUIPMENT_MODIFIER.getKeybind().getTranslatedKeyMessage();
    }

    /** 套名（语言键与本模组护甲物品名同源）。 */
    private static Component wornName(ArmorSet worn) {
        return Component.translatable("createoreexpansion.armor_set." + worn.setName());
    }

    /** 居中绘制：最后一行贴住基线，多出来的行向上生长。 */
    private static void drawCentered(GuiGraphics graphics, Minecraft minecraft, List<Line> lines) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int y = minecraft.getWindow().getGuiScaledHeight() - MARGIN_ABOVE_HOTBAR
            - (lines.size() - 1) * LINE_HEIGHT;
        for (Line line : lines) {
            int x = (width - minecraft.font.width(line.text())) / 2;
            graphics.drawString(minecraft.font, line.text(), x, y, line.color(), true);
            y += LINE_HEIGHT;
        }
    }

    /** 一行提示（文本 + 颜色）。 */
    private record Line(Component text, int color) {
    }
}
