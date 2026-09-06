package com.hjmmd_8.createoreexpansion.compat.jei.animation;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * JEI 分类动画：翡翠应力充能器（发射头朝下，蓄力收缩 → 弹出发射，循环）。
 *
 * <p>布局仿冲压机（AnimatedPress）：传动轴 + 机身 + 发射头（shutter）三层；
 * 发射头在蓄力期缓慢向机身内收缩，发射瞬间弹簧式弹出，随后开始下一轮循环。</p>
 *
 * <p>机型可覆写（{@link #machineState()}/{@link #axisModel()}/{@link #shutterModel()}）：
 * 蓝宝石充能器（{@link AnimatedSapphireCharger}）用于 4/5 级（超载/终极）配方动画。</p>
 *
 * <p><b>传动轴渲染铁律（已踩坑，勿再改）</b>：</p>
 * <ul>
 *     <li>竖直轴：用沿 Y 的轴模型 + {@code rotateBlock(0, angle, 0)} —— 与 Create
 *         {@code AnimatedPress} 的 {@code shaft(Axis.Z) + rotateBlock(0, 0, angle)} 完全对称；</li>
 *     <li><b>禁止</b>用 SHAFT_HALF partial 模型 + {@code rotateBlock(90, ...)} 试图"先转 X 竖直再转 Y"：
 *         GuiRenderBuilder 的旋转矩阵顺序是 Rz·Rx·Ry（顶点先受 Ry 作用），
 *         rotateBlock(90, angle, 0) 实际等价于绕 Z（垂直屏幕）转，且 rotationOffset 偏移会鬼畜；</li>
 *     <li>规则：<b>轴的方向由轴模型自身决定，rotateBlock 只传该轴对应的那个旋转参数</b>：
 *         轴沿 Y → rotateBlock(0, angle, 0)；轴沿 Z → rotateBlock(0, 0, angle)；轴沿 X → rotateBlock(angle, 0, 0)；</li>
 *     <li>轴位置：机身 FACING=DOWN（blockstate xRot=180）是<b>绕方块中心 (8,8,8)</b> 旋转的，
 *         翻转后机身占据模型 y 3~16、传动轴接口在 <b>y=16</b>（不是 y=0！）。
 *         轴用 {@link AllPartialModels#CHARGER_AXIS}（模型 <b>y 12~16</b>）在接口下方伸出 ——
 *         轴与机身同坐标空间，位置物理确定，不依赖任何 GUI 方向；短轴 4px 防止穿模。</li>
 * </ul>
 */
public class AnimatedJadeCharger extends AnimatedKinetics {

	/** 循环周期（tick，按 JEI 渲染帧算）：2.4 秒一循环，其中蓄力 1.8 秒、弹出 0.3 秒、间隔 0.3 秒 */
	private static final int CYCLE = 48;
	/** 蓄力时长（tick） */
	private static final int CHARGE = 36;
	/** 弹出时长（tick） */
	private static final int POP = 6;

	/** 方块状态档位（MODE）：1=低能量、2=高能量、3=伽马能量。由
	 * {@link com.hjmmd_8.createoreexpansion.compat.jei.subcategory.ChargingAssemblySubCategory}
	 * 按配方充能等级设置，默认 3（伽马，蓝色）。 */
	public int mode = 3;

	/** 机身方块状态（本机 = 翡翠充能器；蓝宝石子类覆写换机型）。 */
	protected BlockState machineState() {
		return AllBlocks.JADE_STRESS_CHARGER.getDefaultState()
			.setValue(JadeStressChargerBlock.FACING, Direction.DOWN)
			.setValue(JadeStressChargerBlock.MODE, mode);
	}

	/** 传动轴短轴 partial（本机 = 翡翠；蓝宝石子类覆写）。 */
	protected PartialModel axisModel() {
		return AllPartialModels.CHARGER_AXIS;
	}

	/** 发射头 shutter partial（本机 = 翡翠；蓝宝石子类覆写）。 */
	protected PartialModel shutterModel() {
		return AllPartialModels.CHARGER_SHUTTER;
	}

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack matrixStack = graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(xOffset, yOffset, 200);
		matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
		// 与 Create 注液（AnimatedSpout）/部署（AnimatedDeployer）一致的标准缩放
		int scale = 20;

		// 传动轴：短轴（沿 Y、4px，模型 y 12~16，接口 y=16 下方），从机身底部接口下方伸出 —— 与真实放置一致。
		// 旋转规则与 Create AnimatedPress 的 shaft(Axis.Z)+rotateBlock(0,0,angle) 完全对称，旋转绝对正确；
		// 用短轴替代全尺寸 shaft（16px）防止传动轴过长穿模
		blockElement(axisModel())
			.rotateBlock(0, getCurrentAngle(), 0)
			.scale(scale)
			.render(graphics);

		// 机身：发射头朝下（FACING=DOWN，blockstate 模型自带 xRot=180 翻转，绕方块中心旋转，
		// 翻转后机身占据模型 y 3~16、传动轴接口在 y=16 —— 轴模型 y 16~20 正好从接口伸出）
		blockElement(machineState())
			.scale(scale)
			.render(graphics);

		// 发射头（shutter）：顶面模型翻转朝下，蓄力期向上收缩、发射瞬间向下弹出。
		// 静态上移 0.5px（atLocal 正 Y=屏幕向下，负值=上移），让发射头与粒子轨迹更好对齐
		blockElement(shutterModel())
			.rotateBlock(180, 0, 0)
			.atLocal(0, getShutterOffset() - 0.5f / 16f, 0)
			.scale(scale)
			.render(graphics);

		// 置物台：仿 Create 注液/部署分类（AnimatedSpout/AnimatedDeployer），
		// 在机器正下方画置物台，表示加工物品放置在置物台上被充能器加工。
		// 竖直位置由下方 atLocal 的 Y 值控制（正=向下）：1.6 偏上，2.2 偏下，Create 默认 2.0
		blockElement(com.simibubi.create.AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2.0, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.popPose();
	}

	/** 发射头位移（GUI 局部 Y，正=向上收缩、负=向下弹出；单位像素折算为 JEI 比例） */
	private float getShutterOffset() {
		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % CYCLE;
		float px = 1f / 16f;

		if (cycle < CHARGE) {
			// 蓄力：向机身内（-FACING = 向上）线性收缩 1.5 像素
			float t = cycle / CHARGE;
			return 1.5f * px * t;
		}
		if (cycle < CHARGE + POP) {
			// 弹出：从收缩位 +1.5px 向下冲出到 -1.5px（弹簧）
			float t = (cycle - CHARGE) / POP;
			if (t < 0.35f)
				return px * (1.5f - (t / 0.35f) * 2.5f);
			if (t < 0.55f)
				return px * (-1f - ((t - 0.35f) / 0.2f) * 0.5f);
			return px * (-1.5f + ((t - 0.55f) / 0.45f) * 1.5f);
		}
		// 间隔：回到原位
		return 0;
	}

}
