package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 能量场持久化（每维度一份 SavedData）。
 *
 * <p>场的实体状态（{@link EnergyFields} 内存表）在退出世界/重启服务端后会清空；
 * 本类把当前维度注册的场写入世界存档，世界加载时经 {@link EnergyFields#restore}
 * 重新灌回内存表——退出重进后场仍在（护目镜指示 + 带电波受场作用都不丢）。</p>
 *
 * <p>调试命令建/清场与后续控制器方块刷新场时都会更新本存档。</p>
 */
public class EnergyFieldSavedData extends SavedData {

	private static final String DATA_NAME = "createoreexpansion_energy_fields";

	private final List<EnergyField> fields = new ArrayList<>();

	public static SavedData.Factory<EnergyFieldSavedData> factory() {
		return new SavedData.Factory<>(EnergyFieldSavedData::new, EnergyFieldSavedData::load);
	}

	public static EnergyFieldSavedData get(ServerLevel level) {
		return level.getDataStorage()
			.computeIfAbsent(factory(), DATA_NAME);
	}

	/** 用当前内存表中的场覆盖存档（增/删/清场后调用）。<b>机器配对的场（带 source）不入档</b>
	 * —— 它们由方块位置决定，重进世界后由控制器 tick 自动重新产出，无需持久化。 */
	public void saveFrom(ServerLevel level) {
		fields.clear();
		for (EnergyField f : EnergyFields.in(level)) {
			if (!f.isMachineProduced())
				fields.add(f);
		}
		setDirty();
	}

	/** 存档内恢复出的场（世界加载时灌回内存表）。 */
	public List<EnergyField> getFields() {
		return List.copyOf(fields);
	}

	@Override
	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (EnergyField f : fields) {
			AABB r = f.region();
			Vec3 d = f.direction();
			CompoundTag c = new CompoundTag();
			c.putString("Type", f.type().name());
			c.putDouble("MinX", r.minX);
			c.putDouble("MinY", r.minY);
			c.putDouble("MinZ", r.minZ);
			c.putDouble("MaxX", r.maxX);
			c.putDouble("MaxY", r.maxY);
			c.putDouble("MaxZ", r.maxZ);
			c.putDouble("DirX", d.x);
			c.putDouble("DirY", d.y);
			c.putDouble("DirZ", d.z);
			c.putDouble("Strength", f.strength());
			list.add(c);
		}
		nbt.put("Fields", list);
		return nbt;
	}

	private static EnergyFieldSavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
		EnergyFieldSavedData sd = new EnergyFieldSavedData();
		ListTag list = nbt.getList("Fields", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag c = list.getCompound(i);
			sd.fields.add(new EnergyField(
				EnergyFieldType.valueOf(c.getString("Type")),
				new AABB(c.getDouble("MinX"), c.getDouble("MinY"), c.getDouble("MinZ"),
					c.getDouble("MaxX"), c.getDouble("MaxY"), c.getDouble("MaxZ")),
				new Vec3(c.getDouble("DirX"), c.getDouble("DirY"), c.getDouble("DirZ")),
				c.getDouble("Strength")));
		}
		return sd;
	}

	private EnergyFieldSavedData() {
	}
}
