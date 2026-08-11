package com.hjmmd_8.createoreexpansion.foundation.util.params;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class ParamsPool<T extends IParams> {
    public static ParamsPool<FrameParams> DEFAULT_POOL = new ParamsPool<>(50, FrameParams::new);

    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final int maxSize;
    private final Supplier<T> factory;

    public ParamsPool(int maxSize, Supplier<T> factory) {
        this.maxSize = maxSize;
        this.factory = factory;
        // 预热：提前创建好一批对象，避免首次使用时才创建
        for (int i = 0; i < Math.min(maxSize, 10); i++) {
            pool.offer(factory.get());
        }
    }

    public T borrow() {
        T builder = pool.poll();
        return builder != null ? builder : factory.get();
    }

    public void returnParams(IParams params) {
        recursiveReturn(params, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @SuppressWarnings("unchecked")
    private void recursiveReturn(IParams node, Set<IParams> visited) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);

        for (IParams child : node.getChildren()) {
            recursiveReturn(child, visited);
        }

        node.clear();
        if (pool.size() < maxSize) {
            pool.offer((T) node);
        }
    }
}
