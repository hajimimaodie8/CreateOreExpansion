package com.hjmmd_8.createoreexpansion.content.equipment.armor.energy;

import java.awt.Color;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;

/**
 * <b>护甲能量条的配色表</b>（用户 2026-10-01 逐套指定）。
 *
 * <p>色标顺序 = <b>从左到右</b>；两段就是普通渐变，四段就是分段渐变
 * （渲染由 {@code BarTooltipRender#energyGradient(int, int, int, List)} 负责）。</p>
 *
 * <h2>用户给的配色（原话 → 本表）</h2>
 * <ol>
 *   <li><b>翠玉套</b>：与<b>翠玉之弓</b>同款（上一轮已定）⇒ 绿 {@code 0x55FF55} → 黄 {@code 0xFFFF55}。
 *       这两个值**不是**新定的：它们就是 {@code EnergyTooltipHandler} 里弓那条分支的字面量
 *       （见 {@link #JADE_STOPS} 的注释）。</li>
 *   <li><b>宝石套</b>："从左到右由蓝渐变到红" ⇒ 蓝 {@code 0x55AAFF}（= 本模组蓝宝石蓝
 *       {@link ToolEnergyColorConfig#SAPPHIRE} 的亮色，保持与工具同一套色感）→ 红 {@code 0xFF4A4A}。</li>
 *   <li><b>星界套</b>："从偏绿的黄过渡到绿，再过渡到蓝，最后过渡到粉紫" ⇒ 四段：
 *       偏绿的黄 {@code 0xD8E04A} → 绿 {@code 0x55FF55} → 蓝 {@code 0x55AAFF} →
 *       粉紫 {@code 0xDF98A7}（=<b>星辉石</b>的亮色 {@link ToolEnergyColorConfig#STELLARSTONE}，
 *       与该套的材料同源）。</li>
 *   <li><b>雷鸣套</b>："配色和雷鸣合金能量条的配色一样" ⇒ <b>直接读</b>
 *       {@link ToolEnergyColorConfig#THUNDERITE} 的 {@code light} → {@code dark}
 *       （{@code 0xB514FF} → {@code 0x690C94}）。<b>不复制数值</b>：以后改雷鸣合金的配色，
 *       护甲跟着变（单一数据源）。</li>
 * </ol>
 *
 * <p><b>不属于四套的护甲</b>（原版/他模组护甲 + 散构聚能 ⇒ 也能储能 2500）没有"套"可言，
 * 回落到工具默认色 {@link ToolEnergyColorConfig#DEFAULT}。</p>
 *
 * <h2>这张表不是护甲专用的（作者 2026-10-03 小修）</h2>
 * <p>四把<b>回旋镖</b>的能量条也读这里：作者原话「<i>能量条的样式和颜色应该与那个装备一样，
 * 而不是你自己造出一个新的渐变</i>」⇒ 档→套一一对应（翠玉/宝石/星界/雷鸣），
 * 由 {@code BoomerangItem#energyGradientStops} <b>一行调用</b>
 * {@link #stopsOf(ArmorSet)} 取同一份色标 —— <b>回旋镖侧一个色值都不复写</b>，
 * 因此连色标条数（星界四段）都跟着护甲走。</p>
 *
 * @since 1.0.0
 */
public final class ArmorEnergyColors {

    /**
     * 翠玉套：与翠玉之弓那条<b>逐字同值</b>（{@code EnergyTooltipHandler} 里的
     * {@code new Color(0x55FF55), new Color(0xFFFF55)}）。
     */
    private static final List<Color> JADE_STOPS = List.of(new Color(0x55FF55), new Color(0xFFFF55));

    /** 宝石套：蓝 → 红（用户原话"由蓝渐变到红"）。 */
    private static final List<Color> GEM_STOPS = List.of(new Color(0x55AAFF), new Color(0xFF4A4A));

    /** 星界套：偏绿的黄 → 绿 → 蓝 → 粉紫（用户原话，四段）。 */
    private static final List<Color> ASTRAL_STOPS = List.of(
        new Color(0xD8E04A), new Color(0x55FF55), new Color(0x55AAFF), new Color(0xDF98A7));

    /** 非四套护甲（原版/他模组 + 散构聚能）：回落工具默认色。 */
    private static final List<Color> DEFAULT_STOPS =
        List.of(ToolEnergyColorConfig.DEFAULT.light, ToolEnergyColorConfig.DEFAULT.dark);

    private ArmorEnergyColors() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 该护甲的能量条色标（左 → 右）。
     *
     * @param stack 任意可储能护甲；不是护甲或不是四套之一时回落默认色
     */
    public static List<Color> stopsOf(ItemStack stack) {
        return stopsOf(ArmorSet.of(stack));
    }

    /**
     * 该套的能量条色标（左 → 右）。
     *
     * <p>⚠ <b>第二个消费方是回旋镖</b>（2026-10-03 小修）：四把镖按同名的套取用同一份色标，
     * 所以本方法的返回表就是"回旋镖 ↔ 护甲逐格一致"的保证书。</p>
     *
     * @param set 四套之一；{@code null} ⇒ 默认色
     */
    public static List<Color> stopsOf(@Nullable ArmorSet set) {
        if (set == null) {
            return DEFAULT_STOPS;
        }
        return switch (set) {
            case JADE_TOPAZ -> JADE_STOPS;
            case SAPPHIRE_RUBY -> GEM_STOPS;
            case ASTRAL -> ASTRAL_STOPS;
            // 雷鸣：与雷鸣合金能量条同源（不复制数值）
            case THUNDER -> List.of(ToolEnergyColorConfig.THUNDERITE.light,
                ToolEnergyColorConfig.THUNDERITE.dark);
        };
    }
}
