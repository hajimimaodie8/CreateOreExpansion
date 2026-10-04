package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * <b>动力角磨床每 tick 的加工流程</b>（2026-10-05 行为零变化拆分，从
 * {@code PowerAngleGrinderBlockEntity#tick()} 的 {@code super.tick()} <b>之后那一整段</b>与
 * {@code consumeInputNoOutput} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这一 tick 该不该加工、加工推进多少、什么时候交配方、
 * 什么时候把输入吞掉</b>。三道门（合盖 / 装轮且转速够 / 转速非 0）、{@code remainingTime} 的
 * 递减与两条收尾路径的顺序都逐行照搬，<b>时机一个字未动</b>；配方集合怎么解析、产出怎么落库
 * 不在这里（那是 {@link GrinderRecipeRunner} 的事）。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有三类 —— {@code private} → 包级私有、宿主成员改写成
 * {@code host.} 限定（{@code inventory} / {@code getBlockState()} / {@code getWheelTier()} /
 * {@code getSpeed()} / {@code start(..)} / {@code spawnParticles(..)} / {@code sendData()}），
 * {@code BlockEntity} 的 <b>protected</b> 字段 {@code level} 改走公开谓词 {@code getLevel()}
 * （跨顶层类不可见）。{@code super.tick()} 刻意留在方块实体里（与回旋镖那次拆分同形：
 * 基类在每个 tick 里补的是它自己的记账），所以宿主的 {@code tick()} 仍然只有
 * "{@code super.tick();} + 交给本类接手"。</p>
 */
final class GrinderProcessing {

	private GrinderProcessing() {
		throw new AssertionError("This class should not be instantiated");
	}

	/** 一个服务端/客户端 tick 的加工阶段（{@code super.tick()} 之后）。 */
	static void tick(PowerAngleGrinderBlockEntity host) {
		// 合盖时才能加工
		if (host.getBlockState().getValue(PowerAngleGrinderBlock.OPEN))
			return;
		// 必须安装角磨轮且转速达到该等级最低要求才能加工
		GrindingWheelTier tier = host.getWheelTier();
		if (tier == null || Math.abs(host.getSpeed()) < tier.getMinRpm())
			return;
		if (host.getSpeed() == 0)
			return;

		if (host.inventory.remainingTime == -1) {
			// 仅槽 0 有输入才启动加工（成品区槽 1+ 有成品不影响新输入）
			if (!host.inventory.getStackInSlot(0)
				.isEmpty() && !host.inventory.appliedRecipe)
				host.start(host.inventory.getStackInSlot(0));
			return;
		}

		float processingSpeed = Mth.clamp(Math.abs(host.getSpeed()) / 24, 1, 128);
		host.inventory.remainingTime -= processingSpeed;

		if (host.inventory.remainingTime > 0)
			host.spawnParticles(host.inventory.getStackInSlot(0));

		if (host.inventory.remainingTime < 5 && !host.inventory.appliedRecipe) {
			if (host.getLevel().isClientSide)
				return;
			if (GrinderRecipeRunner.applyRecipe(host)) {
				host.inventory.appliedRecipe = true;
				host.inventory.recipeDuration = 20;
				host.inventory.remainingTime = 20;
				host.sendData();
			} else {
				// 无匹配配方（或结果为空）：消耗 1 个输入但不产出（物品一点一点减少，不瞬间消失）
				consumeInputNoOutput(host);
			}
			return;
		}

		if (host.inventory.remainingTime > 0)
			return;
		host.inventory.remainingTime = 0;

		// 加工完成：成品已存入槽 1+（insertToOutput），立即重置并继续加工槽 0 剩余输入。
		// 成品区 32 格可堆叠、漏斗可随时抽取（见 GrinderInventory.extractItem），
		// 无需阻塞等待成品被抽走（无漏斗时也能连续加工，槽满时 insertToOutput 会掉落不丢失）
		if (host.inventory.appliedRecipe) {
			host.inventory.remainingTime = -1;
			host.inventory.appliedRecipe = false;
			host.sendData();
			return;
		}

		// 无匹配配方（不可加工物品）：消耗 1 个输入但不产出，保留成品区（槽 1+）
		consumeInputNoOutput(host);
	}

	/** 消耗 1 个输入但不产出任何物品（无匹配配方：物品逐个减少，加工节奏/粒子正常，不瞬间消失） */
	static void consumeInputNoOutput(PowerAngleGrinderBlockEntity host) {
		ItemStack input = host.inventory.getStackInSlot(0);
		input.shrink(1);
		if (input.isEmpty())
			host.inventory.setStackInSlot(0, ItemStack.EMPTY);
		host.inventory.remainingTime = -1;
		host.sendData();
	}
}
