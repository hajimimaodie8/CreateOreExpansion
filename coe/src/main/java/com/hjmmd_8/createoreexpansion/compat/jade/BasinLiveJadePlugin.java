package com.hjmmd_8.createoreexpansion.compat.jade;

import com.simibubi.create.content.processing.basin.BasinBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * <b>工作盆物品行的实时化</b>（可选 Jade 集成的插件，见 {@code CreateOreExpansion} 的反射加载）。
 *
 * <p>本插件只做一件事：把 {@link BasinLiveItemStorage} 挂到工作盆上，
 * 让 <b>Jade 自带的那条物品行</b>改由本模组提供数据——而不是像旧实现那样在原生行下面
 * 再补一行"工作盆内容（实时）"文字（用户 2026-09 反馈：那样太生硬，应该替换/覆盖掉原生行）。</p>
 *
 * <p>覆盖之所以成立：Jade 的物品存储提供者按<b>方块类（先）→ 目标对象类（后）</b>两级查找，
 * 同一张表内按优先级升序（<b>数值越小越优先</b>），取第一个返回非 null 的；
 * 通用的那条注册在 {@code Block.class}（优先级 9999），本条注册在 {@code BasinBlock.class}
 * （优先级 1000）→ 工作盆只会用本条。详见 {@link BasinLiveItemStorage} 的类注释。</p>
 */
@WailaPlugin
public class BasinLiveJadePlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration registration) {
		// 必须注册"方块类"：按方块实体类注册会在两级查找里排到后面，抢不过通用物品存储
		registration.registerItemStorage(BasinLiveItemStorage.INSTANCE, BasinBlock.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		// 客户端：按 uid 认领服务端那份数据，渲染复用 Jade 原生口径
		registration.registerItemStorageClient(BasinLiveItemStorage.INSTANCE);
	}
}
