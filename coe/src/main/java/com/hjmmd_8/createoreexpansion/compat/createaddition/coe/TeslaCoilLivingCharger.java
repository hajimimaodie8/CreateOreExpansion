package com.hjmmd_8.createoreexpansion.compat.createaddition.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.energyfield.ChargeApi;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>获得途径 #3：靠近「通有足够电量」的 CC&amp;A 特斯拉线圈 ⇒ 随机染上一种电荷</b>
 * （coe-charge 批 5；需求 §3.2 表格第 3 行）。
 *
 * <h2>★ 为什么住在 {@code :coe}（{@code compat/createaddition/coe/}）而不是 {@code :cews}</h2>
 * <p>需求把这件事写成「<b>只新增</b>『靠近给生物』这一条，<b>不改</b>特斯拉线圈的既有行为」，
 * 而既有的「只认波」那条桥在第二层（{@code cews} 的 {@code TeslaCoilWaveCharger}，
 * 由 {@code WaveMachineIntegrationPoints} 登记、只给<b>波</b>赋电）。两个位置都能编译
 * （{@code CEWS → COE} 是合法方向），本批选 {@code :coe}，理由四条：</p>
 * <ol>
 *   <li><b>四条获得途径必须同层同寿</b>：另外三条（雷击 / 雷鸣合金技能 / 带电波）全在
 *       {@code :coe}，而 {@code :coe} 是本模组的第一层（只装 {@code coe.jar} 也必须在场）。
 *       若把这一条放进 {@code :cews}，单装第一层时它会<b>静默消失</b>，四条途径变成三条，
 *       而且没有任何报错 —— 这正是本仓反复踩过的「静默少一条」形状。</li>
 *   <li><b>施加面与数值真源都在 {@code :coe}</b>：{@link ChargeApi} /
 *       {@link ChargeConfigs} / 两个效果 / 挡牛奶守卫 / 途径 #2 的处理器都住在
 *       {@code content/energyfield/charge/}；「靠近给生物」贴着它们放，改一处就改一处；</li>
 *   <li><b>已有先例</b>：{@code :coe} 早就有 {@code compat/createaddition/coe/CreateAdditionCompat}
 *       （雷击转化顺带执行 CC&amp;A 充电配方），"第一层的 CC&amp;A 隔离"是既成形状；</li>
 *   <li><b>绝不能借既有那条的账</b>：{@code TeslaCoilWaveCharger} 把「下一次可赋电的 gameTime」
 *       写在线圈 BE 的持久数据 {@code co_wave_charge_cd_until} 上（每次赋电 1000 FE + 20 tick
 *       冷却）。生物这条若共用那个键，就会<b>顺手抑制波的赋电</b>（既有行为被改），还要在第二处
 *       复制那个字符串键。本类因此<b>只读电量、不写任何线圈状态</b>。</li>
 * </ol>
 * <p>代价（如实记下）：线圈内部的口径现在有两个读者（CEWS 的波桥 + 本类），
 * 将来 CC&amp;A 改了 API 要改两处；本类刻意只做「读电量」这一件最小的事来控制这个代价。</p>
 *
 * <h2>限频：逐实体，不是全局节拍</h2>
 * <p>判据是<b>每个实体自己的 {@code tickCount}</b> 取模
 * {@link ChargeConfigs#TESLA_COIL_CHECK_INTERVAL_TICKS}（= 每 40 tick 判一次，且各实体的相位
 * 天然错开）—— <b>不是</b> {@code level.getGameTime() %}（那会让全世界同 tick 一起扫）、
 * 也不是任何全局计数器。加上「身上已经有电荷 ⇒ 直接返回」（与既有「只认波」那条对已带电的波
 * 不再赋电同口径），站在线圈旁边只会拿到<b>一次</b>电荷：等级与时长都不会被反复刷新
 * （需求 §5.2 的「不会每 tick 刷」）。</p>
 *
 * <h2>判据三个常量全部来自数值真源</h2>
 * <ul>
 *   <li>距离 {@link ChargeConfigs#TESLA_COIL_RANGE} 格、<b>立方体</b>（Chebyshev，不是球体）
 *       —— 与需求 §3.2 #3「距离阈值 4 格」同口径；</li>
 *   <li>电量门槛 {@link ChargeConfigs#TESLA_COIL_MIN_ENERGY_FE} FE（<b>≥</b>，恰好等于算达标）；</li>
 *   <li>等级 {@link ChargeConfigs#TESLA_COIL_LEVEL}、时长 {@link ChargeConfigs#durationTicks(int)}。</li>
 * </ul>
 *
 * <h2>刻意不做的三件事（最小集合；都写在汇报里）</h2>
 * <ul>
 *   <li><b>不扣电</b>：需求把电量写成<b>条件</b>（「通有 ≥20kFE 时靠近」），没要求扣；
 *       既有波那条的 1000 FE 扣电与 20 tick 冷却<b>一个字都没动</b>；</li>
 *   <li><b>不做放电表现</b>：不切 POWERED 块态、不放音效、不发火花（那套属既有波桥的既有行为；
 *       新表现要不要，等作者点名）；</li>
 *   <li><b>不处理 Sable 物理结构里（sub-level）的线圈</b>：只扫主世界方块（既有波桥那份结构处理
 *       刻意不重复；结构里的线圈给不给生物赋电，同样等作者点名）。</li>
 * </ul>
 *
 * <p>CC&amp;A 是 {@code :coe} 的 required 依赖，但本类仍照仓内既成的隔离形状写：
 * {@code ModList.isLoaded} 先问、整段包 {@code try/catch(Throwable)}，绝不在缺类时抛异常
 * （类签名里没有任何 CC&amp;A 类型，只有方法体里 {@code instanceof} 它）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class TeslaCoilLivingCharger {

    private TeslaCoilLivingCharger() {
    }

    /** CC&amp;A 是否已加载（缺类时连线圈的类名都不碰）。 */
    private static boolean isCreateAdditionLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("createaddition");
    }

    /**
     * 每 tick 的总入口（{@link EntityTickEvent.Post}）：<b>先过两道最便宜的闸</b>
     * （是不是活着的生物 ⇒ 这个实体自己的 40 tick 节拍 ⇒ 身上有没有电荷），
     * 再问 CC&amp;A 装没装，最后才去扫方块。
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (living.level().isClientSide) {
            return; // 服务端权威
        }
        // ★ 逐实体限频（本类唯一的时间判据）：每个实体自己的 tickCount，各实体相位天然错开。
        if (living.tickCount % ChargeConfigs.TESLA_COIL_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        // 已经带电 ⇒ 不再赋（与既有"只认波"那条对已带电的波不再赋电同口径）：
        // 这条同时保证"站在线圈旁不会反复刷新等级/时长"，也不会每 40 tick 随机翻极性。
        if (ChargeApi.hasCharge(living)) {
            return;
        }
        if (!isCreateAdditionLoaded()) {
            return; // 没装 CC&A：静默跳过（波照常飞行、生物照常活动）
        }
        try {
            chargeNearbyCoil(living);
        } catch (Throwable ignored) {
            // CC&A 异常：静默跳过，生物照常活动（与既有波桥的"联动异常：波照常飞行"同款兜底）
        }
    }

    /**
     * 在该生物周围 {@link ChargeConfigs#TESLA_COIL_RANGE} 格的<b>立方体</b>里找一台
     * 电量 ≥ {@link ChargeConfigs#TESLA_COIL_MIN_ENERGY_FE} FE 的 CC&amp;A 特斯拉线圈，
     * 找到就用 {@link ChargeApi#applyRandom} 给它染一次电（等级 / 时长都从数值真源取）。
     *
     * @return true = 这一 tick 真的赋上了一次
     */
    private static boolean chargeNearbyCoil(LivingEntity living) {
        Level level = living.level();
        BlockPos origin = living.blockPosition();
        int reach = (int) Math.ceil(ChargeConfigs.TESLA_COIL_RANGE);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!withinCube(origin, pos)) {
                        continue; // 立方体判据（Chebyshev）——扫描盒是方形的，这里把它钉成"方"，不是球
                    }
                    if (!level.isLoaded(pos)) {
                        continue; // 不为这次扫描加载区块（生物在区块边界时尤其重要）
                    }
                    if (!(level.getBlockEntity(pos) instanceof TeslaCoilBlockEntity)) {
                        continue;
                    }
                    if (energyStored(level, pos) < ChargeConfigs.TESLA_COIL_MIN_ENERGY_FE) {
                        continue; // 电量不足（恰好等于门槛算达标：≥）
                    }
                    ChargeApi.applyRandom(living, ChargeConfigs.TESLA_COIL_LEVEL,
                        ChargeConfigs.durationTicks(ChargeConfigs.TESLA_COIL_LEVEL));
                    return true;
                }
            }
        }
        return false;
    }

    /** 该格是否落在「以生物所在格为中心、Chebyshev 半径 = {@link ChargeConfigs#TESLA_COIL_RANGE}」的立方体里。 */
    private static boolean withinCube(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= ChargeConfigs.TESLA_COIL_RANGE
            && Math.abs(pos.getY() - origin.getY()) <= ChargeConfigs.TESLA_COIL_RANGE
            && Math.abs(pos.getZ() - origin.getZ()) <= ChargeConfigs.TESLA_COIL_RANGE;
    }

    /** 该格方块的 FE 存量（没有能量 capability ⇒ 当作 0，即不达标）。 */
    private static int energyStored(Level level, BlockPos pos) {
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, (Direction) null);
        return storage == null ? 0 : storage.getEnergyStored();
    }
}
