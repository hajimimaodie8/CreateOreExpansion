package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import org.joml.Vector3f;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * <b>回旋镖的存档读写</b>（2026-10-03 行为零变化拆分，从 {@code AbstractBoomerangEntity} 的
 * {@code readAdditionalSaveData} / {@code addAdditionalSaveData} 方法体逐字搬出）。
 *
 * <p>⚠ {@code super.readAdditionalSaveData(tag)} / {@code super.addAdditionalSaveData(tag)}
 * <b>不在这里</b>——它们是 Quark bug a 的修复（owner 的标准持久化在 {@code Projectile} 里），
 * 留在实体的两个覆写上，且必须在调本类之前。</p>
 */
final class BoomerangEntitySaveData {

	private BoomerangEntitySaveData() {
	}

	static void read(AbstractBoomerangEntity host, CompoundTag tag) {
		host.liveTime = tag.getInt("LiveTime");
		host.returnTicks = tag.getInt("ReturnTicks");
		host.hitCount = tag.getInt("HitCount");
		host.slot = tag.getInt("Slot");
		// 本次飞行的耐久账（批 2）：不读回来 = 重载一次就能把欠的耐久一笔勾销。
		host.flightWear = tag.getInt("FlightWear");
		// 穿刺额度（批 3）：-1 表示"还没初始化"（那时不写键，读回来仍是 -1，第一次命中再算）；
		// 已用掉一部分的额度必须读回来，否则"飞出去半趟、卸载区块"就能白刷一份额度。
		host.setPierceMobsLeft(tag.contains("PierceMobsLeft") ? tag.getInt("PierceMobsLeft") : -1);
		host.setPierceBlocksLeft(tag.contains("PierceBlocksLeft") ? tag.getInt("PierceBlocksLeft") : -1);
		// 技能携带标记（同前）：没有该键 = 老存档/没带 ⇒ false（与"不按技能键"同义）
		host.setPierceSkillCarried(tag.getBoolean("PierceSkillCarried"));
		host.setOrbitSkillCarried(tag.getBoolean("OrbitSkillCarried"));
		// 花瓣曲线状态（批 2）：模式 + 锚点 + 基准角 + 进度（写侧见 addAdditionalSaveData）。
		if (tag.getBoolean("PetalFlight")) {
			host.getEntityData().set(AbstractBoomerangEntity.DATA_PETAL, true);
			host.getEntityData().set(AbstractBoomerangEntity.DATA_PETAL_ORIGIN, new Vector3f(
				(float) tag.getDouble("PetalOriginX"),
				(float) tag.getDouble("PetalOriginY"),
				(float) tag.getDouble("PetalOriginZ")));
			host.getEntityData().set(AbstractBoomerangEntity.DATA_PETAL_ANGLE, (float) tag.getDouble("PetalAngle"));
			host.petalProgress = tag.getDouble("PetalProgress");
		}
		// 投掷原点（可选键：键在 ⇒ 已记录。重载后不许重记，否则基准会被挪到重载点）
		if (tag.contains("ThrowOriginX")) {
			host.originX = tag.getDouble("ThrowOriginX");
			host.originY = tag.getDouble("ThrowOriginY");
			host.originZ = tag.getDouble("ThrowOriginZ");
			host.originRecorded = true;
		}
		// 镖本身（含扣过的能量）也要能跨区块重载；老存档没有该键时保持 EMPTY 的兜底形状。
		if (tag.contains("BoomerangStack")) {
			host.setItemStack(ItemStack.parseOptional(host.registryAccess(), tag.getCompound("BoomerangStack")));
		}
		// 回程段是同步值，但它同时驱动 noPhysics：重载后要把本侧的状态补齐
		if (host.getEntityData().get(AbstractBoomerangEntity.DATA_RETURNING)) {
			host.noPhysics = true;
		}
	}

	static void write(AbstractBoomerangEntity host, CompoundTag tag) {
		tag.putInt("LiveTime", host.liveTime);
		tag.putInt("ReturnTicks", host.returnTicks);
		tag.putInt("HitCount", host.hitCount);
		tag.putInt("Slot", host.slot);
		// 本次飞行的耐久账（2026-10-02 批 2）：跨区块重载不许把欠账抹掉，否则
		// "飞出去一趟正好让区块卸载"就成了躲避爆掉的捷径。
		tag.putInt("FlightWear", host.flightWear);
		// 穿刺额度（2026-10-02 批 3）：记过才写（-1 = 还没初始化就不写键，
		// 读回来仍是 -1 ⇒ 第一次命中时按当时的等级现算，与"从没打过东西"完全等价）。
		if (host.pierceMobsLeft() >= 0) {
			tag.putInt("PierceMobsLeft", host.pierceMobsLeft());
			tag.putInt("PierceBlocksLeft", host.pierceBlocksLeft());
		}
		// 技能携带标记（作者 2026-10-02 第二次裁定）：投掷那一刻的按键结果，重载后必须还在，
		// 否则"区块卸载再回来"会把这一发带的效果（以及环绕波的生成资格）抹掉。
		tag.putBoolean("PierceSkillCarried", host.isPierceSkillCarried());
		tag.putBoolean("OrbitSkillCarried", host.isOrbitSkillCarried());
		// 花瓣曲线状态（2026-10-02 批 2）：模式 + 锚点 + 基准角 + 进度。
		// ⚠ 这三项平时走同步数据（两端要一起算曲线），但同步数据不进存档 ⇒ 重载一次就会
		// 退化成"点按直线"（进度归零 = 从头再飞一瓣），所以必须各自落一份 NBT。
		if (host.isPetalFlight()) {
			Vector3f petalOrigin = host.getEntityData().get(AbstractBoomerangEntity.DATA_PETAL_ORIGIN);
			tag.putBoolean("PetalFlight", true);
			tag.putDouble("PetalOriginX", petalOrigin.x());
			tag.putDouble("PetalOriginY", petalOrigin.y());
			tag.putDouble("PetalOriginZ", petalOrigin.z());
			tag.putDouble("PetalAngle", host.getEntityData().get(AbstractBoomerangEntity.DATA_PETAL_ANGLE));
			tag.putDouble("PetalProgress", host.petalProgress);
		}
		// 投掷原点：记过才写（没记过就不写键，读回来仍是"未记录"）
		if (host.originRecorded) {
			tag.putDouble("ThrowOriginX", host.originX);
			tag.putDouble("ThrowOriginY", host.originY);
			tag.putDouble("ThrowOriginZ", host.originZ);
		}
		ItemStack stack = host.getItemStack();
		if (!stack.isEmpty()) {
			tag.put("BoomerangStack", stack.save(host.registryAccess()));
		}
	}
}
