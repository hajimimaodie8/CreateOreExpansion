package com.hjmmd_8.createoreexpansion.common.registry;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 给"同一个命名空间下的多个 Registrate"用的数据提供器：给
 * {@link RegistrateDataProvider} 的名字加一个<b>层标签</b>（{@code layerId == null} 时不加）。
 *
 * <p><b>为什么必须区分名字</b>：{@code RegistrateDataProvider#getName()} 是
 * {@code "Registrate Provider for " + modid + " [" + 各子提供器名字 + "]"}，而三层 Registrate 的
 * modid 与子提供器集合<b>完全相同</b>（这是刻意的：命名空间必须都是 {@code createoreexpansion}）
 * → 三个提供器<b>同名</b>；而 {@code DataGenerator.addProvider} 对重名提供器是<b>直接抛异常</b>
 * （{@code IllegalStateException: Duplicate provider: ...}，实测 runData 第二行就炸）。
 * 条目落盘的路径由各子提供器自己决定（与这个名字无关），所以这里改的只是"标签"。</p>
 *
 * <p>COE（{@code layerId == null}）刻意保持与拆分前<b>逐字相同</b>的提供器名字，
 * 连 {@code .cache} 的文件名都不变。</p>
 */
public class LayerDataProvider extends RegistrateDataProvider {

    private final String layerId;

    /**
     * @param parent    目标 Registrate
     * @param namespace 注册命名空间（恒为 {@code createoreexpansion}）
     * @param layerId   层标签（{@code cews} / {@code transmutation}）；{@code null} = 不加前缀（COE）
     * @param event     datagen 事件
     */
    public LayerDataProvider(AbstractRegistrate<?> parent, String namespace, String layerId, GatherDataEvent event) {
        super(parent, namespace, event);
        this.layerId = layerId;
    }

    @Override
    public String getName() {
        return layerId == null ? super.getName() : "[layer " + layerId + "] " + super.getName();
    }
}
