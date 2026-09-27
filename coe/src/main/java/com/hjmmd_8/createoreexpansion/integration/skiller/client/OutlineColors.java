package com.hjmmd_8.createoreexpansion.integration.skiller.client;

/**
 * 技能预览的着色常量（渲染统一后的自有常量，不再依赖旧框架的 record）。
 *
 * <h2>为什么需要这个类</h2>
 * <p>新渲染器（{@link CoeBlockOutlineRenderer} / {@link CoeEntityOutlineRenderer}）原先只用到一个
 * {@code SkillRendererConfig.ALPHA}，却因此 import 了旧 {@code client/tool/SkillRendererConfig}
 * —— 那个 record 的字段是旧框架的 {@code DataSkill}。渲染统一后旧 {@code SkillRendererConfig}
 * 连同旧渲染栈一起删除，这个常量必须由新路径自己持有，否则新渲染器又会把旧框架拉回来。</p>
 *
 * <p><b>数值与旧值一字不差</b>（{@code 0.5F}）：颜色仍从技能实例 NBT 的 {@code OutlineColor}
 * 子标签读（缺失时默认白色），alpha 恒为 0.5；方块预览的第二层（穿透层）在这个值上再乘
 * {@code CoeBlockOutlineRenderer#TRANSPARENT_ALPHA_FACTOR}（0.3，与旧
 * {@code BlockToolOutlineRenderer} 的第二层完全一致），因此最终可见 alpha 与换核前同值。</p>
 *
 * @since 1.0.0
 */
public final class OutlineColors {

    /** 预览描边的 alpha（= 旧 {@code SkillRendererConfig.ALPHA} = 0.5F，不许改成别的值） */
    public static final float ALPHA = 0.5F;

    private OutlineColors() {
        throw new AssertionError("This class should not be instantiated");
    }
}
