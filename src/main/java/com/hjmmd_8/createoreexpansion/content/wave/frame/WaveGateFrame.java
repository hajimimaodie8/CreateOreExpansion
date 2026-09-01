package com.hjmmd_8.createoreexpansion.content.wave.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波闸本地坐标系解析器（方向向量化接口）。
 *
 * <p><b>职责</b>：把波实体（世界坐标）换算到波闸自身坐标系，用于：</p>
 * <ul>
 *   <li><b>入口判定</b>：波前在入口面板上的<b>面内偏移</b>是否落在中心区域
 *       （中心正方形 4×4，见 {@link AbstractWaveGateRegulation#ENTRY_CENTER_HALF}），
 *       决定"斜射/偏移射"容错度；</li>
 *   <li><b>方向换算</b>：本地方向（反弹/穿出/分裂出口）↔ 世界方向。</li>
 * </ul>
 *
 * <p><b>坐标系约定</b>：本地 Z 轴 = 面板轴（波闸 FACING 法向量），
 * 本地 X/Y 轴 = 面板面内两个正交轴。本地坐标原点 = 机器参考中心（通常为方块中心）。</p>
 *
 * <p><b>实现分层</b>（按实现深度递增）：</p>
 * <ol>
 *   <li><b>静态方块</b>（{@link StaticWaveGateFrame}）：由 blockstate 的 FACING 推导三轴
 *       —— 波每 tick 从方块实体读当前 FACING，轻量、无新依赖；</li>
 *   <li><b>Create 动态结构</b>（待实现）：动力轴承 / 矿车装配站装配的实体，
 *       读取结构旋转矩阵，波方向随结构任意旋转；</li>
 *   <li><b>航空学结构</b>（待实现，条件加载）：飞机 / 船，读结构矩阵 + 本地坐标转换。</li>
 * </ol>
 *
 * <p>实现方只需提供三轴基向量与原点，{@link #toLocal}/{@link #toWorldDir} 等换算
 * 由接口默认方法完成。</p>
 */
public interface WaveGateFrame {

    /** 面板轴（世界单位向量）：波沿此轴进入/穿出，= 波闸 FACING 法向量。 */
    Vec3 panelAxis();

    /** 机器参考中心（世界坐标）：本地坐标原点。 */
    Vec3 origin();

    /** 本地 X 基向量（世界坐标，单位向量，面板面内水平轴）。 */
    Vec3 basisX();

    /** 本地 Y 基向量（世界坐标，单位向量，面板面内竖直轴）。 */
    Vec3 basisY();

    /** 本地 Z 基向量（世界坐标，单位向量，= 面板轴）。 */
    Vec3 basisZ();

    /**
     * 世界坐标点 → 本地坐标（相对 {@link #origin()}）。
     *
     * @param worldPos 世界坐标点（通常为波实体中心）
     * @return 本地坐标 (x, y, z)：x/y 为面板面内偏移，z 为沿面板轴的深度
     */
    default Vec3 toLocal(Vec3 worldPos) {
        Vec3 rel = worldPos.subtract(origin());
        return new Vec3(rel.dot(basisX()), rel.dot(basisY()), rel.dot(basisZ()));
    }

    /**
     * 世界方向向量 → 本地方向向量（仅旋转，无平移）。
     * 用于把波的飞行方向换算到机器坐标系内参与判定。
     */
    default Vec3 toLocalDir(Vec3 worldDir) {
        return new Vec3(worldDir.dot(basisX()), worldDir.dot(basisY()), worldDir.dot(basisZ()));
    }

    /**
     * 本地方向向量 → 世界方向向量（仅旋转，无平移）。
     * 用于把反弹/穿出/分裂出口等本地方向换算回世界方向。
     */
    default Vec3 toWorldDir(Vec3 localDir) {
        return basisX().scale(localDir.x)
            .add(basisY().scale(localDir.y))
            .add(basisZ().scale(localDir.z));
    }

    /**
     * 便捷构造：静态方块的 FACING 推导实现。
     *
     * @param pos    方块位置
     * @param facing 波闸 blockstate 的 FACING（= 齿轮轴/面板轴）
     * @return 静态坐标系解析器
     */
    static WaveGateFrame staticFrame(BlockPos pos, Direction facing) {
        return new StaticWaveGateFrame(pos, facing);
    }
}
