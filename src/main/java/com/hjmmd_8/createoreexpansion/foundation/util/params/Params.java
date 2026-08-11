package com.hjmmd_8.createoreexpansion.foundation.util.params;

import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Params implements IParams {
    private final Map<String, Object> params = Maps.newHashMap();
    private final List<IParams> children = new ArrayList<>();

    public static Params create() {
        return new Params();
    }

    public <T> Params put(String key, T value) {
        params.put(key, value);
        return this;
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(params.get(key));
    }

    public Params putChild(String key, IParams child) {
        params.put(key, child);
        children.add(child);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public List<IParams> getChildren() {
        return children;
    }

    public void clear() {
        params.clear();
        children.clear();
    }
}
