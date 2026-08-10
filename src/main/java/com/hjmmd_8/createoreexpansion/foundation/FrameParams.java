package com.hjmmd_8.createoreexpansion.foundation;

import com.google.common.collect.Maps;

import java.util.*;
import java.util.function.Supplier;

public class FrameParams implements IParams {
    private final Map<String, Supplier<Object>> params = Maps.newHashMap();
    private final Map<String, Object> cache = Maps.newHashMap();
    private final List<IParams> children = new ArrayList<>();

    @Override
    public <T> FrameParams put(String key, T value) {
        params.put(key, () -> value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> FrameParams put(String key, Supplier<T> supplier) {
        params.put(key, (Supplier<Object>) supplier);
        return this;
    }

    @Override
    public FrameParams putChild(String key, IParams child) {
        params.put(key, () -> child);
        children.add(child);
        return this;
    }

    public FrameParams putChild(String key, Supplier<IParams> childSupplier) {
        params.put(key, childSupplier::get);
        return this;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        if (!cache.containsKey(key)) {
            Object value = params.get(key).get();
            cache.put(key, value);

            // 如果取出来的值是 IParams，且尚未注册，才加入 children
            if (value instanceof IParams && !children.contains(value)) {
                children.add((IParams) value);
            }
        }
        return type.cast(cache.get(key));
    }

    @Override
    public List<IParams> getChildren() {
        return children;
    }

    @Override
    public void clear() {
        params.clear();
        cache.clear();
        children.clear();
    }
}
