package com.hjmmd_8.createoreexpansion.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.skill.config.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.FallGuardConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillRuntime;
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
    /** 长按实时预览行（已按住多少秒 / 该级上限多少秒 → 预计扣多少 / 满额多少）。 */
    private static final String HOLD_PREVIEW_KEY = "createoreexpansion.hud.equipment.hold_preview";
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
            // 等级 = **整体技能等级**（逐件算完取最大，见 ArmorSkillRuntime#effectiveLevel），
            // 用罗马数字显示（用户 2026-10-01："技能等级为 1（注意，1 是罗马数字 I）"）
            lines.add(new Line(Component.translatable(SET_ACTIVE_KEY, wornName(worn),
                roman(ArmorSkillRuntime.effectiveLevel(player, worn))), COLOR_ACTIVE));
        } else {
            // 严格全套之外还有一条：3 件同套 + 散构聚能补齐（生效等级与全套相同）
            ArmorSet assembled = ArmorSet.effectiveSet(player);
            if (assembled != null) {
                lines.add(new Line(Component.translatable(SET_BY_ENCHANT_KEY, wornName(assembled),
                    roman(ArmorSkillRuntime.effectiveLevel(player, assembled))), COLOR_ACTIVE));
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
        lines.addAll(holdPreviewLines(player));

        drawCentered(graphics, minecraft, lines);
    }

    /**
     * <b>长按实时预览</b>（用户 2026-10-01："持续按住时并没有动态显示能量的掉落"）。
     *
     * <p>结算时机是用户定的"<b>松手时一次性按比例扣</b>"，所以按住期间服务端不会持续扣能；
     * 玩家想看的"按住越久要花越多"是<b>预览</b> —— 这里用与服务端<b>同一个</b>
     * {@link ArmorSkillRuntime#holdCost} 公式实时算，不会两套账。</p>
     */
    private static List<Line> holdPreviewLines(Player player) {
        List<Line> lines = new ArrayList<>();
        ArmorSet active = ArmorSet.effectiveSet(player);
        if (active == null) {
            return lines;
        }
        int level = ArmorSkillRuntime.effectiveLevel(player, active);
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            int slot = ArmorSkillProvider.SLOT_BASE + index;
            int held = CoeSkillClient.holdTicksOf(slot);
            if (held <= 0) {
                continue;
            }
            // 每个槽位有自己的数值源：槽位 1 = 虚衡坠护、槽位 2 = 蓄能疾骋（别拿一套配置套所有槽位）
            int holdSeconds;
            int holdTotalCost;
            if (index == 1) {
                ChargeDashConfigs.Config config = ChargeDashConfigs.config(level);
                holdSeconds = config.holdSeconds();
                holdTotalCost = config.holdTotalCost();
            } else {
                FallGuardConfigs.Config config = FallGuardConfigs.config(level);
                holdSeconds = config.holdSeconds();
                holdTotalCost = config.holdTotalCost();
            }
            int cost = ArmorSkillRuntime.holdCost(held, holdSeconds, holdTotalCost);
            lines.add(new Line(Component.translatable(HOLD_PREVIEW_KEY,
                oneDecimal(held / 20.0F),
                oneDecimal(holdSeconds),
                cost, holdTotalCost), COLOR_SKILL));
        }
        return lines;
    }

    /**
     * 一位小数（<b>参数类型必须是 {@code float}</b>）。
     *
     * <p>2026-10-01 实测崩溃（{@code IllegalFormatConversionException: f != java.lang.Integer}）：
     * 直接写 {@code String.format("%.1f", 某个int)} 会在**运行期**炸 —— 编译期完全看不出来。
     * 把参数定成 {@code float} 后，传 int 会**自动加宽**，这类错误从此写不出来。</p>
     *
     * <p>用 {@link Locale#ROOT}：否则某些区域设置会把小数点写成逗号（预览行会变得很奇怪）。</p>
     */
    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
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

    /**
     * 等级 → 罗马数字（用户 2026-10-01："技能等级为 1（注意，1 是罗马数字 I）"）。
     *
     * <p>装备技能 3 级封顶 ⇒ 只需 I/II/III；越界值回退成阿拉伯数字（不会崩、也不会显示空）。</p>
     */
    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> Integer.toString(level);
        };
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
