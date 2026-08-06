package com.hjmmd_8.createoreexpansion.client.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * 高级轮廓渲染器 - 合并相邻方块为长方体
 *
 * <p>将连续直线上的方块合并为一个长方体，只在拐弯处分割，
 * 使渲染更美观、性能更好。</p>
 */
public class OutlineRenderer {

    /**
     * 渲染优化的外轮廓
     */
    public static void renderOutline(Level level, Set<BlockPos> positions, PoseStack poseStack,
                                     VertexConsumer consumer, float r, float g, float b, float a) {
        if (positions.isEmpty()) return;

        // 构建包围盒列表
        List<AABB> boxes = buildMergedBoxes(positions, level);

        // 渲染所有包围盒的轮廓
        PoseStack.Pose pose = poseStack.last();

        for (AABB box : boxes) {
            renderBoxOutline(pose, consumer, box, r, g, b, a);
        }
    }

    /**
     * 构建合并后的包围盒列表
     */
    private static List<AABB> buildMergedBoxes(Set<BlockPos> positions, Level level) {
        List<AABB> boxes = new ArrayList<>();
        Set<BlockPos> processed = new HashSet<>();

        for (BlockPos start : positions) {
            if (processed.contains(start)) continue;
            if (level.getBlockState(start).isAir()) continue;

            // BFS 找出连通分量
            Set<BlockPos> component = findConnectedComponent(start, positions, level, processed);
            if (component.isEmpty()) continue;

            // 尝试沿各个方向合并
            mergeAlongDirection(component, Direction.Axis.X, boxes);
            mergeAlongDirection(component, Direction.Axis.Y, boxes);
            mergeAlongDirection(component, Direction.Axis.Z, boxes);
        }

        return boxes;
    }

