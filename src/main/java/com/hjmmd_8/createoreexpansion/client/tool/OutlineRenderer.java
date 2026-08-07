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
import org.joml.Matrix4f;

import java.util.*;

/**
 * 高级轮廓渲染器
 *
 * <p>合并相邻方块为 AABB，用 VoxelShape OR 合并后 optimize 自动去除内部边。</p>
 */
public class OutlineRenderer {

    /**
     * 渲染优化的外轮廓
     */
    public static void renderOutline(Level level, Set<BlockPos> positions, PoseStack poseStack,
                                     VertexConsumer consumer, float r, float g, float b, float a) {
        if (positions.isEmpty()) return;

        List<AABB> boxes = mergeBlocks(positions, level);
        if (boxes.isEmpty()) return;

        VoxelShape combined = combineToShape(boxes);
        PoseStack.Pose pose = poseStack.last();  // 使用poseStack中的变换
        Set<Edge> rendered = new HashSet<>();

        combined.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            addEdge(pose, consumer, rendered, x1, y1, z1, x2, y2, z2, r, g, b, a);
        });
    }

    // ========== 方块合并 ==========

    private static List<AABB> mergeBlocks(Set<BlockPos> positions, Level level) {
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
            boxes.add(createAABB(volume));
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