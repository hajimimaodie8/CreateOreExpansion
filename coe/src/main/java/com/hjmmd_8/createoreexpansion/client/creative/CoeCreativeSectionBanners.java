package com.hjmmd_8.createoreexpansion.client.creative;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeSections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * <b>创造页「分区横幅」的客户端渲染</b>：在 {@code base_tab}（矿物拓展）的空行上叠加画三条横幅
 * + 释词。
 *
 * <p>横幅<b>不是物品</b>（不能进 {@code displayItems} —— 详见落地文档 §6.1），所以只能由渲染钩子
 * 画在最上层。挂点选 {@link ScreenEvent.Render.Post}：它由 NeoForge 在
 * {@code screen.renderWithTooltip(...)} <b>之后</b>立刻发出，此时格子底、物品、滚动条都画完了。</p>
 *
 * <h3>为什么是「自适应判定」而不是「记录行号 − 1」</h3>
 * <p>分区记录（{@link CoeCreativeSections#SECTION_ROWS}）记的是<b>首物品行</b>，理论上横幅行 = 它 − 1。
 * 但那只在「列表内容与记录时完全一致」时成立 —— 别的模组往本页插/删物品、或者搜索改变了内容，
 * 都会失准。所以这里<b>只认「整行空格子」这个事实</b>（{@link #bannerRow}）：目标行整行空就画那里，
 * 否则看上一行，两行都不空就不画。<b>从构造上保证绝不会把横幅画到物品上</b>，
 * 代价是每帧多做十几次 {@code hasItem()}（可忽略）。</p>
 *
 * <h3>三个前置判断，顺序不能变</h3>
 * <ol>
 *     <li>是创造界面吗（{@link CreativeModeInventoryScreen}）；</li>
 *     <li>选中的是本模组的页吗（{@code createoreexpansion:base_tab}）；</li>
 *     <li>本页有分区记录吗（{@link CoeCreativeSections#SECTION_ROWS} 非空）。</li>
 * </ol>
 * <p>少任何一步，就会<b>在别人的页上画出我们的横幅</b>。</p>
 *
 * <h3>反射纪律</h3>
 * <p>只有两处必须反射（本轮已用 {@code javap} 对 1.21.1 + NeoForge 21.1.228 核实字段名与类型）：</p>
 * <ul>
 *     <li>{@code CreativeModeInventoryScreen.selectedTab} —— {@code private static CreativeModeTab}
 *         （static ⇒ {@code Field.get(null)}）；</li>
 *     <li>{@code CreativeModeInventoryScreen.scrollOffs} —— {@code private float}（实例字段）。</li>
 * </ul>
 * <p>物品区左上角与尺寸走公开 API（{@code getGuiLeft()/getGuiTop()}），<b>不需要</b>反射。
 * 反射失败一律<b>静默降级</b>（拿不到就不画）—— <b>绝不能因为装饰性渲染把客户端搞崩</b>。</p>
 *
 * @see CoeCreativeSections 分区规则、排布与行号记录
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class CoeCreativeSectionBanners {

    /** 物品区相对 {@code leftPos} 的偏移（格底 18×18 的左上角，逐像素核实过，见落地文档 §5 步骤 7）。 */
    private static final int ITEM_AREA_X = 8;

    /** 物品区相对 {@code topPos} 的偏移。 */
    private static final int ITEM_AREA_Y = 17;

    /** 一格边长（像素）。 */
    private static final int CELL = 18;

    /** 可见行数（原版 {@code CreativeModeInventoryScreen.NUM_ROWS}；那是 private static final，故照抄）。 */
    private static final int VISIBLE_ROWS = 5;

    /** 横幅宽 = 一整行 = 9 × 18 = 162（三张贴图恰好这个尺寸 ⇒ 1:1 绘制，不拉伸）。 */
    private static final int BANNER_WIDTH = CoeCreativeSections.ITEMS_PER_ROW * CELL;

    /** 横幅高 = 18。 */
    private static final int BANNER_HEIGHT = 18;

    /** 本模组的页 id（字符串比较，避免为此持有页实例）。 */
    private static final String OUR_TAB_ID = CoeCore.REGISTRY_NAMESPACE + ":" + CoeCreativeSections.BASE_TAB_KEY;

    /** 释词底方块：半透明黑。三张横幅都<b>全不透明</b>，没有它白字会压在花纹上（见落地文档 §3.1/§5 步骤 7）。 */
    private static final int LABEL_BACKGROUND = 0x90000000;

    /** 释词文字：不透明白 + 阴影。 */
    private static final int LABEL_COLOR = 0xFFFFFFFF;

    /** 反射字段缓存（{@code Optional.empty()} = 已查过、确实没有）。 */
    private static final Map<String, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        try {
            // ① 是创造界面吗
            if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
                return;
            }
            // ② 选中的是本模组的页吗
            if (!OUR_TAB_ID.equals(selectedTabId())) {
                return;
            }
            // ③ 本页有分区记录吗
            Map<String, Integer> rows = CoeCreativeSections.SECTION_ROWS;
            if (rows.isEmpty()) {
                return;
            }
            CreativeModeInventoryScreen.ItemPickerMenu menu = screen.getMenu();
            if (menu == null) {
                return;
            }

            int firstVisibleRow = firstVisibleRow(menu.items.size(), scrollOffs(screen));
            int left = screen.getGuiLeft() + ITEM_AREA_X;
            int top = screen.getGuiTop() + ITEM_AREA_Y;
            GuiGraphics graphics = event.getGuiGraphics();

            String prefix = CoeCreativeSections.BASE_TAB_KEY + "|";
            for (Map.Entry<String, Integer> entry : rows.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(prefix)) {
                    continue;
                }
                CoeCreativeSections.Section section =
                    CoeCreativeSections.byKey(key.substring(prefix.length()));
                if (section == null) {
                    continue;
                }
                // 记录值记的是「首物品行」；减完首个可见行才是可见行号，越界由 bannerRow 判掉。
                Integer firstItemRow = entry.getValue();
                if (firstItemRow == null) {
                    continue;
                }
                int row = bannerRow(menu, firstItemRow - firstVisibleRow);
                if (row < 0) {
                    continue;
                }
                drawBanner(graphics, section.banner(), Component.translatable(section.langKey()),
                    left, top + row * CELL);
            }
        } catch (Throwable ignored) {
            // 装饰性渲染：任何意外都不许把客户端搞崩（反射失败已各自静默降级，这里是最后一道网）。
        }
    }

    /**
     * <b>首个可见行</b>——照抄原版 {@code CreativeModeInventoryScreen.ItemPickerMenu} 的两个
     * {@code protected} 方法（外部无法调用，故复刻公式）：
     * <pre>
     * calculateRowCount()       = positiveCeilDiv(items.size(), 9) - 5
     * getRowIndexForScroll(s)   = max((int)(s * rowCount + 0.5), 0)
     * </pre>
     *
     * <p><b>最容易写错的一格</b>：{@code s = 0.75}、{@code rowCount = 3} 时
     * {@code 0.75 × 3 + 0.5 = 2.75}，{@code (int)} <b>截断成 2</b>，<b>不是</b> 3。
     * 所以这里<b>不能</b>用 {@code Math.round}。</p>
     */
    private static int firstVisibleRow(int itemCount, float scrollOffs) {
        int rowCount = Math.max((itemCount + CoeCreativeSections.ITEMS_PER_ROW - 1)
            / CoeCreativeSections.ITEMS_PER_ROW, 0) - VISIBLE_ROWS;
        if (rowCount <= 0) {
            return 0;   // 不满 5 行时不能乘出负数
        }
        return Math.max((int) ((double) (scrollOffs * (float) rowCount) + 0.5), 0);
    }

    /**
     * 目标可见行 → <b>真正能画横幅的行</b>，找不到返回 −1（自适应判定）。
     *
     * <p>{@code anchor} 有两种可能的语义：它本身是横幅行（若记录的是首物品行的上一行），
     * 或它是首物品行。两种情况都靠「整行是否为空」区分，不靠算术。</p>
     */
    private static int bannerRow(CreativeModeInventoryScreen.ItemPickerMenu menu, int anchor) {
        if (isRowEmpty(menu, anchor)) {
            return anchor;
        }
        if (isRowEmpty(menu, anchor - 1)) {
            return anchor - 1;
        }
        return -1;
    }

    /**
     * 该可见行是否<b>整行空格子</b>（渲染端唯一依赖的不变量）。
     *
     * <p>读的是<b>槽位</b>而不是 {@code items} 列表 —— 槽位才是真正被绘制的东西
     * （原版 {@code scrollTo} 把 {@code items} 拷进槽位）。</p>
     */
    private static boolean isRowEmpty(CreativeModeInventoryScreen.ItemPickerMenu menu, int row) {
        if (row < 0 || row >= VISIBLE_ROWS) {
            return false;
        }
        int first = row * CoeCreativeSections.ITEMS_PER_ROW;
        int last = first + CoeCreativeSections.ITEMS_PER_ROW;
        if (last > menu.slots.size()) {
            return false;
        }
        for (int i = first; i < last; i++) {
            if (menu.getSlot(i).hasItem()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 画一条横幅：① 162×18 精灵（1:1，不拉伸）→ ② 释词底方块 → ③ 释词文字。
     *
     * <p>{@code blitSprite} 吃的是<b>精灵名</b>（{@code createoreexpansion:section_ore}），
     * 不是完整贴图路径；名字写错会<b>静默</b>退化成紫黑格，没有任何日志（落地文档 §6.2）。
     * 三张图都放在 GUI 图集唯一的扫描目录 {@code textures/gui/sprites/} 下。</p>
     */
    private static void drawBanner(GuiGraphics graphics, ResourceLocation banner,
                                   Component title, int x, int y) {
        graphics.blitSprite(banner, x, y, BANNER_WIDTH, BANNER_HEIGHT);

        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(title);
        graphics.fill(x + 3, y + 3, x + textWidth + 11, y + BANNER_HEIGHT - 3, LABEL_BACKGROUND);
        graphics.drawString(font, title, x + 5, y + 5, LABEL_COLOR, true);
    }

    /** 当前选中的创造页 id（反射 {@code selectedTab}）；拿不到返回 {@code null}（⇒ 不画）。 */
    private static String selectedTabId() {
        Optional<Field> field = findField(CreativeModeInventoryScreen.class, "selectedTab");
        if (field.isEmpty()) {
            return null;
        }
        try {
            Object value = field.get().get(null);   // static 字段
            if (!(value instanceof CreativeModeTab tab)) {
                return null;
            }
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            return id == null ? null : id.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 当前卷动位置 0..1（反射 {@code scrollOffs}）；拿不到返回 0（= 从最顶部按原样判定）。 */
    private static float scrollOffs(CreativeModeInventoryScreen screen) {
        Optional<Field> field = findField(CreativeModeInventoryScreen.class, "scrollOffs");
        if (field.isEmpty()) {
            return 0.0F;
        }
        try {
            Object value = field.get().get(screen);
            return value instanceof Float f ? f : 0.0F;
        } catch (Exception ignored) {
            return 0.0F;
        }
    }

    /**
     * 沿整条继承链找一个字段（{@code leftPos/topPos} 那种定义在父类上的字段就靠这一步），
     * 并设为可访问。结果（含「确实没有」）缓存起来，避免每帧重查。
     */
    private static Optional<Field> findField(Class<?> type, String name) {
        return FIELD_CACHE.computeIfAbsent(type.getName() + "#" + name, key -> {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return Optional.of(field);
                } catch (NoSuchFieldException ignored) {
                    // 继续往父类找
                } catch (RuntimeException ignored) {
                    return Optional.empty();   // 例如 setAccessible 被模块系统拒绝
                }
            }
            return Optional.empty();
        });
    }

    /** 供调试/自检使用：把某一行当前的槽位占用情况转成字符串（不参与渲染逻辑）。 */
    static String describeRow(CreativeModeInventoryScreen.ItemPickerMenu menu, int row) {
        StringBuilder sb = new StringBuilder("row=").append(row).append(" empty=")
            .append(isRowEmpty(menu, row)).append(" items=");
        int first = row * CoeCreativeSections.ITEMS_PER_ROW;
        for (int i = first; i < first + CoeCreativeSections.ITEMS_PER_ROW && i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            sb.append(stack.isEmpty() ? '.' : 'X');
        }
        return sb.toString();
    }

    private CoeCreativeSectionBanners() {}
}
