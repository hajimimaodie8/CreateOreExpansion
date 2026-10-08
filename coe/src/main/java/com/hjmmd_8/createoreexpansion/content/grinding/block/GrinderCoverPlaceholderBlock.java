package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * <b>角磨床盖侧占位方块</b>（{@code createoreexpansion:grinder_cover_placeholder}）——
 * 「开盖时，盖那一格被判成一个真正的方块」这件事的载体。
 *
 * <h2>它解决的问题（作者 2026-10-07 原话）</h2>
 * <p>用户原话：「咱就不能在它开盖的时候，把盖身的那个部分判定成一个方块吗？到时候虽然说不能把它
 * 显示为碰撞箱，但是你可以把它判定成一个方块，这样还用考虑什么活塞推动流体流动的问题？
 * 这不就直接解决了吗？到时候你把这个改成"活塞不能推动这个方块"，这不就 OK 了吗」</p>
 * <p>盖不是独立方块（{@link PowerAngleGrinderBlock#OPEN} 只换模型），方块只占自己那一格、碰撞形状
 * 也不伸到邻格 ⇒ 开盖后盖那一格原本是<b>纯 AIR</b>。批 21 的做法是在放置事件上取消，作者判定
 * 「不那么体面」（放下去又被删掉）。本方块就是那句话的直译：那一格<b>真的有方块</b>，
 * 于是放置 / 活塞 / 流体三件事一次解决，不再需要任何事件取消。</p>
 *
 * <h2>为什么"既看不见又不新增贴图"（美术红线：一张图都不许改、也不许自己画）</h2>
 * <p>照抄本仓既有先例 {@code content/equipment/armor/field/StressInjectorBlock}
 * （内部应力注入器，同款"完全不渲染"要求，已过资产/自足性关卡），三条<b>原版机制</b>：</p>
 * <ol>
 *   <li>{@link #getRenderShape} 返回 {@link RenderShape#INVISIBLE} ——
 *       {@code BlockRenderDispatcher} 只在 {@code MODEL} 时才去取模型 ⇒
 *       <b>方块本体一个像素都不画</b>，也不进渲染管线（与"透明贴图"的区别就在这里）；</li>
 *   <li>{@link #getShape} 返回 {@link Shapes#empty()} —— 玩家视线的射线检测
 *       （{@code ClipContext.Block.OUTLINE} 取的就是 {@code BlockState#getShape}）
 *       <b>打不到它</b> ⇒ 连"选中描边"那圈黑线框都不会出现，玩家既看不到也瞄不到，
 *       更不会在 Jade 之类的悬浮提示里读到它的名字。这条同时是「不挡交互」的构造性保证：
 *       玩家对着角磨床盖侧的面右键，射线命中的永远是角磨床本体；</li>
 *   <li>方块状态指向<b>原版</b> {@code minecraft:block/air}（那个模型文件内容就是空的
 *       {@code {}}、零贴图、零贴图引用）⇒ <b>不新增任何贴图、也不新增任何模型文件</b>。</li>
 * </ol>
 *
 * <h2>属性逐条（注册点见 {@code CoeMachines}；全部以 1.21.1 源码为准）</h2>
 * <ul>
 *   <li>{@code noCollission()}（⚠ 方法名拼写就是 <b>noCollission</b>，双 s 单 l）+
 *       {@code noOcclusion()}：无碰撞、不遮蔽邻面 ⇒ 不挡玩家移动（玩家可以站在那一格里，
 *       不会像普通方块那样被顶出去）；</li>
 *   <li><b>{@code forceSolidOn()} —— 这一条是"挡住流体"的<b>唯一</b>关键</b>，见下面「流体」一节；</li>
 *   <li>{@code pushReaction(PushReaction.BLOCK)}：活塞推不动、也拉不动（作者点名）；</li>
 *   <li>{@code noLootTable()}：{@code Block#getLootTable() == BuiltInLootTables.EMPTY}
 *       ⇒ Registrate 的战利品回调被跳过、{@code BlockLootSubProvider} 也跳过它
 *       ⇒ <b>一个战利品表文件都不生成</b>，被挖/被炸都不掉任何东西；</li>
 *   <li>{@code instabreak()}（{@code strength(0.0F)}）—— ⚠ <b>刻意不给 {@code -1}</b>：
 *       {@code PistonBaseBlock#isPushable} 会先看 {@code getDestroySpeed() == -1.0F}
 *       而直接返回 false，那样"活塞推不动"就变成"因为不可破坏"而不是"因为
 *       {@link net.minecraft.world.level.material.PushReaction#BLOCK}"，
 *       {@code PushReaction} 这一条也就失去了可验证性。{@code instabreak} 让
 *       <b>{@code PushReaction.BLOCK} 成为唯一决定性屏障</b>；</li>
 *   <li>{@code soundType(SoundType.EMPTY)}：被爆炸清掉时零音效，不留听觉痕迹；</li>
 *   <li><b>不调 {@code replaceable()}</b>：默认即 {@code replaceable == false}
 *       ⇒ {@code BlockState#canBeReplaced()} 恒 false ⇒ 那一格<b>不接受任何放置</b>
 *       （空手 / 手持方块 / 潜行 / 其它模组走原版放置链一律一样，见下面「放置」一节）。
 *       ⚠ 一旦打开它，整个修法当场失效，所以它是负向断言的主角之一；</li>
 *   <li><b>没有 {@code .item()}</b>：没有 {@code BlockItem} ⇒ 不进背包、不进创造页、
 *       不进 JEI，创造页搜索里也搜不到（Registrate 只把<b>物品</b>填进创造页）。</li>
 * </ul>
 *
 * <h2>为什么能挡住活塞（1.21.1 实核）</h2>
 * <p>{@code PistonBaseBlock#isPushable}（{@code PistonBaseBlock.java:286-293}）：
 * {@code switch (state.getPistonPushReaction()) { case BLOCK: return false; ... }}；
 * 而 {@code PistonStructureResolver}（{@code :47} / {@code :72} / {@code :90} / {@code :130}）
 * 每一次推进都先问同一个 {@code isPushable(.., allowDestroy=false, ..)} ⇒ 解析失败 ⇒
 * 活塞<b>根本不伸出</b>（不是"推一下又弹回来"）。粘性活塞回拉那一支
 * （{@code :237-248}）看到不可推也只在原位收头，占位方块留在原地不动。</p>
 *
 * <h2>为什么能挡住流体 —— ⚠ 这条必须靠 {@code forceSolidOn()}，不能只靠"有方块"</h2>
 * <p>{@code FlowingFluid#canSpreadTo}（{@code FlowingFluid.java:427-440}）只有三道门，
 * 而对这个方块<b>只剩最后一道有效</b>：</p>
 * <pre>
 * toFluidState.canBeReplacedWith(..)   // 目标格的流体态是 EMPTY，EmptyFluid 覆写为恒 true
 *   &amp;&amp; canPassThroughWall(..)          // 双方 getCollisionShape 都是空 ⇒ isOccludes=false ⇒ true
 *   &amp;&amp; canHoldFluid(..)               // return !state.blocksMotion();   &lt;&lt;&lt; 唯一闸门
 * </pre>
 * <p>而 {@code blocksMotion()} → {@code isSolid()} → {@code legacySolid} ←
 * {@code BlockBehaviour.BlockStateBase#calculateSolid()}（{@code BlockBehaviour.java:535-551}）
 * 的第一句就是 {@code if (this.owner.properties.forceSolidOn) return true;}，否则才去看
 * {@code cache.collisionShape} —— 而 {@code noCollission()} + 空形状会让那个形状为空、
 * 于是返回 <b>false</b> ⇒ {@code canHoldFluid} 为 true ⇒ <b>流体照样灌进来</b>。
 * 也就是说：「是方块就挡流体」这句话在 1.21.1 上<b>不成立</b>，
 * 必须显式声明 {@code forceSolidOn()} 才成立。</p>
 * <p>这正是原版的官方配方：原版<b>悬挂告示牌</b>就是
 * {@code forceSolidOn().noCollission()}（{@code Blocks.java:1815} / {@code :1822}）；
 * 而 {@code minecraft:structure_void} 走的是另一条路（{@code canHoldFluid} 里被单独豁免，
 * {@code FlowingFluid.java:421}），它同时还是 {@code replaceable()} +
 * {@code pushReaction(DESTROY)}，所以<b>不能</b>拿原版方块来当这个占位。</p>
 * <p>{@code forceSolidOn()} 的副作用已逐条核过：只影响 {@code isSolid()}
 * （连带 {@code blocksMotion()} / {@code BlockState#canBeReplaced(Fluid)} /
 * {@code isPossibleToRespawnInThis} / 若干世界生成与告示牌附着判据），<b>不含</b>碰撞
 * ——{@code noCollission()} 让 {@code getCollisionShape} 恒为空，
 * {@code isSuffocating} 的默认式 {@code blocksMotion() && isCollisionShapeFullBlock(..)}
 * 仍为 false ⇒ 不窒息、不挡视线。</p>
 *
 * <h2>为什么能挡住放置（1.21.1 实核）</h2>
 * <p>{@code BlockItem#place}（{@code BlockItem.java:57-61}）第二句就是
 * {@code else if (!context.canPlace()) return InteractionResult.FAIL;}；而
 * {@code BlockPlaceContext#canPlace()}（{@code BlockPlaceContext.java:55-57}）是
 * {@code replaceClicked || level.getBlockState(getClickedPos()).canBeReplaced(this)}。
 * 玩家对着一台开盖角磨床的盖侧面右键时，{@code replaceClicked} 为 false（角磨床不可替换），
 * 于是 {@code getClickedPos()} 是 {@code relativePos} = 盖那一格 = 本方块所在格，
 * 而 {@code BlockBehaviour#canBeReplaced(BlockState, BlockPlaceContext)}
 * （{@code BlockBehaviour.java:293-295}）第一项就是 {@code state.canBeReplaced()} = 本方块的
 * {@code replaceable} = <b>false</b> ⇒ 整个放置链在进入世界写入之前就返回 FAIL。
 * 玩家点别处（例如盖那一格下方的方块、朝上放）也一样：目标格算出来还是这一格，结论相同。
 * <b>空手、手持方块、潜行、其它模组走原版放置链</b>都走同一条判据。</p>
 *
 * <h2>为什么不挡光</h2>
 * <p>{@code BlockBehaviour#getLightBlock}（{@code BlockBehaviour.java:332-338}）：
 * {@code isSolidRender(..)} 为 false（{@code canOcclude} 被 {@code noCollission} /
 * {@code noOcclusion} 置 false），再看 {@code propagatesSkylightDown(..)}，其默认实现是
 * {@code !Block.isShapeFullBlock(state.getShape(..))} —— {@link #getShape} 返回空形状
 * ⇒ true ⇒ <b>光照阻挡恒为 0</b>，机器那一侧（以及穿过这一格的天空光）都不会变暗。</p>
 *
 * <h2>它跟批 21 的关系</h2>
 * <p>批 21 在 {@code BlockEvent.EntityPlaceEvent} 上取消放置；本方块一到位，那条事件
 * <b>根本不会再发</b>（{@code CommonHooks#onPlaceItemIntoWorld} 先跑
 * {@code itemstack.getItem().useOn(context)}，只有返回值
 * {@code consumesAction()} 为真时才发事件；放置已经在 {@code canPlace()} 处 FAIL
 * ⇒ 不发）⇒ 那条监听当场变成死代码，已随本批删除。所以这不是"换一种拦法"，
 * 而是<b>让被拦的那件事从一开始就不发生</b>。</p>
 *
 * <h2>生命周期</h2>
 * <p>放 / 清的唯一出处是 {@link PowerAngleGrinderBlock#syncCoverPlaceholder}；
 * 开盖放、关盖清、机器被拆时由
 * {@link PowerAngleGrinderBlock#onRemove} 清、机器还开着却被抹掉时由角磨床自己的
 * {@code tick} 补回来、机器不存在了的孤儿由
 * {@link GrinderCoverPlaceholderBlockEntity} 自己删。详见那几处的注释。</p>
 *
 * @since 1.0.0
 */
public class GrinderCoverPlaceholderBlock extends Block implements IBE<GrinderCoverPlaceholderBlockEntity> {

	public GrinderCoverPlaceholderBlock(Properties properties) {
		super(properties);
	}

	/**
	 * <b>完全不渲染</b>：{@code BlockRenderDispatcher} 只在
	 * {@code state.getRenderShape() == RenderShape.MODEL} 时才去取模型，
	 * {@link RenderShape#INVISIBLE} 直接跳过 ⇒ 不新增贴图、不新增模型也能做到"看不到任何东西"。
	 */
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	/**
	 * <b>空形状</b>：射线（{@code ClipContext.Block.OUTLINE} 取的就是它）打不到本方块
	 * ⇒ 没有选中描边、没有悬浮提示、也不抢角磨床本体的右键命中；
	 * 碰撞形状同样来自 {@code getShape}，因此"不挡玩家移动"也是这一条保证的
	 * （{@code noCollission()} 已经让 {@code getCollisionShape} 恒为空，这里是显式复述）。
	 *
	 * <p>⚠ 不要让这个形状变成非空：一旦能被射线打中，玩家右键到"那一格"就会改走本方块，
	 * 角磨床盖侧面的交互（空手右键取物/装轮）会被抢走，而且世界里会出现一个画不出来的
	 * 白色线框。</p>
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public Class<GrinderCoverPlaceholderBlockEntity> getBlockEntityClass() {
		return GrinderCoverPlaceholderBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GrinderCoverPlaceholderBlockEntity> getBlockEntityType() {
		return CoeBlockEntityTypes.GRINDER_COVER_PLACEHOLDER.get();
	}

	/**
	 * 方块实体 ticker（服务端）。
	 *
	 * <p><b>为什么这个方块需要方块实体</b>：唯一目的是<b>孤儿自清</b>——机器被拆掉/存档被外部
	 * 工具改过之后，世界里可能留着一个没有任何宿主认领的占位方块。占位方块自己每 tick 问一次
	 * {@link PowerAngleGrinderBlock#isOpenCoverCell}（四个水平邻格），没人认领满
	 * {@link GrinderCoverPlaceholderBlockEntity} 的宽限期就自己
	 * {@code removeBlock(pos, false)} 掉（无掉落）。</p>
	 *
	 * <p>不需要注册渲染器：本方块走 {@link RenderShape#INVISIBLE}，与
	 * {@code StressInjectorBlockEntity} 同款。</p>
	 */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
																 BlockEntityType<T> type) {
		return level.isClientSide ? null : (lvl, pos, st, be) -> {
			if (be instanceof GrinderCoverPlaceholderBlockEntity placeholder)
				placeholder.serverTick();
		};
	}
}
