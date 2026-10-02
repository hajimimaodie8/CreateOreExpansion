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

    /**
     * <b>装备模式开关</b>（用户 2026-10-02 裁定）：左 Alt 由"按住"改成"按一下开 / 再按一下关"。
     *
     * <p>用户原话的痛点：按住装备键的同时还要按技能键 ⇒ 左手必须一直压着左 Alt，非常费事。
     * 所以这里用<b>锁存</b>（latch）而不是按键状态 —— 开关打开后松手，装备技能键照旧生效；
     * 关掉后一直关闭，直到再按一下。</p>
     *
     * <p>只在客户端存在，且<b>离开世界即复位为关</b>（见 {@link #onClientTick}），免得下次进世界
     * 莫名其妙"技能键全变成装备技能"。服务端与内核都不认识这个开关：它们只看到"某个槽位按下没有"，
     * 因此传输与结算语义一字未改。</p>
     */
    private static boolean equipmentModeOn;

    private CoeSkillClient() {
        throw new AssertionError("This class should not be instantiated");
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        handleSettingsKey(minecraft);
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            // 离开世界：清掉记录与装备模式开关，下次进世界重新注入/刷新
            lastSkills = null;
            lastWornSet = null;
            setEquipmentModeOn(false);
            return;
        }

        injectKeySourceOnce();
        // 开关只在这里翻转（必须晚于上面的离世分支：主菜单/加载界面里累积的点击不该改模式）
        handleEquipmentModeKey();
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
     * <b>装备模式键（默认左 Alt）：开关，不是按住</b>（用户 2026-10-02 裁定）。
     *
     * <p>按一下 ⇒ 装备模式<b>开</b>（松手仍然是开）；再按一下 ⇒ <b>关</b>。</p>
     *
     * <p><b>为什么必须是 {@code consumeClick()}</b>：{@code isPressed()} 报的是"此刻是否按着"的
     * <b>电平</b>，每 tick 都会为真 —— 拿它翻转开关等于每 tick 翻一次（几十毫秒内自己抖成随机值）。
     * {@code consumeClick()} 是<b>边沿</b>：只在"距上次读取之间发生过一次按下"时返回一次 true 并
     * 消费掉那个点击，所以 {@code while} 循环里每个 true 恰好对应玩家的一次物理按下
     * （连点两下＝翻两次＝回到原状态，正是开关该有的语义）。</p>
     *
     * <p>键位对象只在客户端由 {@code RegisterKeyMappingsEvent} 赋值，因此照 {@link AllKeys}
     * 的既有约定做 null 保护（未注册时视为没按）。</p>
     */
    private static void handleEquipmentModeKey() {
        KeyMapping key = AllKeys.EQUIPMENT_MODIFIER.getKeybind();
        if (key == null) {
            return;
        }
        while (key.consumeClick()) {
            setEquipmentModeOn(!equipmentModeOn);
        }
    }

    /**
     * 设置装备模式开关，并在<b>真的翻转</b>时打一行日志（方便排查"为什么按技能键没反应/放错技能"）。
     *
     * <p>同值调用是空操作：离开世界那段每 tick 都会调它复位，否则会在主菜单里刷日志。</p>
     *
     * @param on true = 装备模式开（技能键释放装备技能），false = 关（技能键释放工具技能）
     */
    public static void setEquipmentModeOn(boolean on) {
        if (equipmentModeOn == on) {
            return;
        }
        equipmentModeOn = on;
        CoeCore.LOGGER.info("[装备模式] {}（技能键现在释放{}技能）", on ? "开" : "关", on ? "装备" : "工具");
    }

    /**
     * 装备模式开关当前是否为开（客户端）。
     *
     * <p>这是<b>唯一</b>的装备模式判据：键源分流（{@link #isSlotPressed}）与提示层
     * （{@code client.hud.EquipmentSkillHud}）都走它，保证"能放"与"提示"同源。
     * 原始按键状态（{@code EQUIPMENT_MODIFIER.isPressed()}）<b>不再参与任何判定</b>，
     * 它只在 {@link #handleEquipmentModeKey} 里以 {@code consumeClick()} 的形式被消费。</p>
     */
    public static boolean isEquipmentModeOn() {
        return equipmentModeOn;
    }

    /**
     * <b>键源：工具段与装备段的模式切换</b>（用户 2026-10-01 的"装备辅助按键"设计；
     * 2026-10-02 起该按键由"按住"改为"开关"，分流规则一字未改）。
     *
     * <p>内核会为「每个来源声明过的槽位号」各问一次"这个槽位现在按下没有"。两段槽位空间是分开的
     * （工具 0/1/2、装备 3/4/5，见 {@link ArmorSkillProvider}），所以这里只做分流：</p>
     * <ul>
     *     <li><b>装备模式开（左 Alt 开关打开）</b>：槽位 3/4/5 映射到物理键一/二/三，而槽位 0/1/2
     *         <b>一律报未按下</b> —— 这就是"抑制工具技能"。不抑制的话，服务端遍历时会同时命中
     *         槽位 0 的工具技能与槽位 3 的装备技能，两个一起放（能量双扣、效果同 tick）。</li>
     *     <li><b>装备模式关</b>：槽位 0/1/2 照常映射物理键，装备段一律未按下。</li>
     * </ul>
     *
     * <p>刻意<b>不</b>做"两段同时按下"：那正是用户担心的紊乱形态。</p>
     *
     * <p><b>代价（必须让玩家知道）</b>：开关开着的时候，<b>工具技能（键一/二/三）整体让位</b> ——
     * 那三个键此刻释放的是装备技能。要回去用工具技能，就得先按一下左 Alt 把开关关掉。
     * 这是"两段不重叠"这个硬约束的直接后果，不是可以顺手绕开的实现细节。</p>
     *
     * @param slot 内核询问的槽位号（来自各 Provider 的 {@code collectKeys}）
     */
    private static boolean isSlotPressed(int slot) {
        boolean equipmentMode = isEquipmentModeOn();
        if (ArmorSkillProvider.isEquipmentSlot(slot)) {
            return equipmentMode && SLOT_KEYS[slot - ArmorSkillProvider.SLOT_BASE].isPressed();
        }
        if (ArmorSkillProvider.isHeldItemSlot(slot) && slot < SLOT_KEYS.length) {
            return !equipmentMode && SLOT_KEYS[slot].isPressed();
        }
        return false; // 越界或未知槽位：内核契约里一律视为未按下
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
