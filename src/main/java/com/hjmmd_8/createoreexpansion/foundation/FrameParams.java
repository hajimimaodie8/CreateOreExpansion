package com.hjmmd_8.createoreexpansion.foundation;

import com.google.common.collect.Maps;

import java.util.*;
import java.util.function.Supplier;

public class FrameParams implements IParams {
    private final Map<String, Supplier<Object>> params = Maps.newHashMap();
    private final Map<String, Object> cache = Maps.newHashMap();
    private final List<IParams> children = new ArrayList<>();

    public FrameParams() {}

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

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }

    public FrameParams putChild(String key, Supplier<IParams> childSupplier) {
        params.put(key, childSupplier::get);
        return this;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        if (!cache.containsKey(key)) {
            Supplier<Object> supplier = params.get(key);
            if (supplier == null) {
                throw new NullPointerException(
                        dump("Key '" + key + "' not found"));
            }

            Object value;
            try {
                value = supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(
                        dump("Failed to compute value for key '" + key + "'"), e);
            }

            if (value == null) {
                throw new NullPointerException(
                        dump("Value for key '" + key + "' is null (expected " + type.getSimpleName() + ")"));
            }

            if (!type.isInstance(value)) {
                throw new ClassCastException(
                        dump("Value for key '" + key + "' is " + value.getClass().getSimpleName()
                                + ", expected " + type.getSimpleName()));
            }

            cache.put(key, value);

            if (value instanceof IParams && !children.contains(value)) {
                children.add((IParams) value);
            }
        }

        Object cached = cache.get(key);
        try {
            return type.cast(cached);
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    dump("Cached value for key '" + key + "' is " + cached.getClass().getSimpleName()
                            + ", expected " + type.getSimpleName()));
        }
    }

    /**
     * 递归打印当前参数树
     */
    public String dump(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("FrameParams Dump\n");
        sb.append("Message: ").append(message).append("\n");
        sb.append("========================================\n");
        dumpTree(sb, 0);
        sb.append("========================================\n");
        return sb.toString();
    }

    private void dumpTree(StringBuilder sb, int indent) {
        String pad = "  ".repeat(indent);

        for (Map.Entry<String, Supplier<Object>> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = cache.getOrDefault(key, null);

            switch (value) {
                case null -> sb.append(pad).append("  ").append(key).append(" = <not evaluated>\n");
                case FrameParams child -> {
                    sb.append(pad).append("  ").append(key).append(":\n");
                    child.dumpTree(sb, indent + 2);
                }
                case IParams child -> sb.append(pad).append("  ").append(key).append(" = IParams@")
                        .append(Integer.toHexString(System.identityHashCode(child))).append("\n");
                default -> sb.append(pad).append("  ").append(key).append(" = ").append(value)
                        .append(" (").append(value.getClass().getSimpleName()).append(")\n");
            }
        }
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