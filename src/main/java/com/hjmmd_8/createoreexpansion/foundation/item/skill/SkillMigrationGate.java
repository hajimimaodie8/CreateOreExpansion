package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

/**
 * 技能迁移闸门：告诉旧框架「这个技能已经搬到新内核（Skiller）了，别再处理它」。
 *
 * <p>换核是<b>增量并行</b>进行的：一个技能只要注册进了 Skiller 的
 * {@code skiller:skill} 注册表，它的释放就该由 {@code integration.skiller} 一侧负责；
 * 同名技能如果还被旧框架处理一次，玩家就会看到<b>双重效果</b>（而旧的
 * {@link SkillsComponent#releaseSkillAt} 正是逐槽位无条件释放的）。</p>
 *
 * <p>默认判定恒为 {@code false}（即「没有技能被迁移」），所以<b>不接线就完全等于旧行为</b>；
 * 接线方是新内核的注册入口（{@code SkillerIntegration}），它会把判定接到
 * 「技能 id 是否存在于 Skiller 的 SKILLS 注册表」上。旧框架因此不需要 import 任何
 * Skiller 类型——依赖方向保持单向。</p>
 *
 * @since 1.0.0
 */
public final class SkillMigrationGate {

    /** 判定「该技能 id 是否已由新内核接管」。默认恒假。 */
    private static volatile Predicate<ResourceLocation> migratedCheck = id -> false;

    private SkillMigrationGate() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 由新内核的注册入口设置判定。
     *
     * @param check 判定「技能 id 是否已注册进新内核」；传 {@code null} 表示恢复默认（恒假）
     */
    public static void setCheck(Predicate<ResourceLocation> check) {
        migratedCheck = check == null ? id -> false : check;
    }

    /**
     * 该技能是否已迁移到新内核（迁移后旧框架必须跳过它，避免双重生效）。
     *
     * @param skill 旧框架的技能实例；{@code null} 或未注册 id 一律视为未迁移
     */
    public static boolean isMigrated(ItemSkill skill) {
        if (skill == null) return false;
        ResourceLocation id = AllSkills.getId(skill);
        return id != null && migratedCheck.test(id);
    }
}
