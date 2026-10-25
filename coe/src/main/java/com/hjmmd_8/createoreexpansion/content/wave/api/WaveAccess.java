package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.List;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;
import com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * <b>能量波（波实体）的对外窄契约门面</b>——一枚波句柄的「读数 / 施加 / 生成」三个面的唯一承诺入口。
 *
 * <p><b>它解决什么问题</b>（作者 2026-10-03 对 COE 层整改批 3 的原话：「能继承就继承行不？
 * 多暴露 API 接口行不？以后你让那些小写联动的模组，让人家看这么多像天书一样的代码」）：
 * 波实体 {@link AbstractChargerWaveEntity} 是<b>一个 1800 行、上百个公开成员</b>的实现类，
 * 而它一直被别的包和别的层<b>当 API 用</b>——实测「被 :cews / :transmutation / core / 根
 * 直接 import 的 :coe 内部类」里，{@code content.charger.entity} 占了 17 条（第 2 名），
 * 全部落在 {@code AbstractChargerWaveEntity} / {@code StellarWaveEntity} / {@code ChargerWaveFx}
 * 上。一个第三方联动模组想「读一下这枚波的等级」，今天必须先把那个类当文档读一遍。</p>
 *
 * <p><b>本类就是那层承诺</b>：静态门面 + 窄签名，方法名与本模组<b>自己的玩法词汇</b>对齐
 * （波级 / 波型 / 魔素 / 拖尾风格 / 载荷 / 电荷 / 批次 / 环绕锚点），而不是与实体字段对齐。
 * 内部改名、拆类、换实现都不影响这里的签名。</p>
 *
 * <p><b>形状为什么是「简单静态方法」而不是 {@code WaveMachineIntegrationPoints} 那套
 * {@code install(Sink)} 登记机制</b>（两者解决的不是同一个问题，判据见
 * {@code ChargeApi} 的类注释）：{@code install(Sink)} 的存在理由是<b>「对端模块可能整块没装」</b>
 * ——波引擎在 {@code :coe}（第一层）、加工机联动在 {@code :cews}（第二层），单装第一层时
 * 第二层的类<b>在 classpath 上不存在</b>。本门面的对端不是「另一个模块」，而是<b>波实体自己</b>：
 * 它在 {@code :coe}、只要这个模组装了它就一定在场。于是「未装时的安全值」在这里的对应物是
 * <b>「句柄为 null / 句柄不是变体波」的安全值</b>（{@code 0} / {@code false} / {@code null} /
 * {@code List.of()} / {@code FluidStack.EMPTY} / 什么都不做），逐条写在每个方法的 javadoc 里。</p>
 *
 * <p><b>★ 句柄是不透明的（第三方怎么用）</b>：本门面的每个方法都收一个
 * {@link AbstractChargerWaveEntity}，那是<b>引擎交给你的句柄</b>，不是让你读的文档——
 * 第三方拿到它（例如实现 {@code WaveMachineHandler} 时引擎传进来的那个 {@code wave} 参数）
 * 之后，<b>只把它转发给本类</b>，既不需要 import {@link StellarWaveEntity}，也不需要知道
 * {@code getWaveLevel()} 之类的方法名存在。这是本仓既有的「窄契约 + 不透明句柄」口径
 * （与 {@code WaveMachineIntegrationPoints.Sink#chargeNearbyCoil} 同形）。</p>
 *
 * <p><b>⚠ 本类只放「有真实需求方」的方法</b>（作者原话：<b>没有真实需求的方法不许加</b>；
 * 禁止为了好看造 API）。每个方法的 javadoc 都写着「谁需要它、用来干什么」——需求方要么是
 * 已经改走本门面的本层调用点，要么是<b>今天就在调那个内部方法</b>的既有调用点（写清了文件名）。
 * 本文件<b>不做任何加工</b>：每个方法都是逐字委托，没有第二份算术、没有第二张表
 * （所以「行为零变化」是结构性的，不是靠人盯）。</p>
 *
 * <p><b>放哪个包</b>：住 {@code content/wave/api/}（与 {@link WaveType} / {@link WaveTypes} /
 * {@link WaveTrailStyle} / {@link WaveLevels} 同包），<b>刻意不新开 {@code content/api/}</b>。
 * 三条理由：① 本文档 {@code markdown_output/扩展开发接口清单（API 总览）.md} §4 已经把
 * 「{@code content/wave/api/**}」写成「波的三态与属性 → 该包就是给外部用的窄契约」——
 * 第三方只需要记<b>一个</b>包名；② 关卡 {@code check-armor-sets.ps1} 的
 * {@code charge-api-anti-vacuum} 断言在注释里就把这个包称作 "the wave engine contract"，
 * 新开一个包等于把同一件事写成两处；③ 该包<b>已经被第二层 import 了 17 行</b>，
 * 发现路径已经存在，不需要新造。</p>
 *
 * <p><b>★ 批 6（2026-10-24）只做加法：36 → 54 个静态方法，旧 36 个签名一个字未改。</b>
 * 新增的 18 个填的是方案 §2.4「卡住的能力」表里<b>能纯加法填的</b>那些：注册新等级 / 注册新魔素
 * （{@link #registerLevel} / {@link #registerEssence}，各自一行）、魔素的注册项形态
 * （{@link #essenceSpec} / {@link #trySetEssence(AbstractChargerWaveEntity, WaveEssence)}）、
 * 重力读写（{@link #gravityAcceleration} / {@link #hasGravity} / {@link #setGravity}）、
 * 寿命与行程读写（{@link #setRemainingLifetimeTicks} / {@link #maxTravelDistance} /
 * {@link #travelledDistance} / {@link #setMaxTravelDistance}）、第三方属性进 NBT
 * （{@link #putExtraTag} / {@link #extraTag} / {@link #removeExtraTag}）、造子波与穿波继承
 * （{@link #createChildWave} / {@link #inheritForTransmute}）、伤害读数 {@link #damage} 与
 * 变体波机器快照 {@link #carriedMachineIds}。
 * 同批还把第二层（{@code :cews}）原先 150+ 处直调实体访问器全部改走本门面 —— 门面从此是
 * <b>真正的调用面</b>，不再只有 Jade 一家（关卡 {@code cews-via-facade} 守着）。
 * 仍然<b>不做</b>的两项（附理由）：非 {@code charger_wave} 的伤害覆写（要改基类"纯等级表"那条
 * 被关卡钉住的算术 = 玩法面变更）、以及 {@code styleColorRgb} 的转发（{@code wave-essence-display-color}
 * 把它钉在两个显示点，见下一段）。</p>
 *
 * <p><b>不碰的东西</b>：不搬包、不改包名、不改类名、不改任何既有签名（本批对 {@code public}
 * 形状只做加法）；不 import 任何可选模组（{@code createaddition} / {@code curios} / {@code jade}
 * / {@code jei} 一个都不出现——{@code content/**} 的红线）；本类不写日志、不生成粒子、
 * 不往世界里放任何东西（除了 {@link #spawn} 造出一枚实体、由调用方自己 {@code addFreshEntity}）。</p>
 *
 * <p><b>一条被既有断言挡住的边界（记在这里免得后人重踩）</b>：<b>不带</b>
 * {@code ChargerWaveFx#styleColorRgb} 的转发方法。关卡 {@code wave-essence-display-color}
 * 把那个颜色打包器的<b>调用文件集</b>钉成逐字两个（{@code WaveJadePlugin.java/WaveReadout.java}，
 * 调用总数恰好 2）——本文件一旦调它，那条断言立刻变红。这一条留到「统一迁移那一批」
 * （改断言与改调用点同批）再动。</p>
 */
public final class WaveAccess {

	/** 工具类，不允许实例化（与 {@code ChargeApi} / {@code WaveLevels} 同形）。 */
	private WaveAccess() {
	}

	// ==================================================================================
	// 一、读：波句柄的只读面（句柄为 null 时一律返回安全值，逐条写在方法上）
	// ==================================================================================

	/**
	 * <b>波级</b>（1 = α、2 = β、3 = γ、4 = ε、5 = ω；希腊字母显示走 {@link WaveLevels}）。
	 *
	 * <p><b>谁需要它</b>：① 本层已改走本门面的 Jade 波情提示
	 * （{@code compat/jade/WaveJadePlugin}，原来直接调 {@code wave.getWaveLevel()}）；
	 * ② 第二层的一整族机器处置器与读数——{@code content/wave/block/DisperserHitHandler}、
	 * {@code content/wave/block/WaveGateHitHandler}、{@code TransmuterMode}、
	 * {@code StellarWaveTransmuterPass}、{@code content/wave/gauge/WaveReadout}
	 * （待迁移清单，本批不动它们）。用途：决定可加工配方上限 / 伤害 / 波速，以及显示。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0}）
	 */
	public static int level(AbstractChargerWaveEntity wave) {
		return wave == null ? 0 : wave.getWaveLevel();
	}

	/**
	 * <b>波型</b>（普通波 / 全能波 / 攻击波）——「能力」语义，会随平衡调整。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code wave.getWaveType().displayName()}，
	 * 「不在插件里对波型 id 写 switch」）；② 第二层的 {@code WaveReadout} 与
	 * {@code WaveHitResolver} 的调用链（待迁移清单）。用途：判断「这一枚波会不会造成伤害」
	 * （{@link WaveType#dealsDamage()}）与显示。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@link WaveTypes#NORMAL}，<b>不是</b>
	 *             {@code null}——调用方会直接对它取 {@code displayName()}）
	 */
	public static WaveType type(AbstractChargerWaveEntity wave) {
		return wave == null ? WaveTypes.NORMAL : wave.getWaveType();
	}

	/**
	 * <b>魔素</b>（攻击波专有的<b>纯视觉</b>属性；{@code null} = 未设 ⇒ 观感继承波型风格）。
	 *
	 * <p><b>谁需要它</b>：② 第二层的 {@code content/wave/gauge/WaveReadout}（动作栏读数里
	 * 「只有真的设了才占一行」那条分支）。本层 Jade 也读它，但<b>本批刻意没改那一行</b>：
	 * 关卡 {@code wave-essence-display} 把 {@code wave.getEssence()} 这个<b>逐字形状</b>
	 * 钉在两个显示点上，改一处就要同批改断言（见类注释的边界段）。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code null}，与「没有魔素」同义）
	 */
	public static WaveTrailStyle essence(AbstractChargerWaveEntity wave) {
		return wave == null ? null : wave.getEssence();
	}

	/**
	 * <b>魔素的完整注册项</b>（{@link WaveEssence}：id + 颜色 + 可选拖尾档案 + 可选命中行为）。
	 *
	 * <p><b>谁需要它</b>：① 第二层的波情查询仪与变器（要判断"这一枚波的魔素是不是第三方注册的
	 * 那一个"）与任何"按 id 而不是按枚举"处理魔素的联动；② 想把魔素原样转给另一枚波的调用方
	 * （{@link #trySetEssence(AbstractChargerWaveEntity, WaveEssence)} 收的就是它）。
	 * 内置 11 个的 {@code id()} 逐字等于枚举名，第三方是 {@code namespace:path}。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code null}，与"没有魔素"同义）
	 */
	public static WaveEssence essenceSpec(AbstractChargerWaveEntity wave) {
		return wave == null ? null : wave.essenceSpec();
	}

	/**
	 * <b>本波真正生效的拖尾风格</b>（全仓唯一口径：有魔素且已登记 ⇒ 用魔素，否则继承波型风格）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler} / {@code WaveGateHitHandler}
	 * （差波器与波闸把「波长什么样」原样交给子波与粒子层）。用途：拿到「这一枚波该用哪套粒子
	 * 档案」的最终答案，<b>不要</b>自己去写 {@code 魔素 != null ? ... : 波型风格}——
	 * 那是第二条口径，关卡 {@code wave-essence-single-source} 守着这条。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@link WaveTrailStyle#NORMAL}；
	 *             句柄非 null 时该方法是纯函数、恒有值）
	 */
	public static WaveTrailStyle trailStyle(AbstractChargerWaveEntity wave) {
		return wave == null ? WaveTrailStyle.NORMAL : wave.trailStyle();
	}

	/**
	 * <b>实际运行速度（格/秒）</b>——名义基准速度加上速度修正后、再按该波级的上下限夹取的值。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code wave.getWaveSpeed()}，显示「波速」
	 * 这条要素）；② 第二层的 {@code WaveReadout}（待迁移清单）。用途：显示，以及任何
	 * 「这枚波跑得比标称快还是慢」的联动判定。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0})
	 */
	public static double speedPerSecond(AbstractChargerWaveEntity wave) {
		return wave == null ? 0 : wave.getWaveSpeed();
	}

	/**
	 * <b>剩余寿命（tick）</b>——距自动消散还剩多少 tick（{@code <= 0} = 即将消散）。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code wave.getRemainingLifetime()}，
	 * 波情第五要素「剩余寿命」；该行是<b>动态</b>的，Jade 每帧重建）；② 第二层的
	 * {@code WaveReadout}（待迁移清单，与 Jade 同一个取值点）。用途：显示。</p>
	 *
	 * <p><b>为什么这里只给 tick、不替你除 20</b>：寿命口径的唯一取值点就是本方法转发的那个
	 * 值，「秒」是显示层的格式化（{@code / 20.0d}），把它搬进来等于把显示口径塞进 API。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0}）
	 */
	public static int remainingLifetimeTicks(AbstractChargerWaveEntity wave) {
		return wave == null ? 0 : wave.getRemainingLifetime();
	}

	/**
	 * <b>服务端实例上的实时渲染色</b>（RGB 0-1，随波级与种类逐 tick 向目标色插值）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler} / {@code WaveGateHitHandler} /
	 * {@code StellarWaveTransmuterPass}（差波器与波闸让子波继承母波颜色、变器画穿波特效；
	 * 待迁移清单）。用途：把颜色交给表现层。</p>
	 *
	 * <p>⚠ <b>客户端别用这个</b>：该字段不同步。客户端要「按波级取静态色」请用
	 * {@link #levelColor}。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@link Vec3#ZERO}）
	 */
	public static Vec3 renderColor(AbstractChargerWaveEntity wave) {
		return wave == null ? Vec3.ZERO : wave.getRenderColor();
	}

	/**
	 * <b>按波级取的静态渲染色</b>（RGB 0-1）——<b>客户端安全</b>的那个读数。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code wave.getWaveRenderColor()}，
	 * 给「波级」那一行文字着色；Jade 在客户端运行，只能读这个静态色）；
	 * ② 任何在客户端显示波信息的联动模组。用途：显示着色。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@link Vec3#ZERO}）
	 */
	public static Vec3 levelColor(AbstractChargerWaveEntity wave) {
		return wave == null ? Vec3.ZERO : wave.getWaveRenderColor();
	}

	/**
	 * <b>主运动方向</b>（单位向量；速率另算）。
	 *
	 * <p><b>谁需要它</b>：① 本层的回旋镖环绕技能（{@code content/equipment/boomerang}）与
	 * 星芒嬗震（{@code content/equipment/armor/StarShockRuntime}）——环绕波继承主波方向；
	 * ② 第二层的 {@code DisperserHitHandler} / {@code WaveGateHitHandler} /
	 * {@code StellarWaveTransmuterPass}（待迁移清单）。用途：几何判定与推出方向。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@link Vec3#ZERO}）
	 */
	public static Vec3 movement(AbstractChargerWaveEntity wave) {
		return wave == null ? Vec3.ZERO : wave.getMovement();
	}

	/**
	 * <b>出生点</b>（最大行程判定用；读档之前可能为 {@code null}）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler}（差波器把子波安置到别处时
	 * 需要知道母波从哪里来；待迁移清单）。用途：射程判定与子波定位。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code null}）
	 */
	public static Vec3 spawnPos(AbstractChargerWaveEntity wave) {
		return wave == null ? null : wave.getSpawnPos();
	}

	/**
	 * <b>速度修正叠加值（格/秒，可正可负）</b>——波速调节器与能量场的累积修正。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler} / {@code WaveGateHitHandler} /
	 * {@code StellarWaveTransmuterPass}（待迁移清单）。用途：把修正量按比例转给子波。</p>
	 *
	 * <p>⚠ <b>写入请走 {@link #addSpeedOffset}</b>，不要自己算再覆盖。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0})
	 */
	public static double speedOffset(AbstractChargerWaveEntity wave) {
		return wave == null ? 0 : wave.getSpeedOffset();
	}

	/**
	 * <b>当前电荷极性</b>（{@code null} = 不带电）。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code wave.getChargePolarity()}，
	 * 显示「正电荷 / 负电荷 / 未带电」那一行）；② 第二层的
	 * {@code compat/createaddition/TeslaCoilWaveCharger}（特斯拉线圈给不带电的波赋电荷）
	 * 与 {@code StellarWaveTransmuterPass}（待迁移清单）。用途：判定能量场作用前提与显示。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code null}）
	 */
	public static ChargePolarity charge(AbstractChargerWaveEntity wave) {
		return wave == null ? null : wave.getChargePolarity();
	}

	/**
	 * <b>本 tick 的运动线段是否穿过某个盒</b>（射线/扫掠判定的唯一入口）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code TransmuterMode}（变器的攻击态判定「这一枚波会不会
	 * 打到我这个范围」；待迁移清单）。用途：几何命中预判。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code false}）
	 * @param box  世界坐标下的盒
	 */
	public static boolean pathCrosses(AbstractChargerWaveEntity wave, AABB box) {
		return wave != null && box != null && wave.pathCrosses(box);
	}

	/**
	 * <b>这枚波是不是「变体波」</b>（携带载荷与可加工配方类型的那种，即
	 * {@link StellarWaveEntity}）。
	 *
	 * <p><b>为什么需要它</b>：下面七个载荷读数只对变体波有意义；本方法让第三方在
	 * <b>不必 import {@link StellarWaveEntity}</b> 的前提下问出这件事。
	 * <b>谁需要它</b>：本层 Jade（原来写 {@code wave instanceof StellarWaveEntity}，
	 * 用来决定「可加工配方类型」那一段要不要读；载荷走 Jade 的服务端数据通道，见
	 * {@code appendServerData}）。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code false}）
	 */
	public static boolean isVariantWave(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity;
	}

	/**
	 * <b>变体波携带的"机器快照"</b>（变体波专有；不是变体波 ⇒ 空表）——穿波那一刻变器扫描到的
	 * 机器方块 id 清单。
	 *
	 * <p><b>谁需要它</b>：① 第二层的变器穿波日志（"属性 N 个"那一项，改造前调
	 * {@code variant.getAttributes()}）；② 任何"这枚波声称能加工哪些机器"的联动判定。
	 * 它<b>不是</b>"这一枚波实际能执行哪些配方类型"——后者见 {@link #activeRecipeTypes}。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@link List#of()}）
	 */
	public static List<ResourceLocation> carriedMachineIds(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getAttributes() : List.of();
	}

	/**
	 * <b>波携带的物品载荷</b>（变体波专有；不是变体波 ⇒ 空表）。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getPayloadItems()}，
	 * 把载荷经服务端数据通道下发；该字段是服务端运行态、客户端实例读不到）；
	 * ② 第二层的 {@code StellarWaveTransmuterBlockEntity} 与 {@code WavePayloadReadout}
	 * （待迁移清单）。用途：显示与载荷结算。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@link List#of()}）
	 */
	public static List<ItemStack> payloadItems(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getPayloadItems() : List.of();
	}

	/**
	 * <b>波携带的流体载荷</b>（变体波专有；不是变体波 ⇒ {@link FluidStack#EMPTY}）。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getPayloadFluid()}）；
	 * ② 第二层的 {@code WavePayloadReadout} / {@code StellarWaveTransmuterBlockEntity}
	 * （待迁移清单）。用途：显示与载荷结算。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@link FluidStack#EMPTY}）
	 */
	public static FluidStack payloadFluid(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getPayloadFluid() : FluidStack.EMPTY;
	}

	/**
	 * <b>波携带的电量（FE）</b>（变体波专有；不是变体波 ⇒ {@code 0}）。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getPayloadEnergy()}，
	 * 「载荷概要」里的电量段）；② 第二层的 {@code WavePayloadReadout} /
	 * {@code content/machine/stellarwavetransmuter/payload/TransmuterPayloadCollector}
	 * （待迁移清单）。用途：显示与耗电配方结算。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@code 0}）
	 */
	public static int payloadEnergy(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getPayloadEnergy() : 0;
	}

	/**
	 * <b>波携带的「引雷次数」</b>（变体波专有；不是变体波 ⇒ {@code 0}）——穿波时从蓄满的
	 * 强化避雷针抽取的额度，波打中哪里就在哪里落雷。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getRodCharges()}，
	 * 载荷明细里的「引雷次数」）；② 第二层的 {@code StellarWaveTransmuterPass}
	 * （待迁移清单）。用途：显示与落雷结算。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@code 0}）
	 */
	public static int rodCharges(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getRodCharges() : 0;
	}

	/**
	 * <b>波携带的加热档位</b>（变体波专有；不是变体波 ⇒ {@link BlazeBurnerBlock.HeatLevel#NONE}）
	 * ——变器扫描半径内点燃的烈焰燃烧室档位。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getCarriedHeat()}，
	 * 载荷明细里的「携带加热」）；② 第二层的 {@code TransmuterPayloadCollector}
	 * （待迁移清单）。用途：显示与热加工结算。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@link BlazeBurnerBlock.HeatLevel#NONE}）
	 */
	public static BlazeBurnerBlock.HeatLevel carriedHeat(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar
			? stellar.getCarriedHeat()
			: BlazeBurnerBlock.HeatLevel.NONE;
	}

	/**
	 * <b>波携带的加工转速（RPM）</b>（变体波专有；不是变体波 ⇒ {@code 0}）——转速档
	 * （见 {@code util/SpeedBands}）判定的依据。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getCarriedRpm()}，
	 * 载荷明细里的「携带转速」）；② 第二层的 {@code TransmuterPayloadCollector}
	 * （待迁移清单）。用途：显示与转速档结算。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@code 0}）
	 */
	public static float carriedRpm(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getCarriedRpm() : 0;
	}

	/**
	 * <b>本波实际可执行的配方类型全集</b>（变体波专有；不是变体波 ⇒ 空表）——含状态选择与
	 * 「携带电量才能追加执行的耗电配方类型」。
	 *
	 * <p><b>谁需要它</b>：① 本层 Jade（原来调 {@code stellar.getActiveRecipeTypes()}，
	 * 下发「可加工」清单；整段包在 try/catch 里，集成异常时清单缺省为空）；
	 * ② 第二层的 {@code WaveReadout} 与 {@code StellarWaveTransmuterPass}（待迁移清单）。
	 * 用途：显示「这一枚波能加工什么」，以及机器侧的可用性判定。</p>
	 *
	 * @param wave 波句柄（{@code null} 或非变体波 ⇒ {@link List#of()}）
	 */
	public static List<IRecipeTypeInfo> activeRecipeTypes(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getActiveRecipeTypes() : List.of();
	}

	/**
	 * <b>这一枚波当前会造成的命中伤害（点）</b>——命中链真正用的那个值。
	 *
	 * <p><b>谁需要它</b>：① 任何"我的机器/技能要知道这枚波打多疼"的联动（此前门面<b>没有</b>任何
	 * 伤害读数，第三方只能自己按波级查表，而 {@code charger_wave} 还有自定义伤害覆盖 ⇒ 自己查会算错）；
	 * ② 诊断/显示层（护目镜、日志）；③ 第二层的命中处置（判断"值不值得让这枚波过去"）。</p>
	 *
	 * <p>⚠ <b>它只读不写</b>：覆写伤害仍走 {@link #setCustomDamage}（只对 {@code charger_wave} 成立）。
	 * 其它波型的伤害覆盖需要改基类"纯等级表"那条被关卡钉住的算术 = 玩法面变更，本批不做。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0})
	 */
	public static float damage(AbstractChargerWaveEntity wave) {
		return wave == null ? 0.0F : wave.damageValue();
	}

	/**
	 * <b>重力要素的当前值</b>（格/秒²；{@code 0} = 未设 ⇒ 本波走直线）。
	 *
	 * <p><b>谁需要它</b>：① 想"设完再确认"的调用方（此前实体只有 {@code hasGravity()} 的布尔面，
	 * 门面连布尔都没有）；② 护目镜/诊断的"本波受重力"读数。默认 {@code 0.0D} = 与改造前逐字相同。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0})
	 */
	public static double gravityAcceleration(AbstractChargerWaveEntity wave) {
		return wave == null ? 0.0D : wave.gravityAcceleration();
	}

	/**
	 * <b>本波是否设了重力要素</b>（{@code false} = 飞行与改造前逐字相同）。
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code false}）
	 */
	public static boolean hasGravity(AbstractChargerWaveEntity wave) {
		return wave != null && wave.hasGravity();
	}

	/**
	 * <b>本波的行程上限（格）</b>——越过它波就消散，与时间寿命互为兜底。
	 *
	 * <p><b>谁需要它</b>：① 任何"这枚波还能飞多远"的联动/显示；② 要收紧射程的发射方
	 * （先读缺省值 64.0，再决定写多少）。不设 ⇒ 逐字等于旧常量 {@code MAX_TRAVEL_DISTANCE}。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0})
	 */
	public static double maxTravelDistance(AbstractChargerWaveEntity wave) {
		return wave == null ? 0.0D : wave.maxTravelDistance();
	}

	/**
	 * <b>已飞行距离（格）</b>：当前位置到出生点的直线距离（出生点未知 ⇒ {@code 0}）。
	 *
	 * <p><b>谁需要它</b>：显示层（"还剩多少射程"= {@link #maxTravelDistance} − 本值）与
	 * "这枚波是不是快到头了"的机器判定。第三方不必自己拼 {@code distanceTo(getSpawnPos())}
	 * 那两句（漏掉出生点为 null 的分支会 NPE）。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0})
	 */
	public static double travelledDistance(AbstractChargerWaveEntity wave) {
		return wave == null ? 0.0D : wave.travelledDistance();
	}

	/**
	 * <b>读一个"表外" NBT 键</b>（第三方属性袋；见 {@link #putExtraTag}）。
	 *
	 * <p><b>谁需要它</b>：把自定义状态写在波 NBT 上的联动模组（批 3 的未知键保留袋保证它
	 * 读一遍再写一遍不会被抹掉；本方法让第三方真的能读写那个袋子）。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code null}）
	 * @param key  NBT 键名
	 */
	public static Tag extraTag(AbstractChargerWaveEntity wave, String key) {
		return wave == null ? null : wave.extraTag(key);
	}

	// ==================================================================================
	// 二、施加：把「波要素」写到一枚既有波上
	// 这一节的每个方法在句柄为 null 时都是「什么都不做 / 返回 false」——与「写不进去」
	// （波型一生只能改一次、魔素对非攻击波忽略）共用同一个返回值口径。
	// ==================================================================================

	/**
	 * <b>设置波级</b>（调级器升级/降级、增强延迟到期升级用；等级决定可匹配配方上限、伤害、波速）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code content/wave/block/WaveGateHitHandler}
	 * （能量波闸把过闸的波提级/降级；待迁移清单）。用途：改变这一枚波的玩法等级。</p>
	 *
	 * @param wave  波句柄（{@code null} ⇒ 不做事）
	 * @param level 新波级
	 */
	public static void setLevel(AbstractChargerWaveEntity wave, int level) {
		if (wave != null) {
			wave.setWaveLevel(level);
		}
	}

	/**
	 * <b>尝试改变波型</b>（<b>一生只能变一次</b>：当前已经不是普通波 ⇒ 拒绝）。
	 *
	 * <p><b>谁需要它</b>：① 本层的两个技能生成点——回旋镖环绕波
	 * （{@code content/equipment/boomerang/BoomerangOrbitWaves}）与星芒嬗震
	 * （{@code content/equipment/armor/StarShockRuntime}）都要把新造的波打成攻击波
	 * （不设它，波就只是个观光粒子；这就是 {@code dealsDamage()} 那一支的门槛）；
	 * ② 第二层的 {@code TransmuterMode}（变器的穿波转换 / 场点燃；待迁移清单）。</p>
	 *
	 * <p>⚠ <b>顺序不自由</b>：攻击波才允许设魔素，所以本方法必须排在
	 * {@link #trySetEssence} <b>之前</b>（反了就是静默空操作，关卡
	 * {@code wave-essence-star-shock} / {@code boomerang-orbit-essence-order} 守着这条）。</p>
	 *
	 * @param wave   波句柄（{@code null} ⇒ 返回 {@code false}）
	 * @param target 目标波型（{@code null} 或 {@link WaveTypes#NORMAL} ⇒ 返回 {@code false}）
	 * @return 是否真的改成了
	 */
	public static boolean trySetType(AbstractChargerWaveEntity wave, WaveType target) {
		return wave != null && wave.trySetWaveType(target);
	}

	/**
	 * <b>尝试设置魔素</b>（攻击波专有的纯视觉属性；{@code null} = 清除 ⇒ 回落波型风格）。
	 *
	 * <p><b>谁需要它</b>：① 本层的回旋镖环绕波（{@code BoomerangOrbitWaves}，每枚各自抽一种）
	 * 与星芒嬗震（{@code StarShockRuntime}，主波 {@code ARCANE} + 环绕波各抽一种）；
	 * ② 第二层的 {@code TransmuterMode}（待迁移清单）。</p>
	 *
	 * <p>⚠ 对<b>不造成伤害</b>的波型直接返回 {@code false} 且什么都不写
	 * （静默空操作，没有日志）⇒ 调用点必须先 {@link #trySetType} 打成攻击波。</p>
	 *
	 * @param wave   波句柄（{@code null} ⇒ 返回 {@code false}）
	 * @param target 魔素（{@code null} = 清除）
	 * @return 是否真的写进去了
	 */
	public static boolean trySetEssence(AbstractChargerWaveEntity wave, WaveTrailStyle target) {
		return wave != null && wave.trySetEssence(target);
	}

	/**
	 * <b>尝试设置魔素（注册项形态 —— 第三方魔素走这里）</b>。
	 *
	 * <p><b>谁需要它</b>：① 注册了自定义魔素的扩展模组（{@link #registerEssence} 之后，用
	 * {@code WaveEssence.of("yourmod:thunder")} 造出来的那一个）；② 本层/第二层任何"按 id 转交魔素"
	 * 的调用方（从 {@link #essenceSpec} 读到什么就原样转给下一枚波）。
	 * 改造前本门面只收 {@link WaveTrailStyle} 枚举 ⇒ 第三方<b>造不出第 12 个值</b>，这条能力是封死的。</p>
	 *
	 * <p>⚠ 与枚举重载同一个前提：<b>必须先</b> {@link #trySetType} 打成攻击波；对不造成伤害的波型
	 * 直接返回 {@code false} 且什么都不写。⚠ 字面量 {@code trySetEssence(wave, null)} 会因两个重载
	 * 而歧义（编译期就报错，不会静默）——要清空请用 {@code (WaveTrailStyle) null}。</p>
	 *
	 * @param wave   波句柄（{@code null} ⇒ 返回 {@code false}）
	 * @param target 魔素注册项（{@code null} = 清除）
	 * @return 是否真的写进去了
	 */
	public static boolean trySetEssence(AbstractChargerWaveEntity wave, WaveEssence target) {
		return wave != null && wave.trySetEssence(target);
	}

	/**
	 * <b>叠加一次速度修正</b>（在此值上增加 {@code amount}，可正可负）——<b>波速的唯一写入口</b>。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler}（差波器给子波提速）、
	 * {@code WaveGateHitHandler}（波闸按档提速）、{@code StellarWaveTransmuterPass}
	 * （待迁移清单）。用途：波速调节器与能量场之外的第二类速度来源。</p>
	 *
	 * @param wave   波句柄（{@code null} ⇒ 不做事）
	 * @param amount 增量（格/秒）
	 */
	public static void addSpeedOffset(AbstractChargerWaveEntity wave, double amount) {
		if (wave != null) {
			wave.addSpeedOffset(amount);
		}
	}

	/**
	 * <b>写入「加速场」要素</b>（档位 + 剩余作用距离，一次写完，避免只写一半的中间态）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code content/wave/block/WaveGateHitHandler}
	 * （能量波闸把过闸的波置于加速档；待迁移清单）。用途：波闸的加速档位。</p>
	 *
	 * <p>两个参数各自有含义：{@code step} = 每 tick 提速的档位，{@code remainingBlocks} = 还剩多少格
	 * 处于加速场中（{@code <= 0} 表示离开）。</p>
	 *
	 * @param wave            波句柄（{@code null} ⇒ 不做事）
	 * @param step            加速场档位
	 * @param remainingBlocks 剩余作用距离（格）
	 */
	public static void setSpeedBoost(AbstractChargerWaveEntity wave, int step, float remainingBlocks) {
		if (wave != null) {
			wave.setBoostStep(step);
			wave.setBoostRemaining(remainingBlocks);
		}
	}

	/**
	 * <b>覆写主运动方向</b>（只改方向，不动速度修正）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler}（差波器把波折向）、
	 * {@code WaveGateHitHandler}、{@code StellarWaveTransmuterPass}（待迁移清单）。
	 * 用途：机器对波的几何处置。</p>
	 *
	 * @param wave     波句柄（{@code null} ⇒ 不做事）
	 * @param movement 新的单位方向向量
	 */
	public static void setMovement(AbstractChargerWaveEntity wave, Vec3 movement) {
		if (wave != null) {
			wave.setMovement(movement);
		}
	}

	/**
	 * <b>设置出生点</b>（子波按 contraption / 物理结构坐标换算后回写，用于最大行程判定）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code DisperserHitHandler}（子波的出生点是新位置而非母波
	 * 的出生点，不写会让子波一出生就被判「飞满射程」）；待迁移清单。</p>
	 *
	 * @param wave     波句柄（{@code null} ⇒ 不做事）
	 * @param spawnPos 出生点（世界坐标）
	 */
	public static void setSpawnPos(AbstractChargerWaveEntity wave, Vec3 spawnPos) {
		if (wave != null) {
			wave.setSpawnPos(spawnPos);
		}
	}

	/**
	 * <b>赋予 / 清除电荷</b>（{@code null} = 清除）。
	 *
	 * <p><b>谁需要它</b>：第二层的 {@code compat/createaddition/TeslaCoilWaveCharger}
	 * （特斯拉线圈给不带电的波赋电荷）与 {@code StellarWaveTransmuterPass}（待迁移清单）。</p>
	 *
	 * @param wave     波句柄（{@code null} ⇒ 不做事）
	 * @param polarity 极性（{@code null} = 清除）
	 */
	public static void setCharge(AbstractChargerWaveEntity wave, ChargePolarity polarity) {
		if (wave != null) {
			wave.setCharge(polarity);
		}
	}

	/**
	 * <b>读「发射批次号」</b>（0 = 不属于任何批次）。
	 *
	 * <p><b>谁需要它</b>：① 本层的环绕波（{@code StarShockRuntime} 的环绕波继承父波批次号）
	 * 与回旋镖（自造负数批次）；② 第二层的 {@code StellarWaveTransmuterPass}（待迁移清单）。
	 * 用途：{@code sameFiringBatch} 的豁免判据——同一次发射的波<b>不互相湮灭</b>。</p>
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code 0}）
	 */
	public static int firingBatch(AbstractChargerWaveEntity wave) {
		return wave == null ? 0 : wave.getFiringBatch();
	}

	/**
	 * <b>写「发射批次号」</b>（同一次发射的若干枚波共用；两枚非 0 且相等的波相遇时互相豁免）。
	 *
	 * <p><b>谁需要它</b>：① 本层的星芒嬗震（主波 {@code cast.batch}、环绕波继承父波）
	 * 与回旋镖环绕波（每次投掷一个负数批次）；② 第二层的 {@code StellarWaveTransmuterPass}
	 * （待迁移清单）。</p>
	 *
	 * <p>⚠ <b>不要写 {@code 0}</b>：0 = 「不属于任何批次」，而且判据要求<b>双方都非 0</b>。</p>
	 *
	 * @param wave  波句柄（{@code null} ⇒ 不做事）
	 * @param batch 批次号
	 */
	public static void setFiringBatch(AbstractChargerWaveEntity wave, int batch) {
		if (wave != null) {
			wave.setFiringBatch(batch);
		}
	}

	/**
	 * <b>设置主人</b>（不伤发射者那条判定的依据；{@code null} = 没有主人）。
	 *
	 * <p><b>谁需要它</b>：① 本层的星芒嬗震（主波与环绕波都记施法者）与回旋镖环绕波
	 * （记<b>投掷玩家本人</b>，<b>不是</b>镖的 UUID——镖 UUID 是环绕锚点，两者不能混用）；
	 * ② 任何「技能/机器打出的波不该伤到自己人」的联动。用途：命中豁免与显示归属。</p>
	 *
	 * @param wave  波句柄（{@code null} ⇒ 不做事）
	 * @param owner 主人实体（{@code null} = 清除）
	 */
	public static void setOwner(AbstractChargerWaveEntity wave, Entity owner) {
		if (wave != null) {
			wave.setOwner(owner);
		}
	}

	/**
	 * <b>挂上「环绕要素」</b>（锚点 UUID + 半径 + 角速度 + 相位）——锚点不必是波，任何实体
	 * 都行（{@code OrbitAnchor} 契约放宽后的口径）。
	 *
	 * <p><b>谁需要它</b>：① 本层的两个环绕技能——星芒嬗震的环绕波（锚点 = 主波）与回旋镖
	 * 的环绕波（锚点 = <b>镖自己</b>；镖 {@code discard()} 后环绕波下一 tick 自己收尾）；
	 * ② 任何「让波绕着某个实体转」的联动模组。用途：环绕几何。</p>
	 *
	 * @param wave           波句柄（{@code null} ⇒ 不做事）
	 * @param anchor         锚点实体的 UUID
	 * @param radius         半径（格）
	 * @param angularSpeedRad 角速度（弧度/秒）
	 * @param phaseRad       本枚波的相位（弧度；L 枚均分一圈用 {@code 2π·i/L}）
	 */
	public static void setOrbitAnchor(AbstractChargerWaveEntity wave, UUID anchor, double radius,
			double angularSpeedRad, double phaseRad) {
		if (wave != null) {
			wave.setOrbitAnchor(anchor, radius, angularSpeedRad, phaseRad);
		}
	}

	/**
	 * <b>设置命中效果</b>（命中生物时附加的 {@code MobEffect} + 时长 + 等级）。
	 *
	 * <p><b>谁需要它</b>：① 本层的星芒嬗震（主波与环绕波各挂一次嬗乱）；
	 * ② 任何「自定义波的命中效果」的联动模组。用途：命中附加效果。</p>
	 *
	 * @param wave          波句柄（{@code null} ⇒ 不做事）
	 * @param effect        效果（{@code DeferredHolder} / {@code Holder} 都行）
	 * @param durationTicks 时长（tick）
	 * @param amplifier     效果等级（amplifier，不是玩法等级）
	 */
	public static void setHitEffect(AbstractChargerWaveEntity wave, Holder<MobEffect> effect,
			int durationTicks, int amplifier) {
		if (wave != null) {
			wave.setHitEffect(effect, durationTicks, amplifier);
		}
	}

	/**
	 * <b>覆写「自定义命中伤害」</b>（默认 = 按波级查表；本方法给的是<b>可选覆盖</b>）。
	 *
	 * <p><b>谁需要它</b>：① 本层的回旋镖环绕波（{@code 2 × 技能等级}，走既有实体的可选
	 * 自定义伤害、<b>不</b>另写一套波级表）与星芒嬗震的环绕波（主波一半伤害）；
	 * ② 任何「伤害由技能/机器决定」的联动。用途：伤害覆盖。</p>
	 *
	 * <p><b>为什么返回 boolean 而不是静默</b>：这个覆盖只存在于
	 * {@link ChargerWaveEntity} 这一种波上；别的波实现没有这个要素，
	 * 静默吞掉会让「我明明设了伤害」变成一个查不出来的洞。</p>
	 *
	 * @param wave   波句柄（{@code null} 或不是 {@link ChargerWaveEntity} ⇒ 返回 {@code false}）
	 * @param damage 伤害（点）
	 * @return 是否真的写进去了
	 */
	public static boolean setCustomDamage(AbstractChargerWaveEntity wave, float damage) {
		if (wave instanceof ChargerWaveEntity chargerWave) {
			chargerWave.setCustomDamage(damage);
			return true;
		}
		return false;
	}

	/**
	 * <b>设置重力要素</b>（可选、默认关闭；{@code <= 0} 视为不做事 ⇒ 本波走直线）。
	 *
	 * <p><b>谁需要它</b>：① 需要抛物线飞行的发射方（宝石弓的星芒弹幕是今天唯一实例，它直接调
	 * {@code setGravity}；本门面把同一条路开放给第三方）；② 任何"我的机器打出的波要下坠"的联动。
	 * 单位是 <b>格/秒²</b>，强度由调用方传（实体只解释、不存数值）。</p>
	 *
	 * <p>⚠ 不设 = 默认 {@code 0} ⇒ 飞行与改造前逐字相同（关卡 {@code bow4-gravity-gated} 守着）。</p>
	 *
	 * @param wave         波句柄（{@code null} ⇒ 不做事）
	 * @param acceleration 向下加速度（格/秒²）
	 */
	public static void setGravity(AbstractChargerWaveEntity wave, double acceleration) {
		if (wave != null) {
			wave.setGravity(acceleration);
		}
	}

	/**
	 * <b>改写剩余寿命（tick）</b>（时间寿命；夹到 {@code 0..MAX_LIFETIME_TICKS}）。
	 *
	 * <p><b>谁需要它</b>：① "这一枚波只许活 5 秒"的技能/机器（把 200 tick 改小，而不是另造实体）；
	 * ② 延长寿命的联动（例如把穿波后的变体波多留一会儿）。读它请用
	 * {@link #remainingLifetimeTicks}（同一取值点，不另开第二条寿命口径）。</p>
	 *
	 * <p>⚠ 它只改<b>时间</b>寿命：行程上限是另一条独立的消散条件（见 {@link #setMaxTravelDistance}）。</p>
	 *
	 * @param wave  波句柄（{@code null} ⇒ 不做事）
	 * @param ticks 新的剩余寿命（tick）
	 */
	public static void setRemainingLifetimeTicks(AbstractChargerWaveEntity wave, int ticks) {
		if (wave != null) {
			wave.setRemainingLifetime(ticks);
		}
	}

	/**
	 * <b>改写行程上限（格）</b>（{@code <= 0} / 非有限值视为不做事，保持当前上限）。
	 *
	 * <p><b>谁需要它</b>：① 收紧射程的发射方（"我的波只该飞 16 格"）；② 放宽射程的联动
	 * （长程炮台）。不设 ⇒ 逐字等于旧常量 64.0（同一个数、同一个比较、同一个 tick）。</p>
	 *
	 * @param wave  波句柄（{@code null} ⇒ 不做事）
	 * @param blocks 新的行程上限（格）
	 */
	public static void setMaxTravelDistance(AbstractChargerWaveEntity wave, double blocks) {
		if (wave != null) {
			wave.setMaxTravelDistance(blocks);
		}
	}

	/**
	 * <b>写一个"表外" NBT 键</b>（第三方属性袋）——把自定义状态挂在波的存档标签上。
	 *
	 * <p><b>为什么需要它</b>：批 3 的未知键保留袋只保证"本模组读一遍再写一遍不会抹掉别人的键"，
	 * 但第三方自己<b>没有入口</b>（袋子只在包内）。本方法与 {@link #extraTag} /
	 * {@link #removeExtraTag} 就是那对入口，写进去的键由 {@code WaveStateNbt} 在存档时原样带出
	 * （<b>只补不覆盖</b>，父类字段名不会被顶掉）。</p>
	 *
	 * <p>⚠ 本模组已声明的 31 个键<b>拒绝</b>（那些键的真源是 {@code WaveAttrs} 表，从侧门写会被下次
	 * 读写按表覆盖 ⇒ 静默失效）。⚠ 键名请带自己的命名空间（例如 {@code "yourmod:my_state"}），
	 * 本模组不替你做冲突裁决。</p>
	 *
	 * @param wave  波句柄（{@code null} ⇒ 返回 {@code false}）
	 * @param key   NBT 键名（{@code null} / 空串 / 已声明键 ⇒ {@code false}）
	 * @param value 标签（{@code null} ⇒ {@code false}；要删请用 {@link #removeExtraTag}）
	 * @return 是否真的写进去了
	 */
	public static boolean putExtraTag(AbstractChargerWaveEntity wave, String key, Tag value) {
		return wave != null && wave.putExtraTag(key, value);
	}

	/**
	 * <b>删一个"表外" NBT 键</b>（第三方属性袋）。
	 *
	 * @param wave 波句柄（{@code null} ⇒ 返回 {@code false}）
	 * @param key  NBT 键名
	 * @return 是否真的删掉了（原来就没有 ⇒ {@code false}）
	 */
	public static boolean removeExtraTag(AbstractChargerWaveEntity wave, String key) {
		return wave != null && wave.removeExtraTag(key);
	}

	// ==================================================================================
	// 三、生成：造一枚全新的波
	// ==================================================================================

	/**
	 * <b>造一枚全新的波（服务端实体，尚未入世界）</b>——用的是充能器/技能同一种既有波实体与
	 * 同一个构造器（{@code ChargerWaveEntity(level, pos, direction, waveLevel)}）。
	 *
	 * <p><b>谁需要它</b>：① 本层的回旋镖环绕波（{@code BoomerangOrbitWaves}）与星芒嬗震
	 * （{@code StarShockRuntime}）——两处今天的写法都是 {@code new ChargerWaveEntity(...)}
	 * 后逐个设要素；② 任何「我的机器自己发射一枚波」的联动模组（这正是作者说的
	 * 「小写联动的模组」最典型的一步）。</p>
	 *
	 * <p>⚠ <b>调用方必须自己把它放进世界</b>（{@code level.addFreshEntity(wave)}），并且
	 * <b>按顺序</b>设要素：先 {@link #trySetType} 打成攻击波，再 {@link #trySetEssence}
	 * 设魔素，最后设主人/批次/环绕/伤害。本方法<b>不</b>替你设任何要素（默认波 = 普通波、
	 * 无主人、批次 0）。</p>
	 *
	 * <p><b>为什么不需要 {@code EntityType} 参数</b>：本模组的波只有一种「基础波」实体，
	 * 变体波是同一枚波被机器改造出来的形态，不是另一种注册项。</p>
	 *
	 * @param level       服务端世界（{@code null} ⇒ 返回 {@code null}；实体只能住在服务端世界）
	 * @param pos         出生点
	 * @param direction   初始方向（单位向量；{@code null} ⇒ 返回 {@code null}）
	 * @param waveLevel   初始波级
	 * @return 新造的波（<b>尚未入世界</b>）
	 */
	public static AbstractChargerWaveEntity spawn(ServerLevel level, Vec3 pos, Vec3 direction, int waveLevel) {
		if (level == null || pos == null || direction == null) {
			return null;
		}
		return new ChargerWaveEntity(level, pos, direction, waveLevel);
	}

	/**
	 * <b>按父波造一枚分裂子波</b>（差波器均摊分发的那条路；属性整份继承，见
	 * {@code AbstractChargerWaveEntity#createChildWave}）。
	 *
	 * <p><b>谁需要它</b>：① 第二层的差波器家族（四面 / 六面 / 八面 / contraption / 物理结构
	 * 共五处出口），改造前它们调 {@code wave.createChildWave(...)}；② 任何"把一枚波拆成几枚"的联动
	 * （拆出来的子波自动继承父波的全部属性：等级只由 {@code level} 改，其余整份拷贝）。</p>
	 *
	 * <p>⚠ 子波<b>尚未入世界</b>，调用方自己 {@code addFreshEntity}。⚠ 这是<b>分裂</b>语义；
	 * "旧波换新波"请用 {@link #inheritForTransmute}（两者刻意不是同一个方法，见那里）。</p>
	 *
	 * @param wave  父波句柄（{@code null} ⇒ 返回 {@code null}）
	 * @param pos   子波出生点（已换算到世界坐标）
	 * @param dir   子波方向（单位向量）
	 * @param level 子波等级（= 父波等级 − 降级量）
	 * @param index 本子波在本次分裂中的序号（0 起；载荷按份均摊用）
	 * @param total 本次分裂的子波总数（≥1）
	 * @return 新子波（<b>尚未入世界</b>）；{@code null} = 放弃发射
	 */
	public static AbstractChargerWaveEntity createChildWave(AbstractChargerWaveEntity wave, Vec3 pos, Vec3 dir,
			int level, int index, int total) {
		return wave == null ? null : wave.createChildWave(pos, dir, level, index, total);
	}

	/**
	 * <b>穿波转换的继承模板</b>：把"旧波换新波"时要承接的三项一次写全
	 * （速度修正 / 电荷极性 / 当前渲染色）。
	 *
	 * <p><b>谁需要它</b>：① 第二层的星辉波变器（{@code StellarWaveTransmuterPass} 改造前手抄这三行，
	 * 现在只调本方法）；② 任何"把一枚波替换成另一枚"的联动（例如自己把波升级成别的实体）。</p>
	 *
	 * <p><b>为什么与 {@link #createChildWave} 分开</b>：{@code createChildWave} 是<b>分裂</b>
	 * （整份拷贝父波属性，只改等级/方向/出生点/出生色）；穿波是<b>旧波换新波</b>，新波带着机器刚扫到的
	 * 载荷，而旧波被丢弃 ⇒ 继承集合只能是"表现/飞行连续性"那三项，否则<b>改玩法</b>
	 * （新波会开始带主人 ⇒ 不再伤发射者；带批次号 ⇒ 同批波互相不湮灭）。
	 * 逐项清单写在 {@code AbstractChargerWaveEntity#inheritForTransmute} 的 javadoc 上。</p>
	 *
	 * @param from 旧波句柄（{@code null} ⇒ 不做事）
	 * @param to   新波句柄（{@code null} ⇒ 不做事）
	 */
	public static void inheritForTransmute(AbstractChargerWaveEntity from, AbstractChargerWaveEntity to) {
		if (from != null && to != null) {
			from.inheritForTransmute(to);
		}
	}

	// ==================================================================================
	// 四、注册：把"新的等级 / 新的魔素"接进注册表（批 6 新增的两个一句话入口）
	// 本层<b>不</b>持有数据：真源仍是 WaveLevelRegistry / WaveEssenceRegistry，这里只做逐字转发，
	// 所以第三方不必记两个类名，也不会出现第二份表。
	// ==================================================================================

	/**
	 * <b>注册一个新的波等级</b>（一行）——转给 {@link WaveLevelRegistry#register}。
	 *
	 * <p><b>谁需要它</b>：想加第 6 级（或任意等级）的扩展模组。此前这条路<b>在门面上完全不存在</b>
	 * （注册表本身早在批 1 就有，但第三方要先知道 {@code WaveLevelRegistry} 这个名字）。</p>
	 *
	 * <p>示例：</p>
	 * <pre>{@code
	 * WaveAccess.registerLevel(6, WaveLevelSpec.builder("ζ")
	 *     .baseSpeed(9.0d).maxSpeed(14.0d).damage(14.0f).indicatorColor(0xFFB020).build());
	 * }</pre>
	 *
	 * <p>时机：自己的 {@code @Mod} 构造器里（{@code RegisterEvent} 之前、任何波被生成/读取之前）。
	 * 唯一性/越界/非法数值的裁决全在注册表（同等级同内容 = 幂等，同名不同值 = 当场报错）。</p>
	 *
	 * @param level 等级号（≥ {@link WaveLevels#LOW}）
	 * @param spec  该等级的完整数值声明
	 * @return 生效的声明（重复注册时返回先注册的那一份）
	 */
	public static WaveLevelSpec registerLevel(int level, WaveLevelSpec spec) {
		return WaveLevelRegistry.register(level, spec);
	}

	/**
	 * <b>注册一种新的魔素</b>（一行）——转给 {@link WaveEssenceRegistry#register}。
	 *
	 * <p><b>谁需要它</b>：想加第 12 种（或任意种）魔素的扩展模组。改造前魔素是枚举，
	 * 第三方<b>造不出新值</b>；批 4 把注册表建好了，本方法是它的门面入口。</p>
	 *
	 * <p>示例：</p>
	 * <pre>{@code
	 * WaveAccess.registerEssence(WaveEssence.of("yourmod:thunder").color(0x9B5CFF).build());
	 * }</pre>
	 *
	 * <p>⚠ id 规则：内置 11 个的 id 逐字等于枚举名（老存档兼容）；第三方必须是
	 * {@code namespace:path}（不含冒号会被注册表拒绝 —— 那种名字永远读不回来）。</p>
	 *
	 * @param essence 魔素声明
	 * @return 生效的魔素（id 已被占用时返回先注册的那一个）
	 */
	public static WaveEssence registerEssence(WaveEssence essence) {
		return WaveEssenceRegistry.register(essence);
	}
}
