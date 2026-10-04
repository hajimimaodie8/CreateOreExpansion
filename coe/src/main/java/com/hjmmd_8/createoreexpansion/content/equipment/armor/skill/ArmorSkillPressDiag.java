package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.leaf.skiller.server.PlayerPressedKeys;

import net.minecraft.server.level.ServerPlayer;

/**
 * <b>【临时诊断】按住装备技能键的"服务端到底读到了什么"</b>（2026-10-05 行为零变化拆分，
 * 从 {@code ArmorSkillRuntime#tick()} 里那一段诊断循环<b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>服务端读到的按下状态有没有变</b>（变了才打一行日志，
 * 不刷屏）。它<b>不改任何状态</b>、不参与任何判定 —— 抽出来的唯一目的是让"定位完成后整体删掉"
 * 这件事变成"删这一个文件 + 宿主里的一行调用"。</p>
 *
 * <p>搬运口径：循环体逐字相同，差异只有两类 —— 技能 id 的取法改写成
 * {@code ArmorSkillSlots.skillId(set, index)}（原先是宿主里的同名私有方法）、
 * {@code id} 由调用方传入。日志文案与字段顺序一个字未动。</p>
 */
final class ArmorSkillPressDiag {

    private ArmorSkillPressDiag() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 【临时诊断】玩家#槽位 → 上一次读到的按下状态（只在变化时打日志）。 */
    private static final Map<String, Boolean> DIAG_PRESSED = new HashMap<>();

    /**
     * 【临时诊断，定位"按住 Alt+R 服务端毫无反应"】只在状态变化时打印，不刷屏；定位后删掉。
     */
    static void logPressedChanges(ServerPlayer player, ArmorSet set, UUID id) {
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            int slot = ArmorSkillProvider.SLOT_BASE + index;
            boolean now = PlayerPressedKeys.isPressed(player, slot);
            String diagKey = id + "#" + slot;
            Boolean before = DIAG_PRESSED.get(diagKey);
            if (before == null || before != now) {
                DIAG_PRESSED.put(diagKey, now);
                com.hjmmd_8.createoreexpansion.common.CoeCore.LOGGER.info(
                    "[装备技能诊断] 槽位={} 服务端读到按下={} 套装={} 技能={} 能量合计={}",
                    slot, now, set, set == null ? null : ArmorSkillSlots.skillId(set, index),
                    ArmorEnergy.totalEnergy(player));
            }
        }
    }
}
