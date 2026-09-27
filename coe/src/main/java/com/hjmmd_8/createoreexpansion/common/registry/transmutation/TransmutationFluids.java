package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluidBlock;
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
 * 按 P3c/P3k 的既有形状，声明回到本层，集成入口退化成<b>同名转发别名</b>。</p>
 *
 * <p><b>注册 id 与命名空间零变化</b>：流体名 {@code transmutation_fluid}、贴图路径
 * {@code block/transmutation_fluid_still|flowing}、{@code .lang(...)} 文案、
 * 桶标签都在下面这段逐字保留的链式调用里，所以
 * {@code createoreexpansion:transmutation_fluid} 与
 * {@code createoreexpansion:transmutation_fluid_bucket} 都还是老 id。</p>
 *
 * <p><b>注册时机（W6-b2 起）</b>：本类随嬗化整块搬进 {@code :coe}（包名逐字未变），
 * 所以触发点是 {@code CreateOreExpansion} 构造器里的 {@link #register()} ——
 * "谁的东西谁注册"这条口径没变，变的是"谁在场"：只装 {@code coe.jar} 时嬗变液照样注册，
 * 这正是 W5 A1「{@code c:buckets} 标签引用的桶不存在」那个缺陷消失的原因。
 * {@code REGISTRATE} 实例仍由 {@code CoeRegistrate} 静态块初始化（命名空间恒为
 * {@link CoeCore#REGISTRY_NAMESPACE}）。</p>
 *
 * <p><b>W6-b2：core 的嬗化窄契约已整类删除</b>（那个接口原住
 * {@code core/.../common/transmutation/}，W6-b2 连同它的 {@code NONE} / {@code install} /
 * 内部实现一起删掉；历史名称见 {@code build/patch/w6b2-EVIDENCE.txt}）。
 * 它（P3t 引入）存在的唯一理由是「COE 层的凝能佩处理器要读 TRANS 层的嬗变液 FluidType 与嬗乱
 * 效果，而 {@code COE -> TRANS} 是禁止方向」。嬗化机制进第一层之后这条边<b>本来就是同层</b>了
 * （{@code common.registry.transmutation} 与凝能佩处理器同住 {@code :coe}），窄契约随之失去
 * 存在意义——撤掉它同时消掉了「未注入 = false」的降级语义（单装 coe 时嬗乱照样生效）。
 * 两个调用点现在直连本类的 {@code TRANSMUTATION_FLUID}（见 {@code MedallionClientHandler} /
 * {@code MedallionEffectHandler}）。</p>
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

	/**
	 * 注册触发（W6-b2 起由 {@code CreateOreExpansion} 构造器直接调用——本类已随嬗化整块进
	 * {@code :coe}，见类注释"注册时机"）。方法体为空：Registrate 的注册动作就是上面的字段初始化。
	 */
	public static void register() {
	}

}
