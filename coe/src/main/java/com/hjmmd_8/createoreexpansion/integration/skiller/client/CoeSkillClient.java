package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.client.SkillSettingsScreen;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeAreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeEntityStrategy;
import com.leaf.skiller.client.ClientSkillCache;
import com.leaf.skiller.client.renderer.StrategyRenderers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 客户端接线：把本模组的三个技能键位交给 Skiller，并保持"随时可用"的既有玩法。
 *
 * <h2>三件事</h2>
 * <ol>
 *     <li><b>注入键位</b>：{@link ClientSkillCache#setKeySource} —— 内核默认键位是
 *         Ctrl/Shift/Alt，与本模组既有的 Shift/R/G 以及 Create 的 Ctrl 都冲突；
 *         注入后按键完全由本模组决定（且沿用了 {@code AllKeys} 的
 *         {@code ConflictSafeKeyMapping} 与现成中英翻译）。</li>
 *     <li><b>关掉总开关闸</b>：{@link ClientSkillCache#setToggleKeysEnabled}(false) ——
 *         内核自带的"按 R 启用/禁用技能系统"键位默认是 R/Y，其中 R 正好是本模组
 *         技能槽位 2 的键，会一直弹 {@code message.skiller.enabled}。本模组玩法是
 *         随时可用（无总开关），因此进世界直接 {@link ClientSkillCache#enable}。</li>
 *     <li><b>换工具刷新</b>：技能/按键槽位是从主手物品的技能组件推导出来的，
 *         换工具后必须重算，否则按键状态不会同步、策略预览也会停在旧技能上。</li>
 * </ol>
 *
 * <p>按键的实际传输链路（服务端权威）由内核负责：本类只提供"某个槽位当前是否按下"。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class CoeSkillClient {

    /** 槽位 → 键位，顺序即槽位 0/1/2（键一/键二/键三） */
    private static final AllKeys[] SLOT_KEYS = {
            AllKeys.SKILL_RELEASE,
            AllKeys.SKILL_RELEASE_2,
            AllKeys.SKILL_RELEASE_3
    };

    /** 上一次看到的技能组件（技能表/槽位的唯一来源；只比较它，不比较整个物品堆） */
    private static SkillsComponent lastSkills;

    /** 上一次完整穿着的那一套（换护甲 ⇒ 可能成套/不成套翻转 ⇒ 必须重算槽位声明） */
    private static ArmorSet lastWornSet;

    /** 键位注入只需要做一次 */
    private static boolean keySourceInjected;

    private CoeSkillClient() {
        throw new AssertionError("This class should not be instantiated");
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        handleSettingsKey(minecraft);
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            // 离开世界：清掉记录，下次进世界重新注入/刷新
            lastSkills = null;
            lastWornSet = null;
            return;
        }

        injectKeySourceOnce();
        tickHoldCounters();

        // 只比较「技能组件」而不是整个物品堆：能量消耗会改物品堆的其它组件，
        // 若按物品堆比较，每次用技能都会触发一次重算（没必要且有分配开销）。
        SkillsComponent current = SkillItemStack.of(player.getMainHandItem()).getSkillsHolder();

        if (!ClientSkillCache.isEnable()) {
            // ⚠ 2026-10-01 实测根因（字节码实证）：`ClientSkillCache.enable(Player)` 在**未启用**时是
            // **空操作**（`if (enable) refresh(); else return;`）—— 它只负责"已启用时刷新"，
            // 真正的启用开关只有 `handleSyncRequest(true)` 会打开（它同时收集技能组件并把组件回传服务端）。
            // 而内核只在"服务端收到按键包"时才发同步请求 ⇒ **只有护甲带技能**的玩家永远等不到那一刻：
            // 客户端组件恒为空 ⇒ HUD 显示"暂无套装技能"，槽位 3/4/5 也永不轮询（技能根本按不出来）。
            // 这里主动做一次本地握手（与服务端请求时走的是同一段代码，安全）。
            ClientSkillCache.handleSyncRequest(true);
            lastSkills = current;
            return;
        }

        if (!Objects.equals(lastSkills, current)) {
            lastSkills = current;
            ClientSkillCache.refresh(player);
        }

        // 装备段同理：护甲换一件就可能"成套/不成套"翻转，而槽位 3/4/5 是在 refresh 时
        // 由 ArmorSkillProvider.collectKeys 声明出来的 —— 不重算的话那段槽位永远不会被轮询。
        ArmorSet worn = ArmorSet.wornSet(player);
        if (worn != lastWornSet) {
            lastWornSet = worn;
            ClientSkillCache.refresh(player);
        }
    }

    /**
     * 「技能设置」键（默认 J）：按下开 {@link SkillSettingsScreen}。
     *
     * <p>两个必须的写法：</p>
     * <ul>
     *     <li>{@code while (consumeClick())} 而不是 {@code isDown()}——后者是"按住"
     *         状态，会在界面关掉之后继续生效、把界面又弹回来。</li>
     *     <li><b>只在没有任何界面打开时开</b>（{@code screen == null}）：否则会和背包、
     *         Create 的界面打架（在那些界面里按 J 不该弹这个）。点击计数在界面打开期间
     *         本来就不会累积，这里的判断是双保险。</li>
     * </ul>
     *
     * <p>键位对象只在客户端由 {@code RegisterKeyMappingsEvent} 赋值，因此照 {@link AllKeys}
     * 的既有约定做 null 保护（服务端/未注册时视为没按）。</p>
     */
    private static void handleSettingsKey(Minecraft minecraft) {
        KeyMapping key = AllKeys.SKILL_SETTINGS.getKeybind();
        if (key == null) {
            return;
        }
        while (key.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new SkillSettingsScreen());
            }
        }
    }

    private static void injectKeySourceOnce() {
        if (keySourceInjected) {
            return;
        }
        keySourceInjected = true;
        ClientSkillCache.setKeySource(CoeSkillClient::isSlotPressed);
        // 本模组没有总开关：关掉内核的开关键闸，避免按 R（= 槽位 2 的键）弹出启用提示
        ClientSkillCache.setToggleKeysEnabled(false);
        // 策略渲染器注册：必须早于 ClientSkillCache.enable(...)（enable 内部会 schedule()）
        StrategyRenderers.register(CoeAreaAoeStrategy.RENDERER_ID, new CoeBlockOutlineRenderer());
        // 生物描边（skin / plunder）：id 与 CoeEntityStrategy.getRendererId() 对齐
        StrategyRenderers.register(CoeEntityStrategy.RENDERER_ID, new CoeEntityOutlineRenderer());
    }

    /**
     * <b>键源：工具段与装备段的模式切换</b>（用户 2026-10-01 的"装备辅助按键"设计）。
     *
     * <p>内核会为「每个来源声明过的槽位号」各问一次"这个槽位现在按下没有"。两段槽位空间是分开的
     * （工具 0/1/2、装备 3/4/5，见 {@link ArmorSkillProvider}），所以这里只做分流：</p>
     * <ul>
     *     <li><b>按住装备修饰键（默认左 Alt）</b>：槽位 3/4/5 映射到物理键一/二/三，而槽位 0/1/2
     *         <b>一律报未按下</b> —— 这就是"抑制工具技能"。不抑制的话，服务端遍历时会同时命中
     *         槽位 0 的工具技能与槽位 3 的装备技能，两个一起放（能量双扣、效果同 tick）。</li>
     *     <li><b>松开修饰键</b>：槽位 0/1/2 照常映射物理键，装备段一律未按下。</li>
     * </ul>
     *
     * <p>刻意<b>不</b>做"两段同时按下"：那正是用户担心的紊乱形态。</p>
     *
     * @param slot 内核询问的槽位号（来自各 Provider 的 {@code collectKeys}）
     */
    private static boolean isSlotPressed(int slot) {
        boolean equipmentMode = isEquipmentModifierDown();
        if (ArmorSkillProvider.isEquipmentSlot(slot)) {
            return equipmentMode && SLOT_KEYS[slot - ArmorSkillProvider.SLOT_BASE].isPressed();
        }
        if (ArmorSkillProvider.isHeldItemSlot(slot) && slot < SLOT_KEYS.length) {
            return !equipmentMode && SLOT_KEYS[slot].isPressed();
        }
        return false; // 越界或未知槽位：内核契约里一律视为未按下
    }

    /**
     * 装备修饰键是否按下（客户端）。
     *
     * <p>键位对象只在客户端由 {@code RegisterKeyMappingsEvent} 赋值，因此照 {@link AllKeys}
     * 的既有约定做 null 保护（未注册时视为没按）。提示层
     * （{@code client.hud.EquipmentSkillHud}）也复用它，保证"能放"与"提示"同源。</p>
     */
    public static boolean isEquipmentModifierDown() {
        return AllKeys.EQUIPMENT_MODIFIER.isPressed();
    }

    // ================= 长按计时（给 HUD 做"实时扣能预览"用） =================

    /**
     * 客户端长按计时：槽位 → 已按住 tick 数（用户 2026-10-01 要求"长按时要能看见能量怎么降"）。
     *
     * <p>为什么客户端也要自己数：服务端的结算在<b>松手那一刻</b>（用户口径：按比例、向下取整），
     * 所以按住期间服务端不会持续扣能；而玩家想看到的"按住越久要花越多"是<b>预览</b>，
     * 必须由客户端按同一公式实时算。公式<b>与服务端同一个</b>
     * （{@code ArmorSkillRuntime#holdCost}），不会两套账。</p>
     */
    private static final Map<Integer, Integer> HOLD_TICKS = new HashMap<>();

    /** 每客户端 tick 推进长按计时（由 {@link #onClientTick} 调用）。 */
    private static void tickHoldCounters() {
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            int slot = ArmorSkillProvider.SLOT_BASE + index;
            if (isSlotPressed(slot)) {
                HOLD_TICKS.merge(slot, 1, Integer::sum);
            } else {
                HOLD_TICKS.remove(slot);
            }
        }
    }

    /** 该装备槽位当前已按住多少 tick（没按该槽位时为 0）。 */
    public static int holdTicksOf(int slot) {
        return HOLD_TICKS.getOrDefault(slot, 0);
    }

    /**
     * 装备段某个槽位对应的技能键（给提示层显示"这个技能绑哪个键"用）。
     *
     * <p>键位表只此一份（{@link #SLOT_KEYS}），提示层不再自己列一遍 —— 否则改键或加键时两边会漂移。</p>
     *
     * @param slot 内核槽位号；不属于装备段或越界时返回 {@code null}
     */
    public static @Nullable AllKeys skillKeyForEquipmentSlot(int slot) {
        if (!ArmorSkillProvider.isEquipmentSlot(slot)) {
            return null;
        }
        int index = slot - ArmorSkillProvider.SLOT_BASE;
        return index < SLOT_KEYS.length ? SLOT_KEYS[index] : null;
    }
}
