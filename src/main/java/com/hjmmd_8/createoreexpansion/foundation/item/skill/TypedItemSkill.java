package com.hjmmd_8.createoreexpansion.foundation.item.skill;

/**
 * 类型安全的技能接口 - 使用泛型增强类型安全
 * @param <C> 上下文类型
 */
public interface TypedItemSkill<C> extends ItemSkill {

    /**
     * 类型安全的释放技能方法
     * @param context 技能上下文
     */
    void releaseTyped(C context);

    @Override
    default void release(Object context) {
        // 通过 SkillType 进行类型检查
        SkillType type = getType();
        if (type.cast(context)) {
            @SuppressWarnings("unchecked")
            C typedContext = (C) context;
            releaseTyped(typedContext);
        }
    }
}
