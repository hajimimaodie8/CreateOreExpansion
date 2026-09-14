package com.hjmmd_8.createoreexpansion.content.charger.craft;

import com.simibubi.create.content.fluids.transfer.GenericItemFilling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 变体波的<b>载荷注液</b>：把波自己携带的流体灌进命中容器里的"可装流体物品"（空桶、玻璃瓶…）。
 *
 * <p><b>为什么单列一类、且不属于"配方加工"</b>：Create 真机里"给空桶灌水"<b>不是配方</b>——
 * 19 条内置 {@code create:filling} 配方里<b>没有任何一条</b>以空桶为原料；真正让桶变成水桶的是
 * 流体喷口（spout）走的 {@link GenericItemFilling} 特判（借物品自身的
 * {@code Capabilities.FluidHandler.ITEM} 能力完成）。所以本模组那条"只认配方"的
 * 远程加工管线对空桶永远检索不到候选（实测日志：{@code 容器内容 [0]minecraft:bucket×1 … 0 条候选}），
 * 这不是门槛/类型集的缺陷，而是口径缺失：注液是<b>货载动作</b>，与"余料流体注进附近储罐"
 * （{@link WaveOutputPlacer#fillNearby}）同一层级，<b>不需要</b>携带任何加工机属性或配方类型。</p>
 *
 * <p><b>口径与真机对齐</b>：用量与成品一律<b>委托</b> Create 的 {@link GenericItemFilling}
 * 判定与执行（{@code getRequiredAmountForItem} / {@code fillItem}），本类只负责
 * "从哪个槽取、回哪个槽、扣多少载荷流体"这三件事——不自己复刻桶/瓶/药水/储罐的差异语义，
 * 免得与 Create 后续版本分叉。因此：空桶 1000 mB、玻璃瓶 250 mB，且载荷流体的种类必须
 * 在真机里也能灌（{@code FluidBucketWrapper} 分支要求该流体有对应的桶物品，
 * 例如熔融金属灌不进桶——真机同样灌不进）。</p>
 *
 * <p><b>来源只认载荷</b>：命中容器自身的流体槽不参与（那是"容器里已有的液体"，
 * 波没有理由把它倒进容器自己的物品里）；载荷流体不足一份用量则整条跳过，
 * 不做"半份灌进去"的部分注液（真机喷口同样是"够一份才灌"）。</p>
 *
 * <p><b>不消耗链数</b>：注液不占加工额度（{@code chainLeft} 只在真加工成功时 −1），
 * 否则"路过一箱空桶"会把整条波的加工额度吃光。命中容器路径的循环能自然收敛：
 * 每次成功注液至少要扣掉 1 mB 载荷流体，流体见底后本方法必然返回 false。</p>
 *
 * <p><b>OOP 分工</b>：实体状态（载荷流体）的读写只走 {@link WaveCraftConsumption}
 * （{@link WaveCraftConsumption#payloadFluid} 读、{@link WaveCraftConsumption#drainPayloadFluid} 扣），
 * 本类不直接摸实体字段；物品进出容器一律走 {@link WaveOutputPlacer}（回原位、放不下则掉落），
 * 与产物回位同一实现。</p>
 */
public final class WaveItemFluidFilling {

	/** 工具类，不允许实例化。 */
	private WaveItemFluidFilling() {}

	/**
	 * 尝试把载荷流体灌进命中容器里的一个可装流体物品（一次一件）。
	 *
	 * @param host    波实体侧的编排状态（载荷流体从 {@link WaveCraftExecutor.Host#consumption()} 读写）
	 * @param handler 命中容器的物品能力（只在本容器内取物品/回物品，不外扩）
	 * @param pos     命中方块位置（放不下时在该方块上方掉落成品）
	 * @return true = 真灌成了一件（载荷流体已扣）；false = 无处可灌/流体不足/容器已变
	 */
	public static boolean fillOne(WaveCraftExecutor.Host host, IItemHandler handler, BlockPos pos) {
		if (handler == null)
			return false;
		Level level = host.level();
		if (level == null || level.isClientSide)
			return false;

		// 载荷流体只读视图（副本）：判断用；真扣减走 drainPayloadFluid，保持"单一写入口"
		FluidStack payload = host.consumption()
			.payloadFluid();
		if (payload.isEmpty())
			return false;

		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack probe = stack.copy();
			probe.setCount(1); // 一份的量：注液按件处理（真机喷口也是一次处理一件）

			// 用量判定（无副作用：Create 内部只用 SIMULATE / 读组件）；
			// 载荷流体不足一份 → 跳过本槽（不做部分注液）
			int required = GenericItemFilling.getRequiredAmountForItem(level, probe, payload);
			if (required <= 0 || required > payload.getAmount())
				continue;

			// 先算出成品再动容器：成品为空（能力缺失/流体无对应桶物品）则本槽无可灌
			FluidStack available = payload.copyWithAmount(required); // fillItem 会就地扣掉它，故用副本
			ItemStack filled = GenericItemFilling.fillItem(level, required, probe, available);
			if (filled.isEmpty())
				continue;
			int used = required - available.getAmount(); // 实际耗量（Create 未扣完时按实际记）
			if (used <= 0)
				continue;

			// 真取 1 件（并发变化取空则本槽跳过，此时尚未扣任何流体 → 无损失）
			ItemStack taken = handler.extractItem(slot, 1, false);
			if (taken.isEmpty())
				continue;

			// 成品回原槽；放不下（槽被其它物品占满等）则掉在命中方块上方，绝不凭空消失
			ItemStack leftover = WaveOutputPlacer.insertBack(handler, slot, filled);
			if (!leftover.isEmpty())
				WaveOutputPlacer.dropAtBlock(level, pos, leftover);

			int drained = host.consumption()
				.drainPayloadFluid(used);
			host.trace()
				.log("注液：槽 {} 的 {} ×1 + {} mB {} → {}（实扣 {} mB，载荷流体余 {} mB）", slot, probe.getItem(),
					required, BuiltInRegistries.FLUID.getKey(payload.getFluid()), filled.getItem(), drained,
					host.consumption()
						.payloadFluid()
						.getAmount());
			return true;
		}
		return false;
	}
}
