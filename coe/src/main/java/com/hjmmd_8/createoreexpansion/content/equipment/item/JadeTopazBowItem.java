package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowAstralBarrageConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowThunderMightConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowWaveShiftConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.factory.BowContextFactory;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import net.minecraft.server.level.ServerPlayer;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergyColors;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import java.awt.Color;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 弓族物品类 —— <b>四把弓共用</b>（翠玉 {@code jade_topaz_bow} / 宝石 {@code sapphire_ruby_bow}
 * / 星界 {@code astral_bow} / 雷鸣 {@code thunder_bow}），模组组件化能量体系。
 *
 * <p><b>2026-10-03「把弓补齐到 4 把」批 1</b>：本类原先只服务翠玉之弓，现在由四把共用
 * —— 差异<b>只在构造时传进来的 {@link BowTier}</b>（能量上限 / 耐久上限 / 取色 / 能量条色标），
 * 行为逻辑（无箭耗能 {@value #NO_ARROW_COST}、拉弓、发射、技能释放）四把逐字相同。
 * 翠玉那把的注册值也因此一字未变：档位表里 {@code JADE_TOPAZ} 那一行就是它的现行值
 * （能量 2000 / 耐久 1536 / 取色 TOPAZ，见 {@link BowTier} 类注释的"待裁"一节）。</p>
 *
 * <p>能量上限随档走（{@link BowTier#energy()} = 500 / 2000 / 5000 / 5000 的口径表，
 * 翠玉档按红线保持现行 2000），耐久上限同表（{@link BowTier#durability()}）。</p>
 *
 * <p>技能（模组技能体系，绑定于 SKILLS 组件，tooltip 自动显示按键）：</p>
 * <ul>
 *     <li>技能一（键一，默认左 Shift）——凋零诅咒：命中附加凋零+缓慢+药水云；</li>
 *     <li>技能二（键二，默认 R）——缴械风暴：命中范围缴械+怪物扒装备。</li>
 * </ul>
 *
 * <p>释放流程：按下时锁定技能键位 → 松手射击时经 {@link SkillsComponent#releaseSkillAt}
 * 统一释放（能量预检查/消耗/冷却走模组体系）→ 发射时把技能 id 写入箭的 persistentData，
 * 命中后由 {@code JadeTopazBowEventHandler} 读取并调用对应技能效果。</p>
 *
 * <p><b>2026-10-03 弓技能批 1（技能继承）</b>：三把新弓<b>继承</b>翠玉之弓那两条技能
 * ——<b>复用同一对 id</b>（{@code createoreexpansion:bow_curse} / {@code :bow_disarm}，
 * 不新建 id、不新建技能条目、不动语言键），差异只在<b>起始等级</b>与<b>等级上限</b>，
 * 两者都取自 {@link BowTier#baseSkillLevel()} / {@link BowTier#maxSkillLevel()}
 * （绑定在 {@code CoeItems#inheritedBow}，运行时的唯一读取点是
 * {@link #effectiveSkillLevel(ItemStack)}）。翠玉之弓的两条技能与那四行注册链<b>一字未动</b>。</p>
 *
 * <p>本批同时补了两件事，都在本类：① 箭的来源标记 {@link #TAG_SOURCE_BOW}
 * （把基础概率效果收窄到本模组四把弓，见 {@link #isFromOurBow}）；
 * ② 技能冷却的载体边界 {@link BowTier#perSkillCooldown()}（翠玉按物品记、三把继承弓按技能记）。</p>
 *
 * <p><b>2026-10-04 弓技能批 3（被动技能「元矢自生」）</b>：<b>无箭射击</b>时（就是下面
 * {@link #NO_ARROW_COST} 那条"耗能造一支魔法箭"的既有路），按<b>该弓的档位起始等级</b>
 * （{@link BowTier#baseSkillLevel()}）掷一次骰子：中签给这一发箭附上<b>八种魔素里随机的一种</b>
 * （{@link BowMetaArrowTrait}），命中生物时等效于"一枚带魔素的攻击波打中该生物"。它
 * <b>被动、不占键位、不扣能、无冷却</b>，也<b>没有</b>技能条目/语言键/注册项 ⇒
 * {@code AllSkills} 里那两条弓技能与翠玉之弓的四行注册链一个字未动。
 * 载体是 {@link #TAG_META_ESSENCE}（弓上暂存 ⇒ {@link #shootProjectile} 搬到箭上）。</p>
 *
 * <p><b>P3p</b>：实现 {@link EnergyGradientTool} —— 共享库（core）的能量门面/能量 tooltip
 * 不能再 {@code instanceof JadeTopazBowItem}（库不 import 层），改判这个零方法标记契约；
 * 判定结果对现有物品逐个相同。</p>
 *
 * <p><b>2026-10-05 弓技能批 4（宝石弓专属<b>主动</b>技能「量波置换」；同日返工为主动形态）</b>：
 * 作者原话是"发射出的弓箭替换为具有同样重力效果，但是在水中能够沿直线飞行的随机魔素攻击能量波…
 * 2 级生成一枚小伴随波…3 级生成两枚"；同日的第 2 条口径把它的<b>身份</b>钉死：
 * <i>"按住这个技能键释放技能的时候注意哈：只有原始自身是被动技能，只要检测到没有键，就会触发；
 * 其他的都是主动技能哈，都会将射出去的箭换成这个东西"</i>。</p>
 * <ul>
 *   <li><b>只有宝石弓这一档</b>（{@link BowWaveShiftConfigs#appliesTo(BowTier)}，穷尽 switch）
 *       —— 翠玉 / 星界 / 雷鸣三把弓的发射路径<b>一个字节都不变</b>；
 *       ⚠ 批 5 曾把星界那一档加进来、<b>批 6 又按作者的新定义摘掉了</b>（见下面批 6 那一段）
 *       ⇒ 今天这句话<b>重新逐字成立</b>：这条路上只有宝石弓；</li>
 *   <li><b>主动技能的判据 = 这一发按着技能键</b>（{@link #waveShiftKeyHeld} →
 *       {@code CoeSkillRelease#anyHeldItemSkillKeyPressed}，服务端权威、与技能释放路径<b>同一个
 *       来源</b>）：按了键的那一发<b>无论手里有没有箭</b>都换成波；<b>没按键的普通射击
 *       （有箭 / 无箭）一律走 {@code super.shoot} 原路</b>。⚠ 返工前那一版是反的
 *       （"无箭那一发被动替换、按了键的那一发反而不替换"），这正是本次要修掉的地方；</li>
 *   <li><b>与「元矢自生」互斥</b>（作者："只要检测到没有键，就会触发"）：无箭那一发<b>没按键</b>
 *       = 耗 {@value #NO_ARROW_COST} 点造无形魔法箭 + 概率附魔素；无箭那一发<b>按了键</b>
 *       = 换成波（被动不再掷骰子）；</li>
 *   <li><b>箭的替换点</b>：覆写 {@link #shoot}（<b>不是</b> {@code shootProjectile} ——
 *       原版 {@code ProjectileWeaponItem#shoot} 是"先 shootProjectile(..) 再 addFreshEntity(..)"，
 *       在那里 discard 箭会以"Tried to add entity … marked as removed already"的 WARN 收场，
 *       每发一条）；命中替换条件后<b>整支箭都不造</b>，改由
 *       {@link BowWaveShiftLauncher#fire} 发波；</li>
 *   <li><b>挂在弓自己的物品级技能槽上，无新增技能条目 / 语言键 / 注册项</b>：键位不新造
 *       （它问的是"本把弓自己的技能槽有没有被按住"，即既有那两条技能的键），与回旋镖批 3/4 的
 *       "携带式技能"同一个形状（{@code BoomerangItem#skillKeyHeld}）。代价（刻意接受的）：
 *       工具提示 / HUD 里没有它的条目，内核侧也没有它的耗能与冷却 —— 耗能沿用批 4 已落地的口径
 *       （无箭那一发付 {@value #NO_ARROW_COST} 点，有箭那一发箭本身就是代价），冷却无；</li>
 *   <li>⚠ 做成<b>正式技能条目</b>要一次动三处（{@code AllSkills} 新 id + {@code skiller:skill}
 *       内核白名单 + 一条 {@code skill.createoreexpansion.<id>} 语言键），而语言键住在根
 *       {@code src/main/java/.../data/lang/*}（本次返工的硬边界之外，且要跑 {@code runData}）
 *       ⇒ 本次刻意只走"既有形状"，见返工报告；</li>
 *   <li>数值（等级表 / 重力量 / 环绕几何）全部住在 {@link BowWaveShiftConfigs}。</li>
 * </ul>
 *
 * <p><b>2026-10-05 弓技能批 5（星界弓「星元波置」+ 雷鸣弓「雷鸣神力」；同日，两条一起落地）</b>：</p>
 * <ul>
 *   <li>⚠ <b>星界弓那一半已被批 6 整条返工推翻</b>（见下面批 6 那一段）。当时的口径是
 *       "星界弓「星元波置」= 批 4 那条技能原样多一档"（作者裁定："是它自己的 0/1/2，
 *       <b>不是</b>恒定 2 级"），落法是 {@link BowWaveShiftConfigs#appliesTo(BowTier)} 里多一行
 *       {@code case ASTRAL -> true;}，发射等级仍是 {@code this.tier.baseSkillLevel()}（星界 = 3）
 *       ⇒ 当时星界按键那一发 = 主波 γ + <b>2</b> 枚伴随波。<b>这条口径今天不再成立</b>
 *       （那一行已改回 {@code false}）；本类也不再"一个字都没改"（新增了一条私有闸门）。</li>
 *   <li><b>雷鸣弓「雷鸣神力」是另一套</b>（作者："不是发波"）：新增
 *       {@link #fireThunderMightInsteadOfArrow} —— <b>同一个</b>键位读数（同一个
 *       {@link #waveShiftKeyHeld}）、同一处耐久记账、同一条"整支箭都不造"的路，换出来的却是
 *       <b>以命中点为中心的水平方形内每只生物随机染一笔电荷 + 按等级概率劈一道原版闪电</b>
 *       （落点由射线现算，<b>绝不取施放者坐标</b>；数值全住在
 *       {@link BowThunderMightConfigs}，实际发射在 {@code BowThunderMightLauncher}）。
 *       <b>批 6 对它零改动。</b></li>
 *   <li><b>三条技能一个都没变成正式技能条目</b>：零新 id、零内核白名单、零语言键、不跑
 *       {@code runData} —— 键位仍走同一条 {@code CoeSkillRelease#anyHeldItemSkillKeyPressed}。</li>
 *   <li>⚠ <b>不按键的普通射击（有箭 / 无箭）仍然逐字走 {@code super.shoot}</b>：两条闸门都不通过时
 *       本类的 {@link #shoot} 与批 4 一模一样地落回原版路径。</li>
 * </ul>
 *
 * <p><b>2026-10-05 弓技能批 6（星界弓「星元波置」按作者新定义<b>整条返工</b>）</b>：
 * 作者原话（逐字）"<b>不是发射能量波哈，不是替换哈</b>，就是锚定我方前面 4 格的一块圆形区域，
 * 半径等于技能等级加 1，空中落下无数带有嬗乱效果的药水箭与魔速能量波。魔素随机 8 抽 1。
 * 射中之后会造成滞留效果，时间 4 秒、5 秒、6 秒"。</p>
 * <ul>
 *   <li><b>摘掉的那部分</b>：星界弓不再走 {@link #fireWaveShiftInsteadOfArrow}
 *       （{@link BowWaveShiftConfigs#appliesTo(BowTier)} 里 {@code case ASTRAL -> false;}）
 *       —— 它按键那一发<b>既不发射波、也不替换箭</b>；</li>
 *   <li><b>新增的那部分</b>：{@link #fireAstralBarrageInsteadOfArrow} —— 与另外两条闸门
 *       <b>逐条同构</b>（同一个 {@code shoot} 覆写、同样的"档位表 + 技能键"两道闸门、
 *       同样扣一笔耐久、同样整支箭都不造），换出来的却是<b>在锚定圆盘上降下一场弹幕</b>
 *       （嬗乱药水箭 + 随机魔素能量波）；圆心 / 半径 / 波级 / 滞留时长 / 条数 / 节拍全部按名取自
 *       {@link BowAstralBarrageConfigs}，实际降下在 {@code BowAstralBarrageLauncher#fire}
 *       —— <b>本方法一个真源数字都不写</b>；</li>
 *   <li><b>键位口径沿用批 4 / 5 的形状</b>：同一条私有 helper {@link #waveShiftKeyHeld}
 *       （服务端权威、与技能释放在同一个入口），而且照 {@link #fireThunderMightInsteadOfArrow}
 *       的写法把读数存进局部量 ⇒ <b>不新增</b> {@code !waveShiftKeyHeld(..)} 调用点
 *       （批 4 的关卡数着那个数）；<b>不按键的那一发照旧走 {@code super.shoot} 原路</b>；</li>
 *   <li><b>宝石 / 雷鸣 / 翠玉三条一个字未改</b>：三张档位表两两不相交，本批只把星界那一档从
 *       宝石那张表挪进它自己的新表。</li>
 * </ul>
 */
public class JadeTopazBowItem extends BowItem implements EnergyGradientTool {

	/** 箭 persistentData / 弓暂存标记：本次射击携带的技能 id（ResourceLocation 字符串） */
	public static final String TAG_SKILL = "jade_topaz_skill";
	/** 箭 persistentData / 弓暂存标记：本次射击携带的技能有效等级（一技能多等级，命中时按等级取配置） */
	public static final String TAG_SKILL_LEVEL = "jade_topaz_skill_level";
	/**
	 * 箭 persistentData：<b>射出它的那把弓是哪一个本模组物品</b>（注册 id 字符串，
	 * 例如 {@code createoreexpansion:sapphire_ruby_bow}）。
	 *
	 * <p><b>2026-10-03 弓技能批 1 第 5 条（生效范围补门）</b>：基础概率效果
	 * （{@code JadeTopazBowEventHandler#applyBaseEffects} 的 50%/20%/20%/10%）原先对
	 * <b>任何玩家的任何箭</b>生效 —— 于是原版弓、别家模组的弓射出的箭也会带这套效果。
	 * 现在收窄为「箭来自本模组四把弓」，判据就是本标记：</p>
	 * <ul>
	 *   <li><b>写</b>：{@link #shootProjectile}（本模组四把弓共用的发射点，<b>无条件</b>写，
	 *       与是否按了技能键无关）——形状照 {@link #TAG_SKILL} 的做法：同一个
	 *       {@code persistentData} 上的一个字符串键，发射时写、命中时读；</li>
	 *   <li><b>读</b>：{@link #isFromOurBow} —— 唯一判据，命中处理器拿它给基础效果补门。</li>
	 * </ul>
	 * <p>⚠ 技能那一段<b>不</b>改判据：技能箭本来就带 {@link #TAG_SKILL}（只有本模组弓会写），
	 * 且两段式（松手写标记 / 命中读标记）一个字没动。</p>
	 */
	public static final String TAG_SOURCE_BOW = "jade_topaz_source_bow";

	/**
	 * <b>弓 / 箭上的"这一发带着哪种魔素"标记（枚举名）</b>—— 被动技能「元矢自生」的载体
	 * （2026-10-04 弓技能批 3）。
	 *
	 * <p>同一个键名在两个对象上各用一次，形状照 {@link #TAG_SKILL}（同一个
	 * {@code persistentData}/{@code CUSTOM_DATA} 上的一个字符串键：发射时写、命中时读）：</p>
	 * <ul>
	 *   <li><b>写（弓）</b>：{@link #prepareProjectiles} 的"无箭射击"分支里中签时写在弓的
	 *       {@code CUSTOM_DATA} 上（<b>一次性暂存</b>，只有这一条路会写）；</li>
	 *   <li><b>搬（弓 → 箭）</b>：{@link #shootProjectile} 把它搬到箭的 {@code persistentData} 上，
	 *       <b>搬走即在弓上清掉</b>（绝不留到下一发）；</li>
	 *   <li><b>读（箭）</b>：{@code JadeTopazBowEventHandler} 命中时读回来，交给魔素层"对生物"的
	 *       那一支施加（{@code WaveEssenceEffects#applyEssenceOnCreatureHit}）。</li>
	 * </ul>
	 *
	 * <p>它<b>不是</b>技能标记：不参与技能分发、也不影响 {@link #isFromOurBow} 那条生效范围闸门。
	 * 它只可能由本模组四把弓的"无箭射击"写上 ⇒ 原版弓 / 别家模组的弓射出的箭永远没有这个键。</p>
	 */
	public static final String TAG_META_ESSENCE = "jade_topaz_meta_essence";

	/** 无箭时发射魔法箭消耗的能量 */
	public static final int NO_ARROW_COST = 10;

	/** 普通箭伤害倍率（弓的基础高伤特性） */
	public static final float DAMAGE_MULTIPLIER = 2.0F;
	/** 技能二（缴械风暴）箭伤害倍率 */
	public static final float SKILL_B_MULTIPLIER = 4.0F;
	/** 拉满所需 tick */
	public static final float MAX_PULL_TIME = 25.0F;

	/**
	 * 这一把所属的<b>档位</b>（能量上限 / 耐久上限 / 取色 / 能量条色标的唯一来源，
	 * 见 {@link BowTier}）。
	 *
	 * <p>⚠ 它在<b>注册期</b>就固定下来（每把弓 = 一个物品实例 + 一份档），不是逐堆可变的组件值。
	 * 本类自 2026-10-03 弓补齐那轮起由<b>四把弓共用</b>（翠玉 / 宝石 / 星界 / 雷鸣），
	 * 档位差异全部落在这一个字段上，行为逻辑四把逐字相同。</p>
	 */
	private final BowTier tier;

	public JadeTopazBowItem(BowTier tier, Properties properties) {
		// 耐久上限随档走（形态就是本类原来的 384 * 4，只是把那个写死的数换成档位表）。
		super(properties.durability(tier.durability()));
		this.tier = tier;
	}

	/** 本把弓的档位（能量/耐久/取色/技能四项派生量的唯一来源，见 {@link BowTier}）。 */
	public BowTier tier() {
		return tier;
	}

	/**
	 * <b>本把弓上技能的有效等级 —— 唯一读取点</b>（2026-10-03 弓技能批 1；形态照
	 * {@code BoomerangItem#effectiveSkillLevel(ItemStack, int)}，回旋镖那轮的同一件事）。
	 *
	 * <p>口径 = {@code SkillEnergyCost.effectiveLevel(stack, 基准, 上限)}：
	 * 基准取 {@link BowTier#baseSkillLevel()}（1/2/3/3），上限取 {@link BowTier#maxSkillLevel()}
	 * （翠玉 5 / 三把继承弓 3），两者都来自档位表 ⇒ 这一行里没有任何等级字面量。
	 * 技艺提升 / 技艺回溯照旧由 {@code SkillEnergyCost} 从物品附魔读
	 * （{@code skill_boostable} 标签含 {@code #createoreexpansion:skill_tools}，四把弓都在里面）。</p>
	 *
	 * <p><b>为什么等级上限住在档位表而不动 {@code AllSkills} 的 {@code .maxLevel(...)}</b>：
	 * 两条弓技能是<b>复用同一对 id</b>绑到四把弓上的（作者第 3 条：不新建 id），而
	 * {@code AllSkills} 的 {@code maxLevel} 是<b>按技能注册</b>的一份值 —— 改成 3 会连
	 * 翠玉之弓一起改（作者第 7 条明令翠玉那两条的 maxLevel 不动 = 默认 5）。上限本来就是
	 * 「哪把弓」的属性，所以落在档位表这一处真源。</p>
	 *
	 * <p>消费点两处、共用这一个读数：{@code BowShootItemSkill#release}（写进弓/箭的
	 * {@link #TAG_SKILL_LEVEL}）与同类的 {@code consumeResource}（按同一等级算能量费）；
	 * 命中段的 {@code JadeTopazBowEventHandler} 只读箭上写好的那个等级
	 * —— <b>两段式必须同改，改一边不改另一边就是静默失效</b>。</p>
	 */
	public int effectiveSkillLevel(ItemStack stack) {
		return SkillEnergyCost.effectiveLevel(stack, tier.baseSkillLevel(), tier.maxSkillLevel());
	}

	public static float getPowerForTime(int charge) {
		float f = charge / MAX_PULL_TIME;
		f = (f * f + f * 2.0F) / 3.0F;
		return Math.min(f, 1.0F);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean hasArrows = !player.getProjectile(stack).isEmpty();

		// 事件钩子
		InteractionResultHolder<ItemStack> ret = EventHooks.onArrowNock(stack, level, player, hand, hasArrows);
		if (ret != null) return ret;

		// 检查能否射击：无箭且能量不足时不允许
		if (!hasArrows && !ToolEnergy.canAfford(stack, NO_ARROW_COST)) {
			return InteractionResultHolder.fail(stack);
		}

		// 锁定本次射击的技能键位（-1=无技能；0=键一凋零诅咒；1=键二缴械风暴）
		// 同时清除上一箭遗留的技能标记（防止普通箭误带技能），以及上一发"元矢自生"可能遗留的
		// 魔素标记（客户端不发射 ⇒ 那支箭没被搬走时标记会留在弓上，这里是同一道防泄漏闸门：
		// 新的一次拉弓一律从"没有魔素"开始）。
		// ⚠ 主动技能「量波置换」<b>不在这里留任何标记</b>：它在发射那一刻现问服务端键位状态
		// （waveShiftKeyHeld），所以弓上没有"这一发要换成波"的暂存键可泄漏。
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> {
					tag.putInt("PendingSkillSlot", detectSkillSlot());
					tag.remove(TAG_SKILL);
					tag.remove(TAG_SKILL_LEVEL);
					tag.remove(TAG_META_ESSENCE);
				}));
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	// ========== 松手：射击 ==========
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;

		// 1. 计算蓄力
		int pullTime = getUseDuration(stack, entity) - timeLeft;
		pullTime = EventHooks.onArrowLoose(stack, level, player, pullTime,
				player.getProjectile(stack).isEmpty());
		if (pullTime < 0) return;

		float power = getPowerForTime(pullTime);
		if (power < 0.1F) return;

		// 2. 释放技能（若按了技能键）：统一走模组技能释放（能量/冷却/剩余能量提示）
		int pendingSlot = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getInt("PendingSkillSlot");
		// 拉弓时未按技能键（或按键发生在拉弓之后）：松手时实时补测一次，
		// 避免"无箭 + 释放技能"场景下技能被跳过、只扣除魔法箭能量
		if (pendingSlot < 0) {
			pendingSlot = detectSkillSlot();
		}
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> tag.remove("PendingSkillSlot")));
		releaseSkillIfRequested(player, stack, pendingSlot);

		// 3. 准备弹药
		List<ItemStack> projectiles = prepareProjectiles(stack, player);
		if (projectiles == null || projectiles.isEmpty()) return;

		// 4. 发射（shootProjectile 会把弓上的技能标记写入箭）
		if (level instanceof ServerLevel serverLevel) {
			shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
					power * 3.0F, 1.0F, power >= 1.0F, null);
		}
		playShootSound(level, player, power);
		player.awardStat(Stats.ITEM_USED.get(this));
		// 技能标记不清除：下次 use() 会重新覆盖（避免 shoot 时序竞态导致标记提前丢失）
	}

	/**
	 * 按锁定的技能键位释放技能：键一→槽位 0（凋零诅咒）、键二→槽位 1（缴械风暴）。
	 * 能量预检查/消耗/冷却统一由 {@link SkillsComponent#releaseSkillAt} 与技能类完成，
	 * 释放成功后技能类会在弓上写入 {@value #TAG_SKILL} 标记，供发射时写入箭。
	 */
	private void releaseSkillIfRequested(Player player, ItemStack bow, int slot) {
		if (slot < 0) return;

		// 新内核（Skiller）路径：弓技能的执行（耗能 / 冷却 / 写技能标记）全在技能实现里。
		// 弓射击没有对应的 NeoForge 事件 → 用 noEvent + extraData 传弓（绝不能调 getEvent()）。
		//
		// 2026-09-30 第 4 阶段：旧内核分支（SkillsComponent.releaseSkillAt / applySkillBoost /
		// data.skill.getCooldownSeconds）已整体删除 —— 它被迁移闸门全量拦截，属于死代码；
		// 冷却与剩余能量提示现在都由新路径负责。
		if (player instanceof ServerPlayer serverPlayer) {
			CoeSkillRelease.release(serverPlayer, CoeSkillTypes.USE,
					SkillContextEnvironment.noEvent(serverPlayer, serverPlayer.level())
							.extraData(BowContextFactory.KEY_BOW, bow));
		}
	}

	private List<ItemStack> prepareProjectiles(ItemStack bow, Player player) {
		ItemStack ammo = player.getProjectile(bow);

		// 有实体箭
		if (!ammo.isEmpty()) {
			return draw(bow, ammo, player);
		}

		// 无箭但能量够 → 魔法箭（消耗能量，不区分创造模式）
		if (ToolEnergy.canAfford(bow, NO_ARROW_COST)) {
			ToolEnergy.setEnergy(bow, ToolEnergy.getEnergy(bow) - NO_ARROW_COST);
			// 强制物品栏同步，确保客户端立即看到能量变化（与技能消耗逻辑一致）
			player.getInventory().setChanged();
			// 消耗提示（护目镜限定，与技能消耗统一格式：凝能佩行/工具行，佩用佩色、弓用黄→绿渐变）
			ToolEnergy.sendRemainingEnergyWithMedallion(player, bow,
				IMedallion.findBoundMedallion(player, bow));
			// ★ 被动技能「元矢自生」（2026-10-04 弓技能批 3）：补给照旧，之后按<b>该弓的档位起始等级</b>
			// 掷一次骰子 —— 中签就给这一发魔法箭附一种随机魔素（不额外扣能、无冷却）。
			// ⚠ 抽取点只有这一处：有箭的普通射击根本走不到这个分支 ⇒ 那条路一个字未变。
			// ★ 与主动技能「量波置换」的<b>互斥</b>（2026-10-05 批 4 返工）：作者口径是
			// "只有原始自身是被动技能，只要检测到没有键，就会触发" ⇒ 这一发<b>按着技能键</b>时
			// 由主动技能接管（那一发换成波，见 shoot 的闸门），被动<b>不再掷骰子</b>
			// （掷了也没用：那支魔法箭不发射，标记只会在下一次 use() 被清掉）。
			// ⚠ 判据只有一处（waveShiftKeyHeld），发射段与这里读的是同一个键位来源。
			if (!waveShiftKeyHeld(player)) {
				rollMetaArrowEssence(bow, player);
			}
			ItemStack magicArrow = Items.ARROW.getDefaultInstance();
			magicArrow.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
			return List.of(magicArrow);
		}

		return List.of();
	}

	/**
	 * <b>「元矢自生」唯一一处掷骰子 + 写标记</b>（被动技能，2026-10-04 弓技能批 3）：
	 * 按 {@link BowTier#baseSkillLevel()}（翠玉 1 / 宝石 2 / 星界 3 / 雷鸣 3）取概率
	 * （10% / 20% / 30%，表与夹取都住在 {@link BowMetaArrowTrait}），中签则从八种魔素里等概率抽
	 * 一种、把<b>枚举名</b>写进弓的 {@link #TAG_META_ESSENCE}（发射时由
	 * {@link #shootProjectile} 搬到箭上）。
	 *
	 * <p>等级刻意取<b>档位起始等级</b>而不取 {@link #effectiveSkillLevel(ItemStack)}：
	 * 后者会读附魔（技艺提升）⇒ 玩家的附魔会改变这个被动，而作者给的是"该弓的档位起始等级"
	 * （一个按弓固定的量，四把弓各自 10%/20%/30%/30%）。理由全文见 {@link BowMetaArrowTrait} 类注释。</p>
	 */
	private void rollMetaArrowEssence(ItemStack bow, Player player) {
		int level = tier.baseSkillLevel();
		if (!BowMetaArrowTrait.procs(level, player.getRandom())) {
			return;
		}
		String essence = BowMetaArrowTrait.randomEssence(player.getRandom())
			.name();
		bow.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
			data -> data.update(tag -> tag.putString(TAG_META_ESSENCE, essence)));
	}

	/**
	 * <b>这一发是不是按着技能键打出去的</b> —— 本模组三把"主动"弓技能<b>唯一</b>的键位判据
	 * （2026-10-05 弓技能批 4 返工；作者："其他的都是主动技能哈，都会将射出去的箭换成这个东西"）。
	 *
	 * <p>⚠ <b>批 5 起它是两条 / 三档弓共用的读数</b>：宝石弓「量波置换」与星界弓「星元波置」
	 * （{@link #fireWaveShiftInsteadOfArrow}）和雷鸣弓「雷鸣神力」
	 * （{@link #fireThunderMightInsteadOfArrow}）读的<b>都是这一个方法</b>——
	 * 刻意<b>不</b>在第二处再调一次 {@code CoeSkillRelease}，否则"一个键位来源"就变成两处调用，
	 * 两条技能的按键口径也会各自漂移（批次关卡数着 {@code anyHeldItemSkillKeyPressed} 的出现次数）。
	 * 方法名保留批 4 的字面量（它是"波置换的键位判据"那段历史的落点）。</p>
	 *
	 * <p><b>走既有的键位通道，不自己造一套</b>：{@code CoeSkillRelease#anyHeldItemSkillKeyPressed}
	 * ← {@code PlayerPressedKeys#isPressed(ServerPlayer, slot)} —— 与技能释放路径
	 * （{@code CoeSkillRelease#release}）、装备技能、回旋镖的穿刺/环绕读的是<b>同一个入口</b>；
	 * 它由客户端的按键包写入，是<b>服务端权威</b>的（专用服务器同样为真），
	 * 而 {@code AllKeys#isPressed()} 是纯客户端对象（本类只在 {@link #detectSkillSlot()} 那条
	 * 既有路径上用它，本方法<b>刻意不读</b>它）。</p>
	 *
	 * <p><b>为什么不写字面量槽位</b>：判据来自"本把弓自己的技能表"（
	 * {@code CoeSkillProvider#componentOf} 现算出来的绑定表）⇒ 将来给这把弓加减技能、
	 * 或调整登记顺序，这里跟着走，不会出现"按了键一却发波"的静默错位
	 * （与 {@code BoomerangItem#skillSlot} 那条"现算不写死"的理由逐字同源）。</p>
	 *
	 * <p><b>为什么不是标记</b>：返工前那一版把"这一发是无箭补给"写成弓上的一个布尔标记
	 * （{@code TAG_WAVE_SHIFT}）—— 那是<b>被动</b>语义（无箭就替换）。现在判据是"按键"，
	 * 它在发射那一刻就能直接问出来，于是不需要任何一次性暂存键，
	 * "标记泄漏到下一发"那一类无报错的坏法也就<b>不存在</b>了。</p>
	 *
	 * @param shooter 发射者（只有服务端玩家才读得到键位；非玩家 = 没按）
	 */
	private static boolean waveShiftKeyHeld(LivingEntity shooter) {
		return shooter instanceof ServerPlayer serverPlayer
			&& CoeSkillRelease.anyHeldItemSkillKeyPressed(serverPlayer);
	}

	/** 检测本次射击请求的技能键位：键一=0（凋零诅咒）、键二=1（缴械风暴）、无= -1 */
	private int detectSkillSlot() {
		if (AllKeys.SKILL_RELEASE.isPressed()) return 0;
		if (AllKeys.SKILL_RELEASE_2.isPressed()) return 1;
		return -1;
	}

	/**
	 * <b>发射段总闸门</b>（2026-10-05 弓技能批 4「量波置换」；同日返工为<b>主动</b>形态）：
	 * 命中替换条件时<b>整支箭都不造</b>，改由 {@link BowWaveShiftLauncher#fire} 发一枚带重力的
	 * 攻击波（+ 该等级的伴随环绕波）。
	 *
	 * <p><b>为什么覆写 {@code shoot} 而不是 {@code shootProjectile}</b>：原版
	 * {@code ProjectileWeaponItem#shoot} 的顺序是
	 * <code>createProjectile(..) → shootProjectile(..) → level.addFreshEntity(projectile)</code>
	 * （{@code ProjectileWeaponItem.java:99-101}）—— 在 {@code shootProjectile} 里 discard 掉那支箭，
	 * 紧接着的 {@code addFreshEntity} 会撞上 {@code ServerLevel#addEntity} 的
	 * "entity.isRemoved()" 守卫并打一条 WARN（{@code ServerLevel.java:940-942}）：
	 * <b>每一发一条警告日志</b>，而且那支箭其实已经构造过一遍。覆写 {@code shoot} 则从源头就不造箭。</p>
	 *
	 * <p><b>两个闸门</b>（全部通过才替换，任何一条不通过 ⇒ {@code super.shoot(..)} 逐字走原路径）：</p>
	 * <ol>
	 *   <li>{@link BowWaveShiftConfigs#appliesTo(BowTier)} —— 只有宝石弓这一档（另外三把弓
	 *       一个字节都不变）；</li>
	 *   <li>{@link #waveShiftKeyHeld} —— <b>这一发必须真的按着技能键</b>（服务端权威）。
	 *       这一条就是"主动技能"的定义：<b>没按键的普通射击（有箭 / 无箭）一律走原版路径</b>；
	 *       反过来，按了键的那一发<b>无论手里有没有箭</b>都换成波 —— 手里有箭时那一支已经被
	 *       {@code prepareProjectiles} 的 {@code draw(..)} 从物品栏收走了（箭本身就是代价），
	 *       没箭时则已经付过 {@value #NO_ARROW_COST} 点能量（无箭补给那条既有路）。</li>
	 * </ol>
	 *
	 * <p>⚠ <b>返工删掉了什么</b>：旧版这里是"无箭补给"标记闸门 + "这一发已经释放了技能就不替换"
	 * 那道锁（形状是 {@code if (!getSkill(weapon).isEmpty()) return false;}）。作者当日裁定
	 * "一次只能释放一个主动技能"，那两条闸门<b>语义整个反了</b>：现在<b>只有按了技能键的那一发
	 * 才替换</b>，而弓上不再有任何"要换成波"的暂存标记（判据现问，见 {@link #waveShiftKeyHeld}）。</p>
	 */
	@Override
	protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
						 List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
						 @Nullable LivingEntity target) {
		if (fireWaveShiftInsteadOfArrow(level, shooter, hand, weapon)) {
			return;
		}
		// 批 5：雷鸣弓「雷鸣神力」（另一套：电荷 + 按概率的真雷）。两把弓各问各的档位表，互不相干
		// （宝石 / 星界在那张表里是 false，雷鸣在这张表里是 false 的那一边）。
		if (fireThunderMightInsteadOfArrow(level, shooter, hand, weapon)) {
			return;
		}
		// 批 6：星界弓「星元波置」（作者按新定义返工：不发射波、不替换箭 ⇒ 在锚定区域降下弹幕）。
		// 三张档位表两两不相交（宝石 / 雷鸣在这张表里是 false），所以三条闸门里最多只有一条为真。
		if (fireAstralBarrageInsteadOfArrow(level, shooter, hand, weapon)) {
			return;
		}
		super.shoot(level, shooter, hand, weapon, projectileItems, velocity, inaccuracy, isCrit, target);
	}

	/**
	 * 两个闸门 + 发波 + 记账（见 {@link #shoot} 的说明）。
	 *
	 * @return {@code true} = 这一发已经由能量波替代（调用方<b>不得</b>再走 {@code super.shoot}）
	 */
	private boolean fireWaveShiftInsteadOfArrow(ServerLevel level, LivingEntity shooter, InteractionHand hand,
											   ItemStack weapon) {
		if (!BowWaveShiftConfigs.appliesTo(this.tier)) {
			return false;
		}
		// ★ 主动技能的判据：这一发必须真的按着本把弓自己的技能键（服务端权威读数，
		//   与技能释放路径同一个来源）。没按键 ⇒ 走原版路径（有箭发箭、无箭发那支魔法箭）。
		if (!waveShiftKeyHeld(shooter)) {
			return false;
		}
		// 等级 = 该弓的档位起始等级（宝石弓 2 ⇒ 1 枚伴随波），与「元矢自生」同一处真源。
		BowWaveShiftLauncher.fire(level, shooter, this.tier.baseSkillLevel());
		// 耐久与原版同一笔账（{@code ProjectileWeaponItem#shoot} 射出一发后扣 1 点）：
		// 少了这一行，这条技能会静默变成"宝石弓的按键射击不再磨损弓"（白赚耐久）。
		weapon.hurtAndBreak(getDurabilityUse(weapon), shooter, LivingEntity.getSlotForHand(hand));
		return true;
	}

	/**
	 * <b>雷鸣弓「雷鸣神力」的发射闸门</b>（2026-10-05 弓技能批 5；作者："不是发波，是另一套"）。
	 *
	 * <p>形状与 {@link #fireWaveShiftInsteadOfArrow} <b>逐条同构</b>（同一个 {@code shoot} 覆写里调用、
	 * 同样的两个闸门、同样扣一笔耐久、同样"整支箭都不造"），差别只在"换成了什么"与数值表：</p>
	 * <ol>
	 *   <li><b>档位闸门</b>：{@link BowThunderMightConfigs#appliesTo(BowTier)} —— 只有雷鸣弓这一档
	 *       （宝石 / 星界走另一张表的另一条路，翠玉两处都是 false ⇒ 它那两条技能与普通射击一字未动）；</li>
	 *   <li><b>技能键闸门</b>：<b>同一个</b>服务端权威键位读数（{@link #waveShiftKeyHeld}，
	 *       它问的是 {@code CoeSkillRelease#anyHeldItemSkillKeyPressed} —— 全模组唯一的键位通道，
	 *       与技能释放路径 / 回旋镖 / 装备技能读的是同一个入口）。⚠ 这里刻意<b>不</b>再写一次
	 *       {@code CoeSkillRelease.…}：那会让"一个键位来源"变成两处调用（批 4 的关卡数着这个数）。</li>
	 * </ol>
	 *
	 * <p><b>等级</b>：与另外两条弓技能<b>同一处真源</b> —— {@link BowTier#baseSkillLevel()}
	 * （雷鸣 = 3）⇒ 范围 4×4、真雷 60%。刻意<b>不</b>用 {@link #effectiveSkillLevel(ItemStack)}
	 * （那会读附魔）：作者给的"该弓的档位"是一个按弓固定的量，三条技能都是这个口径。</p>
	 *
	 * <p><b>耐久</b>：与原版 {@code ProjectileWeaponItem#shoot} 同一笔账 —— 少了这一行，
	 * 这条技能会静默变成"雷鸣弓按着技能键射击不再磨损弓"（白赚耐久）。</p>
	 *
	 * <p>⚠ <b>本方法一个数字都不写</b>（连等级都不是字面量）：范围边长 / 真雷概率 / 射程 /
	 * 垂直容差全部按名取自 {@link BowThunderMightConfigs}，实际发射在
	 * {@code BowThunderMightLauncher#strike}。本方法只回答"这一发该不该换成雷鸣神力"。</p>
	 *
	 * @return {@code true} = 这一发已经由雷鸣神力接管（调用方<b>不得</b>再走 {@code super.shoot}）
	 */
	private boolean fireThunderMightInsteadOfArrow(ServerLevel level, LivingEntity shooter, InteractionHand hand,
												   ItemStack weapon) {
		if (!BowThunderMightConfigs.appliesTo(this.tier)) {
			return false;
		}
		// 与「量波置换」同一个键位读数（唯一的键位来源）。写成局部量是为了让"按着键"这件事
		// 只在一处判定（下面那一行是它唯一的消费点）。
		boolean keyHeld = waveShiftKeyHeld(shooter);
		if (!keyHeld) {
			return false;
		}
		// 等级 = 该弓的档位起始等级（雷鸣 3 ⇒ 4×4 / 60%），与「元矢自生」「量波置换」同一处真源。
		BowThunderMightLauncher.strike(level, shooter, this.tier.baseSkillLevel());
		weapon.hurtAndBreak(getDurabilityUse(weapon), shooter, LivingEntity.getSlotForHand(hand));
		return true;
	}

	/**
	 * <b>星界弓「星元波置」的发射闸门</b>（2026-10-05 弓技能批 6；<b>作者按新定义返工</b>）：
	 * 作者原话是"<b>不是发射能量波哈，不是替换哈</b>，就是锚定我方前面 4 格的一块圆形区域，
	 * 半径等于技能等级加 1，空中落下无数带有嬗乱效果的药水箭与魔速能量波…"。
	 *
	 * <p>形状与另外两条闸门（{@link #fireWaveShiftInsteadOfArrow} /
	 * {@link #fireThunderMightInsteadOfArrow}）<b>逐条同构</b>：同一个 {@code shoot} 覆写里调用、
	 * 同样的两个闸门（本技能的档位表 + 同一个服务端权威键位读数）、同样扣一笔耐久、
	 * 同样"整支箭都不造"，差别只在"换成了什么"与数值表：</p>
	 * <ol>
	 *   <li><b>档位闸门</b>：{@link BowAstralBarrageConfigs#appliesTo(BowTier)} —— 只有星界弓这一档
	 *       （⚠ <b>本批把它从 {@link BowWaveShiftConfigs#appliesTo} 里摘掉了</b>：星界那条
	 *       {@code case ASTRAL -> true;} 已改回 {@code false} ⇒ 批 5 的"按键那一发换成波 +
	 *       环绕伴随波"对星界弓<b>不再生效</b>，这不是放宽，而是作者撤回了那条口径）；</li>
	 *   <li><b>技能键闸门</b>：<b>同一个</b>服务端权威键位读数（{@link #waveShiftKeyHeld}，
	 *       它问的是 {@code CoeSkillRelease#anyHeldItemSkillKeyPressed} —— 全模组唯一的键位通道）。
	 *       写成局部量 {@code keyHeld} 是照 {@link #fireThunderMightInsteadOfArrow} 的形状：
	 *       "按着键"这件事只在一处判定，而且 <b>不新增一个 {@code !waveShiftKeyHeld(..)} 调用点</b>
	 *       （批 4 的关卡数着这个数）。</li>
	 * </ol>
	 *
	 * <p><b>等级</b>：与另外三条弓技能<b>同一处真源</b> —— {@link BowTier#baseSkillLevel()}
	 * （星界 = 3）⇒ 半径 4 格、滞留 120 tick。刻意<b>不</b>用
	 * {@link #effectiveSkillLevel(ItemStack)}（那会读附魔）：作者给的"该弓的档位"是按弓固定的量。</p>
	 *
	 * <p><b>耐久</b>：与原版 {@code ProjectileWeaponItem#shoot} 同一笔账 —— 少了这一行，
	 * 这条技能会静默变成"星界弓按着技能键射击不再磨损弓"（白赚耐久）。</p>
	 *
	 * <p>⚠ <b>本方法一个真源数字都不写</b>（连等级都不是字面量）：圆心前推量 / 半径 / 波级 /
	 * 滞留时长 / 条数 / 节拍 / 高度全部按名取自 {@link BowAstralBarrageConfigs}，
	 * 实际降下在 {@code BowAstralBarrageLauncher#fire}。本方法只回答"这一发该不该换成弹幕"。</p>
	 *
	 * @return {@code true} = 这一发已经由星元波置接管（调用方<b>不得</b>再走 {@code super.shoot}）
	 */
	private boolean fireAstralBarrageInsteadOfArrow(ServerLevel level, LivingEntity shooter, InteractionHand hand,
													ItemStack weapon) {
		if (!BowAstralBarrageConfigs.appliesTo(this.tier)) {
			return false;
		}
		boolean keyHeld = waveShiftKeyHeld(shooter);
		if (!keyHeld) {
			return false;
		}
		// 等级 = 该弓的档位起始等级（星界 3 ⇒ 半径 4 / 滞留 120 tick），与其余三条弓技能同一处真源。
		BowAstralBarrageLauncher.fire(level, shooter, this.tier.baseSkillLevel());
		weapon.hurtAndBreak(getDurabilityUse(weapon), shooter, LivingEntity.getSlotForHand(hand));
		return true;
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
								   float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {

		projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle,
				0.0F, velocity, inaccuracy);

		if (projectile instanceof Arrow arrow && shooter instanceof Player player) {
			// 生效范围补门（本批第 5 条）：本模组四把弓射出的箭带来源标记，无条件写
			// （与是否按了技能键无关；命中处理器据它决定要不要滚基础概率效果）。
			// 形状照下面 TAG_SKILL 的做法：persistentData 上一个字符串键，发射时写、命中时读。
			arrow.getPersistentData().putString(TAG_SOURCE_BOW,
					BuiltInRegistries.ITEM.getKey(this).toString());

			// 「元矢自生」（2026-10-04 批 3）：这一发箭若带着魔素（只有"无箭射击"会写上弓），
			// 在这里把标记搬到箭上并在弓上清掉 —— 与来源标记一样是"发射时写、命中时读"。
			stampMetaArrowEssence(player, arrow);

			// 从弓读取本次射击携带的技能与等级（技能类 release 时写入）；无标记则普通箭
			String skill = getSkill(player.getUseItem());
			boolean skillB = "bow_disarm".equals(lastPathOf(skill));

			// 伤害：普通箭 ×2（弓基础高伤）；技能二（缴械风暴）箭 ×4
			float multiplier = skillB ? SKILL_B_MULTIPLIER : DAMAGE_MULTIPLIER;
			arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
			arrow.getPersistentData().putString(TAG_SKILL, skill);
			arrow.getPersistentData().putInt(TAG_SKILL_LEVEL, getSkillLevel(player.getUseItem()));
		}
	}

	/**
	 * <b>把这发箭抽到的魔素从弓上搬到箭上</b>（「元矢自生」的发射段，2026-10-04 弓技能批 3）。
	 *
	 * <p>两段式（写标记 / 读标记）与 {@link #TAG_SKILL} 完全同形，而且<b>搬走即在弓上清掉</b>：
	 * 弓上那个标记是"一次性暂存"，留着就会让下一发（哪怕是有箭的普通箭）误带魔素 ——
	 * 那是本批最容易静默发生的一种坏法（没有报错、只有效果偶尔出现在不该出现的箭上）。</p>
	 *
	 * <p>读的是 {@code player.getUseItem()}（与同一段里读技能标记同一个来源）：{@code shoot}
	 * 就发生在 {@code releaseUsing} 期间，此刻"正在使用的物品"还是这把弓。</p>
	 */
	private static void stampMetaArrowEssence(Player player, Arrow arrow) {
		ItemStack bow = player.getUseItem();
		String essence = getMetaEssence(bow);
		if (essence.isEmpty()) {
			return; // 无标记 = 普通箭（没中签 / 有箭射击 / 非本模组弓）
		}
		arrow.getPersistentData()
			.putString(TAG_META_ESSENCE, essence);
		bow.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
			data -> data.update(tag -> tag.remove(TAG_META_ESSENCE)));
	}

	/**
	 * 读弓（或箭）上暂存的"元矢魔素"枚举名；没有标记时返回空串
	 * （形态照 {@link #getSkill(ItemStack)}，判据就是"空串 = 没有"）。
	 */
	public static String getMetaEssence(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
			.copyTag()
			.getString(TAG_META_ESSENCE);
	}

	/** 提取技能 id 的最后一段（如 "createoreexpansion:bow_disarm" → "bow_disarm"） */
	private static String lastPathOf(String skillId) {
		if (skillId == null || skillId.isEmpty()) return "";
		int colon = skillId.lastIndexOf(':');
		return colon >= 0 ? skillId.substring(colon + 1) : skillId;
	}

	public static String getSkill(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getString(TAG_SKILL);
	}

	/**
	 * <b>这支箭是不是本模组四把弓射出来的</b> —— 生效范围闸门的唯一判据
	 * （2026-10-03 弓技能批 1 第 5 条；标记由 {@link #shootProjectile} 写，见 {@link #TAG_SOURCE_BOW}）。
	 *
	 * <p>原版弓 / 别家模组的弓 / 任何别的来源射出的箭都<b>没有</b>这个键 ⇒ 返回 false
	 * ⇒ 命中处理器不滚那套基础概率效果（技能那一段另有它自己的 {@link #TAG_SKILL} 判据，不受影响）。</p>
	 */
	public static boolean isFromOurBow(Arrow arrow) {
		return !arrow.getPersistentData().getString(TAG_SOURCE_BOW).isEmpty();
	}

	/** 读取弓上暂存的技能有效等级（技能类 release 时写入；无标记为 0） */
	public static int getSkillLevel(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getInt(TAG_SKILL_LEVEL);
	}

	private void playShootSound(Level level, Player player, float power) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
	}

	/**
	 * <b>能量条/能量行的色标——按档问护甲那张取色表</b>（2026-10-03 弓补齐那轮）。
	 *
	 * <p>形态与回旋镖四把<b>逐字相同</b>（{@code BoomerangItem#energyGradientStops}）：
	 * 档 → 同名护甲套（{@link BowTier#armorSet()}）→ 护甲自己的取色源
	 * （{@link ArmorEnergyColors#stopsOf(com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet)}）。
	 * 弓侧<b>一个色值都不复写</b>、也不自己拼渐变，因此色标条数跟着护甲走
	 * （翠玉 2 / 宝石 2 / <b>星界 4</b> / 雷鸣 2），渲染器就是护甲 tooltip 用的那个多段重载。</p>
	 *
	 * <p>⚠ <b>翠玉之弓的渲染结果零变化</b>：翠玉套的色标（{@code 0x55FF55 → 0xFFFF55}）
	 * 与 {@code EnergyTooltipHandler} 里弓那条历史默认<b>逐字同值、同序</b>
	 * （护甲那张表的注释写明"与翠玉之弓同款"，两边本来就是一份）。
	 * 所以本方法只是把"翠玉之弓靠默认值"改成"四把弓都自己回答"，颜色一格没动。</p>
	 */
	@Override
	public List<Color> energyGradientStops(ItemStack stack) {
		return ArmorEnergyColors.stopsOf(tier.armorSet());
	}
}
