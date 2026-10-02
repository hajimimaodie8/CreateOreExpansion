package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * <b>应力注入器</b>（{@code createoreexpansion:stress_injector}）—— <b>不可获取的内部方块</b>。
 *
 * <h2>⚠ 它拿不到：这是用户 2026-10-01 最强调的一条</h2>
 * <p>用户原话："这个注入器方块，可千万千万不要真的把它注册成一个能够被获取的方块。
 * 这个玩意儿应该是非常隐藏的那种。" ⇒ 六条硬约束逐条落点（每条都能用命令取证，见
 * {@code tools/check-armor-sets.ps1} 第 23 节）：</p>
 * <ol>
 *   <li><b>没有合成配方</b>：datagen 里一条都没有（配方提供器里 0 处引用本 id）；</li>
 *   <li><b>不进创造页任何分区</b>：Registrate 只把<b>物品</b>填进创造页
 *       （{@code AbstractRegistrate#item(...)} 里才读 {@code defaultCreativeModeTab}），
 *       本类<b>没有</b> {@code .item()} ⇒ 全模组唯一那一页（{@code base_tab}）里没有它的入口；</li>
 *   <li><b>不在战利品表 / 交易里</b>：注册时调 {@code Properties#noLootTable()}
 *       ⇒ {@code Block#getLootTable() == BuiltInLootTables.EMPTY}
 *       ⇒ Registrate 的战利品回调被跳过（{@code BlockBuilder#loot} 里有这条判定）
 *       且 {@code BlockLootSubProvider#generate} 会跳过它 ⇒ <b>一个战利品表文件都不生成</b>；</li>
 *   <li><b>没有物品形态</b>：没有 {@code BlockItem} ⇒ 不进背包、不进 JEI、
 *       创造页搜索里也搜不到（搜索搜的是物品）；</li>
 *   <li><b>只由技能临时放置、结束时移除</b>：放置与移除全部在 {@code FieldChargeRuntime}
 *       的成对调用里（{@code placeInjector} / {@code removeInjector}），并且还多一层自愈：
 *       本方块实体<b>不持久化任何状态</b>，区块重载后必然处于未赋能态，
 *       闲置超过宽限期就自己把自己移除（见 {@code StressInjectorBlockEntity#tick}）；</li>
 *   <li><b>命名空间仍是 {@code createoreexpansion}</b>（红线）：注册走 {@code CoeBlocks}
 *       → {@code CoeRegistrate}（命名空间 = {@code CoeCore.REGISTRY_NAMESPACE}），
 *       id 取内部风格 {@code stress_injector}。</li>
 * </ol>
 *
 * <h2>样子：<b>完全不渲染</b>（用户 2026-10-02 实测要求：全透明，"玩家不该看到任何东西"）</h2>
 * <p>用户原话："我发现我的面前居然生成了一个玻璃材质、里面还是灰色的方块……我想把这个方块的材质
 * 设置成全透明。" ⇒ 这里用<b>原版机制</b>做到"什么都看不到"，而不是画一张透明贴图
 * （美术红线：不许改/新增任何 {@code textures/**} 下的 png）：</p>
 * <ol>
 *   <li>{@link #getRenderShape} 覆写为 {@link RenderShape#INVISIBLE}：{@code BlockRenderDispatcher}
 *       只在 {@code RenderShape.MODEL} 时才去取模型渲染 ⇒ <b>方块本体一个像素都不画</b>
 *       （与"透明贴图"的区别：连渲染管线都不进）；</li>
 *   <li>{@link #getShape} 返回 {@link Shapes#empty()}：{@code BlockGetter#clip} 对玩家视线走
 *       {@code ClipContext.Block.OUTLINE} = {@code BlockState#getShape}，空形状 ⇒
 *       <b>射线根本打不到它</b>，于是连"选中描边"（那圈黑线框）也不会出现，
 *       玩家既看不到、也瞄不到、更不会在 Jade 之类的悬浮提示里看到它的名字；</li>
 *   <li>方块状态里的模型改成原版的 {@code minecraft:block/air}（其模型文件内容就是
 *       <b>空的 {@code {}}</b>、零贴图、零贴图引用）—— 双保险，且不新增任何本仓资源；</li>
 *   <li>{@code noCollission() / noOcclusion() / noLootTable()} 三条注册属性保持不变
 *       （无碰撞、不遮挡邻面、无掉落表）。</li>
 * </ol>
 *
 * <h2>它不接轴（与"接入同一动力网络"的关系）</h2>
 * <p>{@code KineticBlock#hasShaftTowards} 默认就是 false，本类<b>不覆写</b> ⇒ 注入器与任何方块
 * 都不做轴连接。这不是偷懒，是<b>唯一可行</b>的做法：手摇曲柄只有背向一个轴面，而那一格正是它
 * 挂载的那一格（{@code HandCrankBlock#canSurvive} 要求那一格有碰撞箱 ⇒ 永远是占用状态）。
 * 详见 {@link StressInjectorBlockEntity} 的类注释（应力改为用 Create 自己的
 * {@code KineticBlockEntity#setNetwork} → {@code KineticNetwork#add} 挂进曲柄所在网络）。</p>
 *
 * @since 1.0.0
 */
public class StressInjectorBlock extends DirectionalKineticBlock implements IBE<StressInjectorBlockEntity> {

    public StressInjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /**
     * <b>完全不渲染</b>（原版机制，用户 2026-10-02："我想把这个方块的材质设置成全透明"）。
     *
     * <p>依据：{@code BlockRenderDispatcher#renderBatched} 只在
     * {@code state.getRenderShape() == RenderShape.MODEL} 时取模型；{@code INVISIBLE}
     * 直接跳过 ⇒ 不新增贴图、不新增模型也能做到"看不到任何东西"。</p>
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * <b>空形状</b>：连"选中描边"都不给，且射线检测（{@code ClipContext.Block.OUTLINE} 取的就是
     * {@code BlockState#getShape}）打不到它 ⇒ 玩家瞄不准、也读不到它的名字。
     *
     * <p>注：方块实体的 tick、动力网络登记、护目镜信息都不依赖形状；注入器由技能用
     * {@code Level#setBlock} 直接放置，从不需要"可被右键/可替换"的形状。</p>
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public Class<StressInjectorBlockEntity> getBlockEntityClass() {
        return StressInjectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StressInjectorBlockEntity> getBlockEntityType() {
        return CoeBlockEntityTypes.STRESS_INJECTOR.get();
    }
}