    /**
     * BFS 找出连通分量
     */
    private static Set<BlockPos> findConnectedComponent(BlockPos start, Set<BlockPos> positions,
                                                         Level level, Set<BlockPos> processed) {
        Set<BlockPos> component = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(start);
        component.add(start);
        processed.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // 检查6个方向的邻居
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (positions.contains(neighbor) && !processed.contains(neighbor)) {
                    BlockState state = level.getBlockState(neighbor);
                    if (!state.isAir()) {
                        component.add(neighbor);
                        processed.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return component;
    }

    /**
     * 沿指定方向合并方块
     */
    private static void mergeAlongDirection(Set<BlockPos> component, Direction.Axis axis, List<AABB> boxes) {
        Set<BlockPos> remaining = new HashSet<>(component);

        while (!remaining.isEmpty()) {
            // 找一个未处理的方块作为起点
            BlockPos start = remaining.iterator().next();
            remaining.remove(start);

            // 沿轴向延伸找出连续方块
            List<BlockPos> line = new ArrayList<>();
            line.add(start);

            // 向正方向延伸
            BlockPos current = start;
            while (true) {
                BlockPos next = current.relative(getDirection(axis, true));
                if (!remaining.contains(next)) break;
                line.add(next);
                remaining.remove(next);
                current = next;
            }

            // 向负方向延伸
            current = start;
            while (true) {
                BlockPos next = current.relative(getDirection(axis, false));
                if (!remaining.contains(next)) break;
                line.addFirst(next); // 添加到开头
                remaining.remove(next);
                current = next;
            }

            // 检查是否可以在其他轴上也合并
            AABB mergedBox = createBoundingBox(line);
            List<BlockPos> lineList = new ArrayList<>(line);

            // 尝试在垂直方向扩展
            List<BlockPos> expanded = tryExpandPerpendicular(lineList, axis, remaining);
            if (expanded.size() > lineList.size()) {
                mergedBox = createBoundingBox(expanded);
                lineList = expanded;
            }

            boxes.add(mergedBox);
        }
    }

    /**
     * 尝试在垂直方向扩展方块序列
     */
    private static List<BlockPos> tryExpandPerpendicular(List<BlockPos> line, Direction.Axis mainAxis,
                                                          Set<BlockPos> remaining) {
        if (line.isEmpty()) return line;

        List<BlockPos> result = new ArrayList<>(line);
        boolean expanded;

        do {
            expanded = false;
            Set<BlockPos> toAdd = new HashSet<>();

            // 对当前结果中的每个方块，检查其垂直方向的邻居
            for (BlockPos pos : result) {
                for (Direction.Axis perpAxis : getPerpendicularAxes(mainAxis)) {
                    for (int dir = -1; dir <= 1; dir += 2) {
                        Direction expandDir = getDirection(perpAxis, dir > 0);
                        BlockPos neighbor = pos.relative(expandDir);

                        // 检查是否可以扩展（需要整行都能扩展）
                        if (remaining.contains(neighbor) && canExpandLine(result, neighbor, mainAxis)) {
                            toAdd.add(neighbor);
                        }
                    }
                }
            }

            if (!toAdd.isEmpty()) {
                result.addAll(toAdd);
                remaining.removeAll(toAdd);
                expanded = true;
            }
        } while (expanded);

        return result;
    }

    /**
     * 检查是否可以扩展行（需要整行都存在）
     */
    private static boolean canExpandLine(List<BlockPos> currentLine, BlockPos newBlock, Direction.Axis mainAxis) {
        // 找出新块在主轴方向上的坐标
        int mainCoord = getCoordinate(newBlock, mainAxis);

        // 检查当前行中所有块在相同主轴坐标处是否有对应的块
        Set<BlockPos> lineSet = new HashSet<>(currentLine);

        for (BlockPos pos : currentLine) {
            if (getCoordinate(pos, mainAxis) == mainCoord) {
                continue; // 相同位置，跳过
            }

            // 检查是否有对应位置的块
            BlockPos corresponding = shiftCoordinate(pos, mainAxis, mainCoord - getCoordinate(pos, mainAxis));
            if (!lineSet.contains(corresponding)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 创建包围盒
     */
    private static AABB createBoundingBox(List<BlockPos> positions) {
        if (positions.isEmpty()) return null;

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

    /**
     * 渲染单个包围盒的轮廓
     */
    private static void renderBoxOutline(PoseStack.Pose pose, VertexConsumer consumer, AABB box,
                                        float r, float g, float b, float a) {
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
        double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;

        // 12条边
        // X方向的边（4条）
        renderEdge(pose, consumer, x1, y1, z1, x2, y1, z1, r, g, b, a);
        renderEdge(pose, consumer, x1, y1, z2, x2, y1, z2, r, g, b, a);
        renderEdge(pose, consumer, x1, y2, z1, x2, y2, z1, r, g, b, a);
        renderEdge(pose, consumer, x1, y2, z2, x2, y2, z2, r, g, b, a);

        // Y方向的边（4条）
        renderEdge(pose, consumer, x1, y1, z1, x1, y2, z1, r, g, b, a);
        renderEdge(pose, consumer, x2, y1, z1, x2, y2, z1, r, g, b, a);
        renderEdge(pose, consumer, x1, y1, z2, x1, y2, z2, r, g, b, a);
        renderEdge(pose, consumer, x2, y1, z2, x2, y2, z2, r, g, b, a);

        // Z方向的边（4条）
        renderEdge(pose, consumer, x1, y1, z1, x1, y1, z2, r, g, b, a);
        renderEdge(pose, consumer, x2, y1, z1, x2, y1, z2, r, g, b, a);
        renderEdge(pose, consumer, x1, y2, z1, x1, y2, z2, r, g, b, a);
        renderEdge(pose, consumer, x2, y2, z1, x2, y2, z2, r, g, b, a);
    }

    /**
     * 渲染单条边
     */
    private static void renderEdge(PoseStack.Pose pose, VertexConsumer consumer,
                                   double x1, double y1, double z1, double x2, double y2, double z2,
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
                .setUv(0, 0)
                .setNormal(pose, dx, dy, dz);

        consumer.addVertex(pose, (float)x2, (float)y2, (float)z2)
                .setColor(r, g, b, a)
                .setUv(0, 0)
                .setNormal(pose, dx, dy, dz);
    }

    // 辅助方法

    private static Direction getDirection(Direction.Axis axis, boolean positive) {
        return switch (axis) {
            case X -> positive ? Direction.EAST : Direction.WEST;
            case Y -> positive ? Direction.UP : Direction.DOWN;
            case Z -> positive ? Direction.SOUTH : Direction.NORTH;
        };
    }

    private static int getCoordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static BlockPos shiftCoordinate(BlockPos pos, Direction.Axis axis, int delta) {
        return switch (axis) {
            case X -> new BlockPos(pos.getX() + delta, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), pos.getY() + delta, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), pos.getZ() + delta);
        };
    }

    private static Direction.Axis[] getPerpendicularAxes(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
    }
}
