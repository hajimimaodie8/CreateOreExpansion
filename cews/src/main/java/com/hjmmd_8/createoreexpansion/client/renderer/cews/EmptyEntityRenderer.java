package com.hjmmd_8.createoreexpansion.client.renderer.cews;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 空渲染器：不渲染任何模型（用于纯粒子视觉的实体，如充能器能量波）。
 *
 * <p><b>P7a：为什么从 {@code :coe} 搬进 {@code :cews}</b>。它唯一的消费者是 CEWS 的两个能量波实体
 * （{@code AllEntityTypes.CHARGER_WAVE} / {@code STELLAR_WAVE}，注册见
 * {@code client.cews.CewsClientSetup}）。留在 {@code :coe} 会让 {@code :cews} 的渲染器注册
 * 引用一个"可选模块"里的类：{@code :cews} 的 {@code mods.toml} 把 {@code createoreexpansion}
 * 声明为 optional，单装 cews.jar 时那个类不存在 ⇒ {@code EntityRenderers.register} 的
 * 方法引用在解析时 {@code NoClassDefFoundError}。</p>
 *
 * <p>（P6 的方案是"搬进 core"。实测那条路要另外给 core 造一个不叫
 * {@code client.renderer} 的包 —— JPMS 红线：同一个 Java 包不能同时属于两个 mod 文件，
 * 而 {@code :coe} 还占着 {@code client.renderer}（{@code GrinderRenderer}）。
 * 搬进 CEWS 自己既满足红线、又让 {@code :cews} 对 COE 的引用数变成 0，所以选后者。）</p>
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
