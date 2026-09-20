package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeAreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeEntityStrategy;
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
            logCache("enable");
            return;
        }

        if (!Objects.equals(lastSkills, current)) {
            lastSkills = current;
            ClientSkillCache.refresh(player);
            logCache("refresh(换工具)");
        }
    }

    /**
     * 临时诊断（定位完"预览不显示"后连同 CoeBlockOutlineRenderer 里的 trace 一起删）：
     * 打印客户端技能缓存的实际内容 —— 它是 strategy 渲染调度的唯一来源。
     * 若这里"实例数=0"，说明客户端根本没拿到技能，渲染器永远排不上。
     */
    private static void logCache(String reason) {
        try {
            var skills = ClientSkillCache.skills;
            if (skills == null) {
                CreateOreExpansion.LOGGER.info("[SkillerRender] 客户端技能缓存（{}）：null", reason);
                return;
            }
            var all = skills.getAllData();
            long strategyCount = all.stream()
                    .filter(i -> i.skill() != null && i.skill().getSkill() instanceof com.leaf.skiller.foundation.skill.StrategySkill)
                    .count();
            CreateOreExpansion.LOGGER.info("[SkillerRender] 客户端技能缓存（{}）：槽位={}, 实例数={}, 其中策略技能={}",
                    reason, skills.bindings().keySet(), all.size(), strategyCount);
        } catch (Throwable t) {
            CreateOreExpansion.LOGGER.warn("[SkillerRender] 客户端技能缓存诊断本身出错", t);
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
        // 生物描边（skin / plunder）：id 与 CoeEntityStrategy.getRendererId() 对齐
        StrategyRenderers.register(CoeEntityStrategy.RENDERER_ID, new CoeEntityOutlineRenderer());
        CreateOreExpansion.LOGGER.info("[SkillerRender] 已注册预览渲染器：方块={}，生物={}",
                CoeAreaAoeStrategy.RENDERER_ID, CoeEntityStrategy.RENDERER_ID);
    }
}
