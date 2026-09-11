package com.hjmmd_8.createoreexpansion.mixin;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让强化避雷针参与原版「雷暴自动引雷」：原版 {@code ServerLevel.findLightningRod} 通过 POI 管理器
 * 按 {@link PoiTypes#LIGHTNING_ROD} 类型搜索避雷针，而该 POI 类型只注册了原版方块的状态。
 * 这里把强化避雷针的所有状态映射到原版 lightning_rod 的 POI holder：
 *
 * <ul>
 *     <li>{@code PoiTypes.forState} → 方块放置/破坏（{@code ServerLevel.onBlockStateChange}）
 *         与区块扫描（{@code PoiManager.updateFromSection}）都会自动登记/注销 POI；</li>
 *     <li>{@code PoiTypes.hasPoi} → 区块 section 快速筛选（maybeHas）识别我们的方块；</li>
 *     <li>holder 复用原版 {@code LIGHTNING_ROD}，{@code findLightningRod} 的
 *         {@code is(PoiTypes.LIGHTNING_ROD)} 判定与「地表最高点（露天）」位置过滤全部天然生效。</li>
 * </ul>
 *
 * <p><b>⚠ 本 mixin 禁止引用本 mod 的任何类（2026-09 启动崩溃修复）</b>：{@code PoiTypes} 在
 * {@code BuiltInRegistries.<clinit>}（bootstrap 极早期）就会被加载，Mixin 在<b>类转换阶段</b>
 * 必须解析本类方法体里的每一处静态引用；此时 mod 类加载器尚未就绪——原先写
 * {@code state.is(AllBlocks.REINFORCED_LIGHTNING_ROD.get())} 会触发
 * {@code ClassNotFoundException: ...common.AllBlocks} → {@code MixinPreProcessorException}
 * → 启动直接崩溃。故此处改为<b>注册 id 字符串 + 懒解析</b>：类转换期只需 MC 自带类型
 * （{@link ResourceLocation} 常量），方块引用推迟到方法真正执行时（那时 BLOCK 注册表已就绪）
 * 才从 {@link BuiltInRegistries#BLOCK} 查得。</p>
 */
@Mixin(PoiTypes.class)
public class PoiTypesMixin {

	/** 强化避雷针的方块注册 id（纯字符串常量，不触发 mod 类加载）。 */
	@Unique
	private static final ResourceLocation createoreexpansion$ROD_ID =
		ResourceLocation.fromNamespaceAndPath("createoreexpansion", "reinforced_lightning_rod");

	/** 懒解析的强化避雷针方块引用（首次真正调用时查注册表；查不到保持 null）。 */
	@Unique
	private static Block createoreexpansion$rodBlock;

	/** 缓存的 lightning_rod POI holder（原版注册，运行时必存在） */
	@Unique
	private static Holder<PoiType> createoreexpansion$lightningRodHolder;

	@Unique
	private static boolean createoreexpansion$isReinforcedRod(BlockState state) {
		if (createoreexpansion$rodBlock == null)
			createoreexpansion$rodBlock = BuiltInRegistries.BLOCK.getOptional(createoreexpansion$ROD_ID)
				.orElse(null);
		return createoreexpansion$rodBlock != null && state.is(createoreexpansion$rodBlock);
	}

	@Inject(method = "forState", at = @At("HEAD"), cancellable = true)
	private static void createoreexpansion$forState(BlockState state, CallbackInfoReturnable<Optional<Holder<PoiType>>> cir) {
		if (createoreexpansion$isReinforcedRod(state)) {
			if (createoreexpansion$lightningRodHolder == null)
				createoreexpansion$lightningRodHolder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.LIGHTNING_ROD);
			cir.setReturnValue(Optional.of(createoreexpansion$lightningRodHolder));
		}
	}

	@Inject(method = "hasPoi", at = @At("HEAD"), cancellable = true)
	private static void createoreexpansion$hasPoi(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (createoreexpansion$isReinforcedRod(state))
			cir.setReturnValue(true);
	}
}
