package com.hjmmd_8.createoreexpansion.compat.jei.transmutation;

import com.hjmmd_8.createoreexpansion.compat.jei.base.ProcessingViaFanCategory;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationFluids;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;

/**
 * <b>TRANS（机械嬗化学）的 JEI 配方类别渲染</b>（催化剂 = 嬗变液源）。
 *
 * <p><b>P3t</b>：本类改读本层声明 {@link TransmutationFluids#TRANSMUTATION_FLUID}
 * （原来是集成层别名 {@code common.hub.AllFluids}）。取的还是同一个 {@code FluidEntry}、
 * 同一个 {@code getSource()}，渲染结果一字未变；差别只在"层文件不再 import 集成层"。</p>
 *
 * <p><b>为什么本包算 TRANS</b>：{@code compat/jei/coe/} 与 {@code compat/jei/cews/} 早在 P3r
 * 就各有一条层归属规则（{@code Get-FileLayer}），TRANS 这一条当时漏了，于是本类被当成 SHARED
 * 路径上的类——{@code TransmutationJeiCategories}（TRANS）import 它就成了 TRANS 的一个 blocker。
 * P3t 在 {@code tools/check-layering.ps1} 与 {@code tools/layer-usage.ps1} 里补上同一条规则
 * （三个兄弟子树的规则文本逐字对齐，没有为任何名字开洞）：本包本来就只装 TRANS 的 JEI 类别，
 * 名字与归属一致。</p>
 */
public class TransmutingCategory extends ProcessingViaFanCategory.MultiOutput<AllTransmutingRecipe> {

	public TransmutingCategory(Info<AllTransmutingRecipe> info) {
		super(info);
	}

	@Override
	protected void renderAttachedBlock(GuiGraphics graphics) {
		GuiGameElement.of(TransmutationFluids.TRANSMUTATION_FLUID.get().getSource())
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(AnimatedKinetics.DEFAULT_LIGHTING)
			.render(graphics);
	}

}
