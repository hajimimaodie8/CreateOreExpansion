package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.google.common.collect.Lists;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 实体描边预览的「发光登记表」——渲染器与 mixin 之间的隐式协议，现在只有一套。
 *
 * <h2>它为什么必须是一个独立的类</h2>
 * <p>实体描边的"边缘发光"是两段配合：</p>
 * <ol>
 *   <li>{@link CoeEntityOutlineRenderer#render} 把当前目标实体塞进这张表；</li>
 *   <li>{@code mixin/renderers/MinecraftMixin} 在
 *       {@code Minecraft#shouldEntityAppearGlowing} 的 {@code @At("TAIL")} 读这张表
 *       （读到即移除），让实体走原版发光渲染、用我们设好的 outline 颜色。</li>
 * </ol>
 * <p>这个静态字段原先挂在旧类 {@code client/tool/renderer/EntityOutlineRenderer} 上，
 * 于是"读它的人"（mixin）与"写它的人"（新渲染器）跨了两个框架 —— 旧栈删除时
 * <b>字段必须比旧类先搬走</b>，否则 {@code MinecraftMixin} 立刻编译不过。搬到这里之后，
 * 写方（{@link CoeEntityOutlineRenderer}）与读方（mixin）都指向本类，不再有第二个字段。</p>
 *
 * <p><b>为什么不是 {@link CoeEntityOutlineRenderer} 自己的静态字段</b>：mixin 读的是
 * Minecraft 的静态注入点，指向"谁的字段"只是一个 import 的距离；独立成类可以把
 * "这是跨类的隐式协议"这件事写在类注释里，而不是藏在渲染器的私有细节中。
 * （旧类上那个返回 {@code List.of()} 的 {@code glowingEntities()} 方法从来没被使用过，
 * 是纯粹的死代码 + 误导性命名，已随旧类删除，不再在新类上复制。）</p>
 *
 * <p><b>并发与生命周期</b>：只在客户端渲染线程被触碰（渲染器每帧 {@code clear()} 后写入，
 * mixin 在同一个渲染线程里读后移除），没有跨线程访问，不需要同步。
 * 用 Guava 的 {@code Lists.newArrayList()} 与旧字段逐字一致（可 {@code contains} + {@code remove}）。</p>
 *
 * @since 1.0.0
 */
public final class GlowingEntities {

    /**
     * 需要走原版发光渲染的实体（渲染器每帧写入，mixin 读到即移除）。
     *
     * <p>旧字段名是 {@code EntityOutlineRenderer.glowingEntities}；改名成常量风格是
     * 为了让"这是一个全局可变登记表"在调用点一眼可见，语义未变。</p>
     */
    public static final List<Entity> ENTITIES = Lists.newArrayList();

    private GlowingEntities() {
        throw new AssertionError("This class should not be instantiated");
    }
}
