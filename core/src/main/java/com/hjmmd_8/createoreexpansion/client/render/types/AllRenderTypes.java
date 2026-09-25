package com.hjmmd_8.createoreexpansion.client.render.types;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * 技能预览线框渲染层。
 *
 * <p><b>纯客户端类</b>：静态字段在类加载时直接调用
 * {@link RenderType#create}（客户端 API），本类<b>绝不能被服务端加载</b>——
 * 只由客户端渲染代码引用。</p>
 *
 * <p><b>P3s：本类已从 {@code client/} 搬进 core 共享库</b>（落点
 * {@code core/.../client/render/types/}，包名随之改为 {@code client.render.types}）。
 * 原先它住在 {@code client/} 这个 SHARED 路径下，是 COE 与 CEWS 两边
 * 仅有的两个渲染器共同依赖的类——{@code client/tool/renderer/BlockToolOutlineRenderer}（COE 技能预览）
 * 与 {@code content/energyfield/EnergyFieldGoggleOutlineRenderer}（CEWS 能量场）
 * 各自 import 它一次，于是它成了跨层的公共契约。既然两层的渲染器都要用它、
 * 它自己又不引用任何层的内容，唯一诚实的落点就是共享库。</p>
 *
 * <p><b>"core 看不见 Minecraft client" 这条旧前提已失效</b>：P3m 之后 core 与根 mod
 * 在 dev 里是同一个 GAME 层 mod 文件、在生产里是 JarJar 嵌套库，两者都能看见 MC 的
 * 客户端类。本类搬进去后<b>真的被调用过一次</b>（P3s 探针：构造器 + runData 各调
 * {@code LINES_TRANSPARENT} / {@code CUBOID_QUADS_TRANSLUCENT}，拿到非 null 的
 * {@link RenderType} 并打进日志），不是只靠 {@code compileJava} 放行。</p>
 */
public class AllRenderTypes extends RenderType {
    public AllRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static final RenderType LINES_TRANSPARENT = RenderType.create(
            "skills_lines_transparent",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            RenderType.CompositeState.builder()
                .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                .setLineState(new LineStateShard(OptionalDouble.empty()))
                .setLayeringState(NO_LAYERING)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(NO_DEPTH_TEST)
                .createCompositeState(false)
    );

    /**
     * 能量场指示框：QUADS 细长棱线（像 Create 的 outlineSolid 那种有粗度的框，
     * 而非 1px 线条）。顶点格式 POSITION_COLOR，带半透明混合与 alpha 渐变。
     */
    public static final RenderType CUBOID_QUADS_TRANSLUCENT = RenderType.create(
            "energy_field_cuboid_lines",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            262144,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .createCompositeState(false)
    );
}
