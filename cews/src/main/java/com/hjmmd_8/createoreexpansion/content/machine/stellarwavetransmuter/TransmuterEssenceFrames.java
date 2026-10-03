package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * <b>「机器正上方一格的物品展示框 ⇒ 魔素」的唯一读取入口</b>
 * （2026-10-03 需求 <b>cews-ess</b> §3.2）。
 *
 * <p><b>职责切分</b>：本类只管<b>世界读取</b>（哪一格、那格上站着谁、它手里拿着什么），
 * "物品 → 魔素"的规则表在 {@link TransmuterEssence}（纯映射，不碰世界）。
 * 两者各自只有一个变化理由，调用方也只需要认识 {@link #resolve} 一个名字。</p>
 *
 * <p><b>⚠ 展示框是实体，不是方块</b>：{@code ItemFrame} 继承
 * {@code HangingEntity}，世界读取必须走<b>实体查询</b>（{@link Level#getEntitiesOfClass}）。
 * 写成 {@code level.getBlockEntity(pos.above())} 会<b>永远返回 null</b>——而那是静默的：
 * 解析恒为"没有魔素"⇒ 攻击态永远不点燃，表现成"这个功能没生效"而不是报错。</p>
 *
 * <p><b>普通框与发光框一次覆盖</b>：1.21.1 的发光展示框是 {@code GlowItemFrame}，
 * 而它<b>继承 {@link ItemFrame}</b> ⇒ {@code instanceof ItemFrame} 一类通吃，
 * <b>不需要</b>（也不该）为两种框写两条分支。作者裁定的"普通框与发光框都算"因此是结构自带的。</p>
 *
 * <p><b>只看正上方一格</b>（作者裁定）：查询盒取该格本身，再用
 * {@link ItemFrame#blockPosition()} 复核归属——AABB 查询负责"看得到谁"，
 * "到底站没站在那一格"由复核收口（实体碰撞箱与格子边界不完全重合）。</p>
 *
 * <p><b>不做缓存</b>：每发波点燃时重新解析一次，所以<b>换掉框里的物品下一发波立刻生效</b>，
 * 不必重放机器。解析是机器级的（一个框对应一种魔素）⇒ 调用方在波的循环<b>外</b>做一次，
 * 不要塞进每枚波的循环里。</p>
 *
 * <p><b>服务端契约</b>：唯一调用点是 {@link TransmuterMode#applyField}，它只在服务端跑
 * （客户端不跑场，见 {@code StellarWaveTransmuterBlockEntity}）⇒ 本类不需要自己判端。</p>
 */
public final class TransmuterEssenceFrames {

	private TransmuterEssenceFrames() {
	}

	/**
	 * 读取机器正上方一格展示框里的物品，解析成魔素。
	 *
	 * @param level 世界（服务端；见类注释的契约）
	 * @param pos   变器方块位置
	 * @return 命中的魔素；<b>没有展示框 / 空框 / 框里物品认不出来 ⇒ {@code null}</b>
	 *         （调用方据此<b>不点燃</b>，见 {@link TransmuterMode} 的 {@code ATTACK.applyField}）
	 */
	@Nullable
	public static WaveTrailStyle resolve(Level level, BlockPos pos) {
		ItemFrame frame = frameAbove(level, pos);
		return frame == null ? null : TransmuterEssence.resolve(frame.getItem());
	}

	/**
	 * 站在机器正上方那一格上的展示框（没有则 {@code null}）。
	 *
	 * <p>内容来源是 {@link ItemFrame#getItem()}（框里的物品），<b>与物品的旋转角度无关</b>——
	 * 作者裁定只看内容。</p>
	 */
	@Nullable
	private static ItemFrame frameAbove(Level level, BlockPos pos) {
		BlockPos above = pos.above();
		for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(above)))
			if (frame.blockPosition()
				.equals(above))
				return frame;
		return null;
	}
}
