package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
 *       且 {@code BlockLootSubProvider#generate} 会跳过它 ⇒ <b>一个战利品表文件都不生成</b>；
 *       村民交易表与它无关（没有任何物品 id 指向它）；</li>
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
 * <h2>样子：不新增任何贴图 / 模型</h2>
 * <p>方块状态直接指向 <b>原版</b>的 {@code minecraft:block/glass}（A1 归属关卡只判本命名空间的引用，
 * 外部命名空间是"计数不判"）。它是半透明的立方体，配合 {@code noOcclusion() / noCollission()}，
 * 存在感极低 —— 美术红线"不要改贴图、也不要自己画贴图"因此零触碰。</p>
 *
 * <h2>它不接轴（与"接入同一动力网络"的关系）</h2>
 * <p>{@code KineticBlock#hasShaftTowards} 默认就是 false，本类<b>不覆写</b> ⇒ 注入器与任何方块
 * 都不做轴连接。原因见 {@link StressInjectorBlockEntity} 的类注释（手摇曲柄唯一那个轴面就是它
 * 挂载的那一格）。应力是通过 {@code KineticNetwork#updateCapacityFor} 直接登记进曲柄所在网络的。</p>
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

    @Override
    public Class<StressInjectorBlockEntity> getBlockEntityClass() {
        return StressInjectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StressInjectorBlockEntity> getBlockEntityType() {
        return CoeBlockEntityTypes.STRESS_INJECTOR.get();
    }
}
