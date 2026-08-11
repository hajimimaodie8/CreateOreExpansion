package com.hjmmd_8.createoreexpansion.foundation.util.params;

import java.util.List;

public interface IParams {
    <T> IParams put(String key, T value);

    <T> T get(String key, Class<T> type);

    List<IParams> getChildren();

    IParams putChild(String key, IParams child);

    boolean isEmpty();

    void clear();
}
