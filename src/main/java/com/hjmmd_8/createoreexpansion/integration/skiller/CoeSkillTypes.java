package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.leaf.skiller.foundation.skill.SkillType;
import com.leaf.skiller.foundation.skill.SkillTypeFactory;
import net.minecraft.resources.ResourceLocation;

/**
 * 模组接入 Skiller 新内核时使用的 {@link SkillType} 常量。
 *
 * <p>三个 id 必须与旧枚举
 * {@link com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType} 的
 * {@code name().toLowerCase(Locale.ROOT)} <b>逐字一致</b>
 * （{@code excavation_skill} / {@code hit_skill} / {@code use_skill}）：
 * Skiller 的 tooltip 用 {@code "skillType." + id.getNamespace() + "." + id.getPath()} 拼翻译键，
 * 守住 id 就等于守住了现有 12 条中英 lang 键（{@code skillType.createoreexpansion.*}）。</p>
 *
 * <p>注意：{@link SkillType} 必须经 {@link SkillTypeFactory#of(ResourceLocation)} 取得——
 * 该工厂内部按 id 缓存单例，而 {@code HashSkillType} 只覆写了 {@code hashCode} 没有覆写
 * {@code equals}，自己 new 一个实现会让 {@code Map<SkillType, ...>} 出现"hash 相同却查不到"的分裂。</p>
 */
public final class CoeSkillTypes {

    /** 挖掘类（对应旧 {@code SkillType.EXCAVATION_SKILL}） */
    public static final SkillType EXCAVATION =
            SkillTypeFactory.of(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "excavation_skill"));

    /** 攻击命中类（对应旧 {@code SkillType.HIT_SKILL}） */
    public static final SkillType HIT =
            SkillTypeFactory.of(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "hit_skill"));

    /** 使用物品类（对应旧 {@code SkillType.USE_SKILL}） */
    public static final SkillType USE =
            SkillTypeFactory.of(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "use_skill"));

    private CoeSkillTypes() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 旧枚举 → 新内核 {@link SkillType} 的小工具（本轮不注册技能，供 W4 迁移时直接取用）。
     *
     * @param legacy 旧枚举常量（可为 null）
     * @return 对应的 Skiller 类型；入参为 null 时返回 null
     */
    public static SkillType of(com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType legacy) {
        if (legacy == null) {
            return null;
        }
        return switch (legacy) {
            case EXCAVATION_SKILL -> EXCAVATION;
            case HIT_SKILL -> HIT;
            case USE_SKILL -> USE;
        };
    }
}
