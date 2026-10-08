package com.hjmmd_8.createoreexpansion.content.grinding.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>角磨床盖侧占位方块的方块实体</b>——唯一职责是<b>孤儿自清</b>。
 *
 * <h2>为什么需要一个方块实体（而不是靠方块自己的 tick）</h2>
 * <p>需要"逐 tick 在<em>占位方块自己那一格</em>上跑一段逻辑"，只有两条原版路：
 * ① 方块实体 ticker；② 自己给自己 {@code scheduleTick} 再在 {@code Block#tick} 里续期。
 * 这里选 ①，因为本仓已有同款先例——{@code StressInjectorBlockEntity#tick} 的孤儿自愈
 * （闲置超宽限期就 {@code level.removeBlock(pos, false)} 自删），
 * 并且那条路已经在真机跑过一轮（注释里记着"在方块实体的 tick 里移除方块是安全的"：
 * {@code Level#tickBlockEntities} 用迭代器，而 {@code LevelChunk#removeBlockEntityTicker}
 * 只是把 ticker 重新绑到空实现，迭代器结构不变）。</p>
 *
 * <h2>它到底判什么</h2>
 * <p>唯一判据是 {@link PowerAngleGrinderBlock#isOpenCoverCell}
 * ——「我这一格是不是某台<b>已开盖</b>角磨床的盖板那一格」。是 ⇒ 续命（清零计数）；
 * 不是 ⇒ 计数，连续超过 {@link #ORPHAN_GRACE_TICKS} 就
 * {@code level.removeBlock(worldPosition, false)} 自删（无掉落、无物品形态）。</p>
 *
 * <h2>为什么会有孤儿（这一条是必须处理的红线）</h2>
 * <ol>
 *   <li><b>机器被拆掉</b>：正常路径上 {@link PowerAngleGrinderBlock#onRemove} 会当场把占位方块清掉
 *       （那是第一道）；本类只是第二道兜底；</li>
 *   <li><b>存档被外部工具改过</b>（地图编辑器、结构方块、备份还原、别的模组直写同一格）：
 *       机器没了、占位方块还在，这是 {@code onRemove} 看不见的形态；</li>
 *   <li><b>占位方块所在的区块单独载入</b>（玩家站在已载入区域的边缘，机器那一区块此刻没载入）：
 *       这时 {@code isOpenCoverCell} 会因为邻格没载入而返回 true（判不了 ⇒ 按"被认领"处理），
 *       所以<b>不会</b>被误删——宁可漏清一次，也绝不在机器还没载入的时候把它的占位方块删掉。
 *       邻格没载入时直接返回，还顺带避免了"在方块实体 tick 里同步载入邻区块"这种会把区块
 *       永久钉住的操作（{@code Level#getBlockState} 对未载入区块是会去载入的）。</li>
 * </ol>
 *
 * <h2>不持久化任何状态</h2>
 * <p>本类一个字段都不写进 NBT（{@code orphanTicks} 是纯内存计数）⇒ 存档里不会因为
 * 这个方块实体而多出任何数据，载入后从 0 重新计数。</p>
 *
 * @since 1.0.0
 */
public class GrinderCoverPlaceholderBlockEntity extends BlockEntity {

	/**
	 * 连续多少 tick 没人认领就判定为孤儿并自删（20 tick = 1 秒）。
	 *
	 * <p>给足宽限期的理由是"跨区块载入顺序"：占位方块与它服务的角磨床可能落在相邻两个区块里，
	 * 两边的方块实体不一定在同一 tick 被登记进 tick 列表。1 秒远长于任何一次相邻区块载入，
	 * 而真正的孤儿多等 1 秒毫无代价。</p>
	 */
	private static final int ORPHAN_GRACE_TICKS = 20;

	/** 连续未被人认领的 tick 数（纯内存，不落盘）。 */
	private int orphanTicks;

	public GrinderCoverPlaceholderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 服务端每 tick 一次：被认领就续命，否则计数到点自删。 */
	void serverTick() {
		if (level == null || level.isClientSide) {
			return;
		}
		if (PowerAngleGrinderBlock.isOpenCoverCell(level, worldPosition)) {
			orphanTicks = 0;
			return;
		}
		if (++orphanTicks > ORPHAN_GRACE_TICKS && !isRemoved()) {
			level.removeBlock(worldPosition, false);
		}
	}
}
