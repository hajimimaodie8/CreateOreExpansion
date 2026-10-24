package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.Optional;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * <b>7 个同步视图与 {@link WaveState} 之间的唯一映射</b>（2026-10-24 波系统重构批 3，方案 §3.2）。
 *
 * <p><b>不新增任何 accessor</b>（方案 R-6 / 本批硬要求 1）：那 7 个
 * {@code SynchedEntityData.defineId} 仍原样声明在 {@code AbstractChargerWaveEntity} 里，
 * 序列化器、缺省值、{@code define} 顺序一个字都没动；本类只负责"把状态里的 7 个值按旧口径
 * 推送到那 7 个视图"——{@code Charge} 的 int 编码、{@code WaveType} 的 id 字符串、
 * {@code Essence} 的<b>魔素 id</b>（内置 = 枚举名，批 4 起第三方 = {@code namespace:path}）、
 * {@code Owner} 的 {@code Optional<UUID>}，这四种编码就是改造前
 * 散在各 setter 里的那段代码，现在只有这一份。</p>
 *
 * <p><b>为什么不做反向（同步值 → 状态）</b>：客户端本来就没有权威状态（波是 {@code noSave()}、
 * 服务端权威），改造前的读法也是"服务端读字段、客户端读同步值"；本批把这个读法原样保留在
 * 实体的 7 个 getter 里，所以<b>客户端行为逐字不变</b>。第三方要加同步属性时，方案给的口径是
 * 一个 {@code EXTRA} compound 袋（同样不扩单个 accessor），但那属于后续批次（B6），本批不做。</p>
 */
public final class WaveStateSync {

	private WaveStateSync() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 7 个同步视图的句柄（由实体构造一次；accessor 的声明仍在实体类里）。
	 *
	 * <p>做成一个小对象只是为了避免每次推送都传 7 个参数；它<b>不持有值</b>，只是把 accessor
	 * 引用捆在一起。</p>
	 */
	public static final class Views {

		private final EntityDataAccessor<Integer> level;
		private final EntityDataAccessor<Float> speedOffset;
		private final EntityDataAccessor<Integer> charge;
		private final EntityDataAccessor<String> waveType;
		private final EntityDataAccessor<Integer> firingBatch;
		private final EntityDataAccessor<String> essence;
		private final EntityDataAccessor<Optional<UUID>> owner;

		public Views(EntityDataAccessor<Integer> level, EntityDataAccessor<Float> speedOffset,
			EntityDataAccessor<Integer> charge, EntityDataAccessor<String> waveType,
			EntityDataAccessor<Integer> firingBatch, EntityDataAccessor<String> essence,
			EntityDataAccessor<Optional<UUID>> owner) {
			this.level = level;
			this.speedOffset = speedOffset;
			this.charge = charge;
			this.waveType = waveType;
			this.firingBatch = firingBatch;
			this.essence = essence;
			this.owner = owner;
		}
	}

	/** 电荷 → int 编码（0 无 / 1 正 / 2 负）；旧代码在两个 setter/读档里各写了一遍，现在只此一处。 */
	public static int chargeCode(ChargePolarity charge) {
		return charge == null ? 0 : charge == ChargePolarity.POSITIVE ? 1 : 2;
	}

	/**
	 * 把状态里的 7 个值推送到同步视图。
	 *
	 * <p>值没变时 {@link SynchedEntityData#set} 本身是空操作（不置脏），所以"每次都推 7 个"
	 * 不会产生额外网络流量，也不会改变旧 setter 只推一个字段的可观察行为。</p>
	 */
	public static void push(WaveState state, SynchedEntityData data, Views views) {
		data.set(views.level, state.get(WaveAttrs.LEVEL));
		data.set(views.speedOffset, (float) (double) state.get(WaveAttrs.SPEED_OFFSET));
		data.set(views.charge, chargeCode(state.get(WaveAttrs.CHARGE)));
		data.set(views.waveType, state.get(WaveAttrs.WAVE_TYPE)
			.id()
			.toString());
		data.set(views.firingBatch, state.get(WaveAttrs.FIRING_BATCH));
		// 2026-10-24 批 4：写的是魔素的 id。内置 11 个的 id 逐字等于枚举名 ⇒ 同步字节与改造前相同；
		// 第三方魔素写 namespace:path（客户端按同一个注册表解析回来）。
		WaveEssence essence = state.get(WaveAttrs.ESSENCE);
		data.set(views.essence, essence == null ? "" : essence.id());
		UUID owner = state.get(WaveAttrs.OWNER);
		data.set(views.owner, owner == null ? Optional.empty() : Optional.of(owner));
	}
}
