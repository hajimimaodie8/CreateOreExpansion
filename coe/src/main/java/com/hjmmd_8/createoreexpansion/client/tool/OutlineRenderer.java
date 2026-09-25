package com.hjmmd_8.createoreexpansion.client.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

/**
 * 高级轮廓渲染器
 *
 * <p>合并相邻方块为 AABB，用 VoxelShape OR 合并后 optimize 自动去除内部边。</p>
 *
 * <p>两个入口：旧签名（顶点即原坐标）与带 {@code lookupOffset} 的重载（顶点整体减偏移）。
 * 后者是给物理结构（局部坐标在 {@code 2.048e7} 量级）用的抗浮点抵消手段，见该重载的说明。
 * 两者共用同一套合并/取状态逻辑，旧签名的行为一字未改。</p>
 */
public class OutlineRenderer {

    /**
     * 渲染优化的外轮廓（旧签名：顶点平移量为 0）。
     *
     * <p>行为与引入 {@link #renderOutline(Level, Set, PoseStack, VertexConsumer, float, float, float, float, BlockPos)}
     * 之前<b>逐字一致</b>——它只是一行委托（{@code lookupOffset = BlockPos.ZERO}），
     * 所有既有调用方（主世界预览）不需要任何改动。</p>
     */
    public static void renderOutline(Level level, Set<BlockPos> positions, PoseStack poseStack,
                                     VertexConsumer consumer, float r, float g, float b, float a) {
        renderOutline(level, positions, poseStack, consumer, r, g, b, a, BlockPos.ZERO);
    }

