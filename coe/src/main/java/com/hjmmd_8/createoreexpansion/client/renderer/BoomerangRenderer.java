package com.hjmmd_8.createoreexpansion.client.renderer;

import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.AbstractBoomerangEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * <b>回旋镖渲染器：直接复用物品外观，实体侧零新增模型/贴图</b>（需求 7）。
 *
 * <p>{@link #render} 里只做一件事：把镖的物品堆按 {@link ItemDisplayContext#FIXED} 静态渲染一次。
 * 于是"镖长什么样"永远等于"图标长什么样"——四张贴图就是全部美术资源，
 * 没有任何 {@code models/entity/**}、也没有 {@code textures/entity/**}（关卡 §29 断言）。</p>
 *
 * <p>为什么 {@link #getTextureLocation} 返回 {@link InventoryMenu#BLOCK_ATLAS}：
 * {@code EntityRenderer} 的契约要求一个贴图位置，而我们的绘制走物品图集 ⇒ 返回物品图集本身，
 * 这样父类/其它原版路径拿到它也不会去找一张并不存在的实体贴图。</p>
 *
 * <p>姿态（需求 7）：{@code translate(0, 0.2, 0)} + {@code XP.rotationDegrees(90)}
 * （让镖面朝上、像飞盘）+ {@code ZP.rotationDegrees(tickCount * 20)}
 * （每 tick 转 20°，看起来在自旋）；{@code tickCount < 2} 不画
 * ——刚生成的那两 tick 位置还没稳定，画了只会闪一下。</p>
 *
 * <p>⚠ 注册点：{@code client/WaveEntityRendererRegistration}（与实体类型注册在同一个改动里，
 * 否则"注册了实体没注册渲染器"就是客户端进视野即 NPE，见那个类的注释）。</p>
 */
public class BoomerangRenderer extends EntityRenderer<AbstractBoomerangEntity> {

	public BoomerangRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(AbstractBoomerangEntity entity) {
		// 不引入任何实体贴图：外观全交给物品图集
		return InventoryMenu.BLOCK_ATLAS;
	}

	@Override
	public void render(AbstractBoomerangEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
					   MultiBufferSource buffer, int packedLight) {
		if (entity.tickCount < 2) {
			return;
		}
		ItemStack stack = entity.getItemStack();
		if (stack.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.2D, 0.0D);
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTicks) * 20.0F));
		// 唯一绘制调用：物品外观（FIXED 姿态）+ 世界光照，没有第二个模型
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
			packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
		poseStack.popPose();
	}
}
