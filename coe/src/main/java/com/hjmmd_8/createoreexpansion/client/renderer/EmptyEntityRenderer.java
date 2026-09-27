package com.hjmmd_8.createoreexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 空渲染器：不渲染任何模型（用于纯粒子视觉的实体，如充能器能量波）。
 *
 * <p><b>W6-c：它又搬回 {@code :coe}（{@code client.renderer}）</b>。P7a 把它从 {@code :coe} 搬进
 * {@code :cews} 的理由是"两个波实体的注册者也在 {@code :cews}，而 {@code :cews} 对
 * {@code createoreexpansion} 只是 optional，单装 cews.jar 时这个类不存在会
 * {@code NoClassDefFoundError}"。W6-c 之后这两个波实体（{@code AllEntityTypes.CHARGER_WAVE} /
 * {@code STELLAR_WAVE}）随波引擎回到第一层，注册者与渲染器必须同 jar ⇒ 它跟着回来。
 * 顺带解开 P7a 那条死结：那时 {@code :coe} 占着 {@code client.renderer} 包，
 * 所以 P6 的"搬进 core"做不到——现在整个包都在 {@code :coe}。</p>
 *
 * <p>注册点见 {@code com.hjmmd_8.createoreexpansion.client.WaveEntityRendererRegistration}。
 * 缺了它，波实体第一次进入视野时 {@code EntityRenderDispatcher.getRenderer} 返回 {@code null}
 * → {@code getPackedLightCoords} NPE（P7a 实测的那条路径）。</p>
 */
public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {

	public EmptyEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return null;
	}

	@Override
	public void render(T entity, float yaw, float partialTicks, PoseStack poseStack,
					   MultiBufferSource buffer, int light) {
	}
}
