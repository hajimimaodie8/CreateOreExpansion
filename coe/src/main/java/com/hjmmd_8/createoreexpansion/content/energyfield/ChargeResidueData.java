package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * ★ <b>电荷残留的持久化载体（coe-charge 批 7）</b>—— 逐维度一份
 * {@link SavedData}，名字 = {@link ChargeConfigs#RESIDUE_DATA_NAME}。
 *
 * <h2>为什么是 SavedData，以及「零新注册」怎么成立</h2>
 * <p>{@code SavedData} 是<b>维度存档的附件</b>：它不占 {@code Registries} 的任何条目、
 * 不需要 {@code DeferredRegister}、不进数据包、不改 {@code neoforge.mods.toml}
 * ⇒ 本批<b>零新注册</b>（作者裁定的硬条件；关卡有负向断言守着）。
 * 需求 §六 #11 允许的另一条路是「一个不可见的残留实体」，那要注册一个
 * {@code EntityType} 并让三个模块 jar 的自足性跟着动，本批不走。</p>
 *
 * <h2>内存表 + 落盘 = 同一个对象（没有第二份拷贝可以走散）</h2>
 * <p>{@link #residues()} 返回的就是本对象持有的那个 {@code List}：维度加载期间它常驻内存，
 * {@link ChargeResidues#tick} 每个 tick 直接读写它；{@link #save}/{@link #load} 是同一份数据的
 * 序列化与反序列化。任何改动之后<b>必须</b>调 {@link #setDirty()}（否则下次存档不会写盘，
 * 而这些残留与它们那份「已染电实体」账本会一起消失）。</p>
 *
 * <h2>★ 老存档（根本没有这个文件）的行为 = 无残留</h2>
 * <ul>
 *   <li>{@link #get} 走 {@code computeIfAbsent}：文件不存在时维度数据存储<b>直接调工厂的
 *       构造器</b>（{@link #ChargeResidueData()} 给出一张空表），<b>不会</b>调用 {@link #load}
 *       —— 所以「没有这个键」和「有键但列表为空」在行为上完全一样；</li>
 *   <li>{@link ChargeResidues#tick} 走 {@link #find}（可空读表）：老存档里它返回 {@code null}，
 *       于是每 tick 只花一次查表，<b>既不凭空造表、也不会往老存档里写一个空标签</b>；</li>
 *   <li>{@link #load} 读的是 {@code getList(..)}：<b>缺键返回空列表</b>（原版
 *       {@code CompoundTag#getList} 对缺失键返回空 {@code ListTag}，不抛），
 *       所以即使有人手工删掉整个 {@code Residues} 键，结果也只是「没有残留」。</li>
 * </ul>
 * <p>⇒ 老存档进游戏：残留数为 0、无报错、无需任何迁移代码。这就是「老存档缺它 = 无残留」
 * 的可执行形式，关卡对上面每一条都有断言。</p>
 *
 * <h2>键（★ 写清，别再改）</h2>
 * <p>根键 {@link #TAG_RESIDUES} 下每条残留一个 {@code CompoundTag}：坐标
 * {@link #TAG_X} / {@link #TAG_Y} / {@link #TAG_Z}、爆炸等级 {@link #TAG_LEVEL}、
 * 极性 {@link #TAG_POLARITY}（{@code ChargePolarity#name()}）、到期时刻
 * {@link #TAG_EXPIRES_AT}、以及账本 {@link #TAG_CHARGED}
 * （每条一个 {@code CompoundTag}，实体 {@link UUID} 在 {@link #TAG_ENTITY}）。
 * <b>坐标读不到就是 {@code 0}、等级读不到就是 {@code 0}、列表读不到就是空</b>
 * —— 这也是为什么全部用 {@code getXxx} 而不是「先 {@code contains} 再读」：
 * 缺失键的原版默认值恰好就是我们要的「这条残留不成立 / 这个实体没被染过」那一侧。
 * <b>唯一的例外是极性</b>（它的默认值是空串，而 {@code valueOf("")} 会抛）：
 * 它走 {@link #parsePolarity}，认不出的<b>那一条</b>被跳过，读盘这条路一句都不抛
 * （理由与后果见 {@link #load}）。</p>
 */
public class ChargeResidueData extends SavedData {

	/** 根键：残留列表（{@code ListTag} of {@code CompoundTag}）。 */
	private static final String TAG_RESIDUES = "Residues";

	/** 爆炸中心 x（{@code double}）。 */
	private static final String TAG_X = "X";

	/** 爆炸中心 y（{@code double}）。 */
	private static final String TAG_Y = "Y";

	/** 爆炸中心 z（{@code double}）。 */
	private static final String TAG_Z = "Z";

	/** 中和爆炸等级 {@code L}（{@code int}）—— 寿命 / 给的等级 / 区域 / 粒子都由它派生。 */
	private static final String TAG_LEVEL = "Level";

	/** 这条残留的极性（{@code String}，取自 {@code ChargePolarity#name()}）。 */
	private static final String TAG_POLARITY = "Polarity";

	/** 到期时刻（{@code long}，{@code level.getGameTime()} 的绝对值；缺失读作 0 = 立即过期）。 */
	private static final String TAG_EXPIRES_AT = "ExpiresAt";

	/** ★ 账本：已经被这条残留染过电的实体（「同一实体对同一块残留只染一次」的载体）。 */
	private static final String TAG_CHARGED = "Charged";

	/** 账本里一个实体的 {@code UUID}。 */
	private static final String TAG_ENTITY = "Entity";

	/** 本维度当前的残留（<b>活表</b>：改动后必须 {@link #setDirty()}）。 */
	private final List<ChargeResidues.Residue> residues = new ArrayList<>();

	private ChargeResidueData() {
	}

	/** 存档工厂（构造器 + 反序列化器；键名由 {@link ChargeConfigs#RESIDUE_DATA_NAME} 给）。 */
	public static SavedData.Factory<ChargeResidueData> factory() {
		return new SavedData.Factory<>(ChargeResidueData::new, ChargeResidueData::load);
	}

	/**
	 * 取本维度的残留表，<b>不存在就建一张空的</b>（只在真的要写残留时用
	 * —— 见 {@link ChargeResidues#spawn}）。
	 */
	public static ChargeResidueData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(factory(), ChargeConfigs.RESIDUE_DATA_NAME);
	}

	/**
	 * 取本维度的残留表，<b>不存在返回 {@code null}</b>（每个 tick 的读路径用这个：
	 * 从来没有过残留的维度，一次查表就结束，不会凭空造表、也不会往老存档写空标签）。
	 */
	public static ChargeResidueData find(ServerLevel level) {
		return level.getDataStorage().get(factory(), ChargeConfigs.RESIDUE_DATA_NAME);
	}

	/** 本维度残留的<b>活表</b>（{@link ChargeResidues} 直接增删；改完记得 {@link #setDirty()}）。 */
	public List<ChargeResidues.Residue> residues() {
		return residues;
	}

	/** 写盘：每条残留一个复合标签，<b>账本（{@link #TAG_CHARGED}）也在这里写入</b>。 */
	@Override
	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (ChargeResidues.Residue residue : residues) {
			CompoundTag entry = new CompoundTag();
			entry.putDouble(TAG_X, residue.x());
			entry.putDouble(TAG_Y, residue.y());
			entry.putDouble(TAG_Z, residue.z());
			entry.putInt(TAG_LEVEL, residue.explosionLevel());
			entry.putString(TAG_POLARITY, residue.polarity().name());
			entry.putLong(TAG_EXPIRES_AT, residue.expiresAt());
			ListTag charged = new ListTag();
			for (UUID uuid : residue.charged()) {
				CompoundTag entity = new CompoundTag();
				entity.putUUID(TAG_ENTITY, uuid);
				charged.add(entity);
			}
			entry.put(TAG_CHARGED, charged);
			list.add(entry);
		}
		nbt.put(TAG_RESIDUES, list);
		return nbt;
	}

	/**
	 * 读盘：<b>只在文件存在时被调用</b>（文件不存在时维度数据存储直接用构造器造空表）。
	 * 读列表用的是 {@code getList(..)} ⇒ 缺键 = 空表 = 无残留，不抛。
	 *
	 * <p>⚠ <b>只有极性那一格不能靠原版默认值</b>：别的键读不到都落在「这条残留不成立」那一侧
	 * （坐标 0 / 等级 0 / 到期 0 = 立刻过期），而极性的默认值是<b>空串</b>，
	 * {@code ChargePolarity.valueOf("")} 会抛 ⇒ 一次抛就把<b>整个维度</b>的残留列表丢掉
	 * （维度数据存储捕获后记一条错误并返回 null）。所以极性走 {@link #parsePolarity}：
	 * 认不出来的那<b>一条</b>被跳过，同维度的其它残留照常恢复，
	 * <b>读存档这条路一句都不抛</b>。</p>
	 */
	private static ChargeResidueData load(CompoundTag nbt, HolderLookup.Provider registries) {
		ChargeResidueData data = new ChargeResidueData();
		ListTag list = nbt.getList(TAG_RESIDUES, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			ChargePolarity polarity = parsePolarity(entry.getString(TAG_POLARITY));
			if (polarity == null) {
				continue; // 认不出的极性：只丢这一条，不连累同维度的其它残留
			}
			Set<UUID> charged = new HashSet<>();
			ListTag chargedTags = entry.getList(TAG_CHARGED, Tag.TAG_COMPOUND);
			for (int j = 0; j < chargedTags.size(); j++) {
				charged.add(chargedTags.getCompound(j).getUUID(TAG_ENTITY));
			}
			data.residues.add(new ChargeResidues.Residue(
				entry.getDouble(TAG_X),
				entry.getDouble(TAG_Y),
				entry.getDouble(TAG_Z),
				entry.getInt(TAG_LEVEL),
				polarity,
				entry.getLong(TAG_EXPIRES_AT),
				charged));
		}
		return data;
	}

	/**
	 * 极性名 → 枚举；<b>认不出来返回 {@code null}</b>（调用点据此跳过那一条残留）。
	 *
	 * <p>不用 {@code ChargePolarity.valueOf}：它对手工改过 / 写坏的那一格会抛，
	 * 而抛出去等于把整个维度的残留列表一起丢掉（见 {@link #load} 的注释）。
	 * 也不遍历 {@code values()}：两个常量各写一遍，正是「极性只有这两极」这句话本身。</p>
	 */
	private static ChargePolarity parsePolarity(String name) {
		if (ChargePolarity.POSITIVE.name().equals(name)) {
			return ChargePolarity.POSITIVE;
		}
		if (ChargePolarity.NEGATIVE.name().equals(name)) {
			return ChargePolarity.NEGATIVE;
		}
		return null;
	}
}
