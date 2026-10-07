package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * <b>「带电生物受能量场作用」这条链的唯一窄面</b>（coe-charge 批 8；需求 §3.7 / §六 #9）。
 *
 * <h2>它收的是什么链条</h2>
 * <p>需求 §3.7 只有一句话：「带电的生物在能量场里也受力，但<b>比波小</b>」。落成代码就是五个
 * 步骤，全部收在本类<b>唯一那个公开方法</b> {@link #tick(LivingEntity)} 里（顺序即判据）：</p>
 * <ol>
 *   <li><b>是不是该走</b>：{@code null} / 已死 / 客户端 ⇒ 安全值 {@code false}（什么都不做）；</li>
 *   <li><b>带不带电</b>：{@link ChargeApi#hasCharge(LivingEntity)}（<b>唯一</b>的"谁带电"判据，
 *       与中和、残留、四条获得途径共用；本类不碰任何 {@code MobEffect}）⇒ 不带电即返回；</li>
 *   <li><b>水里 / 飞行 ⇒ 跳过</b>（需求 §六 #9 的裁定，见 {@link #skipsField(LivingEntity)}）；</li>
 *   <li><b>受场</b>：交给既有的唯一场实现 {@link EnergyFields#applyFields(net.minecraft.world.level.Level, FieldedEntity)}
 *       —— ⚠ <b>不另写一套公式</b>（需求 §5.3 陷阱 10）：加速场/偏转场、正负号、结构场适配
 *       全部在那一条路上，本类只提供一个 {@link FieldedEntity} 句柄；</li>
 *   <li><b>限幅 + 写回</b>：单 tick 增量上限 → 水平速度上限 → {@code setDeltaMovement}
 *       （位移通道<b>只有这一个</b>：不加属性修饰符、不碰 {@code Attributes}）；
 *       <b>只有真的改了位移才写</b>（没改就一个字节都不写，也因此不会走第 6 步）；</li>
 *   <li>★ <b>让客户端的玩家收得到这一笔</b>（2026-10-04 修「带电玩家在场里一动不动」）：
 *       命中 {@code instanceof Player} 时置 {@code hurtMarked = true} —— <b>原版击退形状</b>
 *       （{@code Entity#markHurt()} 干的就是这一件事，消费者是
 *       {@code ServerEntity#sendChanges()} 尾部的 {@code ClientboundSetEntityMotionPacket}）。
 *       逐条理由见 {@link #tick(LivingEntity)} 的 javadoc。</li>
 * </ol>
 *
 * <h2>为什么是「静态门面 + 安全值」这个形状（照 {@link ChargeApi} / {@code WaveAccess}）</h2>
 * <p>驱动点（{@code EntityFieldHandler}，每 tick 扫已加载维度的生物）不该知道这条链有几段：
 * 它只调 {@link #tick(LivingEntity)}，判据与数值全在这里。所谓"安全值"就是
 * 「{@code null} / 不带电 / 死后 / 客户端 / 水里 / 飞着 / 该维度根本没有场」时的返回值
 * {@code false}（本方法唯一的返回值），逐条写在 {@link #tick(LivingEntity)} 的 javadoc 里。
 * 与 {@code WaveMachineIntegrationPoints} 那套 {@code install(Sink)} 登记机制不同，这里
 * <b>没有"对端模块可能没装"</b>这个问题：场引擎、{@code FieldedEntity} 与两个电荷效果
 * 同住 {@code :coe}（第一层，永远在场）⇒ 不需要间接层，只需要一个窄签名。</p>
 *
 * <h2>数值零字面量（口径与 {@link ChargeApi} 相同）</h2>
 * <p>缩放 0.05、两个限幅、格↔tick 的换算<b>全部按名取自</b> {@link ChargeConfigs}
 * （{@code LIVING_FIELD_STRENGTH_SCALE} / {@code LIVING_FIELD_MAX_DELTA_PER_TICK} /
 * {@code LIVING_FIELD_MAX_SPEED} / {@code TICKS_PER_SECOND} / {@code perTickFactor()}）。
 * 本文件里<b>一个数值字面量都没有</b>：关卡有负向断言逐字扫 {@code 0.05 / 0.25 / 8.0}
 * 这三个数不得在本文件出现（出现了就说明有人把真源复制过来了）。</p>
 *
 * <h2>单位（一处必须讲清楚的事）</h2>
 * <p>{@link FieldedEntity} 契约里的速度是<b>格/秒</b>（波那侧一直如此，
 * {@link EnergyField#apply} 的加速项也按"每 tick 增量 = 强度 ÷ 20"的格/秒口径定义），
 * 而 {@code Entity#setDeltaMovement} 吃的是<b>格/tick</b> ⇒ 句柄在
 * {@link LivingFieldHandle#fieldVelocity()} / {@link LivingFieldHandle#setFieldVelocity(Vec3)}
 * 两个方向上各换算一次，换算本身按名取自 {@link ChargeConfigs}。两个限幅因此都在
 * <b>格/秒</b> 这个单位上判定（与 {@code ChargeConfigs} 里那两条 javadoc 的措辞一致）。</p>
 *
 * <h2>★ 玩家为什么必须额外做两件事（2026-10-04 修「带电玩家不偏转」）</h2>
 * <p>AI 生物与玩家走的<b>不是同一条权威链</b>，所以"写进 {@code setDeltaMovement}"对前者有效、
 * 对后者<b>等于什么都没做</b>。逐条（全部读原版源码定位，见 {@link #tick(LivingEntity)}）：</p>
 * <ol>
 *   <li><b>读</b>：服务端玩家的 {@code getDeltaMovement()} <b>不是</b>他在动的速度。玩家的位置
 *       由客户端上报（{@code ServerGamePacketListenerImpl#handleMovePlayer} → {@code absMoveTo}，
 *       全程<b>不</b>把客户端速度写回服务端）⇒ 服务端这一侧只有它自己的惯性（摩擦力衰减到
 *       水平 ≈ 0）。偏转场是 {@code q·v×B}（旋转 <b>现行速度向量</b>），拿 ≈0 去转还是 ≈0
 *       —— <b>恒等变换</b>，玩家在偏转场里连"被改了位移"都不会发生。⇒ 速度必须取
 *       {@code Entity#getKnownMovement()}（原版那句"这个实体被知道的运动"：玩家 = 客户端
 *       上报的位移，其余实体 = 它自己的 {@code getDeltaMovement()}，见句柄）。</li>
 *   <li><b>写</b>：服务端写玩家的 {@code setDeltaMovement} <b>客户端收不到</b>（客户端自己
 *       跑 travel，且每 tick 用位置包把服务端位置覆盖回它的）⇒ 按<b>原版击退形状</b>置
 *       {@code hurtMarked = true}：{@code ServerEntity#sendChanges()} 尾部会把这一笔发成
 *       {@code ClientboundSetEntityMotionPacket}，客户端 {@code lerpMotion} =
 *       {@code setDeltaMovement}，它才会照这一笔移动。</li>
 * </ol>
 * <p>⚠ <b>2026-10-06 更正（写回必须"加增量"，不是"换速度"）</b>：上面第 2 条只说了
 * "客户端怎么收到这一笔"，<b>没规定写的是什么</b> —— 而这一点正是本类出过 bug 的地方。
 * 批 8 修正（{@code ec404c6e}）改完<b>读</b>那一侧之后，<b>写</b>那一侧仍是
 * "把场算出的整个速度写回实体" ⇒ 对玩家等于<b>每 tick 把他自己上报的位移重灌成权威速度</b>：
 * 作者实测表现是"<b>无输入却持续漂移，方向垂直于场线</b>"（漂移方向 = 他自己上一次的移动方向），
 * 而场真正的那一笔（tier 1 = {@code 4 × 0.05 ÷ 20 = 0.01} 格/秒 每 tick）被完全淹没。
 * ⇒ 句柄现在只叠加 {@code 增量 = 场速度 − 实体自己的速度}：对波 / AI 生物
 * （它们的 {@code getKnownMovement()} 就是 {@code getDeltaMovement()}）<b>逐位等价</b>，
 * 对玩家则不再自我重放。逐条机理见
 * {@link LivingFieldHandle#setFieldVelocity(Vec3)} 的 javadoc。</p>
 *
 * <p><b>两条都只在真的改了位移时做</b>：位移没变（场没覆盖这一点 / 增量被夹成 0 / 偏转场里
 * 静止的实体）⇒ {@code setDeltaMovement} 都不调，{@code hurtMarked} 也就不置 ——
 * 免得"带电但站在场外"的玩家每 tick 白挨一个位移包。</p>
 *
 * <h2>代价</h2>
 * <p>每只生物每 tick 只有：两次 {@code hasEffect}（{@code HashMap} 命中，在
 * {@link ChargeApi#hasCharge} 里）、一次"水里/飞行"读、以及——<b>只有真的带电</b>时才会走到的
 * 一次场遍历（{@link EnergyFields#applyFields} 对没有场的维度是一次哈希查表）。
 * 带电实体要雷击 / 通电线圈 / 带电波命中才会出现 ⇒ 常态开销可忽略。</p>
 *
 * <h2>不碰的东西</h2>
 * <p>不改 {@link EnergyField} / {@link EnergyFields} 的任何既有语义（只把两处写死的 1.0 换成
 * 「问实体要一次缩放」——对既有实现者同一个数）；不加任何注册项、不写日志、不生成粒子、
 * 不 import 任何可选模组类（{@code createaddition} / {@code curios} / {@code jade} / {@code jei}）；
 * 不碰 {@link ChargeApi} 的五个公开签名。</p>
 */
public final class EntityFieldBridge {

	private EntityFieldBridge() {
	}

	/**
	 * <b>把一只生物接进"受能量场作用"这条链</b>（本类唯一的公开方法，驱动每 tick 每只生物调一次）。
	 *
	 * <p><b>返回值 = "这一 tick 真的走了受力路径"</b>，不是"速度变了"：它只用来区分
	 * 「带电生物被算过」与「被上述任一闸门挡下」。安全值（一律返回 {@code false} 且
	 * <b>不产生任何副作用</b>）：</p>
	 * <ul>
	 *   <li>{@code null} / 已经不活着（{@code isAlive()} 为假）：可能正在被移除的实体；</li>
	 *   <li>客户端（{@code level().isClientSide}）：能量场是<b>服务端权威</b>
	 *       （场表在服务端注册、{@code setDeltaMovement} 只有服务端算数），
	 *       客户端即使拿着同一个 {@code LivingEntity} 也只是静默返回；</li>
	 *   <li>不带电：{@link ChargeApi#hasCharge(LivingEntity)} 为假 —— 这是绝大多数实体走的路径；</li>
	 *   <li>水里 / 飞行：见 {@link #skipsField(LivingEntity)}（需求 §六 #9 的裁定）；</li>
	 *   <li>所在维度此刻没有任何场：{@link EnergyFields#applyFields} 原速返回，
	 *       修正后的速度与受场前<b>逐位相同</b> ⇒ 本方法这一 tick <b>连
	 *       {@code setDeltaMovement} 都不调</b>（幂等，无可见副作用，也不会标记位移包）。</li>
	 * </ul>
	 *
	 * <p><b>★ 写回通道（2026-10-04 修「带电玩家不偏转」，两件事缺一不可）</b>：</p>
	 * <ol>
	 *   <li><b>只有真的改了位移才写回</b>：{@code clampSpeed(clampStep(before, after))}
	 *       与 {@code before} 相等时（场没覆盖这一点 / 偏转场里静止的实体）整段跳过 ——
	 *       写回同一个值既是白做功，也会把下面那个位移包标记连带置上；</li>
	 *   <li><b>玩家（含创造模式玩家，不飞行时）另置 {@code hurtMarked = true}</b>：
	 *       玩家位移是<b>客户端权威</b>（原版 {@code ServerGamePacketListenerImpl#handleMovePlayer}
	 *       只把位置 {@code absMoveTo} 回来，从不把客户端速度写回服务端）⇒ 服务端这一笔
	 *       {@code setDeltaMovement} 客户端永远收不到，表现就是"带电玩家在场里一动不动"。
	 *       置 {@code hurtMarked}（{@code Entity#markHurt()} 的同一个字段 = <b>原版击退形状</b>）
	 *       之后，{@code ServerEntity#sendChanges()} 尾部会 {@code broadcastAndSend(new
	 *       ClientboundSetEntityMotionPacket(entity))}（对 {@code ServerPlayer} 直接发给本人
	 *       连接），客户端 {@code lerpMotion} → {@code setDeltaMovement} 才会照它移动。
	 *       <b>不按模式分叉</b>：创造玩家只要不在飞行就走同一条链（需求原文「创造模式受力」），
	 *       唯一被读的能力字段是 {@link #skipsField(LivingEntity)} 里的 {@code abilities.flying}。</li>
	 * </ol>
	 *
	 * @param entity 本 tick 走到检查的那只生物（可能不带电、可能 {@code null}）
	 * @return 是否真的把这只生物送进了场作用（{@code false} = 被上面任一条闸门挡下）
	 */
	public static boolean tick(LivingEntity entity) {
		if (entity == null || !entity.isAlive() || entity.level().isClientSide) {
			return false;
		}
		if (!ChargeApi.hasCharge(entity)) {
			return false; // 不带电（场对其无效）—— 与场应用器同一条判据，不另问一次
		}
		if (skipsField(entity)) {
			return false;
		}
		LivingFieldHandle handle = new LivingFieldHandle(entity);
		Vec3 before = handle.fieldVelocity();
		Vec3 after = EnergyFields.applyFields(entity.level(), handle);
		Vec3 clamped = clampSpeed(clampStep(before, after));
		if (!clamped.equals(before)) {
			// 真的改了位移才写回；没改就一个字节都不写（也因此不会把下面的位移包标记置上）。
			handle.setFieldVelocity(clamped);
			if (entity instanceof Player player) {
				// 原版击退形状：玩家位移客户端权威，服务端写速度必须同时标记，客户端才收得到
				// （ServerEntity#sendChanges → ClientboundSetEntityMotionPacket）。
				// 这里刻意不按游戏模式分叉：创造玩家不飞行时走同一条链。
				player.hurtMarked = true;
			}
		}
		return true;
	}

	/**
	 * <b>水里 / 飞行 ⇒ 这一 tick 跳过受力</b>（需求 §六 #9 的裁定，本批执行会话定值）。
	 *
	 * <p>为什么这两类要跳过（各自的理由不同）：</p>
	 * <ul>
	 *   <li><b>水里</b>：{@code Entity#setDeltaMovement} 在水里的结算被游泳/浮力逻辑接管
	 *       （原版 {@code travel} 的水分支会重算速度并按流体推动量修正），写进去的增量当 tick
	 *       就被冲掉 —— 表现上只会变成"水里莫名其妙抖一下"；</li>
	 *   <li><b>飞行</b>：鞘翅滑翔（{@code isFallFlying()}）与创造/旁观飞行
	 *       （{@code Player#getAbilities().flying}）时，位移由玩家的飞行输入/滑翔空气动力学决定，
	 *       服务端速度写入要么被忽略、要么把玩家从空中按下去 —— 需求明确要求跳过。</li>
	 * </ul>
	 *
	 * <p><b>只判这三条</b>（「创造飞行」被点名，就老实读那个 {@code Abilities} 字段）。
	 * 没有把"创造模式的玩家"整体排除：创造玩家<b>不飞</b>的时候照常受场（需求只让"飞行"跳过），
	 * 这一点记在批 8 的交付说明里，免得后人以为漏了一条。
	 * ★ <b>2026-10-04 作者裁定加严</b>：创造模式玩家<b>也必须受力</b> ——
	 * 「不飞时与生存玩家完全同链（同样被推动/减速），飞行时不受力」。
	 * ⇒ 本文件<b>不得</b>出现任何按游戏模式的分叉（{@code isCreative()} /
	 * {@code abilities.instabuild} / {@code GameType} / {@code isSpectator} 一个都不许有），
	 * 唯一允许读的能力字段就是下面这一条 {@code flying}（关卡有负向断言守着）。</p>
	 *
	 * @param entity 已经确认"活着 + 服务端 + 带电"的生物
	 * @return {@code true} = 这一 tick 跳过（水里 / 鞘翅 / 飞行中）
	 */
	private static boolean skipsField(LivingEntity entity) {
		if (entity.isInWater()) {
			return true;
		}
		if (entity.isFallFlying()) {
			return true;
		}
		return entity instanceof Player player && player.getAbilities().flying;
	}

	/**
	 * <b>限幅一：单 tick 的速度增量</b>（格/秒）不超过
	 * {@link ChargeConfigs#LIVING_FIELD_MAX_DELTA_PER_TICK}。
	 *
	 * <p>为什么需要它（{@code ChargeConfigs} 那条 javadoc 的量化依据）：一块场给的增量上限是
	 * {@code 100 × 0.05 ÷ 20 = 0.25}，所以<b>单块场永远打不满</b>——这条只截"同一个点被若干块场
	 * 同时覆盖"的叠加。超出时按<b>方向不变、只把长度缩到上限</b>处理（不是逐轴夹取，
	 * 那会把受力方向一起掰弯）。</p>
	 *
	 * @param before 受场前的速度（格/秒）
	 * @param after  受场后的速度（格/秒）
	 * @return 增量已被限幅的速度（格/秒）
	 */
	private static Vec3 clampStep(Vec3 before, Vec3 after) {
		Vec3 step = after.subtract(before);
		double limit = ChargeConfigs.LIVING_FIELD_MAX_DELTA_PER_TICK;
		double length = step.length();
		if (length <= limit) {
			return after;
		}
		return before.add(step.scale(limit / length));
	}

	/**
	 * <b>限幅二：受场后的水平速度上限</b>（格/秒）= {@link ChargeConfigs#LIVING_FIELD_MAX_SPEED}。
	 *
	 * <p><b>只夹水平分量</b>（{@code x}/{@code z}，整体等比缩小），{@code y} 原样保留：
	 * 竖直方向由原版的重力/跳跃/坠落结算，场若去压它就会和那些机制打架
	 * （{@code ChargeConfigs} 那条 javadoc 的"0.4 格/tick 小于玩家碰撞箱的 0.6 格"讲的正是
	 * 水平位移不会整块穿墙）。速度不超过上限时<b>原样返回</b>（连对象都不新建）。</p>
	 *
	 * @param velocity 已过限幅一的速度（格/秒）
	 * @return 水平分量不超过上限的速度（格/秒）
	 */
	private static Vec3 clampSpeed(Vec3 velocity) {
		double limit = ChargeConfigs.LIVING_FIELD_MAX_SPEED;
		double horizontal = velocity.horizontalDistance();
		if (horizontal <= limit) {
			return velocity;
		}
		double factor = limit / horizontal;
		return new Vec3(velocity.x * factor, velocity.y, velocity.z * factor);
	}

	/**
	 * <b>把一只 {@link LivingEntity} 包成 {@link FieldedEntity} 的窄句柄</b>——
	 * 生物<b>不是</b> {@code FieldedEntity} 的实现者（那个接口是波实体的契约），
	 * 所以这条链必须自带一个适配器；适配器是本类<b>私有</b>的嵌套类，
	 * 外面看不到、也不会有人拿它去当第二套契约。
	 *
	 * <p>三个取值口的对应关系：位置 = {@code position()}（与场区域判定同一个坐标空间）、
	 * 速度 = {@code getKnownMovement()}（<b>格/tick → 格/秒</b>，按名换算）、
	 * 电荷 = {@link ChargeApi#polarityOf(LivingEntity)}（门面的判据，本类不自己查效果）。</p>
	 *
	 * <p>★ <b>为什么速度取 {@code getKnownMovement()} 而不是 {@code getDeltaMovement()}</b>
	 * （2026-10-04 修「带电玩家不偏转」的那一半，读原版源码定位）：
	 * {@code Entity#getKnownMovement()} 是原版自己那句「这个实体<b>被知道的</b>运动」——
	 * 对 {@code ServerPlayer} 它返回 {@code lastKnownClientMovement}
	 * （{@code ServerGamePacketListenerImpl#handleMovePlayer} 每次收到位置包就
	 * {@code setKnownMovement(本次位移)}），对其它实体它<b>逐字返回</b>
	 * {@code getDeltaMovement()}（见 {@code Entity#getKnownMovement} 与
	 * {@code ServerPlayer#getKnownMovement}）。⇒</p>
	 * <ul>
	 *   <li><b>AI 生物行为一字不变</b>：它们的 {@code getKnownMovement()} 就是
	 *       {@code getDeltaMovement()}，与批 8 完全等价（只有"被玩家驾驶的载具"会改取驾驶员的
	 *       运动量——那正是原版对这个访问器的定义，也与"载具按驾驶员的意思动"一致）；</li>
	 *   <li><b>玩家不再拿服务端惯性当速度</b>：服务端玩家的 {@code getDeltaMovement()} 只有它
	 *       自己的摩擦力衰减（水平 ≈ 0），偏转场（{@code q·v×B}，旋转<b>现行速度</b>）拿它转
	 *       出来还是 0 ⇒ 修之前玩家在偏转场里是<b>恒等变换</b>，修之后转的是他真实在动的速度。</li>
	 * </ul>
	 * <p>⚠ 这个访问器是<b>原版公开 API</b>（{@code Entity#getKnownMovement()}），不是本批新造的
	 * 通道，也不需要任何新数值：写回仍走 {@code setDeltaMovement}，格子↔秒的换算仍按名取自
	 * {@link ChargeConfigs}。</p>
	 *
	 * <p>★ <b>{@link #fieldStrengthScale()} 的覆写是本批需求的那一条</b>：带电生物一律
	 * {@link ChargeConfigs#LIVING_FIELD_STRENGTH_SCALE}（0.05 = 波那侧的 1/20，需求 §六 #9
	 * 「比波小」）。能走到这个句柄的实体已经过 {@link EntityFieldBridge#tick(LivingEntity)}
	 * 的"带电"闸门 ⇒ 这里的常量读的就是"带电生物"的缩放，不需要在句柄里再问一次
	 * （问两次就有了第二处"谁带电"的口径）。</p>
	 */
	private static final class LivingFieldHandle implements FieldedEntity {

		private final LivingEntity entity;

		private LivingFieldHandle(LivingEntity entity) {
			this.entity = entity;
		}

		@Override
		public String fieldLevelKey() {
			return entity.level()
				.dimension()
				.location()
				.toString();
		}

		@Override
		public Vec3 fieldPosition() {
			return entity.position();
		}

		@Override
		public Vec3 fieldVelocity() {
			// ★ 玩家取"客户端上报的运动"（getKnownMovement），其余实体与批 8 逐字等价
			//   （它们这个访问器就是 getDeltaMovement）—— 理由见本类 javadoc 与句柄注释。
			return entity.getKnownMovement()
				.scale(ChargeConfigs.TICKS_PER_SECOND);
		}

		/**
		 * <b>只把"场这一 tick 给的增量"叠加到实体自身的位移上</b>——<b>绝不</b>用场算出的速度
		 * 替换实体自己的速度。
		 *
		 * <p>⚠ <b>2026-10-06 更正</b>（作者实测：「带电玩家在场里<b>无输入也持续漂移</b>，
		 * 且漂移方向<b>垂直于场线</b>」）。本方法原先是一句
		 * {@code entity.setDeltaMovement(velocity.scale(perTickFactor()))} —— 把场算出来的
		 * <b>整个速度</b>写回实体。批 8 的修正（{@code ec404c6e}）只改了<b>读</b>的那一侧
		 * （改用 {@link Entity#getKnownMovement()} 读玩家真实速度），<b>写</b>这一侧没跟着改，
		 * 于是紧接出一个结构性错误：</p>
		 * <ul>
		 *   <li>{@code getKnownMovement()} 对玩家是<b>客户端上一次上报的位移</b>
		 *       （{@code ServerGamePacketListenerImpl#handleMovePlayer} 里的
		 *       {@code vec3 = 玩家位置 − 上一位置}）⇒ {@code ×20} 得到的是
		 *       <b>玩家自己上一次的运动</b>，<b>不是</b>场的产物；</li>
		 *   <li>把"玩家自己的运动 + 场增量"整套写回、并置 {@code hurtMarked} 推给客户端，
		 *       等于<b>每 tick 把玩家自己的运动重灌成他的权威速度</b> ⇒ 观察到的方向
		 *       就是他<b>上一次移动的方向</b>（横穿场时即<b>垂直于场线</b>），
		 *       而且不需要任何输入就会持续；</li>
		 *   <li>场真正的那一份增量小得多（tier 1：{@code strength 4 × 0.05 ÷ 20 = 0.01 格/秒}
		 *       每 tick）⇒ 被完全淹没，玩家<b>感受不到"沿场线被推"</b>。</li>
		 * </ul>
		 *
		 * <p>⇒ 现在计算 {@code 增量 = 场算出的速度 − 实体自己的速度}，只把它叠加到
		 * {@code entity.getDeltaMovement()} 上（格子↔秒仍按名取自 {@link ChargeConfigs}）。
		 * 三类实体的行为：</p>
		 * <ul>
		 *   <li><b>波 / AI 生物</b>：它们的 {@code getKnownMovement()} <b>逐字就是</b>
		 *       {@code getDeltaMovement()} ⇒ 旧式 {@code dv + Δ/20} 与新式 {@code dv + Δ/20}
		 *       <b>逐位相同</b>，行为零变化（关卡 {@code charge-field-living-impulse} 钉着这一条）；</li>
		 *   <li><b>玩家</b>：只多出"场的那一笔"（加速场 = 沿场线；偏转场 = 绕场轴），
		 *       不再把自己的位移当成场的产物重放 ⇒ 自激漂移消失；</li>
		 *   <li><b>骑乘中的玩家</b>：{@code getKnownMovement()} 可能是<b>载具</b>的运动
		 *       （{@code ServerPlayer#getKnownMovement}）—— 旧式会把载具速度灌进玩家自己，
		 *       新式只叠增量，危害面缩到"力律读的是载具速度"这一点。</li>
		 * </ul>
		 */
		@Override
		public void setFieldVelocity(Vec3 velocity) {
			Vec3 selfVelocity = entity.getKnownMovement()
				.scale(ChargeConfigs.TICKS_PER_SECOND);
			Vec3 step = velocity.subtract(selfVelocity);
			entity.setDeltaMovement(entity.getDeltaMovement()
				.add(step.scale(ChargeConfigs.perTickFactor())));
		}

		@Override
		public ChargePolarity getChargePolarity() {
			return ChargeApi.polarityOf(entity);
		}

		@Override
		public double fieldStrengthScale() {
			return ChargeConfigs.LIVING_FIELD_STRENGTH_SCALE;
		}
	}
}
