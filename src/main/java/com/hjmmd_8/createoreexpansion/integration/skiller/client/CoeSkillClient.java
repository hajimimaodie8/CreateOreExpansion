package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeAreaAoeStrategy;
import com.leaf.skiller.client.ClientSkillCache;
import com.leaf.skiller.client.renderer.StrategyRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

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
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID, value = Dist.CLIENT)
public final class CoeSkillClient {

    /** 槽位 → 键位，顺序即槽位 0/1/2（键一/键二/键三） */
    private static final AllKeys[] SLOT_KEYS = {
            AllKeys.SKILL_RELEASE,
            AllKeys.SKILL_RELEASE_2,
            AllKeys.SKILL_RELEASE_3
    };

    /** 上一次看到的技能组件（技能表/槽位的唯一来源；只比较它，不比较整个物品堆） */
    private static SkillsComponent lastSkills;

    /** 键位注入只需要做一次 */
    private static boolean keySourceInjected;

    private CoeSkillClient() {
        throw new AssertionError("This class should not be instantiated");
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            // 离开世界：清掉记录，下次进世界重新注入/刷新
            lastSkills = null;
            return;
        }

        injectKeySourceOnce();

        // 只比较「技能组件」而不是整个物品堆：能量消耗会改物品堆的其它组件，
        // 若按物品堆比较，每次用技能都会触发一次重算（没必要且有分配开销）。
        SkillsComponent current = SkillItemStack.of(player.getMainHandItem()).getSkillsHolder();

        if (!ClientSkillCache.isEnable()) {
            ClientSkillCache.enable(minecraft, player);
            lastSkills = current;
            return;
        }

        if (!Objects.equals(lastSkills, current)) {
            lastSkills = current;
            ClientSkillCache.refresh(player);
        }
    }

    private static void injectKeySourceOnce() {
        if (keySourceInjected) {
            return;
        }
        keySourceInjected = true;
        // 槽位越界一律视为未按下（内核契约要求）
        ClientSkillCache.setKeySource(slot ->
                slot >= 0 && slot < SLOT_KEYS.length && SLOT_KEYS[slot].isPressed());
        // 本模组没有总开关：关掉内核的开关键闸，避免按 R（= 槽位 2 的键）弹出启用提示
        ClientSkillCache.setToggleKeysEnabled(false);
        // 策略渲染器注册：必须早于 ClientSkillCache.enable(...)（enable 内部会 schedule()）
        StrategyRenderers.register(CoeAreaAoeStrategy.RENDERER_ID, new CoeBlockOutlineRenderer());
        CreateOreExpansion.LOGGER.info("[SkillerRender] 已注册挖掘预览渲染器 id={}", CoeAreaAoeStrategy.RENDERER_ID);
    }
}
