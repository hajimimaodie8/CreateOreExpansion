package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>「动力源方块」的驱动契约</b> —— {@link StressSourceRegistry} 里登记的那种"能被临域充力赋能"
 * 的方块，到底怎么被判定、怎么被驱动、怎么被同一处收尾。
 *
 * <h2>为什么要有这一层</h2>
 * <p>用户 2026-10-01 规格 §2.1 第 4 条明确要求"兼容扩展"：除 Create 的手摇曲柄之外，
 * 还要能登记别的动力源方块（用户原话举的例子是"某些附属模组里与大/小齿轮绑定、
 * 本质区别于齿轮的<b>动力源方块</b>"）。而"怎么让一个方块真的转起来"是<b>每个模组自己的知识</b>
 * ——Create 的手摇曲柄靠 {@code HandCrankBlockEntity#inUse}，别的动力源可能有自己的开关字段、
 * 自己的网络、自己的旋转入口。所以技能侧只认这个接口，绝不 import 任何具体模组的类
 * （Create 也不例外：手摇曲柄的实现集中在 {@link HandCrankStressSource} 一处）。</p>
 *
 * <h2>实现者要保证的四件事</h2>
 * <ol>
 *   <li>{@link #matches(BlockState)} 是<b>纯判定</b>（无副作用），技能用它来扫玩家周围的立方体；</li>
 *   <li>{@link #drive} 让方块真的转起来（对 Create 的发电机而言 = {@code getGeneratedSpeed()} 非零），
 *       返回 {@code false} 表示"这个方块此刻驱动不了"（结构不完整等）⇒ 技能换下一个候选；</li>
 *   <li>{@link #keepAlive} 每 tick 被调一次（<b>必须幂等</b>）：许多方块的"被赋能"状态本身是
 *       会自减/超时的心跳（Create 的手摇曲柄 {@code inUse} 每 tick 自减 1），不续期就会自己停；</li>
 *   <li>{@link #halt} 必须让方块<b>立刻静止</b>，且必须在<b>所有</b>退出路径上被调到
 *       （松手 / 到时限 / 能量见底 / 登出 / 死亡 / 换维度 / 会话心跳超时）。漏一处就会留下
 *       一个"永远在转"的曲柄 —— 这是本轮用户最强调的收尾纪律。</li>
 * </ol>
 *
 * <h2>关于注入器的容量</h2>
 * <p>具体应力由 {@link StressInjectorBlockEntity} 提供（容量按等级 8192/16384/32768 SU），
 * 由技能放在 {@link #injectorSockets} 给出的某一格上，再把容量登记进<b>被赋能方块所在的动力网络</b>。
 * 因此本接口只需要告诉技能"注入器能放在哪几格"。</p>
 *
 * @see StressSourceRegistry
 * @see HandCrankStressSource
 * @since 1.0.0
 */
public interface StressSourceKind {

    /**
     * 稳定标识（只用于日志与去重，例如 {@code "create:hand_crank"}）。
     * 命名空间用**提供方的**（Create 的曲柄就是 {@code create:}），不是本模组。
     */
    String id();

    /** 该方块状态是不是本类动力源（纯判定、无副作用）。 */
    boolean matches(BlockState state);

    /**
     * 驱动它转起来（首次调用时通常要触发一次网络重算 + {@code sendData()}）。
     *
     * @return {@code false} = 驱动不了（方块实体不存在 / 结构不允许）⇒ 技能换下一个候选，
     *         并保证不留下任何"已驱动"的痕迹
     */
    boolean drive(ServerLevel level, BlockPos pos);

    /** 每 tick 续期（必须幂等、必须便宜）。 */
    void keepAlive(ServerLevel level, BlockPos pos);

    /** 立刻静止（所有退出路径共用；必须幂等）。 */
    void halt(ServerLevel level, BlockPos pos);

    /**
     * 注入器<b>可以放置</b>的候选格（按优先级，技能会洗牌后逐个试）。
     *
     * <p>实现者只需返回"空着 + 放下之后不会破坏自己结构"的位置；技能会再统一过滤一次
     * （世界高度、已加载、可替换）。Create 的手摇曲柄只有背向一个轴面，但那一面就是它挂载的
     * 那一格、永远是占用状态，所以它的实现返回的是<b>六个相邻格</b>（见
     * {@link HandCrankStressSource#injectorSockets}）。</p>
     */
    List<BlockPos> injectorSockets(ServerLevel level, BlockPos pos);
}
