package com.hjmmd_8.createoreexpansion.client.ponder;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>本模组的 Ponder 插件（第一层侧）</b>—— 动力角磨床的思索场景从这里进场。
 *
 * <h2>一、它是什么 / 谁调它</h2>
 * <p>Ponder 库（{@code net.createmod.ponder}）只认一个扩展面：{@link PonderPlugin}。
 * Create 自己也是在客户端入口里交一份（{@code CreateClient} 的
 * {@code PonderIndex.addPlugin(new CreatePonderPlugin())}），本类是同一形状的本模组那一份。</p>
 * <p>注册动作 = {@link #registerScenes(PonderSceneRegistrationHelper)} → 本包
 * {@link PowerAngleGrinderPonderScenes#register}；真正的调用点在
 * {@link PowerAngleGrinderPonderClient}（{@code FMLClientSetupEvent}，{@code Dist.CLIENT}）。</p>
 *
 * <h2>二、为什么 modid 取 {@code CoeCore.MOD_ID}</h2>
 * <p>{@code PonderIndex} 的静态块依赖 {@code Minecraft.getInstance()}（客户端专属），所以整条
 * 注册链只能在客户端触发；而 {@code getModId()} 的返回值在这里有<b>两个</b>实打实的用途，
 * 两处都必须等于本模组的<b>命名空间</b>（红线：注册命名空间恒 {@code createoreexpansion}）：</p>
 * <ol>
 *   <li>{@code DefaultPonderSceneRegistrationHelper#asLocation} 用它拼结构资源命名空间 ⇒
 *       决定 {@code assets/createoreexpansion/ponder/**.nbt} 能否命中；</li>
 *   <li>{@code PonderSceneBuilder#title} 用它拼 {@code sceneId} 的命名空间 ⇒ 决定 lang 键前缀
 *       （{@code createoreexpansion.ponder.<path>.header}）。</li>
 * </ol>
 * <p>本模组两项取值相同（{@code MOD_ID == REGISTRY_NAMESPACE == "createoreexpansion"}），
 * 这里按 Ponder 的语义取 {@code MOD_ID}。</p>
 *
 * <h2>三、本批（批 A）不实现的可选面</h2>
 * <ul>
 *   <li>{@code registerTags}：Ponderer 原 JSON 的 {@code "tags"} 是空数组 ⇒ 角磨床<b>不进思索索引页</b>，
 *       只在"悬停物品 + 按住 W"时可见（与作者裁定一致）。</li>
 *   <li>{@code indexExclusions} / {@code onPonderLevelRestore}：本模组无 {@code SmartBlockEntity}
 *       家族需要修复，且没有需要从索引里排除的变体方块。</li>
 * </ul>
 */
public final class PowerAngleGrinderPonderPlugin implements PonderPlugin {

    private PowerAngleGrinderPonderPlugin() {}

    /** 便利构造：调用方（客户端入口）不必知道具体实现类。 */
    public static PonderPlugin create() {
        return new PowerAngleGrinderPonderPlugin();
    }

    @Override
    public String getModId() {
        return CoeCore.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PowerAngleGrinderPonderScenes.register(helper);
    }
}
