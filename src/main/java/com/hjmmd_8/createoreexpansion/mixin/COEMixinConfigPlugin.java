package com.hjmmd_8.createoreexpansion.mixin;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.FMLLoader;

/**
 * Mixin 配置插件：createaddition（CC&amp;A）未安装时跳过依赖它的 Mixin。
 *
 * <p>CC&amp;A 是可选依赖（runtimeOnly）：{@link ChargingRecipeAssemblyMixin} 的目标类
 * 位于 CC&amp;A jar 内，若玩家未安装 CC&amp;A，Mixin 应用会因找不到目标类而报错。
 * 本插件在 Mixin 应用前检查 CC&amp;A 是否在加载模组列表中，未安装则返回空列表。</p>
 *
 * <p><b>注意</b>：Mixin 应用发生在 PREPARE 阶段（{@code ModList.get()} 尚为 null），
 * 必须用 {@link FMLLoader#getLoadingModList()} 查询，不能访问 {@code ModList.get()}。</p>
 */
public class COEMixinConfigPlugin implements IMixinConfigPlugin {

	private static boolean createAdditionLoaded;

	static {
		try {
			createAdditionLoaded = FMLLoader.getLoadingModList()
				.getModFileById("createaddition") != null;
		} catch (Throwable e) {
			createAdditionLoaded = false;
		}
	}

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		// ChargingRecipeAssemblyMixin 目标类在 CC&A 内：未安装 CC&A 时不应用
		if (mixinClassName.equals("com.hjmmd_8.createoreexpansion.mixin.ChargingRecipeAssemblyMixin"))
			return createAdditionLoaded;
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
