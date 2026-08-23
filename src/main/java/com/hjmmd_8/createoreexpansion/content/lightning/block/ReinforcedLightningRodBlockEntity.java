package com.hjmmd_8.createoreexpansion.content.lightning.block;

import java.awt.Color;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.lightning.ReinforcedLightningRodEffects;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 强化避雷针方块实体：伽马能量波充能状态（全部<b>私有封装</b>，仅通过公开方法读写）。
 *
 * <p>两条充能路径互不冲突：</p>
 * <ul>
 *     <li>路径 A【原版原生】：被自然闪电击中——原版方块逻辑（Block 层）自动生效，维持原版全部行为；</li>
 *     <li>路径 B【伽马能量波】：{@link #onGammaWaveHit()} 每次 +1，攒满 {@link #MAX_CHARGE}
 *         获得 1 次可手动释放的引雷机会并重置进度。</li>
 * </ul>
 *
 * <p>方块被破坏掉落时物品<b>不携带</b>充能状态（充能 NBT 仅存于方块实体，破坏即清空）。</p>
 */
public class ReinforcedLightningRodBlockEntity extends BlockEntity implements IHaveGoggleInformation {

	/** 伽马充能最大总进度（攒满获得 1 次手动引雷机会） */
	public static final int MAX_CHARGE = 10;
	/** 手动引雷释放冷却（tick；8 秒 = 160 tick） */
	public static final int RELEASE_COOLDOWN_TICKS = 160;
	/** 有充能时金色粒子的播放间隔（tick） */
	private static final int PARTICLE_INTERVAL = 10;

	/** 伽马能量波充能进度（私有，仅方法读写） */
	private int gammaChargeProgress;
	/** 已获得的、可手动释放的引雷次数（私有） */
	private int readyCharges;
	/** 手动引雷释放冷却剩余 tick（私有） */
	private int releaseCooldown;
	/** 客户端粒子播放计时（私有） */
	private int particleTick;

	public ReinforcedLightningRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ========== 充能（路径 B：伽马能量波） ==========

	/**
	 * 伽马能量波命中充能：进度 +1；攒满 {@link #MAX_CHARGE} 获得 1 次引雷机会。
	 * 就绪后（进度满格）停止充能，等待释放后才重新开始攒。
	 * 外部事件（能量波实体/附属模组）直接调用，不与该波实体耦合。
	 */
	public void onGammaWaveHit() {
		if (gammaChargeProgress >= MAX_CHARGE)
			return; // 已就绪：停止充能（进度条停在满格 + 就绪提示），释放后才重新攒
		gammaChargeProgress++;
		if (gammaChargeProgress >= MAX_CHARGE) {
			readyCharges++; // 获得 1 次可手动释放的引雷机会
		}
		setChanged();
		syncToClient();
		if (level != null && !level.isClientSide)
			ReinforcedLightningRodEffects.spawnChargeParticles(level, worldPosition);
	}

	// ========== 手动释放 ==========

	/**
	 * 手动释放引雷：消耗引雷机会，在方块上方生成闪电。
	 * 进度满格（攒满 {@link #MAX_CHARGE}）才可释放；露天校验（上方有遮挡不可释放，
	 * 与原版规则一致）+ 冷却防连点。无充能/冷却中/未露天时返回 PASS（不消费右键）。
	 */
	public ItemInteractionResult tryReleaseLightning(Player player) {
		if (level == null || level.isClientSide)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (releaseCooldown > 0 || readyCharges <= 0 || gammaChargeProgress < MAX_CHARGE)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		// 露天校验（继承原版规则）：方块上方有遮挡，满充也无法释放
		if (!level.canSeeSky(worldPosition))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		readyCharges = 0; // 单段制：释放后清空（不留累积残留）
		gammaChargeProgress = 0; // 释放后重新开始攒（进度条归零）
		releaseCooldown = RELEASE_COOLDOWN_TICKS;
		ReinforcedLightningRodEffects.spawnLightning(level, worldPosition);
		setChanged();
		syncToClient();
		return ItemInteractionResult.SUCCESS;
	}

	// ========== 状态查询（公开方法） ==========

	/** 当前伽马充能进度（tooltip 用） */
	public int getGammaChargeProgress() {
		return gammaChargeProgress;
	}

	/** 可手动释放的引雷次数 */
	public int getReadyCharges() {
		return readyCharges;
	}

	/** 是否处于引雷就绪状态：进度满格且持有引雷机会（就绪粒子/tooltip 判定） */
	public boolean hasReadyCharge() {
		return gammaChargeProgress >= MAX_CHARGE && readyCharges > 0;
	}

	// ========== 护目镜提示（工程师护目镜显示伽马充能进度） ==========

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		// 标题行前加 4 空格缩进（护目镜物品图标画在 tooltip 左上角，会盖住第一行开头；
		// Create 惯例 forGoggles 同样缩进 4 格）
		tooltip.add(Component.literal("    ")
			.append(Component.translatable("block.createoreexpansion.reinforced_lightning_rod"))
			.withStyle(ChatFormatting.GRAY));
		// 伽马充能进度：冒号后直接跟竖线进度条（金色）
		// 注意：withStyle 必须在 append 之前调用，否则会把进度条也染成白色
		tooltip.add(Component.translatable("createoreexpansion.tooltip.lightning_rod.charge")
			.withStyle(ChatFormatting.WHITE)
			.append(BarTooltipRender.energy(gammaChargeProgress, MAX_CHARGE, MAX_CHARGE, new Color(0xFFD700))));
		// 引雷就绪提示：进度满格才显示（避免旧存档残留的累积 readyCharges 造成"没满就就绪"）
		if (gammaChargeProgress >= MAX_CHARGE) {
			tooltip.add(Component.translatable("createoreexpansion.tooltip.lightning_rod.ready")
				.withStyle(ChatFormatting.GOLD));
		}
		return true;
	}

	// ========== tick：冷却递减 + 粒子 ==========

	public void tick() {
		if (level == null)
			return;
		if (level.isClientSide) {
			// 有引雷充能：播放金色电光粒子
			if (hasReadyCharge() && ++particleTick % PARTICLE_INTERVAL == 0)
				ReinforcedLightningRodEffects.spawnChargeParticles(level, worldPosition);
			return;
		}
		if (releaseCooldown > 0 && --releaseCooldown == 0) {
			setChanged();
			syncToClient();
		}
	}

	/**
	 * 将充能状态同步到客户端：blockChanged 会发送 ClientboundBlockEntityDataPacket
	 * （走 getUpdateTag → 客户端 loadAdditional），护目镜进度条/就绪粒子才能拿到最新值。
	 * （sendBlockUpdated 只更新方块状态，不含 BE NBT，进度条仍会是旧值）
	 */
	private void syncToClient() {
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.getChunkSource().blockChanged(worldPosition);
		}
	}

	// ========== NBT 序列化（充能状态仅存于方块实体，破坏掉落不携带） ==========

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("GammaChargeProgress", gammaChargeProgress);
		tag.putInt("ReadyCharges", readyCharges);
		tag.putInt("ReleaseCooldown", releaseCooldown);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		gammaChargeProgress = tag.getInt("GammaChargeProgress");
		readyCharges = tag.getInt("ReadyCharges");
		releaseCooldown = tag.getInt("ReleaseCooldown");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	/**
	 * 关键：vanilla 默认返回 null，ChunkHolder 广播方块更新时会跳过（拿不到包，什么都不发）。
	 * Create 的机器（SyncedBlockEntity）正是覆写了此方法才能正常同步 BE 数据。
	 */
	@Override
	public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}
}
