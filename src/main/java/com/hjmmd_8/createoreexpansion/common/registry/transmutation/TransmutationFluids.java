package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.content.transmuting.block.TransmutationFluidBlock;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluid;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.Tags;

/**
 * <b>TRANS（机械嬗化学）层的流体声明</b>（P3s：从 {@code common/hub/AllFluids} 搬来）。
 *
 * <p><b>为什么搬</b>：{@code transmutation_fluid} 的整条实现链
 * （{@link TransmutationFluid}、{@link TransmutationFluidBlock}）本来就住在 TRANS 层
 * （{@code content/transmuting/**}），声明却挂在集成层的 {@code common/hub/AllFluids} 里——
 * 这正是 AGENTS「破环的设计规则」说的「层自己的东西住在聚合入口里」。
 * 按 P3c/P3k 的既有形状，声明回到本层，集成入口退化成<b>同名转发别名</b>，
 * 于是三个调用点（COE 的 {@code MedallionClientHandler}/{@code MedallionEffectHandler}
 * 与 TRANS 的 {@code TransmutationEventHandler}，外加 JEI 的 {@code TransmutingCategory}）
 * 只改 import 一行，字段名与用法一字未动。</p>
 *
 * <p><b>注册 id 与命名空间零变化</b>：流体名 {@code transmutation_fluid}、贴图路径
 * {@code block/transmutation_fluid_still|flowing}、{@code .lang(...)} 文案、
 * 桶标签都在下面这段逐字保留的链式调用里，所以
 * {@code createoreexpansion:transmutation_fluid} 与
 * {@code createoreexpansion:transmutation_fluid_bucket} 都还是老 id。</p>
 *
 * <p><b>注册时机零变化</b>：真正的触发仍是 {@code CreateOreExpansion} 构造器里的
 * {@code AllFluids.register()}（原第 140 行的位置与顺序都没动），它转发到
 * {@link #register()}；而 Registrate 的 {@code REGISTRATE} 实例由
 * {@code CoeRegistrate} 静态块初始化，与拆分前同一个（命名空间恒为
 * {@link CoeCore#REGISTRY_NAMESPACE}）。</p>
 */
public final class TransmutationFluids {

	/**
	 * 嬗变液（TRANS 层唯一的流体）。
	 *
	 * <p>方法体与拆分前 {@code common/hub/AllFluids#TRANSMUTATION_FLUID} 逐字相同，
	 * 只把两个实现类的 import 换成了搬迁后的包名。</p>
	 */
	public static final FluidEntry<TransmutationFluid.Flowing> TRANSMUTATION_FLUID =
		CoeRegistrate.REGISTRATE.fluid("transmutation_fluid",
				ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "block/transmutation_fluid_still"),
				ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "block/transmutation_fluid_flowing"),
				CreateRegistrate::defaultFluidType,
				TransmutationFluid.Flowing::new)
			.lang("Transmutation Fluid")
			.properties(b -> b.viscosity(6000).density(3000).temperature(1300).lightLevel(8))
			.fluidProperties(p -> p.levelDecreasePerBlock(2).tickRate(10).slopeFindDistance(3).explosionResistance(100f))
			.source(TransmutationFluid.Source::new)
			.block(TransmutationFluidBlock::new)
			.build()
			.bucket()
			.tag(Tags.Items.BUCKETS)
			.build()
			.register();

	private TransmutationFluids() {
	}

	/** 注册触发（由 {@code common.hub.AllFluids#register} 转发，调用点与拆分前一致）。 */
	public static void register() {
	}

}
