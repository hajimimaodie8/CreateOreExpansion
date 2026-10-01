package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>「动力源方块」登记表</b>（用户 2026-10-01 规格 §2.1 第 4 条：<b>必须暴露公开接口</b>）——
 * 临域充力要能赋能哪些方块，只有这张表说了算。
 *
 * <h2>用户要求</h2>
 * <p>规格原文："兼容扩展：除手摇曲柄外要能兼容别的『动力源方块』（如某些附属模组里与大/小齿轮绑定、
 * 本质区别于齿轮的动力源方块）⇒ <b>必须暴露公开接口</b>给后续扩展登记。"</p>
 *
 * <h2>手摇曲柄是第一个实现</h2>
 * <p>{@link HandCrankStressSource} 在 {@code CreateOreExpansion} 的构造器里显式登记进本表
 * （{@code HandCrankStressSource.register()}）。技能侧（{@link FieldChargeRuntime}）<b>只通过本表</b>
 * 访问动力源，不 import 任何 Create 的类 —— "让 Create 的手摇曲柄真的转起来"这件事只住在
 * {@link HandCrankStressSource} 一个文件里。</p>
 *
 * <h2>别的模组怎么登记（照抄这三步）</h2>
 * <pre>{@code
 * // ① 实现 StressSourceKind（见该类注释的"四件事"）：
 * public final class MyFlywheelSource implements StressSourceKind {
 *     public String id() { return "mymod:flywheel_source"; }
 *     public boolean matches(BlockState s) { return s.is(MyBlocks.FLYWHEEL_SOURCE.get()); }
 *     public boolean drive(ServerLevel level, BlockPos pos) {   // 让它真的转
 *         if (!(level.getBlockEntity(pos) instanceof MyFlywheelBlockEntity be)) return false;
 *         be.setEnergized(true);                                  // 自己的开关
 *         be.updateGeneratedRotation();                           // Create 侧：触发网络重算 + sendData
 *         return be.getGeneratedSpeed() != 0;
 *     }
 *     public void keepAlive(ServerLevel level, BlockPos pos) { ... }   // 幂等续期
 *     public void halt(ServerLevel level, BlockPos pos) { ... }        // 立刻静止
 *     public List<BlockPos> injectorSockets(ServerLevel level, BlockPos pos) {
 *         return List.of(pos.above(), pos.below());                    // 注入器能放哪几格
 *     }
 * }
 *
 * // ② 在 mod 构造器（或 FMLCommonSetupEvent）里登记一次：
 * StressSourceRegistry.register(MyBlocks.FLYWHEEL_SOURCE.get(), new MyFlywheelSource());
 * //    也可以按方块标签整族登记：
 * StressSourceRegistry.register(MyTags.DYNAMO_LIKE, new MyFlywheelSource());
 * }</pre>
 * <p>登记是<b>纯静态</b>的（不注册任何注册表条目），登记时机只要在玩家第一次按技能键之前即可；
 * 重复登记同一个 {@link StressSourceKind} 会被忽略（按 {@code id()} 去重）。</p>
 *
 * <h2>匹配顺序</h2>
 * <p>先登记的优先（因此本模组内建的曲柄永远先命中）。找不到归属的方块一律<b>不</b>算动力源 ——
 * 这样"和大/小齿轮绑定但本质区别于齿轮"的方块不会被齿轮误命中（齿轮本来也不在本表里）。</p>
 *
 * @since 1.0.0
 */
public final class StressSourceRegistry {

    /** 一条登记：判定器 + 驱动实现。 */
    private record Entry(Predicate<BlockState> matcher, StressSourceKind kind) {}

    /**
     * 登记表。{@code CopyOnWriteArrayList}：登记发生在 mod 构造期/初始化期（可能有别的模组线程），
     * 读取发生在每个 tick 的判定路径上 —— 写少读多、且读绝不能抛
     * {@code ConcurrentModificationException}。
     */
    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private StressSourceRegistry() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * <b>公开登记入口</b>：把一个方块状态判定器绑定到一种动力源的驱动实现上。
     *
     * @param matcher 判定器（纯函数，别在里面改世界）
     * @param kind    驱动实现；同一 {@code id()} 重复登记会被忽略（幂等）
     */
    public static void register(Predicate<BlockState> matcher, StressSourceKind kind) {
        if (matcher == null || kind == null) {
            return;
        }
        for (Entry entry : ENTRIES) {
            if (entry.kind().id().equals(kind.id())) {
                return;
            }
        }
        ENTRIES.add(new Entry(matcher, kind));
        CoeCore.LOGGER.info("[临域充力] 登记动力源方块：kind={}（当前共 {} 种）", kind.id(), ENTRIES.size());
    }

    /** 按方块登记（最常见形态）。 */
    public static void register(Block block, StressSourceKind kind) {
        if (block == null) {
            return;
        }
        register(state -> state.is(block), kind);
    }

    /** 按方块标签整族登记（"与大/小齿轮绑定的一整类动力源"用这个）。 */
    public static void register(TagKey<Block> tag, StressSourceKind kind) {
        if (tag == null) {
            return;
        }
        register(state -> state.is(tag), kind);
    }

    /** 该状态归属的动力源实现；未登记 ⇒ {@code null}。 */
    public static @Nullable StressSourceKind kindOf(@Nullable BlockState state) {
        if (state == null) {
            return null;
        }
        for (Entry entry : ENTRIES) {
            if (entry.matcher().test(state)) {
                return entry.kind();
            }
        }
        return null;
    }

    /** 该状态能不能被临域充力赋能。 */
    public static boolean isStressSource(@Nullable BlockState state) {
        return kindOf(state) != null;
    }

    /**
     * 以 {@code center} 为中心、<b>边长 = 2×radius+1 的立方体</b>（规格 §2.1 第 1 条）内扫出全部
     * 已登记的动力源方块位置。
     *
     * @return 命中的位置（已去皮、可直接存；顺序 = 遍历顺序，调用方负责随机抽取）
     */
    public static List<BlockPos> findSources(ServerLevel level, BlockPos center, int radius) {
        if (level == null || center == null || radius < 0) {
            return List.of();
        }
        int r = radius;
        BlockPos min = center.offset(-r, -r, -r);
        BlockPos max = center.offset(r, r, r);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            // ⚠ betweenClosed 复用同一个可变 BlockPos ⇒ 必须先 immutable() 再存。
            BlockPos pos = cursor.immutable();
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isStressSource(state)) {
                found.add(pos);
            }
        }
        return found;
    }

    /** 已登记的动力源 id（诊断/日志用；顺序 = 登记顺序）。 */
    public static List<String> registeredIds() {
        List<String> ids = new ArrayList<>(ENTRIES.size());
        for (Entry entry : ENTRIES) {
            ids.add(entry.kind().id());
        }
        return Collections.unmodifiableList(ids);
    }
}
