package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.FieldChargeConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>应力注入器在世界里的落子与拆除</b>（2026-10-05 行为零变化拆分，从 {@code FieldChargeRuntime} 的
 * {@code pickSocket} / {@code canPlaceInjector} / {@code placeInjector} / {@code energizeInjector} /
 * {@code facingTowards} / {@code removeInjector} / {@code teardown} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>那一格放得下注入器吗、放下去朝哪边、怎么把容量并进曲柄的网络、
 * 以及收尾时怎么把它干净地拆掉</b>。会话的生死、能量记账、退出路径的枚举一律不在这里
 * （那是 {@link FieldChargeRuntime} 的事）。</p>
 *
 * <p><b>为什么值得单独一个类</b>：这里每一处都在动世界（{@code Level#setBlock} / {@code removeBlock} /
 * 方块实体的赋能与心跳），而"放 / 驱 / 赋"的<b>顺序是承重的</b>（放注入器会让相邻动能方块吃到一次
 * {@code updateIndirectNeighbourShapes}，把曲柄刚建立的网络字段清掉）—— 顺序的唯一说明写在
 * {@code FieldChargeRuntime#start} 的调用点上，本类只提供每一步的实现。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、会话类型由原来的私有嵌套
 * {@code Session} 改写成 {@link FieldChargeRuntime.Session}（同一个值类型，只放宽可见性）。
 * 判定条件、顺序、日志文案一个字未动；{@link #teardown} 仍保持"先移除注入器、再让曲柄静止"。</p>
 */
final class StressInjectorSites {

    private StressInjectorSites() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 在候选位里挑第一个"可替换"的格子（世界已加载 + 不是别的注入器 + {@code canBeReplaced()}）。 */
    static @Nullable BlockPos pickSocket(ServerLevel world, StressSourceKind kind, BlockPos source) {
        List<BlockPos> sockets = kind.injectorSockets(world, source);
        if (sockets == null || sockets.isEmpty()) {
            return null;
        }
        java.util.ArrayList<BlockPos> shuffled = new java.util.ArrayList<>(sockets.size());
        for (BlockPos pos : sockets) {
            if (pos != null) {
                shuffled.add(pos.immutable());
            }
        }
        Collections.shuffle(shuffled, new Random(world.getRandom().nextLong()));
        for (BlockPos pos : shuffled) {
            if (canPlaceInjector(world, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 那一格能不能放注入器（纯判定）。 */
    static boolean canPlaceInjector(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return false;
        }
        if (!world.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (state.is(CoeMachines.STRESS_INJECTOR.get())) {
            // 已经是注入器：可能是别的会话占着，也可能是上一次没收干净的孤儿（它会自己移除）
            return false;
        }
        return state.canBeReplaced();
    }

    /**
     * <b>只放置</b>注入器（不赋能、不驱动）—— 顺序承重见 {@code FieldChargeRuntime#start} 的 ②。
     *
     * @return 放好的方块实体；{@code false} 情形（放不下 / 方块实体没建起来）返回 {@code null}
     *         并保证不留半成品
     */
    static @Nullable StressInjectorBlockEntity placeInjector(ServerLevel world, BlockPos socket,
                                                             BlockPos source) {
        BlockState state = CoeMachines.STRESS_INJECTOR.get()
            .defaultBlockState()
            .setValue(StressInjectorBlock.FACING, facingTowards(socket, source));
        if (!world.setBlock(socket, state, Block.UPDATE_ALL)) {
            return null;
        }
        if (!(world.getBlockEntity(socket) instanceof StressInjectorBlockEntity injector)) {
            // 方块实体没建起来（理论上不会）：立刻把方块撤掉，不留半成品
            world.removeBlock(socket, false);
            return null;
        }
        return injector;
    }

    /** 赋能（把容量并入源方块的动力网络）；方块实体已经不存在 ⇒ {@code false}。 */
    static boolean energizeInjector(StressInjectorBlockEntity injector, BlockPos source,
                                    FieldChargeConfigs.Config config) {
        if (injector.isRemoved()) {
            return false;
        }
        injector.energize(source, config.stressSu(), StressInjectorBlockEntity.HEARTBEAT_TICKS);
        return true;
    }

    /** 从 {@code from} 指向 {@code to} 的方向（只可能差一格，所以按轴从大到小取）。 */
    static Direction facingTowards(BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());
        if (dx != 0) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (dy != 0) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        if (dz != 0) {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return Direction.NORTH;
    }

    /**
     * 把世界侧的东西收干净：<b>先移除注入器</b>（顺带把容量从网络里撤掉），<b>再让曲柄静止</b>。
     *
     * <p>顺序承重：注入器先撤容量再消失，曲柄再失速 ⇒ 不会出现"曲柄还在转、容量却没了"的一瞬间，
     * 也不会留下一个没有任何宿主却仍在提供应力的方块。</p>
     */
    static void teardown(@Nullable MinecraftServer server, FieldChargeRuntime.Session session) {
        if (server == null) {
            // 服务器已经在关了：方块会随存档保存，下次载入时注入器（不持久化状态）会自己移除，
            // 曲柄的 inUse 也会在 10 tick 内自减到 0 ⇒ 不会永远在转。
            return;
        }
        ServerLevel world = server.getLevel(session.dimension);
        if (world == null) {
            return;
        }
        removeInjector(world, session.injector);
        session.kind.halt(world, session.source);
    }

    /** 移除注入器（无掉落；只移除"那一格确实是注入器"的情况）。 */
    static void removeInjector(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return;
        }
        if (!world.getBlockState(pos).is(CoeMachines.STRESS_INJECTOR.get())) {
            return;
        }
        if (world.getBlockEntity(pos) instanceof StressInjectorBlockEntity injector) {
            injector.deEnergize();
        }
        // removeBlock（不是 destroyBlock）⇒ 不产生任何掉落物。
        world.removeBlock(pos, false);
    }
}
