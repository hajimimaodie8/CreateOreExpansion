package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveProcessor;
import com.hjmmd_8.createoreexpansion.util.RadiusScan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * <b>借来的充能源</b>：全能波在"自身没有充能加工能力"的前提下，因<b>外部条件</b>
 * （变器读取半径内存在星辉石应力充能器）而临时获得的充能加工来源。
 *
 * <p><b>机制口径（用户定义）</b>：全能波本身没有充能加工（{@code createoreexpansion:charging} 配方），
 * 但只要变器读取半径内摆着一台星辉石应力充能器，它也能进行充能加工；此时
 * <b>充能加工用的波级 = 那台充能器的发射等级</b>，与波在变体化之前的自身等级<b>无关</b>；
 * 半径内有多台时取发射等级<b>最高</b>的那一台。</p>
 *
 * <p><b>这是"外部条件放行"，不是"本性"</b>：借用充能<b>不改变波型</b>（仍是
 * {@code WaveTypes.OMNI}），也<b>不改变</b> {@code WaveType#allowsChargingProcessing()} 的返回值——
 * 那个方法描述的是"波是什么波"，而这里描述的是"波此刻恰好站在谁旁边"。两者必须分开：
 * 一旦把借用结果写回波型/能力开关，波飞离那台充能器后仍会永久保留充能能力。</p>
 *
 * <p><b>为什么做成"查询结果记录 + 静态解析器"</b>：判定与取值只允许一处实现——
 * "哪些方块算充能源""多台怎么取""半径怎么定""未加载区块怎么跳过"全部收在本类的
 * {@link #resolve} 里；波实体侧只调这一个入口拿结果，不持有任何等级判定或方块扫描。</p>
 *
 * <p><b>为什么不落盘、不同步</b>：借来的充能源是"每次命中的瞬时外部条件"——
 * 波飞走了、充能器被拆了，条件就没了。写进 NBT 会让读档后的波凭空拥有充能能力，
 * 写进 SynchedEntityData 会让客户端与服务端的方块状态产生无意义的同步负担。</p>
 *
 * @param pos       借来的充能源方块位置（星辉石应力充能器；已 {@code immutable()}）
 * @param waveLevel 借用的波级（= 那台充能器的发射等级 1~5；充能配方匹配的等级上限）
 */
public record BorrowedChargingSource(BlockPos pos, int waveLevel) {

	/**
	 * 在波源周围解析"借来的充能源"：以 {@code waveOrigin} 为中心、半径 {@code radius}
	 * （≤0 按 1）遍历方块，取出<b>发射等级最高</b>的星辉石应力充能器。
	 *
	 * <p><b>扫描口径</b>：遍历与守卫（跳过中心格、<b>跳过未加载区块</b>、不触发同步加载）
	 * 全部由 {@link RadiusScan#forEachInRadius} 承担——本模块"半径立方体遍历"的唯一实现，
	 * 这里不再手写 {@code BlockPos.betweenClosed} 循环。</p>
	 *
	 * <p><b>读取口</b>：等级一律取
	 * {@link StellarstoneStressChargerBlockEntity#getManualLevel()}（公开只读方法：
	 * 手动发射波级 1~5，槽未就绪时回退伽马 3）。<b>不使用反射</b>——
	 * 需要镜像内部状态时应当补公开读取口，而不是绕过封装。</p>
	 *
	 * <p><b>调用时机</b>：只允许在<b>命中时</b>调用（掉落物命中 / 方块物品槽命中），
	 * 绝不允许放进每 tick 的飞行循环——波每 tick 都在飞，每 tick 扫方块是不可接受的成本。
	 * 单次命中内的复用由调用方（波实体）缓存，见 {@code StellarWaveEntity} 的命中级缓存字段。</p>
	 *
	 * @param level      所在世界（客户端返回 null：借用充能是服务端行为）
	 * @param waveOrigin 波源位置（变器出射面位置；可 null）
	 * @param radius     解析半径（波携带的变器读取半径；≤0 按 1）
	 * @return 最高等级的那台充能器的借用结果；半径内没有可用的充能器（或被跳过/未加载）时返回 null
	 */
	public static BorrowedChargingSource resolve(Level level, BlockPos waveOrigin, int radius) {
		if (level == null || waveOrigin == null || level.isClientSide)
			return null;
		BlockPos center = waveOrigin.immutable();
		// 单元素数组充当可变游标（forEachInRadius 的 action 无返回值，取最大值须就地累积）
		BorrowedChargingSource[] best = { null };
		RadiusScan.forEachInRadius(level, center, Math.max(1, radius), null, pos -> {
			if (!(level.getBlockEntity(pos) instanceof StellarstoneStressChargerBlockEntity charger))
				return; // 不是充能器：不是充能源（不取"最近的"、也不取其它充能器机型）
			int chargerLevel = charger.getManualLevel();
			// 并列时保留先遇到的那台（结果与遍历顺序无关：只比等级高低）
			if (best[0] == null || chargerLevel > best[0].waveLevel())
				best[0] = new BorrowedChargingSource(pos.immutable(), chargerLevel);
		});
		return best[0];
	}

	/**
	 * 以借来的波级构造充能加工器（= 普通波充能加工的实现
	 * {@link ChargerWaveProcessor}，<b>复用而非复制</b>）。
	 *
	 * <p>波的自身等级只影响"这一台处理器"的配方等级上限，不影响波的飞行/伤害/链式次数——
	 * 借用是"借一台充能器的手"，不是"把波改成那台充能器"。</p>
	 */
	public ChargerWaveProcessor processor(Level level) {
		return new ChargerWaveProcessor(level, waveLevel);
	}
}
