package com.hjmmd_8.createoreexpansion.content.machine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * <b>本模组机器的统一交互契约</b>（用户 2026-09-15 定稿，全部机器一体适用）。
 *
 * <p><b>规则（用户原话整理）</b>：</p>
 * <ol>
 *   <li><b>空手右键某个面</b>：该面若有开口 → 开/关该开口；</li>
 *   <li><b>空手右键对应的指示灯</b>：只切换"那盏灯所对应的开口"；</li>
 *   <li><b>扳手右键</b>：机器<b>有</b>特殊切换模式 → 扳手<b>只</b>切模式（不动面、不旋转）；
 *       机器<b>没有</b>模式 → 扳手照样切开口开关（与空手同一个逻辑）；</li>
 *   <li><b>旋转</b>：一律服从 Create 常规（{@link IWrenchable} 的默认旋转），但本模组所有机器
 *       <b>必须按住 Ctrl + 手持扳手右键</b>才旋转 —— 由客户端拦截后走
 *       {@code MachineRotatePayload}，服务器校验并调用 {@link #rotateAsCreate}。</li>
 * </ol>
 *
 * <p><b>为什么"Ctrl"要客户端拦截</b>：Minecraft 的方块交互包不带修饰键，服务器读不到玩家是否按着 Ctrl
 * （只有潜行状态会同步）。所以 Ctrl 只能在客户端判定，命中时取消本地交互并发一个自定义包，
 * 由服务器执行旋转——这样"没按 Ctrl 时扳手绝不旋转"才是<u>服务端权威</u>的（而不是靠客户端自觉）。</p>
 *
 * <p><b>默认实现</b>：{@link #hasModeSwitch()} 默认 false（多数机器没有模式）；
 * {@link #onEmptyHandPortToggle} 默认 {@code PASS}（没有开口的机器例如三种充能器、调级器不需要实现它）。
 * 于是新机器的接入方式只有两件事：{@code implements CewsMachine}，在有开口时覆写
 * {@link #onEmptyHandPortToggle} ∪ 在 {@link IWrenchable#onWrenched} 里调用同一个开口逻辑。</p>
 */
public interface CewsMachine extends IWrenchable {

	/**
	 * 本机是否有"特殊切换模式"（例如波变器的 加工波变态 ↔ 攻击波变态、场控的 加速场 ↔ 偏转场、
	 * 角磨床的磨轮等级）。
	 *
	 * <p>有模式 ⇒ 扳手只做这一件事（不切开口、不旋转）；没有模式 ⇒ 扳手照旧切开口开关。</p>
	 */
	default boolean hasModeSwitch() {
		return false;
	}

	/**
	 * <b>默认：扳手右键什么都不做</b>（但吞掉交互）。
	 *
	 * <p><b>为什么是"什么都不做"而不是沿用 {@link IWrenchable} 的默认旋转</b>：本模组机器的旋转一律
	 * 走"Ctrl + 扳手"（客户端拦截 → {@link MachineRotatePayload}）；若这里保留默认实现，
	 * Create 会在<b>没按 Ctrl</b>时照样旋转，规则 ④ 就形同虚设。所以默认吞掉，
	 * 有开口的机器覆写它去切开口（规则 ③），有模式的机器覆写它去切模式。</p>
	 */
	@Override
	default InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.SUCCESS;
	}

	/**
	 * <b>Ctrl + 扳手 = 按 Create 常规旋转</b>。
	 *
	 * <p>直接复用 {@link IWrenchable} 的默认实现（{@code getRotatedBlockState} + 摆正 + 音效），
	 * <b>不复制任何旋转逻辑</b>——Create 以后调整旋转规则，本模组自动跟随。</p>
	 */
	default InteractionResult rotateAsCreate(BlockState state, UseOnContext context) {
		return IWrenchable.super.onWrenched(state, context);
	}

	/**
	 * <b>空手右键的开口开关</b>（规则 ①②）：实现方按自己的"面 / 指示灯分区"映射切换对应开口。
	 *
	 * <p>默认 {@code PASS} = 本机没有开口（充能器、调级器、波速调节器等），交给其它逻辑。</p>
	 *
	 * @param state 方块状态
	 * @param level 世界（实现方须自判 {@code isClientSide}，只改服务端）
	 * @param pos   方块位置
	 * @param player 玩家
	 * @param hitResult 命中结果（面 + 精确位置：指示灯分区要用）
	 * @return 已处理返回 {@code SUCCESS}；本机没有开口返回 {@code PASS}
	 */
	default InteractionResult onEmptyHandPortToggle(BlockState state, Level level, BlockPos pos, Player player,
		BlockHitResult hitResult) {
		return InteractionResult.PASS;
	}
}
