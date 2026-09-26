package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.common.transmutation.TransmutationLink;
import com.hjmmd_8.createoreexpansion.content.transmuting.block.TransmutationFluidBlock;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluid;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
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
 * <p><b>注册时机零变化</b>：真正的触发仍是 {@code CreateOreExpansion} 构造器里的
 * {@code AllFluids.register()}（原第 140 行的位置与顺序都没动），它转发到
 * {@link #register()}；而 Registrate 的 {@code REGISTRATE} 实例由
 * {@code CoeRegistrate} 静态块初始化，与拆分前同一个（命名空间恒为
 * {@link CoeCore#REGISTRY_NAMESPACE}）。</p>
 *
 * <p><b>P3t：本类同时是 core 契约 {@link TransmutationLink} 的注入点</b>（见下方静态块与
 * {@link Link}）。原本 COE 层的两个凝能佩处理器直接 import 集成层的 {@code common/hub/AllFluids}、
 * {@code common/hub/AllModEffects} 去读这里的 {@code FluidType} 与嬗乱效果——那是
 * <b>禁止方向</b>（{@code COE -> TRANS}），也是 {@code layer-closure} 报表里最后两个 COE
 * blocker。现在它们改读 core 的契约，本层在自己的静态块里把实现注入进去：液体侧的写法变化只有
 * "谁去调 {@code .get()} 拿 FluidType"，语义（同一个 FluidType）一字未变；嬗乱侧的判据随后在
 * P3u 按用户裁定修正（见下方 {@code Link#isTransmutationDisorder} 的注释）。</p>
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

	// P3t：把本层的"嬗变液流体类型 + 嬗乱效果"注入 core 契约。放在这里而不是别处的理由：
	// ① 注入时机与本声明的初始化时机<b>完全相同</b>——都由根构造器那句 AllFluids.register()
	//    触发，所以契约可用的时刻不晚于拆分前这两个对象可用的时刻；
	// ② 实现体不在这里解引用任何 holder（只在被调用时才 get()），因此不会撞上
	//    「DeferredHolder 在 @Mod 构造期未绑定」那个坑。
	static {
		TransmutationLink.install(new Link());
	}

	private TransmutationFluids() {
	}

	/** 注册触发（由 {@code common.hub.AllFluids#register} 转发，调用点与拆分前一致）。 */
	public static void register() {
	}

	/**
	 * {@link TransmutationLink} 的 TRANS 侧实现：两个查询逐字对应旧的直读写法。
	 *
	 * <p>方法体故意与搬迁前的调用点保持同一形状（同一个 {@code getFluidTypeHeight} 判定、
	 * 同一处 {@code ==} 身份比较），只是把"读哪个声明"从 hub 别名换成了本层字段。</p>
	 */
	private static final class Link implements TransmutationLink {

		@Override
		public boolean isInTransmutationFluid(Entity entity) {
			return entity != null
				&& entity.getFluidTypeHeight(TRANSMUTATION_FLUID.get().getFluidType()) > 0.0D;
		}

		/**
		 * <b>P3u：判据已按用户裁定修正</b>。旧代码是
		 * {@code event.getEffectInstance().getEffect() == TRANSMUTATION_DISORDER.get()}——左边
		 * {@code Holder<MobEffect>}、右边 {@code MobEffect}，编译能过（非 final 类可转型成接口）
		 * 却<b>恒为 {@code false}</b>，所以凝能佩只拦得住"接触嬗变液"那一路。现在去掉右侧的
		 * {@code .get()}，改成比较同一个 {@code Holder}：{@code effect == TRANSMUTATION_DISORDER}
		 * ——本模组施放嬗乱的三个现场（{@code AllTransmutingType}、{@code TransmutationEventHandler}、
		 * 黄玉弓事件处理器）都是把<b>这个 {@code DeferredHolder} 本身</b>塞进
		 * {@code new MobEffectInstance(...)} 的，所以这个身份比较对它们成立。
		 * 根因说明见 {@link TransmutationLink} 与 {@code MedallionEffectHandler} 的类注释。
		 *
		 * <p>{@code effect != null} 是空值护栏：旧代码的左侧
		 * （{@code MobEffectInstance#getEffect()}）永不为 {@code null}，所以它不改变任何行为；
		 * 它挡住的是一个时序坑——{@code ==} 不会短路，而 {@code TRANSMUTATION_DISORDER}
		 * 是 {@code TransmutationEffects} 的静态字段，读它本身不碰 holder，但旧写法右侧那句
		 * {@code .get()} 会，在 {@code DeferredHolder} 尚未绑定的时刻（{@code @Mod} 构造期）
		 * 抛 {@code Trying to access unbound value}（P3t 的探针现场撞到过一次）。
		 * 改后右侧已不解引用 holder，这条护栏仍保留（契约的查询不该因为空入参爆掉）。</p>
		 */
		@Override
		public boolean isTransmutationDisorder(Holder<MobEffect> effect) {
			return effect != null && effect == TransmutationEffects.TRANSMUTATION_DISORDER;
		}
	}

}
