package com.hjmmd_8.createoreexpansion.client;

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
 * 必须放在 client 包并只由客户端渲染代码引用。</p>
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
}
