package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>Create 手摇曲柄 = 第一个「动力源方块」实现</b>（用户 2026-10-01 规格 §2.1/§4.1）。
 *
 * <p>本文件是<b>唯一</b>碰 Create 手摇曲柄的地方：判定（{@link #matchesState}）、驱动
 * （{@link #drive}）、续期（{@link #keepAlive}）、静止（{@link #halt}）、注入器候选位
 * （{@link #injectorSockets}）五件事全在这里；技能侧（{@code FieldChargeRuntime}）只通过
 * {@link StressSourceRegistry} 拿到 {@link StressSourceKind} 接口，不 import 任何 Create 的类。</p>
 *
 * <h2>Create 侧的事实（照 {@code _create_src} 的留档源码逐条落地）</h2>
 * <ul>
 *   <li><b>驱动 = 写 {@code inUse}</b>：{@code HandCrankBlockEntity#getGeneratedSpeed()} 是
 *       {@code (inUse == 0 ? 0 : clockwise() ? -1 : 1) * 32}，所以只要让 {@code inUse} 非零，
 *       它的生成转速就是 ±32 RPM —— 渲染、网络、下游机器全都跟着走。</li>
 *   <li><b>为什么不能"把 inUse 维持为 1"</b>：{@code HandCrankBlockEntity#tick()} 每 tick 执行
 *       {@code inUse--}，并且在 {@code inUse == 0} 时调 {@code updateGeneratedRotation()} 把速度归零。
 *       服务端 tick 的顺序是"方块实体先 tick、{@code ServerTickEvent.Post} 后跑"，所以如果我们
 *       在 Post 里把 {@code inUse} 写成 1，下一 tick 它就会被减到 0 → 触发一次"停转"重算 →
 *       我们下一 tick 又写回 1 ⇒ <b>每秒 20 次速度抖动</b>（网络与渲染都会抖）。
 *       因此这里走 Create 自己的入口 {@link HandCrankBlockEntity#turn(boolean)}：它把 {@code inUse}
 *       写成 {@code 10} 并且<b>只在"速度由 0 变非零 / 方向变化"时</b>触发一次
 *       {@code updateGeneratedRotation()}（= 网络重算 + {@code sendData()}）。
 *       {@code 10 > 1} 保证了 tick 里的自减永远见不到 0，也就不会触发那次"停转"。</li>
 *   <li><b>方向恒定</b>：{@code turn(false)} 同时把 {@code backwards} 写成 false
 *       ⇒ {@code clockwise() == false} ⇒ 生成转速恒为 {@code convertToDirection(+32, FACING)}。
 *       方向固定是为了让注入器（{@link StressInjectorBlockEntity}）的转速符号与它一致。</li>
 *   <li><b>停止 = {@code inUse = 0} + 一次重算</b>：{@code updateGeneratedRotation()} 里
 *       {@code applyNewSpeed(32 → 0)} 会 {@code detachKinetics()}（把下游整棵子网的 source 摘掉）
 *       + {@code setSpeed(0)} + {@code setNetwork(null)}，最后 {@code sendData()} 把速度 0 发给客户端
 *       ⇒ <b>立刻静止</b>（不是"等它自己衰减到 0"）。</li>
 * </ul>
 *
 * <h2>注入器放哪：六个相邻格</h2>
 * <p>手摇曲柄<b>只有背向一个轴面</b>（{@code HandCrankBlock#hasShaftTowards(face == FACING.getOpposite())}），
 * 而那一面正是它挂载的那一格 —— {@code HandCrankBlock#canSurvive} 要求那一格有碰撞箱，
 * 所以它<b>永远是占用状态</b>。⇒ "用轴连到曲柄上"这条路在一般情形下走不通（详见
 * {@link StressInjectorBlockEntity} 的类注释：注入器改为把容量登记进曲柄所在的动力网络）。
 * 因此这里返回的是<b>六个相邻格</b>，注入器放哪一格都能把应力送进同一个网络。</p>
 *
 * @since 1.0.0
 */
public final class HandCrankStressSource implements StressSourceKind {

    /** 登记 id（Create 的方块，命名空间是 create）。 */
    public static final String ID = "create:hand_crank";

    /** Create 手摇曲柄的转速（{@code HandCrankBlock#getRotationSpeed()} = 32 RPM）。 */
    public static final int SPEED_RPM = 32;

    /**
     * 续期值：直接走 {@code turn()} 就等于 Create 自己的 10。写成常量只为把"必须 &gt; 1"这条
     * 纪律留成可读的一行（见类注释"为什么不能维持为 1"）。
     */
    public static final int MAX_IN_USE = 10;

    private static final HandCrankStressSource INSTANCE = new HandCrankStressSource();

    private HandCrankStressSource() {
        // 单例：登记表里只放一个实例
    }

    /**
     * 把本实现登记进 {@link StressSourceRegistry}（由 {@code CreateOreExpansion} 的构造器调用一次）。
     *
     * <p>幂等：{@link StressSourceRegistry#register} 按 {@code id()} 去重。</p>
     */
    public static void register() {
        StressSourceRegistry.register(HandCrankStressSource::matchesState, INSTANCE);
    }

    /**
     * 判定：只认 {@code create:hand_crank} 本体。
     *
     * <p><b>刻意不认铜阀门手轮</b>（{@code AllBlocks.COPPER_VALVE_HANDLE}）：它的方块实体
     * {@code ValveHandleBlockEntity} 同样继承 {@code HandCrankBlockEntity}，但用途是流体阀门、
     * 生成转速语义不同 —— 用 {@code AllBlocks.HAND_CRANK.has(state)} 精确判定即可排除。</p>
     */
    public static boolean matchesState(@Nullable BlockState state) {
        return state != null && AllBlocks.HAND_CRANK.has(state);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean matches(BlockState state) {
        return matchesState(state);
    }

    @Override
    public boolean drive(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank)) {
            return false;
        }
        // Create 自己的"有人在摇"入口：inUse = 10 + 方向 false；速度 0 → ±32 时触发一次网络重算。
        crank.turn(false);
        // 兜底：万一速度字段此刻已经被别的路径清零（例如刚放下的曲柄还没 attach 上网络），
        // 再显式重算一次，保证"驱动成功"的返回值与"真的在转"一致。
        if (!crank.hasNetwork() || crank.getTheoreticalSpeed() == 0) {
            crank.updateGeneratedRotation();
        }
        return crank.getGeneratedSpeed() != 0;
    }

    @Override
    public void keepAlive(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank)) {
            return;
        }
        // 幂等续期：turn() 只在"速度 0→非零 / 方向变化"时才做重算，所以每 tick 调它是便宜的。
        crank.turn(false);
        if (crank.getTheoreticalSpeed() == 0 && crank.getGeneratedSpeed() != 0) {
            // 速度字段被清零但生成转速还在（例如网络重算把它当成了消费者又解绑）⇒ 重新挂上。
            crank.updateGeneratedRotation();
        }
    }

    @Override
    public void halt(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank)) {
            return;
        }
        crank.inUse = 0;
        if (crank.getTheoreticalSpeed() != 0) {
            // 触发一次网络重算：detachKinetics（下游子网摘源）+ setSpeed(0) + setNetwork(null) + sendData。
            crank.updateGeneratedRotation();
        }
        // 无论速度字段是否已经为 0，都再同步一次：客户端拿到速度 0 才会立刻静止。
        crank.sendData();
    }

    @Override
    public List<BlockPos> injectorSockets(ServerLevel level, BlockPos pos) {
        List<BlockPos> sockets = new ArrayList<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            sockets.add(pos.relative(dir));
        }
        return sockets;
    }
}
