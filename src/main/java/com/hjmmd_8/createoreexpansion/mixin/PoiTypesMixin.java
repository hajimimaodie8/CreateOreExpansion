package com.hjmmd_8.createoreexpansion.mixin;

import java.util.Optional;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
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
 */
@Mixin(PoiTypes.class)
public class PoiTypesMixin {

	/** 缓存的 lightning_rod POI holder（原版注册，运行时必存在） */
	@Unique
	private static Holder<PoiType> createoreexpansion$lightningRodHolder;

	@Unique
	private static boolean createoreexpansion$isReinforcedRod(BlockState state) {
		return state.is(AllBlocks.REINFORCED_LIGHTNING_ROD.get());
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