    /**
     * 渲染优化的外轮廓，并把<b>写进顶点缓冲的顶点</b>整体平移 {@code -lookupOffset}。
     *
     * <p><b>为什么需要它（物理结构 / 浮点精度）</b>：结构局部（plot）坐标在 {@code 2.048e7} 量级，
     * 而 float 只有 24 bit 尾数——该量级的整数 ulp 已经是 2，顶点一旦以局部大数写进顶点缓冲，
     * 再乘"局部 → 世界"位姿矩阵就会出现<b>灾难性抵消</b>（框被画到别处 / 退化成不可见）。
     * 调用方把顶点整体减掉一个近处原点（{@code lookupOffset}）后，顶点只剩几十格量级，
     * 精度立刻恢复；只要同时把位姿矩阵的平移列补成"该原点的世界坐标"，几何位置就分毫不动。</p>
     *
     * <p><b>两个坐标的分工（关键）</b>：{@code positions} 里的坐标仍是<b>原始坐标</b>——
     * 合并相邻方块（按 {@code BlockPos} 邻接）与读方块状态（{@code level.getBlockState(pos)}）
     * 都必须用原坐标，否则会读到空白/错格；<b>只有最终写进顶点缓冲的 AABB 减
     * {@code lookupOffset}</b>。所以偏移发生在"合并、取状态"之后（见
     * {@link #mergeBlocks(Set, Level, BlockPos)} 的最后一步），而不是在入场处整体平移集合。</p>
     *
     * @param lookupOffset 顶点整体平移量（顶点坐标 = 原坐标 − {@code lookupOffset}）；
     *                     传 {@link BlockPos#ZERO} 即完全等价于旧签名
     */
    public static void renderOutline(Level level, Set<BlockPos> positions, PoseStack poseStack,
                                     VertexConsumer consumer, float r, float g, float b, float a,
                                     BlockPos lookupOffset) {
        if (positions.isEmpty()) return;

        List<AABB> boxes = mergeBlocks(positions, level, lookupOffset);
        if (boxes.isEmpty()) return;

        VoxelShape combined = combineToShape(boxes);
        PoseStack.Pose pose = poseStack.last();  // 使用poseStack中的变换
        Set<Edge> rendered = new HashSet<>();

        combined.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            addEdge(pose, consumer, rendered, x1, y1, z1, x2, y2, z2, r, g, b, a);
        });
    }

    // ========== 方块合并 ==========

    /**
     * 合并相邻方块为若干 AABB。
     *
     * <p><b>合并与"空气过滤"都发生在原始坐标系</b>（{@code level.getBlockState(pos)} 必须吃原坐标，
     * 否则结构场景会读到空白格而把所有方块过滤掉）；只有产出的 AABB 在最后一步整体
     * 平移 {@code -lookupOffset}——这样"画在哪"变了、"合并成几块"没变。</p>
     */
    private static List<AABB> mergeBlocks(Set<BlockPos> positions, Level level, BlockPos lookupOffset) {
        Set<BlockPos> remaining = new HashSet<>();
        for (BlockPos pos : positions) {
            if (!level.getBlockState(pos).isAir()) {
                remaining.add(pos.immutable());
            }
        }

        List<AABB> boxes = new ArrayList<>();
        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            remaining.remove(start);

            List<BlockPos> line = expandLine(start, remaining, Direction.Axis.Z);
            List<BlockPos> plane = expandPlane(line, remaining, Direction.Axis.X);
            List<BlockPos> volume = expandPlane(plane, remaining, Direction.Axis.Y);

            volume.forEach(remaining::remove);
            // 偏移只加在这里：顶点坐标 = 原坐标 − lookupOffset（BlockPos.ZERO 时值完全不变，
            // AABB.move(0,0,0) 返回等值框，故旧调用方行为逐字一致）
            boxes.add(createAABB(volume).move(-lookupOffset.getX(), -lookupOffset.getY(), -lookupOffset.getZ()));
        }
        return boxes;
    }

    private static List<BlockPos> expandLine(BlockPos start, Set<BlockPos> remaining, Direction.Axis axis) {
        List<BlockPos> line = new ArrayList<>();
        line.add(start);

        Direction posDir = getDirection(axis, true);
        Direction negDir = getDirection(axis, false);

        BlockPos current = start;
        while (true) {
            BlockPos next = current.relative(posDir);
            if (!remaining.contains(next)) break;
            line.add(next);
            current = next;
        }

        current = start;
        while (true) {
            BlockPos next = current.relative(negDir);
            if (!remaining.contains(next)) break;
            line.addFirst(next);
            current = next;
        }

        return line;
    }

    private static List<BlockPos> expandPlane(List<BlockPos> line, Set<BlockPos> remaining, Direction.Axis axis) {
        List<BlockPos> result = new ArrayList<>(line);
        Direction posDir = getDirection(axis, true);
        Direction negDir = getDirection(axis, false);

        // 正方向扩展
        int layer = 1;
        while (true) {
            List<BlockPos> nextLayer = new ArrayList<>();
            for (BlockPos pos : line) {
                BlockPos next = pos.relative(posDir, layer);
                if (!remaining.contains(next)) return result;
                nextLayer.add(next);
            }
            result.addAll(nextLayer);
            layer++;
        }
    }

    private static AABB createAABB(List<BlockPos> positions) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    // ========== 形状合并（原版方法）==========

    private static VoxelShape combineToShape(List<AABB> boxes) {
        VoxelShape result = Shapes.empty();

        for (AABB box : boxes) {
            result = Shapes.joinUnoptimized(
                    result,
                    Shapes.create(box.inflate(0.005D)),  // 膨胀防 Z-fighting
                    BooleanOp.OR
            );
        }

        return result.optimize();  // 自动去除内部边
    }

    // ========== 边去重渲染 ==========

    private record Edge(
            double x1, double y1, double z1,
            double x2, double y2, double z2
    ) {
        Edge normalized() {
            // 标准化：确保起点字典序 < 终点
            if (x1 < x2) return this;
            if (x1 > x2) return new Edge(x2, y2, z2, x1, y1, z1);
            if (y1 < y2) return this;
            if (y1 > y2) return new Edge(x2, y2, z2, x1, y1, z1);
            if (z1 < z2) return this;
            if (z1 > z2) return new Edge(x2, y2, z2, x1, y1, z1);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge that)) return false;
            Edge a = this.normalized();
            Edge b = that.normalized();
            return a.x1 == b.x1 && a.y1 == b.y1 && a.z1 == b.z1
                    && a.x2 == b.x2 && a.y2 == b.y2 && a.z2 == b.z2;
        }

        @Override
        public int hashCode() {
            Edge n = normalized();
            return Objects.hash(n.x1, n.y1, n.z1, n.x2, n.y2, n.z2);
        }
    }

    private static void addEdge(PoseStack.Pose pose, VertexConsumer consumer,
                                Set<Edge> rendered,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float r, float g, float b, float a) {
        Edge edge = new Edge(x1, y1, z1, x2, y2, z2);
        if (rendered.add(edge)) {
            renderEdge(pose, consumer, x1, y1, z1, x2, y2, z2, r, g, b, a);
        }
    }

    private static void renderEdge(PoseStack.Pose pose, VertexConsumer consumer,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   float r, float g, float b, float a) {
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        consumer.addVertex(pose, (float)x1, (float)y1, (float)z1)
                .setColor(r, g, b, a)
                .setNormal(pose, dx, dy, dz);

        consumer.addVertex(pose, (float)x2, (float)y2, (float)z2)
                .setColor(r, g, b, a)
                .setNormal(pose, dx, dy, dz);
    }

    private static Direction getDirection(Direction.Axis axis, boolean positive) {
        return switch (axis) {
            case X -> positive ? Direction.EAST : Direction.WEST;
            case Y -> positive ? Direction.UP : Direction.DOWN;
            case Z -> positive ? Direction.SOUTH : Direction.NORTH;
        };
    }
}