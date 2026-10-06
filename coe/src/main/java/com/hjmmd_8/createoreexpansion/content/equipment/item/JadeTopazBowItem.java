package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowAstralBarrageConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowThunderMightConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowWaveShiftConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillProvider;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.factory.BowContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.BowExclusiveShotItemSkill;
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
 * {@link #effectiveSkillLevel(ItemStack)}）。翠玉之弓的两条技能与那四行注册链<b>一字未动</b>。
 * <br>⚠ <b>批 12 已按作者的新技能表重排，上面这段的一半不再成立</b>：三把弓<b>不再</b>一律继承
 * 同一对 id —— 星界 / 雷鸣<b>摘掉了缴械风暴</b>、槽 1 换成共用的「量波置换」，等级也不再取
 * {@code baseSkillLevel()}（那是 1/2/3/3，只服务元矢自生了）而是 {@code skillLevel()} /
 * {@code thirdSkillLevel()}；helper 已改名为 {@code CoeItems#threeSkillBow}。见文末批 12 一节。</p>
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
 *   <li><b>主动技能的判据 = 这一发按着技能键</b>（批 4 当时是 {@code waveShiftKeyHeld} →
 *       {@code CoeSkillRelease#anyHeldItemSkillKeyPressed}；⚠ <b>批 7 已把它换成"专属技能
 *       release 写的标记"</b>，见文末批 7 一节）：按了键的那一发<b>无论手里有没有箭</b>都换成波；
 *       <b>没按键的普通射击（有箭 / 无箭）一律走 {@code super.shoot} 原路</b>。⚠ 返工前那一版是反的
 *       （"无箭那一发被动替换、按了键的那一发反而不替换"），这正是本次要修掉的地方；</li>
 *   <li><b>与「元矢自生」互斥</b>（作者："只要检测到没有键，就会触发"）：无箭那一发<b>没按键</b>
 *       = 耗 {@value #NO_ARROW_COST} 点造无形魔法箭 + 概率附魔素；无箭那一发<b>按了键</b>
 *       = 换成波（被动不再掷骰子）；</li>
 *   <li><b>箭的替换点</b>：覆写 {@link #shoot}（<b>不是</b> {@code shootProjectile} ——
 *       原版 {@code ProjectileWeaponItem#shoot} 是"先 shootProjectile(..) 再 addFreshEntity(..)"，
 *       在那里 discard 箭会以"Tried to add entity … marked as removed already"的 WARN 收场，
 *       每发一条）；命中替换条件后<b>整支箭都不造</b>，改由
 *       {@link BowWaveShiftLauncher#fire} 发波；</li>
 *   <li><b>挂在弓自己的物品级技能槽上，无新增技能条目 / 语言键 / 注册项</b>（⚠ 批 4 当时的口径；
 *       <b>批 7 已把三条专属技能做成正式条目，这一条不再成立</b>，见文末批 7 一节）：键位不新造
 *       （它问的是"本把弓自己的技能槽有没有被按住"，即既有那两条技能的键），与回旋镖批 3/4 的
 *       "携带式技能"同一个形状（{@code BoomerangItem#skillKeyHeld}）。代价（刻意接受的）：
 *       工具提示 / HUD 里没有它的条目，内核侧也没有它的耗能与冷却 —— 耗能沿用批 4 已落地的口径
 *       （无箭那一发付 {@value #NO_ARROW_COST} 点，有箭那一发箭本身就是代价），冷却无；</li>
 *   <li>⚠ 做成<b>正式技能条目</b>要一次动三处（{@code AllSkills} 新 id + {@code skiller:skill}
 *       内核白名单 + 一条 {@code skill.createoreexpansion.<id>} 语言键），而语言键住在根
 *       {@code src/main/java/.../data/lang/*}（本次返工的硬边界之外，且要跑 {@code runData}）
 *       ⇒ 本次刻意只走"既有形状"，见返工报告；<b>（批 7 已按这三处补齐，见文末）</b></li>
 *   <li>数值（等级表 / 波级分布表 / 环绕几何）全部住在 {@link BowWaveShiftConfigs}。
 *       （⚠ 批 4 时这一行还写着"重力量"；批 10 撤回了主波重力，那个常量已从表里删除。）</li>
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
 *       {@link #fireThunderMightInsteadOfArrow} —— 批 5 当时用的是<b>同一个</b>键位读数
 *       （同一个 {@code waveShiftKeyHeld}；⚠ <b>批 7 已换成标记</b>）、同一处耐久记账、
 *       同一条"整支箭都不造"的路，换出来的却是
 *       <b>以命中点为中心的水平方形内每只生物随机染一笔电荷 + 按等级概率劈一道原版闪电</b>
 *       （落点由射线现算，<b>绝不取施放者坐标</b>；数值全住在
 *       {@link BowThunderMightConfigs}，实际发射在 {@code BowThunderMightLauncher}）。
 *       <b>批 6 对它零改动。</b></li>
 *   <li><b>三条技能一个都没变成正式技能条目</b>（⚠ 批 5 当时的形状；<b>批 7 已全部补成正式条目</b>）：
 *       零新 id、零内核白名单、零语言键、不跑 {@code runData} —— 键位仍走同一条
 *       {@code CoeSkillRelease#anyHeldItemSkillKeyPressed}。</li>
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
 *   <li><b>键位口径沿用批 4 / 5 的形状</b>：同一条私有 helper {@code waveShiftKeyHeld}
 *       （服务端权威、与技能释放在同一个入口），而且照 {@link #fireThunderMightInsteadOfArrow}
 *       的写法把读数存进局部量 ⇒ <b>不新增</b> {@code !waveShiftKeyHeld(..)} 调用点
 *       （批 4 的关卡数着那个数）；<b>不按键的那一发照旧走 {@code super.shoot} 原路</b>；
 *       ⚠ <b>这条已被批 7 整条推翻</b>（那条 helper 只问"<b>任一</b>技能键"，见文末批 7 一节）；</li>
 *   <li><b>宝石 / 雷鸣 / 翠玉三条一个字未改</b>：三张档位表两两不相交，本批只把星界那一档从
 *       宝石那张表挪进它自己的新表。</li>
 * </ul>
 *
 * <p><b>2026-10-05 弓技能批 7（把三条专属技能做成正式技能条目 + 扩到"每把弓 3 槽"）</b>：
 * 作者裁定走"扩到 3 槽"——每把弓在 tooltip 里显示<b>三条</b>（继承的 2 条 + 自己的专属 1 条），
 * 而专属那条<b>只认自己那个键</b>（键三，默认 G）。本批把它做成了正式技能条目，并顺手修掉了
 * 批 4/5/6 那个<b>判据歧义</b>：</p>
 * <ul>
 *   <li><b>修掉的副作用</b>：批 4/5/6 的专属技能判据是 {@code waveShiftKeyHeld}（= "本把弓的
 *       <b>任一</b>技能键被按住"）⇒ 按 Shift / R 同样命中它，于是这一发被专属技能接管，而内核
 *       已经先按槽位 0/1 放出了继承的 {@code bow_curse} / {@code bow_disarm}（扣了能量、进了冷却、
 *       写了 {@link #TAG_SKILL} 标记）—— 那两笔账白付、效果一个字没生效。现在专属技能
 *       <b>只</b>在键三那一个槽位被按下时由内核派发（槽位来自本把弓自己的技能表），
 *       按 Shift / R 只放继承那两条，<b>不再白付能量</b>；</li>
 *   <li><b>新的触发链（唯一判据 = 标记）</b>：内核按真实槽位调到<b>专属技能自己的</b>
 *       {@code release} → 它在弓上写"这一发要换成我"的标记
 *       （{@code BowExclusiveShotItemSkill#release} → {@link #consumePendingShot} 的读侧）
 *       → 本类的 {@link #shoot} <b>读一次并清掉</b>那个标记 → 命中哪一道闸门就走哪条路。
 *       ⛔ 弓侧<b>不再</b>自己读任何按键（{@code waveShiftKeyHeld} /
 *       {@code CoeSkillRelease#anyHeldItemSkillKeyPressed} 在本文件里一次都不出现）；
 *       "按的是哪个键"这件事由内核按槽位回答，弓只回答"这一发被指定成了什么"；</li>
 *   <li><b>三条专属技能成为正式条目</b>：{@code AllSkills.BOW_WAVE_SHIFT / BOW_ASTRAL_BARRAGE /
 *       BOW_THUNDER_MIGHT}（各自 {@code .maxLevel(3)}）+ 内核白名单三条
 *       （{@code SkillerIntegration#registerUseSkills}）+ 三条
 *       {@code skill.createoreexpansion.<id>} 语言键（根 {@code data/lang/*} + {@code runData}）。
 *       ⚠ 这<b>推翻</b>了上面批 4/5/6 里"零新 id、零白名单、零语言键、不跑 {@code runData}"
 *       那几句（它们描述的是当时的形状，不是现在的）；</li>
 *   <li><b>三套行为的表现一个字未改</b>：发波 / 降弹幕 / 落雷的数值表、发射点、等级来源
 *       （档位起始等级 {@link BowTier#baseSkillLevel()}）、耐久记账全部逐字不动 ——
 *       本批只改<b>显示与触发路径</b>（硬边界 2）。（⚠ 等级来源这半句已被批 12 改掉：
 *       三条各读自己的列，见文末批 12。）</li>
 *   <li><b>翠玉之弓一个字节未改</b>：它只有槽位 0/1（没有第三条技能），按 G 什么也不发生；
 *       它那两条技能的释放路径与标记机制一个字没动。</li>
 * </ul>
 *
 * <p><b>2026-10-05 弓技能批 8（三件修补；本类只承担第 ① 件）</b>：</p>
 * <ul>
 *   <li><b>修掉的真 bug</b>：两处"这次拉弓按了技能键没有"的读数原先都只问 {@code AllKeys}
 *       （纯客户端对象，服务端 keybind 为 null ⇒ {@code isPressed()} 恒 false）
 *       ⇒ 专用服务器上 {@code PendingSkillSlot} 恒 -1 ⇒ {@link #releaseSkillIfRequested}
 *       直接 return ⇒ <b>弓类技能（含三条专属）在专用服务器上完全不释放</b>
 *       （单人 / 局域网主机不受影响：客户端按键状态在同一个进程里可读）。</li>
 *   <li><b>修法（读数分流，两处同改）</b>：{@code player instanceof ServerPlayer} 时问
 *       {@code CoeSkillProvider.pressedSlot(..)}（服务端权威：{@code PlayerPressedKeys}
 *       经集成层，与 {@code CoeSkillRelease#release} 同一份绑定表），否则才问
 *       {@link #detectSkillSlot()}（客户端读数，关卡 §46 钉着它的三条键位）。
 *       ⛔ 弓侧<b>不</b>直接 import 内核键位表（关卡 §43 钉着"只问集成层"）。</li>
 *   <li><b>语义一个字没改</b>：两段式时机（拉弓起点记录 ⊕ 松手补测）仍是"或"；
 *       一个槽位都没按 ⇒ 没有技能意图 ⇒ 普通射击照旧不释放技能；
 *       真正放哪一条仍由内核按真实槽位派发。</li>
 *   <li>②（专属接管的那一发不扣继承那条的能量与冷却）落在
 *       {@code BowShootItemSkill} + {@code BowExclusiveShotItemSkill}，不在本类；
 *       ③（删零调用的 {@code anyHeldItemSkillKeyPressed}）在 {@code CoeSkillRelease}。</li>
 * </ul>
 *
 * <p><b>2026-10-05 弓技能批 9（星界弓「星元波置」：数量 + 预选框 + 箭波同速）</b>：</p>
 * <ul>
 *   <li><b>数量</b>：药水箭 {@code 20 → 10}、能量波 {@code 10 → 20} —— 两个数只改数值真源
 *       {@link BowAstralBarrageConfigs}，本类与发射处<b>一个字都没动</b>；</li>
 *   <li><b>本类只多了一条"暂存"</b>：{@link #TAG_DRAW_TICKS}（<b>私有</b>）——
 *       {@link #releaseUsing} 把松手那一刻的 {@code pullTime} 写进去，
 *       {@link #shoot} 顶部由 {@link #consumeDrawTicks} 读一次并清掉，转交给星界那条闸门
 *       → {@code BowAstralBarrageLauncher#fire}。它喂的是<b>预选框 / 弹幕圆心的同一处算法</b>
 *       （{@code BowAstralBarrageConfigs#previewCenter}），因此"客户端画的圈"与"服务端落的点"
 *       不可能分家；</li>
 *   <li><b>预选框本身不在本类</b>：它是<b>纯客户端</b>的渲染
 *       （{@code BowAstralBarragePreviewRenderer}），颜色取本把弓档位表里那一色
 *       （{@code BowTier#skillOutlineColor()}：星界 = 星辉石粉），<b>零新增贴图 / 模型</b>；</li>
 *   <li><b>其它三把弓、两条翠玉技能、三条专属技能的触发通道一个字节未改</b>：
 *       本类那三道闸门、两段式标记、服务端权威键位读数全部照旧（关卡 §43/§46/§47 仍在守）。</li>
 * </ul>
 *
 * <p><b>2026-10-05 弓技能批 10（「量波置换」的等级来源 + 主波重力；本类只承担第 ① 件）</b>：</p>
 * <ul>
 *   <li><b>等级来源改口径</b>：{@link #fireWaveShiftInsteadOfArrow} 由
 *       {@code this.tier.baseSkillLevel()}（宝石弓恒 2）改成
 *       {@link #effectiveSkillLevel(ItemStack)}（基准 2 + 技艺提升 / 记忆回溯，上限
 *       {@code BowTier#maxSkillLevel()} = 3）⇒ 附魔提升后波级分布随之升档。
 *       ⚠ 这<b>推翻</b>了上面批 4～7 里"等级 = 该弓的档位起始等级、三条技能都是这个口径"
 *       那几句 —— <b>只推翻「量波置换」这一条</b>：雷鸣（{@code fireThunderMightInsteadOfArrow}）
 *       与星界（{@code fireAstralBarrageInsteadOfArrow}）<b>继续</b>用档位起始等级，
 *       本批一个字节都没动它们；（⚠ <b>批 12 已把这两条也改了</b>：它们改读
 *       {@code BowTier#thirdSkillLevel()} = 1，而「量波置换」的基准也从 2 变成 1。见文末批 12。）</li>
 *   <li><b>主波重力撤回</b>：波级与重力的数值都在 {@link BowWaveShiftConfigs}，本类只负责把
 *       等级读数交出去；重力那一句删在发射点（{@code BowWaveShiftLauncher}），本类无感；</li>
 *   <li><b>拉弓动画</b>：四个 {@code pull}/{@code pulling} item property 的注册住在
 *       {@code client/JadeTopazBowModelRegistration}（本批把另外三把弓一并注册），不在本类。</li>
 * </ul>
 *
 * <p><b>2026-10-06 弓技能批 12（技能表重排 + 预选框两项调整）</b>：</p>
 * <ul>
 *   <li><b>作者给的三槽表</b>（逐把逐槽）：翠玉 凋零诅咒① ｜ 缴械风暴①；宝石 凋零诅咒① ｜
 *       缴械风暴① ｜ 量波置换①；<b>星界 凋零诅咒② ｜ 量波置换② ｜ 星元波置①</b>；
 *       <b>雷鸣 凋零诅咒② ｜ 量波置换② ｜ 雷鸣神力①</b>。⇒ ①「量波置换」由宝石专属变成
 *       <b>宝石 / 星界 / 雷鸣共用</b>（等级 1/2/2），② 缴械风暴从"三把继承"变成
 *       <b>只有翠玉 / 宝石</b>（星界 / 雷鸣摘掉它），③ 凋零诅咒 1/1/2/2。</li>
 *   <li><b>本类改的三处 + 一处口径</b>：{@link #effectiveSkillLevel(ItemStack)} 的基准由
 *       {@code baseSkillLevel()}（1/2/3/3）换成 {@link BowTier#skillLevel()}（1/1/2/2）；
 *       {@link #fireThunderMightInsteadOfArrow} 与 {@link #fireAstralBarrageInsteadOfArrow}
 *       由 {@code baseSkillLevel()} 换成 {@link BowTier#thirdSkillLevel()}（= 1 ⇒ 星界半径 2 /
 *       滞留 4 秒，雷鸣 2×2 / 20%）。⛔ {@link #fireWaveShiftInsteadOfArrow} 一个字未改
 *       （它读的 {@code effectiveSkillLevel} 自己换了基准）；⛔ 元矢自生那条
 *       （{@link #rollMetaArrowEssence}）仍读 {@code baseSkillLevel()} —— 它的 10/20/30%
 *       是硬边界，本批不许动。</li>
 *   <li><b>槽位顺序 = 绑定顺序</b>：内核按主手技能表的下标派发
 *       （{@code CoeSkillProvider#convert}，同类型技能按 {@code .addSkills} 的声明顺序落 0/1/2），
 *       而本类客户端读数 {@link #detectSkillSlot()} 恒是"键一→0 / 键二→1 / 键三→2"。
 *       批 12 之后星界 / 雷鸣的槽 1 由缴械风暴换成量波置换 ⇒ <b>键二改放量波置换</b>，
 *       键三仍是各自的专属技能（{@code BowExclusiveShotItemSkill#ownSlot()} = 2 依旧成立）。</li>
 *   <li><b>预选框两项</b>（纯客户端，都在 {@code BowAstralBarragePreviewRenderer} +
 *       {@link BowAstralBarrageConfigs}）：能跑的距离上限 5 → <b>15</b> 格（推进速度
 *       {@value BowAstralBarrageConfigs#PREVIEW_FORWARD_BLOCKS_PER_TICK} 格/tick 不动）；
 *       边界外观由"一圈线"改成<b>闪烁的能量波样式粒子</b>。两处的数值/形状真源仍在
 *       {@link BowAstralBarrageConfigs} 与那个渲染器里，本类只转交拉弓 tick 数。</li>
 *   <li><b>本批没动的</b>：翠玉之弓那四行注册链、元矢自生、耐久记账、两道标记闸门、
 *       服务端权威键位读数、三条技能的发射点与数值表（除上面那两处等级读数）。</li>
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

	/**
	 * <b>弓 {@code CUSTOM_DATA} 上的"这一次松手时已经拉了多少 tick"暂存键</b>（弓技能批 9）。
	 *
	 * <p>它存在的唯一理由：星界弓「星元波置」的<b>预选框 / 弹幕圆心</b>是"松手那一刻的拉弓时长"的
	 * 函数（{@code BowAstralBarrageConfigs#previewCenter}），而这个时长只在一个地方是权威的 ——
	 * {@link #releaseUsing} 的形参 {@code timeLeft}（{@code pullTime = getUseDuration - timeLeft}）。
	 * 真正发射发生在{@link #shoot}（{@code ProjectileWeaponItem#shoot} 的签名不能加参数），
	 * 两处之间只有这一个栈上的 {@code ItemStack} 可以携带它。</p>
	 *
	 * <p><b>形状照 {@code PendingSkillSlot}</b>（同一个两段式：{@code releaseUsing} 写、
	 * {@code shoot} 读一次并清掉），并且<b>刻意不猜原版 {@code stopUsingItem()} 的调用序</b>：
	 * 在 {@code shoot} 里去问 {@code shooter.getUseItemRemainingTicks()} 能不能读到值，
	 * 取决于原版是先 {@code releaseUsing} 还是先清 {@code useItem} —— 那是一个静默回归
	 * （读到 0 ⇒ 圆心退回 4 格的起点、和客户端画的圈差一格），所以这里用显式携带。</p>
	 *
	 * <p>⛔ 键名<b>只在本文件出现</b>（写它、读它、清它的都是本类），因此它不会变成第二个判据：
	 * 弓侧不读按键、别的技能也看不见它。它<b>不是</b>技能标记、不参与技能分发，
	 * 也<b>不是</b> {@link #TAG_SOURCE_BOW} 那种"来源"标记（弹幕箭根本不带来源标记）。</p>
	 */
	private static final String TAG_DRAW_TICKS = "jade_topaz_draw_ticks";

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
	 * <b>铁砧里「用材料修」本把弓</b>（2026-10-06 批 13 新增，作者裁定）。
	 *
	 * <p>作者原话：「冷却是3秒、4秒、5秒」（弓专属技能的冷却）与「雷鸣不是融合 ⇒ 只认雷鸣合金锭」
	 * —— 后半句就是修复材料的口径：<b>融合套认两种锭、雷鸣套认一种</b>。四档的名单唯一真源 =
	 * {@link BowTier#repairIngredient()}（本方法<b>一个材料名都不写</b>），与
	 * {@code CoeArmorMaterials} / {@code BoomerangTier#repairIngredient()} 三处同源。</p>
	 *
	 * <p><b>为什么必须覆写</b>：原版 {@code Item#isValidRepairItem} 直接 {@code return false}
	 * （{@code Item.java}），四把弓在本批之前<b>完全没有</b>「用材料修」这条途径；它们<b>一直能</b>
	 * 用"两把同名弓合耐久"（{@code AnvilMenu} 里是另一个分支，不经本方法）⇒ 本批补的是缺失的那条，
	 * 不是替换任何既有修复途径。</p>
	 *
	 * <p>回落 {@code super}（形状照原版 {@code ArmorItem#isValidRepairItem}）：本档材料不命中时
	 * 交还父类裁决 —— 今天父类恒 {@code false}，但把回落写出来，"以后父类长出新判据被静默吃掉"
	 * 这件事就不可能发生。</p>
	 */
	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
		return tier.repairIngredient().test(repairCandidate) || super.isValidRepairItem(stack, repairCandidate);
	}

	/**
	 * <b>本把弓上技能的有效等级 —— 唯一读取点</b>（2026-10-03 弓技能批 1；形态照
	 * {@code BoomerangItem#effectiveSkillLevel(ItemStack, int)}，回旋镖那轮的同一件事）。
	 *
	 * <p>口径 = {@code SkillEnergyCost.effectiveLevel(stack, 基准, 上限)}：
	 * 基准取 {@link BowTier#skillLevel()}（<b>批 12 起</b>；槽 0/1 的绑定等级 1/1/2/2），
	 * 上限取 {@link BowTier#maxSkillLevel()}（翠玉 5 / 三把继承弓 3），两者都来自档位表
	 * ⇒ 这一行里没有任何等级字面量。
	 * 技艺提升 / 技艺回溯照旧由 {@code SkillEnergyCost} 从物品附魔读
	 * （{@code skill_boostable} 标签含 {@code #createoreexpansion:skill_tools}，四把弓都在里面）。</p>
	 *
	 * <p>⚠ <b>批 12 把基准从 {@link BowTier#baseSkillLevel()}（1/2/3/3）换成了
	 * {@link BowTier#skillLevel()}（1/1/2/2）</b>：前者现在只服务被动「元矢自生」
	 * （{@link #rollMetaArrowEssence}），后者才是"凋零诅咒 / 缴械风暴 / 量波置换"这三条
	 * <b>在不同弓上等级不同</b>的技能的共同基准。改这一行会同时移动这三条的取配置等级、
	 * 能量费等级与箭上写的等级（三处读的就是本方法这一个值）。</p>
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
		return SkillEnergyCost.effectiveLevel(stack, tier.skillLevel(), tier.maxSkillLevel());
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

		// 锁定本次射击的技能键位（-1=无技能；0=键一凋零诅咒；1=键二缴械风暴；2=键三专属技能）
		// 同时清除上一箭遗留的技能标记（防止普通箭误带技能），以及上一发"元矢自生"可能遗留的
		// 魔素标记（客户端不发射 ⇒ 那支箭没被搬走时标记会留在弓上，这里是同一道防泄漏闸门：
		// 新的一次拉弓一律从"没有魔素"开始）。
		// ⚠ 批 7：专属技能（键三）的"这一发要换成我"标记也在这道闸门里清掉
		// （{@link BowExclusiveShotItemSkill#clearPendingShot}，只清不读）：一次"没打成"的拉弓
		// （无箭且能量不足 ⇒ releaseUsing 在 shoot 之前就 return）会把标记留在弓上，
		// 不在新的一次拉弓起点清掉就会泄漏到下一发普通射击上 —— 与上面那道魔素闸门同一个理由。
		//
		// ★ 批 8 ①：这一次的键位读数 —— <b>服务端一律走服务端权威通道</b>。
		// ⚠ AllKeys 是纯客户端对象（服务端 keybind 为 null ⇒ isPressed() 恒 false），
		// 所以专用服务器上原先这里恒写 -1 ⇒ 松手时 {@link #releaseSkillIfRequested} 直接 return
		// ⇒ 弓类技能（含三条专属）在专用服务器上<b>永远不释放</b>（单人 / 局域网主机看不出）。
		// detectSkillSlot() 保留为<b>客户端</b>读数（关卡 §46 钉着它的三条键位），
		// 服务端改问集成层的 CoeSkillProvider.pressedSlot(..)：同一份绑定表
		// （CoeSkillProvider#componentOf）+ 同一个键位来源（PlayerPressedKeys，由客户端按键包写入）。
		// 两处读数（拉弓起点 + 松手补测）口径必须一致 —— 改一处不改另一处就是"专用服务器上时好时坏"。
		int pendingSlot = player instanceof ServerPlayer serverPlayer
				? CoeSkillProvider.pressedSlot(serverPlayer)
				: detectSkillSlot();
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> {
					tag.putInt("PendingSkillSlot", pendingSlot);
					tag.remove(TAG_SKILL);
					tag.remove(TAG_SKILL_LEVEL);
					tag.remove(TAG_META_ESSENCE);
					// ★ 批 9：与上面三道同形的防泄漏回闸 —— 上一次松手写下的"拉弓时长"若没被
					// 那一发消费掉（例如客户端那一份：shoot 只在服务端跑），在新的一次拉弓起点清掉。
					tag.remove(TAG_DRAW_TICKS);
				}));
		BowExclusiveShotItemSkill.clearPendingShot(stack);
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
		// ★ 批 8 ①：同一条读数口径 —— 服务端问服务端权威通道（PlayerPressedKeys 经集成层），
		// 客户端才用 detectSkillSlot()。少了这一处，专用服务器上"拉弓之后才按技能键"
		// 这一种时机仍然不会释放。
		if (pendingSlot < 0) {
			pendingSlot = player instanceof ServerPlayer serverPlayer
					? CoeSkillProvider.pressedSlot(serverPlayer)
					: detectSkillSlot();
		}
		// ★ 批 9：这一次松手的"拉弓时长"（= 上面那个 pullTime，事件钩子之后的值）。
		//   下面那个 lambda 只捕获等价 final 的局部量，所以先落成一个 final 副本。
		int drawTicksForShot = pullTime;
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> {
					tag.remove("PendingSkillSlot");
					// ★ 批 9：把"这一刻已经拉了多少 tick"留给 shoot（同一个栈、同一发）。
					//   它同时是客户端预选框的输入口径（BowAstralBarrageConfigs#drawnTicks），
					//   于是服务端落的点与客户端画的圈同源。
					tag.putInt(TAG_DRAW_TICKS, drawTicksForShot);
				}));
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
	 * 按锁定的技能键位释放技能：键一→槽位 0（凋零诅咒）、键二→槽位 1（缴械风暴）、
	 * <b>键三→槽位 2（本把弓的专属技能，批 7）</b>。
	 * 能量预检查/消耗/冷却统一由内核的 {@link CoeSkillRelease#release} 与各技能实现完成；
	 * 两条继承技能在弓上写 {@value #TAG_SKILL} 标记供发射时写入箭，
	 * 专属技能则写"这一发要换成我"的标记（{@code BowExclusiveShotItemSkill#release}），
	 * 由 {@link #shoot} 读一次并清掉。
	 *
	 * <p>⚠ 传进来的 {@code slot} 只用来回答"这次松手有没有技能意图"（{@code < 0} 就整个跳过）；
	 * <b>真正放哪一条由内核按 {@code PlayerPressedKeys} 的真实槽位决定</b> ——
	 * 这正是批 7 修掉"任一键"歧义的地方（弓不再自己判断按的是哪个键）。</p>
	 *
	 * <p><b>批 8 ①：这个 {@code slot} 在服务端已经是服务端权威的</b> —— 它的两个来源
	 * （拉弓起点的记录、松手时的补测）在服务端都问 {@code CoeSkillProvider.pressedSlot(..)}
	 * （{@code PlayerPressedKeys}），只有客户端才用 {@code detectSkillSlot()}。
	 * 于是"不按键的普通射击不释放技能"仍然成立（一个槽位都没按 ⇒ 恒 {@code -1} ⇒ 这里 return），
	 * 而专用服务器上按了键的那一发不再被误判成"没按"。</p>
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
			// ★ 与主动技能（宝石「量波置换」/ 星界「星元波置」/ 雷鸣「雷鸣神力」）的<b>互斥</b>
			// （2026-10-05 批 4 返工、批 7 换判据）：作者口径是"只有原始自身是被动技能，
			// 只要检测到没有键，就会触发" ⇒ 这一发<b>已被专属技能接管</b>时被动<b>不再掷骰子</b>
			// （掷了也没用：那支魔法箭不发射，魔素标记会留在弓上、泄漏到下一发）。
			// ⚠ 判据只有一处：专属技能自己的 release 写在弓上的那个标记
			// （{@code BowExclusiveShotItemSkill#hasPendingShot} —— 只问"有没有"，<b>不读值、
			// 不消费</b>；真消费在 {@link #shoot} 那一次）。批 7 起这里<b>不再</b>读任何按键。
			if (!BowExclusiveShotItemSkill.hasPendingShot(bow)) {
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
	 * <b>这一发被指定成了哪个专属技能 —— 读一次并清掉</b>（2026-10-05 弓技能批 7 起，
	 * 这是三条专属技能<b>唯一</b>的判据；它取代了批 4/5/6 的 {@code waveShiftKeyHeld}）。
	 *
	 * <p><b>为什么必须换成标记（批 7 的起因）</b>：旧判据问的是
	 * {@code CoeSkillRelease#anyHeldItemSkillKeyPressed} = "本把弓的<b>任一</b>技能槽被按住"。
	 * 键一 / 键二也是本把弓的技能槽 ⇒ 按 Shift / R 同样命中它，于是这一发被专属技能接管，
	 * 而内核已经先按槽位 0/1 放出了继承的 {@code bow_curse} / {@code bow_disarm}
	 * （扣了能量、进了冷却、写了 {@link #TAG_SKILL}）——那两笔账白付、效果一个字没生效。</p>
	 *
	 * <p><b>新判据的来源是"技能自己写的声明"</b>：内核按<b>真实槽位</b>派发
	 * （槽位来自 {@code CoeSkillProvider#componentOf} 现算出来的绑定表），调到哪条专属技能的
	 * {@code release}，就由<b>那条技能自己</b>把注册 id 写进弓
	 * （{@code BowExclusiveShotItemSkill#release}）。于是"按的是哪个键"由内核回答、
	 * "这一发换成什么"由标记回答，弓侧<b>一个按键都不读</b>：
	 * <ul>
	 *   <li>不按任何技能键 ⇒ 没有标记 ⇒ 走 {@code super.shoot} 原路；</li>
	 *   <li>按 Shift / R ⇒ 写的是 {@link #TAG_SKILL}（继承那两条走自己的两段式），
	 *       <b>没有</b>专属标记 ⇒ 依然走原路，两条继承技能照常生效；</li>
	 *   <li>按键三（默认 G）⇒ 内核放出<b>本把弓自己的那条</b>专属技能 ⇒ 它写标记 ⇒ 这一发被它接管。</li>
	 * </ul>
	 *
	 * <p><b>为什么它读一次就清</b>：方法体就是 {@code consumePendingShot}（读 + 同一次调用里清），
	 * 所以标记最多只能影响一发；弓侧对标记的全部接触就是这一处 + 拉弓起点那道"只清不读"的
	 * 防泄漏闸门（{@code use()}）+ 被动互斥的"只问有没有"（{@code hasPendingShot}）。
	 * ⛔ 弓侧<b>不认识</b>那个键名（{@link BowExclusiveShotItemSkill#TAG_PENDING_SHOT} 只在
	 * 那个类里出现），因此"读第二次"在编译期就没有地方可写。</p>
	 *
	 * @param weapon 本次射击所用的弓
	 * @return 标记里写的技能 id（空串 = 这一发没有任何专属技能）
	 */
	private static String consumePendingShot(ItemStack weapon) {
		return BowExclusiveShotItemSkill.consumePendingShot(weapon);
	}

	/**
	 * <b>读一次并清掉"这一发松手时已经拉了多少 tick"</b>（弓技能批 9）—— 与
	 * {@link #consumePendingShot} <b>逐字同形</b>的两段式读侧：同一个 {@code persistentData}
	 * 思路（这里是 {@code CUSTOM_DATA}）上的一个整数键，写侧唯一（{@link #releaseUsing}），
	 * 读 + 清唯一（本方法，由 {@link #shoot} 在三条专属闸门之前调一次）。
	 *
	 * <p>为什么必须"读一次并清掉"而不是"读一下就算了"：这个键是<b>暂存</b>，不是状态。
	 * 留着它，弓就变成"带着上一次拉弓时长"的物品 —— 一是它会被同步/存档（形状与
	 * {@code PendingSkillSlot} 同一个理由），二是"这一发到底拉了多久"就不再只有一个来源。
	 * 键名 {@link #TAG_DRAW_TICKS} <b>只在本文件出现</b>（写、读、清都在这一个类里）。</p>
	 *
	 * @param weapon 本次射击所用的弓
	 * @return 松手那一刻的拉弓 tick 数（键不存在时= 0，与 {@code getInt} 的默认一致）
	 */
	private static int consumeDrawTicks(ItemStack weapon) {
		int ticks = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getInt(TAG_DRAW_TICKS);
		weapon.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				custom -> custom.update(tag -> tag.remove(TAG_DRAW_TICKS)));
		return ticks;
	}

	/**
	 * <b>客户端读数</b>：检测本次射击请求的技能键位 —— 键一=0（凋零诅咒）、键二=1（缴械风暴）、
	 * <b>键三=2（本把弓的专属技能，批 7）</b>、无= -1。
	 *
	 * <p>⚠ 这里用的是 {@code AllKeys}（<b>纯客户端</b>对象：服务端没有 keybind，
	 * {@code isPressed()} 恒 false）。<b>批 8 ① 之后它只服务客户端</b>：
	 * 两处调用点（{@link #use} 的拉弓起点、{@link #releaseUsing} 的松手补测）都是
	 * {@code player instanceof ServerPlayer ? CoeSkillProvider.pressedSlot(..) : detectSkillSlot()}
	 * —— 服务端走服务端权威通道（{@code PlayerPressedKeys} 经集成层），客户端才走这里。
	 * 因此本方法<b>不再是</b>服务端释放闸门的判据（那正是批 8 修掉的坏法：
	 * 专用服务器上这个方法恒 -1 ⇒ 弓类技能永不释放）。</p>
	 *
	 * <p>产物 {@code PendingSkillSlot} 只用来回答"这次拉弓有没有技能意图"（负数 = 没有），
	 * 真正放哪一条由内核按 {@code PlayerPressedKeys} 的真实槽位决定（见
	 * {@link #releaseSkillIfRequested} → {@code CoeSkillRelease#release}）。</p>
	 */
	private int detectSkillSlot() {
		if (AllKeys.SKILL_RELEASE.isPressed()) return 0;
		if (AllKeys.SKILL_RELEASE_2.isPressed()) return 1;
		if (AllKeys.SKILL_RELEASE_3.isPressed()) return 2;
		return -1;
	}

	/**
	 * <b>发射段总闸门</b>（2026-10-05 弓技能批 4「量波置换」；同日返工为<b>主动</b>形态）：
	 * 命中替换条件时<b>整支箭都不造</b>，改由 {@link BowWaveShiftLauncher#fire} 发一枚
	 * 攻击波（+ 该等级的伴随环绕波）。
	 *
	 * <p><b>等级（批 10 改口径）</b>：本闸门传的是 {@link #effectiveSkillLevel(ItemStack)}
	 * —— 含附魔提升的技能等级（上限 3）。批 4~9 传的是档位起始等级 {@code tier.baseSkillLevel()}
	 * （宝石弓恒 2），作者批 10 明确改成读技能等级，好让附魔提升后波级分布也跟着变。</p>
	 *
	 * <p><b>为什么覆写 {@code shoot} 而不是 {@code shootProjectile}</b>：原版
	 * {@code ProjectileWeaponItem#shoot} 的顺序是
	 * <code>createProjectile(..) → shootProjectile(..) → level.addFreshEntity(projectile)</code>
	 * （{@code ProjectileWeaponItem.java:99-101}）—— 在 {@code shootProjectile} 里 discard 掉那支箭，
	 * 紧接着的 {@code addFreshEntity} 会撞上 {@code ServerLevel#addEntity} 的
	 * "entity.isRemoved()" 守卫并打一条 WARN（{@code ServerLevel.java:940-942}）：
	 * <b>每一发一条警告日志</b>，而且那支箭其实已经构造过一遍。覆写 {@code shoot} 则从源头就不造箭。</p>
	 *
	 * <p><b>标记在这里被读一次并清掉</b>（批 7）：{@link #consumePendingShot} 是本类对那个标记的
	 * <b>唯一</b>一次消费（读 + 清在同一次调用里），三道闸门只比较传下去的那个局部值，
	 * 谁也不再去读弓 —— 于是"这一发被指定成了什么"只有一个真相，而且最多只影响一发。</p>
	 *
	 * <p><b>三道闸门各自的两个条件</b>（全部通过才替换，任何一条不通过 ⇒
	 * {@code super.shoot(..)} 逐字走原路径）：</p>
	 * <ol>
	 *   <li><b>档位闸门</b>：该技能自己的 {@code *Configs#appliesTo(this.tier)} —— 三张表两两不相交，
	 *       钉住"哪把弓有哪条专属技能"这件注册期的事实（宝石 / 星界 / 雷鸣各一条，
	 *       翠玉三条都是 false ⇒ 它一个字节都没改）；</li>
	 *   <li><b>标记闸门</b>：{@code pendingShot} 必须<b>正好等于</b>该技能自己的注册 id
	 *       （{@code matches(..)} 逐字符比较）。这一条就是"专属技能只认自己那个键"：标记只可能由
	 *       <b>那条技能自己的</b> {@code release} 写下，而内核只在它的槽位被按下时调到它 ——
	 *       按 Shift / R 时弓上根本没有专属标记，这一发照旧走原版路径。</li>
	 * </ol>
	 *
	 * <p>⚠ <b>批 7 删掉了什么</b>：批 4/5/6 的 {@code waveShiftKeyHeld}
	 * （= {@code CoeSkillRelease#anyHeldItemSkillKeyPressed}，问的是"本把弓的<b>任一</b>技能键"）。
	 * 它是个<b>歧义判据</b>：按 Shift / R 同样命中它 ⇒ 那一发被专属技能接管，而内核已经先放出了
	 * 继承的 {@code bow_curse} / {@code bow_disarm}（能量与冷却白付、效果一个字没生效）。
	 * 现在本文件<b>一个按键都不读</b>。</p>
	 */
	@Override
	protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
						 List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
						 @Nullable LivingEntity target) {
		// ★ 批 7：唯一一次读取并清除「这一发要换成哪个专属技能」的标记
		//   （读 + 清在同一次调用里 ⇒ 标记最多只能影响一发；弓侧不认识那个键名）。
		String pendingShot = consumePendingShot(weapon);
		// ★ 批 9：同一次调用里读一次并清掉「松手时已经拉了多少 tick」（形状与上面逐字同形）。
		//   它只喂给星界那条闸门，由它转交给 BowAstralBarrageLauncher#fire —— 圆心由此与
		//   客户端预选框同源（BowAstralBarrageConfigs#previewCenter）。
		int drawnTicks = consumeDrawTicks(weapon);
		if (fireWaveShiftInsteadOfArrow(level, shooter, hand, weapon, pendingShot)) {
			return;
		}
		// 批 5：雷鸣弓「雷鸣神力」（另一套：电荷 + 按概率的真雷）。两把弓各问各的档位表，互不相干
		// （宝石 / 星界在那张表里是 false，雷鸣在这张表里是 false 的那一边）。
		if (fireThunderMightInsteadOfArrow(level, shooter, hand, weapon, pendingShot)) {
			return;
		}
		// 批 6：星界弓「星元波置」（作者按新定义返工：不发射波、不替换箭 ⇒ 在锚定区域降下弹幕）。
		// 三张档位表两两不相交（宝石 / 雷鸣在这张表里是 false），所以三条闸门里最多只有一条为真。
		if (fireAstralBarrageInsteadOfArrow(level, shooter, hand, weapon, pendingShot, drawnTicks)) {
			return;
		}
		super.shoot(level, shooter, hand, weapon, projectileItems, velocity, inaccuracy, isCrit, target);
	}

	/**
	 * <b>宝石弓「量波置换」的发射闸门</b> + 发波 + 记账（见 {@link #shoot} 的说明）。
	 *
	 * @param pendingShot {@link #consumePendingShot} 从弓上读出来并已清掉的值（空串 = 这一发不是专属技能）
	 * @return {@code true} = 这一发已经由能量波替代（调用方<b>不得</b>再走 {@code super.shoot}）
	 */
	private boolean fireWaveShiftInsteadOfArrow(ServerLevel level, LivingEntity shooter, InteractionHand hand,
											   ItemStack weapon, String pendingShot) {
		if (!BowWaveShiftConfigs.appliesTo(this.tier)) {
			return false;
		}
		// ★ 批 7 的判据：这一发必须真的由**本技能自己**的 release 指定（标记逐字符相等）。
		//   没被指定 ⇒ 走原版路径（有箭发箭、无箭发那支魔法箭）——按 Shift / R 的那一发就在这一支。
		if (!BowExclusiveShotItemSkill.WAVE_SHIFT.matches(pendingShot)) {
			return false;
		}
		// ★ 批 10：等级 = 该弓的<b>技能等级</b> {@link #effectiveSkillLevel(ItemStack)}
		//   （基准 = 档位起始等级 2，含技艺提升 / 记忆回溯的附魔加成，上限 BowTier#maxSkillLevel() = 3）
		//   —— 波级分布与伴随波枚数都按它取。批 4~9 这里传的是档位起始等级
		//   {@code this.tier.baseSkillLevel()}（宝石弓恒 2 ⇒ 分布恒为 Lv2），作者批 10 改成读技能等级。
		BowWaveShiftLauncher.fire(level, shooter, this.effectiveSkillLevel(weapon));
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
	 *   <li><b>技能键闸门</b>（批 7 换判据）：<b>同一个</b>标记读数（{@code pendingShot}，
	 *       由 {@link #consumePendingShot} 在本条闸门之前读一次并清掉）——必须<b>正好等于本技能的
	 *       注册 id</b>。⚠ 批 5 当时读的是"任一技能键"（{@code waveShiftKeyHeld}），
	 *       那正是批 7 修掉的歧义；现在这里的 {@code matches(..)} 才是"只认自己那个键"的落点。</li>
	 * </ol>
	 *
	 * <p><b>等级（批 12 改口径）</b>：{@link BowTier#thirdSkillLevel()}（雷鸣 = <b>1</b>）⇒
	 * 范围 <b>2×2</b>、真雷 <b>20%</b>（表见 {@link BowThunderMightConfigs} 的 Lv1 行）。
	 * 刻意<b>不</b>用 {@link #effectiveSkillLevel(ItemStack)}（那会读附魔）：作者批 12 给的是
	 * "该弓那条技能的等级"（雷鸣神力 <b>①</b>），一个注册期固定、按弓不同的量。
	 * ⚠ 批 12 之前这里读 {@link BowTier#baseSkillLevel()}（雷鸣 3 ⇒ 4×4 / 60%）；
	 * 星界那条同批同改（星元波置 ①），宝石那条（读技能等级 {@code effectiveSkillLevel}）一个字未动。</p>
	 *
	 * <p><b>耐久</b>：与原版 {@code ProjectileWeaponItem#shoot} 同一笔账 —— 少了这一行，
	 * 这条技能会静默变成"雷鸣弓按着技能键射击不再磨损弓"（白赚耐久）。</p>
	 *
	 * <p>⚠ <b>本方法一个数字都不写</b>（连等级都不是字面量）：范围边长 / 真雷概率 / 射程 /
	 * 垂直容差全部按名取自 {@link BowThunderMightConfigs}，实际发射在
	 * {@code BowThunderMightLauncher#strike}。本方法只回答"这一发该不该换成雷鸣神力"。</p>
	 *
	 * @param pendingShot {@link #consumePendingShot} 读出来并已清掉的值（空串 = 这一发不是专属技能）
	 * @return {@code true} = 这一发已经由雷鸣神力接管（调用方<b>不得</b>再走 {@code super.shoot}）
	 */
	private boolean fireThunderMightInsteadOfArrow(ServerLevel level, LivingEntity shooter, InteractionHand hand,
												   ItemStack weapon, String pendingShot) {
		if (!BowThunderMightConfigs.appliesTo(this.tier)) {
			return false;
		}
		// 与「量波置换」同一个标记读数（唯一一次读取在 shoot 里，这里只比较局部值）。
		if (!BowExclusiveShotItemSkill.THUNDER_MIGHT.matches(pendingShot)) {
			return false;
		}
		// 等级 = 该弓槽 2 那条技能的绑定等级（批 12：雷鸣 = 1 ⇒ 2×2 / 20%；批 12 之前是
		// 档位起始等级 3 ⇒ 4×4 / 60%）。真源 = BowTier#thirdSkillLevel()，链上一个数字都不写。
		BowThunderMightLauncher.strike(level, shooter, this.tier.thirdSkillLevel());
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
	 *   <li><b>技能键闸门</b>（批 7 换判据）：<b>同一个</b>标记读数（{@code pendingShot}，
	 *       由 {@link #consumePendingShot} 在本条闸门之前读一次并清掉）——必须<b>正好等于本技能的
	 *       注册 id</b>。⚠ 批 6 当时读的是 {@code waveShiftKeyHeld}（"任一技能键"），
	 *       那是批 7 修掉的歧义；三条闸门现在读的<b>都是同一个局部值</b>，谁也不再去问弓。</li>
	 * </ol>
	 *
	 * <p><b>等级（批 12 改口径）</b>：{@link BowTier#thirdSkillLevel()}（星界 = <b>1</b>）⇒
	 * 半径 = 等级 + 1 = <b>2</b> 格、滞留 = <b>4 秒</b> = 80 tick（表见
	 * {@link BowAstralBarrageConfigs} 的 Lv1 行；作者批 12 的表把「星元波置」定为 <b>①</b>）。
	 * 刻意<b>不</b>用 {@link #effectiveSkillLevel(ItemStack)}（那会读附魔）：这是注册期按弓固定的量。
	 * ⚠ 批 12 之前读 {@link BowTier#baseSkillLevel()}（星界 3 ⇒ 半径 4 / 120 tick）；
	 * 客户端预选框的半径同批改成读<b>同一个</b> {@code thirdSkillLevel()} ⇒ 画的圈与落的盘仍是同一个数。</p>
	 *
	 * <p><b>耐久</b>：与原版 {@code ProjectileWeaponItem#shoot} 同一笔账 —— 少了这一行，
	 * 这条技能会静默变成"星界弓按着技能键射击不再磨损弓"（白赚耐久）。</p>
	 *
	 * <p>⚠ <b>本方法一个真源数字都不写</b>（连等级都不是字面量）：圆心前推量 / 半径 / 波级 /
	 * 滞留时长 / 条数 / 节拍 / 高度全部按名取自 {@link BowAstralBarrageConfigs}，
	 * 实际降下在 {@code BowAstralBarrageLauncher#fire}。本方法只回答"这一发该不该换成弹幕"。</p>
	 *
	 * @param pendingShot {@link #consumePendingShot} 读出来并已清掉的值（空串 = 这一发不是专属技能）
	 * @param drawnTicks  {@link #consumeDrawTicks} 读出来并已清掉的"松手时已拉弓 tick 数"（批 9）：
	 *                    圆心是它的函数，本方法<b>只转交不算</b>（算圆心的只有数值真源一处）
	 * @return {@code true} = 这一发已经由星元波置接管（调用方<b>不得</b>再走 {@code super.shoot}）
	 */
	private boolean fireAstralBarrageInsteadOfArrow(ServerLevel level, LivingEntity shooter, InteractionHand hand,
													ItemStack weapon, String pendingShot, int drawnTicks) {
		if (!BowAstralBarrageConfigs.appliesTo(this.tier)) {
			return false;
		}
		if (!BowExclusiveShotItemSkill.ASTRAL_BARRAGE.matches(pendingShot)) {
			return false;
		}
		// 等级 = 该弓槽 2 那条技能的绑定等级（批 12：星界 = 1 ⇒ 半径 2 / 滞留 80 tick；
		// 批 12 之前是档位起始等级 3 ⇒ 半径 4 / 120 tick）。真源 = BowTier#thirdSkillLevel()。
		// ★ 批 9：连"松手时拉了多久"一起转交 —— 弹幕圆心 = 预选框圆心，两边同一个规则方法算出来。
		BowAstralBarrageLauncher.fire(level, shooter, this.tier.thirdSkillLevel(), drawnTicks);
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
