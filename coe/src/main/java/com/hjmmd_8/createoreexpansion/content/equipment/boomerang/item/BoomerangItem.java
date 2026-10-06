package com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergyColors;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.leaf.skiller.server.PlayerPressedKeys;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.AbstractBoomerangEntity;

/**
 * <b>回旋镖物品</b>（四把镖共用一个类，数值与实体类型由构造参数给）。
 *
 * <h2>一、两种投掷模式（2026-10-02 批 2：需求 §3.2 / §3.3）</h2>
 * <p>按键时长分界（作者原话"右键时间 &lt; 0.5 秒 = 点按；&gt; 0.5 秒 = 长按"，
 * 0.5 秒 = 10 tick）：</p>
 * <ul>
 *   <li><b>点按</b>（{@code heldTicks < 10}）：直线飞行，消耗与冷却按档取
 *       {@link BoomerangTier#tapCost()} / {@link BoomerangTier#tapCooldownTicks()}；</li>
 *   <li><b>长按</b>（{@code heldTicks >= 10}，<b>含等号</b>）：花瓣曲线飞行，取
 *       {@link BoomerangTier#holdCost()} / {@link BoomerangTier#holdCooldownTicks()}。</li>
 * </ul>
 * <p>机制照抄 {@code JadeTopazBowItem:107-108}（作者裁定 D4）：{@link #use} 里
 * {@code player.startUsingItem(hand)} + 返回 {@code consume}，松手走原版的
 * {@link #releaseUsing}。<b>"轻点"（同一 tick 按下抬起）也走同一条路</b>：那一 tick 里
 * {@code useItemRemaining} 还是 {@code MAX_USE_DURATION} ⇒ {@code heldTicks = 0 < 10} ⇒ 点按照投
 * （不会出现"轻点没反应"）。</p>
 *
 * <h2>二、蓄力封顶 40 tick（2 秒）—— 之后与 2 秒那一刻完全等价（裁定 D5-A）</h2>
 * <p>{@link #chargeTicks} 把按住 tick 钳在 {@link #HOLD_CHARGE_CAP_TICKS} 上。它只喂
 * <b>姿态</b>（客户端握手模型"往里收"的进度）；<b>轨迹、消耗、耐久都不吃蓄力进度</b>
 * （模式消耗是平坦的一个数、花瓣曲线只由模式决定、耐久只在回程结算）⇒ 2 秒之后继续按着
 * <b>零额外影响</b>，只是姿态停住。{@link #onUseTick} 因此是一个<b>刻意的空覆写</b>
 * （占住"按住期间每 tick"这个服务端钩子，并在此写明为什么它无事可做）。</p>
 *
 * <h2>三、判定与扣款都在同一处（松手那一 tick）</h2>
 * <p>{@link #use} <b>不做任何</b>冷却/能量/耐久判定——模式要等松手才知道，按下就判会变成
 * "两份口径"（按下判一次、松手再判一次，迟早漂移）。投掷的全部判定与扣款收在
 * {@link #releaseUsing}：</p>
 * <ol>
 *   <li>冷却中 ⇒ <b>不投掷、不扣耐久</b>（裁定 D13；冷却圈本身在原版 {@code ItemCooldowns} 上
 *       显示，就是给玩家的反馈）；</li>
 *   <li>能量不够 ⇒ {@code ToolEnergy.sendLowEnergy} + 不投掷、<b>不扣耐久</b>（现状口径）；</li>
 *   <li>都过了 ⇒ <b>同一处</b>算总消耗（{@link #throwCost} =
 *       模式消耗 + <b>按住的</b>技能附加费（穿刺 20L / 环绕 15L））→ 扣 → 造实体 →
 *       写"这一发带不带那两个效果" → 记耐久损耗 → 入世界 → 生成环绕波 → 上冷却。</li>
 * </ol>
 * <p>⚠ <b>点按与长按共用同一条 {@code ItemCooldowns} 键</b>（原版按物品计）⇒ 两组冷却
 * <b>互相覆盖</b>：点按之后 5 秒内连长按也投不出去。这是<b>预期行为</b>（一条右键操作路径，
 * 不按模式分键）。</p>
 *
 * <h2>四、投掷流程</h2>
 * <p>能量预检 → 扣能量 → 造实体（记 owner / 镖本身 / 背包槽位 / 模式）→ 点按走
 * {@code shootFromRotation(1.5, 1.0)}（<b>逐字沿用现状</b>）、长按走
 * {@code startPetalFlight(...)} → 入世界 → <b>把手上那一格清空</b> → 上冷却 + 统计。
 * 清空是必要的：镖回来时会走"原槽 → 背包 → 掉地上"三步交还（见
 * {@link AbstractBoomerangEntity}），不先清空就会在手上复制出一把。</p>
 *
 * <h2>五、为什么 {@code getDestroySpeed} 恒 0</h2>
 * <p>⚠ 本节原文的前提是「镖<b>不吃耐久、吃能量</b>」——<b>该前提已被作者 2026-10-02 推翻</b>
 * （批 1 给镖加了真耐久，见下节；依据：开工需求 §3.1 数值表 + §3.8 耐久扣减/回程结算 + §3.9）。
 * 但下面这条<b>结论仍然成立</b>，只是理由换了：挖方块<b>不是</b>靠"手持左键"，而是实体去程/回程命中时
 * 用它自己那一档的 {@code digSpeed} 复刻原版进度——所以手上一律不许有挖掘速度。所以：</p>
 * <ul>
 *   <li>{@link #getDestroySpeed} 恒 {@code 0.0F} ⇒ 原版挖掘进度恒 0 ⇒ <b>拿在手上永远挖不动方块</b>；
 *       真正挖方块只发生在实体回程/去程命中时（{@link AbstractBoomerangEntity} 用它自己那一档的
 *       {@code digSpeed} 去复刻原版进度，那才是唯一入口）。</li>
 *   <li>{@link #isCorrectToolForDrops} 仍然按档位答"是"——它决定
 *       {@code ServerPlayerGameMode#destroyBlock} <b>掉不掉东西</b>（原版口径：需要正确工具的方块，
 *       手上不算正确工具就只掉经验不给掉落物），以及原版进度里的 30/100 因子。没有这一条，
 *       镖挖石头/矿石<b>什么都不会掉</b>。</li>
 * </ul>
 *
 * <p>能量条与能量文案走 {@link EnergyGradientTool}：色标<b>与同档护甲套同一个取色源</b>
 * （{@link #energyGradientStops} → {@link BoomerangTier#armorSet()} →
 * {@code ArmorEnergyColors#stopsOf}，作者 2026-10-03 小修），四把镖因此与四套护甲逐格一致。</p>
 *
 * <h2>六、耐久（作者 2026-10-02 批 1 建面、批 2 接扣减；需求 §3.1 / §3.8）</h2>
 * <p>四把镖各有耐久上限（{@link BoomerangTier#durability()} = 1000 / 2000 / 3500 / 3500），
 * 由构造器交给原版 {@link Item.Properties#durability}（形态照 {@code JadeTopazBowItem:76}），
 * <b>出生即满耐久</b>（该 API 自己会写 {@code DAMAGE = 0}；见原版 {@code Item.java:360-365}）。</p>
 * <ul>
 *   <li><b>不走 {@code hurtAndBreak}</b>：那会把栈当场打成空，做不到需求 §3.8 的
 *       「耐久见底时不立即归零、不当场销毁，等回到玩家身上再批量结算」。
 *       读写接口是 {@link #getDurability} / {@link #setDurability} / {@link #addWear}。</li>
 *   <li><b>投掷的损耗不在这里写</b>：批 2 起投掷把"点按 −2 / 长按 −5"交给实体累计
 *       （{@code AbstractBoomerangEntity#addFlightWear}，数值取
 *       {@link BoomerangTier#throwWear(boolean)}），挖方块与命中生物也各 −1
 *       （{@link BoomerangTier#WEAR_PER_HIT}）。<b>飞行期间一次都不写回</b>，回到玩家怀里时
 *       由 {@code settleWear} 一次结算（够了就扣、不够就爆）。</li>
 *   <li><b>给原版耐久条</b>（<b>作者 2026-10-03 澄清</b>：他问的「回旋镖下面怎么没有能量条」
 *       指的是<b>耐久条</b>，原话「<b>耐久条！说错了</b>」）：{@link #isBarVisible}
 *       <b>不再恒 {@code false}</b>，回落原版判定（{@code super.isBarVisible} =
 *       {@code stack.isDamaged()}）⇒ 物品格下方按原版口径画<b>耐久条</b>
 *       （<b>满耐久不画、损坏了才画</b>）。耐久数字另有一行文字
 *       （{@link #appendHoverText}），两者不冲突。
 *       <p>⚠ 批 1 的旧形状（恒 {@code false}）与它的旧理由「<b>本模组口径是只留能量条</b>」
 *       <b>都已被推翻</b>（留档见 {@link #isBarVisible} 的方法注释，未静默删除）：模组里
 *       <b>根本不存在</b>"物品格下方的能量条"——能量条只画在 tooltip 里
 *       （{@code EnergyTooltipHandler} + {@link #energyGradientStops}）⇒ 那条理由从建立起
 *       就不成立，它当时的唯一效果是<b>连原版耐久条都不显示</b>。</p></li>
 *   <li><b>可附魔，但只认本模组附魔</b>：加了 {@code MAX_DAMAGE} 之后原版
 *       {@code Item#isEnchantable}（{@code 堆叠 1 && 有 MAX_DAMAGE}）自动为真 ⇒ 镖变成可附魔物；
 *       因此覆写 {@link #supportsEnchantment} 只放行本模组命名空间的附魔（原版"耐久/经验修补"
 *       这类耐久类附魔与全部第三方附魔一律被挡）。<b>具体哪几条本模组附魔仍然只由附魔 JSON 的
 *       {@code supported_items}（{@code #createoreexpansion:skill_boostable}）决定</b>——
 *       这里不再登记第二份名单（依据见 {@code AnvilEnchantmentGuard} 的注释：第二份表示必然漂移）。
 *       批 1 已把四把镖加进 {@code #skill_boostable}（需求 §3.7 的前置）。</li>
 * </ul>
 *
 * <h2>七、技能（2026-10-02 批 3 穿刺 / 批 4 环绕；裁定 D11）——⚠ 口径已在同日被作者两次改写</h2>
 * <p>四把镖都绑了 <b>{@code createoreexpansion:pierce}</b>（穿刺）与
 * <b>{@code createoreexpansion:orbit}</b>（环绕），基准等级都取本档
 * {@link BoomerangTier#baseSkillLevel()}（1 / 2 / 3 / 3）。等级的实际读取点是
 * {@link #effectiveSkillLevel(ItemStack, int)}（唯一一处），消费点是
 * {@link #throwCost(ItemStack, Player, boolean, boolean, boolean)}（附加费 20L + 15L）、
 * 实体侧的穿透额度（3L / 5L）与环绕波生成（数量 L、单枚伤害 2L）。</p>
 *
 * <p><b>⛔ 被推翻的旧口径（批 3/批 4 原文：「不需要开关：只要投掷就生效」，需求 §六 推断值 #9）</b>
 * —— 作者 2026-10-02 的第二次裁定把它整个改掉，原话要点：
 * 「<i>穿刺和环绕是技能按键。就是你把回旋镖抛出来的时候，<b>按住技能按键才有这两个技能出现</b>；
 * 如果你<b>不按住技能按键</b>，抛出去是<b>没有任何技能释放</b>的</i>」，并且
 * 「<i>技能的释放，与点按或长按决定回旋镖是直线飞出还是花瓣型飞出，<b>也是独立的</b></i>」。
 * 第三次澄清又强调：它们是「<b>按住技能键时，该次投掷自带的效果</b>」，与装备技能同一种东西，
 * <b>没有任何"释放"动作</b>。⇒ 现在的实现：</p>
 * <ul>
 *   <li><b>不是主动释放</b>：<b>不</b>走 {@code CoeSkillRelease} / 内核释放路径、
 *       <b>不</b>注册主动触发、<b>没有</b>技能冷却；没有投掷就没有任何效果；</li>
 *   <li><b>技能键只是一个开关</b>：{@link #releaseUsing} 在投掷那一刻<b>读一次</b>
 *       "这次带不带"（{@link #skillKeyHeld} → {@code PlayerPressedKeys}），
 *       结果写在实体上（{@code AbstractBoomerangEntity#setCarriedSkills}）；</li>
 *   <li><b>槽位映射按 {@code CoeItems} 的登记顺序</b>：链上是
 *       {@code .addSkills(AllSkills.PIERCE, …)} 然后 {@code .addSkills(AllSkills.ORBIT, …)}
 *       （{@code CoeItems.java:819-820}），而槽位 = <b>该技能在自己 SkillType 列表里的下标</b>
 *       （{@code CoeSkillsComponent#getDataSkills} 的 Javadoc 与
 *       {@code CoeSkillProvider#convert} 的 {@code for (slot = 0; …)} 逐字同源）
 *       ⇒ <b>穿刺 = 槽位 0（键一）/ 环绕 = 槽位 1（键二）</b>。映射由
 *       {@link #skillSlot(ItemStack, DataSkill)} <b>现算</b>（不写字面量 0/1），改登记顺序也不会错位；</li>
 *   <li><b>能量只按按住的技能收</b>：没按 ⇒ 只扣模式那部分（10/9/8/8 或 50/45/40/40）；
 *       按一个 ⇒ 加扣它那一份（20L 或 15L）；两个都按 ⇒ 加扣两份；
 *       <b>判与扣仍在 {@link #throwCost} 同一处</b>（需求 §3.7 陷阱 #7）；</li>
 *   <li><b>与点按/长按完全独立</b>（2×2）：短按+无技能 = 直线无效果；短按+键一 = 直线 + 穿刺额度；
 *       长按+无技能 = 花瓣（自带"无限制破坏/伤害"，见第九节）；长按+键二 = 花瓣 + 环绕波。</li>
 * </ul>
 * <p>⚠ <b>为什么右键投掷不会"顺带释放"这条技能</b>：技能类型是
 * {@code SkillType.USE_SKILL}，而 {@code UseItemHandler} 会在
 * {@code PlayerInteractEvent.RightClickItem} 上把主手物品的 USE 族技能送进内核释放 ——
 * 对镖来说那就是"右键 = 又扣一次能量"。因此那里按<b>类型</b>（不是物品 id）给
 * {@link BoomerangItem} 开了一条豁免（裁定 D11）——<b>这条豁免现在更重要</b>：
 * 它保证了"按住技能键 + 右键投掷"不会被内核当成一次技能释放（那正是作者禁止的"主动释放"）。
 * 详见 {@code UseItemHandler#release} 里的注释。</p>
 *
 * <h2>八、作者 2026-10-02 的第二个报告（释放与轨迹 + 长按姿态）：逐条处置</h2>
 * <ol>
 *   <li><b>「为什么每次都能默认释放环绕的技能」</b> —— 作者随后给出的裁定把它定成了<b>缺陷</b>：
 *       环绕必须<b>按住键二</b>才有（第二次裁定），本类据此把生成处收进
 *       {@code AbstractBoomerangEntity#spawnOrbitWaves} 的"携带环绕技能"第一道门
 *       （见 {@link #releaseUsing} 里那一行）。旧口径「点按与长按都生成、不需要开关」
 *       已作废并留档在本节与第七节。</li>
 *   <li><b>「每次都是长按的花瓣轨迹」</b> —— 按住时长的<b>公式链本身没问题</b>：
 *       {@link #MAX_USE_DURATION}（{@code getUseDuration} 的明确返回值 72000）＋
 *       {@code heldTicks = getUseDuration − timeLeft}（{@link #heldTicks}：与弓的 pull
 *       逐字同源、客户端与服务端<b>同一处同一公式</b>；客户端在 {@link #releaseUsing}
 *       第一行就返回，实际投掷只用服务端那一份）。真因要靠<b>实机读数</b>才能定论 ⇒
 *       本轮加了诊断日志 {@link #DEBUG_HOLD_TICKS}。源码侧查到的<b>两个只会"多算"的窗口</b>
 *       （① 客户端只在"按下/松手"之后的<b>下一个 tick 边界</b>才上报，加上包处理偏移 ⇒
 *       每发多算 0..+3 tick；② {@code Minecraft#handleKeybinds} 在<b>任何界面/遮罩打开时
 *       完全不跑</b>（原版 {@code Minecraft.java:1847}）⇒ 松手那一刻若界面开着，那一整段
 *       会被算成长按）逐条写在 {@link #heldTicks} 的方法注释里。</li>
 *   <li><b>「长按没有像弓箭那样往里收的动画」</b> —— <b>真因 = 缺
 *       {@link #getUseAnimation} 覆写</b>（原版 {@code Item#getUseAnimation} 默认
 *       {@code stack.has(FOOD) ? EAT : NONE}，镖没有 FOOD ⇒ {@code NONE} ⇒
 *       第一人称那个 {@code switch} 只做一次普通手臂变换）。已按裁定改为
 *       {@link UseAnim#BOW}；客户端缩放（{@code BoomerangChargeClient}）照旧叠加。</li>
 *   <li><b>「长按没有降低移动速度」</b> —— <b>与动画无关</b>（这一条把上面的怀疑推翻了）：
 *       原版减速的唯一处是 {@code LocalPlayer#aiStep}（{@code LocalPlayer.java:746-750}），
 *       判据只有 {@code isUsingItem()}，<b>全程不看 {@link #getUseAnimation}</b>。
 *       详见交付报告的真因表。</li>
 * </ol>
 *
 * <h2>九、作者 2026-10-02 第三次裁定：花瓣段的三条新规则（都在实体侧）</h2>
 * <ol>
 *   <li><b>必须飞完一整瓣才允许返回</b>（R 到 0 再回到原点）—— 批 2 起就是"飞完一瓣"优先，
 *       本次<b>未改动</b>该优先级；</li>
 *   <li><b>近程无限制</b>：沿花瓣路径上<b>所有方块都破坏、所有生物都伤害、不吃任何额度</b>
 *       （"长按自带 1 生物 / 1 方块"那个暂定值<b>当场作废</b>，见
 *       {@link BoomerangSkillConfigs} 里的留档）。破坏仍走<b>同一个入口</b>
 *       （临时换主手 + {@code gameMode.destroyBlock} + {@code finally} 还原），
 *       <b>没有</b>第二条旁路；</li>
 *   <li><b>飞远了回到上限</b>：超出本档"能力范围"（{@link BoomerangTier#capabilityRange()}，
 *       <b>暂定值 = 收回距离</b>，待作者确认）之后，遇到<b>超出本档能力</b>的方块
 *       ⇒ 既不破坏也不伤害，直接返回；</li>
 *   <li><b>主人瞬移兜底</b>：主人换维度或一 tick 跳变超过 16 格 ⇒ <b>当场清除实体并把镖交还玩家</b>
 *       （{@code finishFlight(false)}）；"回程超时就地落地"也一并改成"交还玩家"。</li>
 * </ol>
 */
public class BoomerangItem extends Item implements EnergyGradientTool {

	/** 投掷初速（照抄 Quark Pickarang）—— <b>点按专用</b>；长按走花瓣曲线自己的速率剖面。 */
	public static final float THROW_SPEED = 1.5F;
	/** 投掷散布（照抄 Quark Pickarang）—— 同上，点按专用。 */
	public static final float THROW_INACCURACY = 1.0F;

	/**
	 * <b>点按 / 长按的分界</b>（tick）：按住 {@code >= 它} 算长按（<b>含等号</b>）。
	 *
	 * <p>10 tick = 0.5 秒（作者口径"&lt; 0.5 秒 = 点按、&gt; 0.5 秒 = 长按"；边界按"含等号算长按"
	 * 裁定，即恰好 0.5 秒算长按）。</p>
	 */
	public static final int HOLD_THRESHOLD_TICKS = 10;

	/**
	 * <b>蓄力封顶</b>（tick）：40 tick = 2.0 秒（需求 §3.3"到 2 秒的时候停止"）。
	 *
	 * <p>只钳<b>姿态</b>：2 秒之后继续按着对轨迹/消耗/耐久<b>零额外影响</b>（裁定 D5-A）。</p>
	 */
	public static final int HOLD_CHARGE_CAP_TICKS = 40;

	/**
	 * {@link #getUseDuration} 的返回值（原版弓同口径 72000）。
	 *
	 * <p>它只决定"最多能按住多久"：本模组<b>不</b>自动松手（按到封顶之后玩家可以继续按着，
	 * 也可以立刻松手），所以取一个远大于 2 秒的数即可。⚠ 它<b>不是</b>蓄力上限——蓄力上限是
	 * {@link #HOLD_CHARGE_CAP_TICKS}。</p>
	 */
	public static final int MAX_USE_DURATION = 72000;

	/**
	 * <b>按住时长诊断日志开关</b>（作者 2026-10-02 第二个报告 (b)；<b>默认关</b>）。
	 *
	 * <p>为什么要它：「每次都是长按的花瓣轨迹」这件事，<b>静态源码推不出定论</b>——
	 * 公式链（{@code getUseDuration − timeLeft}）与弓逐字同源、且弓在作者手上是正常工作的；
	 * 能查到的只有"只会多算"的两个窗口（见 {@link #heldTicks}）。到底是"作者按了 ≥ 0.5 s"
	 * 还是"多算了 tick"，只有把<b>每一发真实的 heldTicks</b> 打出来才能分辨。</p>
	 *
	 * <p><b>怎么开</b>（两种都行，不用问执行会话）：</p>
	 * <ol>
	 *   <li>加 JVM 参数 {@code -Dcreateoreexpansion.boomerang.debugHolds=true}（启动器里给
	 *       实例加"JVM 参数 / Java 参数"，改完重启游戏）；</li>
	 *   <li>或者把下面这行直接改成 {@code = true;} 再重新编译（排查完改回去）。</li>
	 * </ol>
	 *
	 * <p><b>看什么</b>：日志前缀 {@value #DEBUG_TAG}，每发投掷在 {@link #releaseUsing} 里打一行
	 * {@code heldTicks=… timeLeft=… useDuration=… useAnim=… isUsingItem=… longHold=…}，
	 * 按住期间每 5 tick 由 {@link #onUseTick} 再打一行 {@code onUseTick}。判读：</p>
	 * <ul>
	 *   <li>{@code heldTicks} 与"作者自己数的秒数 ×20"对得上 ⇒ 公式没问题，是按住时长的口径；
	 *       对不上（明显偏大）⇒ 多算窗口命中，看 {@code side=client} 那些行的时间戳；</li>
	 *   <li>{@code useAnim=BOW} 必须出现（{@link #getUseAnimation} 生效的机器证据）；</li>
	 *   <li>{@code isUsingItem=true} 必须出现（否则姿态与减速都不会发生——那是另一条 bug 路径）。</li>
	 * </ul>
	 *
	 * <p>⚠ 关掉时是<b>一条 {@code static final boolean} 读</b>，代价可忽略；但它<b>不是</b>编译期
	 * 常量（读系统属性），所以调用点不会被 {@code javac} 消除——量级仍然是"每发一行 + 每 5 tick
	 * 一行"，不会刷屏。</p>
	 */
	public static final boolean DEBUG_HOLD_TICKS =
		Boolean.getBoolean("createoreexpansion.boomerang.debugHolds");

	/** 诊断日志前缀（**沿用既有出口**：{@code AbstractBoomerangEntity} 的命中循环兜底日志也是这个前缀）。 */
	public static final String DEBUG_TAG = "[回旋镖] ";

	private final BoomerangTier tier;
	private final Supplier<EntityType<? extends AbstractBoomerangEntity>> entityType;

	public BoomerangItem(BoomerangTier tier, Supplier<EntityType<? extends AbstractBoomerangEntity>> entityType,
						 Properties properties) {
		// 耐久上限来自档位表（本档唯一真源），形态照 JadeTopazBowItem 的 properties.durability(...)：
		// 它同时写入 MAX_DAMAGE 与 DAMAGE = 0（原版 Item.java:360-365）⇒ 新造出来的镖满耐久。
		super(properties.durability(tier.durability()));
		this.tier = tier;
		this.entityType = entityType;
	}

	public BoomerangTier tier() {
		return tier;
	}

	/**
	 * <b>铁砧里「用材料修」本把镖</b>（2026-10-06 批 13 新增，作者裁定）。
	 *
	 * <p>作者原话：「雷鸣不是融合 ⇒ 只认雷鸣合金锭」—— 融合套认两种锭、雷鸣套认一种。四档的名单
	 * 唯一真源 = {@link BoomerangTier#repairIngredient()}（本方法<b>一个材料名都不写</b>），与
	 * {@code CoeArmorMaterials} / {@code JadeTopazBowItem#isValidRepairItem} 三处同源。</p>
	 *
	 * <p><b>为什么必须覆写</b>：原版 {@code Item#isValidRepairItem} 直接 {@code return false}
	 * （{@code Item.java}），四把镖在本批之前<b>完全没有</b>「用材料修」这条途径；它们<b>一直能</b>
	 * 用"两把同名镖合耐久"（{@code AnvilMenu} 里是另一个分支，不经本方法）⇒ 本批补的是缺失的那条。</p>
	 *
	 * <p>⚠ 它修的正是本类的<b>原版 DAMAGE 组件</b>（{@link #getDurability} / {@link #setDurability}
	 * 读写的就是 {@code MAX_DAMAGE}/{@code DAMAGE}）⇒ 铁砧那份「用材料修回 1/4 上限」的既有算术
	 * 对本族天然成立，不需要另写一条修复路径。</p>
	 *
	 * <p>回落 {@code super}（形状照原版 {@code ArmorItem#isValidRepairItem}）：本档材料不命中时
	 * 交还父类裁决 —— 今天父类恒 {@code false}，但把回落写出来，"以后父类长出新判据被静默吃掉"
	 * 这件事就不可能发生。</p>
	 */
	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
		return tier.repairIngredient().test(repairCandidate) || super.isValidRepairItem(stack, repairCandidate);
	}

	// ========== 两模式：按下 / 每 tick / 松手（2026-10-02 批 2） ==========

	/**
	 * <b>按下右键</b>：只做一件事——进入"使用中"（模式要等松手才知道）。
	 *
	 * <p>形态照 {@code JadeTopazBowItem:107-108}：{@code player.startUsingItem(hand)} +
	 * {@code InteractionResultHolder.consume(stack)}。两端都调（客户端要靠它进入使用姿态、
	 * 服务端要靠它让 {@code releaseUsing} 有东西可松）。</p>
	 *
	 * <p>⚠ <b>这里刻意不判冷却、不判能量、不扣款、不扣耐久</b>（见类注释第三节）：
	 * 按下就判会让同一件事出现两份口径；投掷的全部判定收在 {@link #releaseUsing}。</p>
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	/**
	 * 最多能按住多久（tick）—— 见 {@link #MAX_USE_DURATION}（<b>不是</b>蓄力上限）。
	 *
	 * <p>不覆写它的话原版默认返回 0（{@code Item.getUseDuration} 只认食物），
	 * {@code startUsingItem} 会立刻把"使用中"结束掉，{@code releaseUsing} 根本不会被调到
	 * ——长按与轻点<b>都会没反应</b>。</p>
	 */
	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return MAX_USE_DURATION;
	}

	/**
	 * <b>进入"正在使用"的原版姿态</b>（作者 2026-10-02 第二个报告：「长按的时候，回旋镖为什么
	 * 没有像弓箭那样往里收的动画」）。
	 *
	 * <p>⚠ <b>为什么必须覆写它</b>：原版 {@code Item#getUseAnimation} 的默认实现是
	 * {@code stack.has(FOOD) ? EAT : NONE}（{@code Item.java:317-319}）——镖没有 FOOD 组件
	 * ⇒ 恒 {@link UseAnim#NONE}；而第一人称"使用中姿态"的<b>唯一分派点</b>是
	 * {@code ItemInHandRenderer:464-527} 的 {@code switch (stack.getUseAnimation())}，
	 * 其中 {@code case NONE} 只做一次普通的 {@code applyItemArmTransform}（{@code :467-469}）
	 * ⇒ <b>按住期间手上与站着不动完全一样</b>。本模组那个 PoseStack 缩放
	 * （{@code BoomerangChargeClient}）只是"再收一点"的加强，它给不出原版那套拉弓位移。</p>
	 *
	 * <p><b>为什么取 {@link UseAnim#BOW}</b>（候选逐一对照）：</p>
	 * <ul>
	 *   <li><b>BOW</b>：拉弓式收拢姿态（{@code ItemInHandRenderer:478-501}：位移 + 三轴旋转 +
	 *       沿 Z 的 {@code 1.0→1.2} 缩放）＋ 第三人称手臂姿态（{@code PlayerRenderer:117}
	 *       取 {@code UseAnim} 决定手臂姿势）。与弓同形，正是作者要的"像弓箭那样往里收"；</li>
	 *   <li>{@code SPEAR}：三叉戟的"举枪前伸"姿态（{@code :502-524}），与"往里收"方向相反；</li>
	 *   <li>{@code CROSSBOW}：弩的托举姿态，语义是"已装填"；</li>
	 *   <li>{@code BLOCK / EAT / DRINK / TRIDENT / BRUSH}：语义不符（EAT/DRINK 还会额外触发
	 *       吃/喝的音效与粒子，见 {@code LivingEntity.java:3240-3255}）。</li>
	 * </ul>
	 *
	 * <p><b>副作用审计（作者关心的"会不会有别的后果"，逐条查过原版源码）</b>——
	 * 全仓 {@code getUseAnimation()} 的读取点<b>只有 5 处，全是表现层</b>：
	 * {@code ItemInHandRenderer:466}、{@code PlayerRenderer:117}、
	 * {@code LivingEntity:3242/3246}（只认 EAT/DRINK）、以及 {@code ItemStack:700} 这个访问器。
	 * 因此：</p>
	 * <ul>
	 *   <li><b>不参与"按住多久"的记账</b>：{@code useItemRemaining} 只由
	 *       {@code LivingEntity#startUsingItem}（{@code :3192-3204}）、
	 *       {@code #updateUsingItem}（{@code :3151-3163}）与 {@code #tick}
	 *       （{@code :2453-2455}）维护，全程不读动画；</li>
	 *   <li><b>不参与"要不要减速"</b>：减速的唯一处 {@code LocalPlayer#aiStep}
	 *       （{@code LocalPlayer.java:746-750}）判据只有 {@code isUsingItem()}；</li>
	 *   <li><b>不改变松手语义</b>：{@code Item#useOnRelease} 只认弩（{@code Item.java:396-398}）
	 *       ⇒ 选 BOW 既不加也不减 {@code releaseUsingItem} 的后续动作
	 *       （{@code LivingEntity.java:3303-3316}）；</li>
	 *   <li><b>不引入"必须蓄力到某个 tick 才算"</b>：那个 {@code switch} 的外层门槛是
	 *       {@code player.isUsingItem() && getUseItemRemainingTicks() > 0}
	 *       （{@code ItemInHandRenderer:464}），没有任何"至少 X tick 才显示"的判定；
	 *       里面的 {@code f12 = f8 / 20F} 只是<b>姿态进度</b>（20 tick 拉满），
	 *       与投掷判定/消耗/耐久全无关系。</li>
	 * </ul>
	 *
	 * <p>⇒ 结论：这条覆写<b>只改表现</b>（第一/第三人称姿态）。"按住时长记账"与"移速减速"
	 * 的真相都在 {@code isUsingItem()} 那条线上，与本方法无关——见交付报告的真因表与
	 * {@link #heldTicks}。</p>
	 */
	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	/**
	 * <b>按住期间每 tick 的服务端回调</b>—— 本类<b>刻意空着</b>（裁定 D5-A 的可执行形态）。
	 *
	 * <p>为什么不做事：模式消耗是平坦的一个数（点按 10/9/8/8 或长按 50/45/40/40，与按住多久无关）、
	 * 轨迹只由模式决定、耐久只在回程结算 ⇒ <b>超过 2 秒之后继续按着对玩法零影响</b>。
	 * 真正吃蓄力进度的只有客户端姿态，它直接读 {@link #chargeTicks(int)}（同一个封顶常量），
	 * 不需要服务端每 tick 写任何状态。</p>
	 *
	 * <p>留这个覆写的意义是<b>把钩子显式占住</b>：批 3/4 若要"按住期间"做点什么（技能预览之类），
	 * 位置就是这里；而"投掷的判定与扣款"永远只有 {@link #releaseUsing} 一处。</p>
	 *
	 * <p><b>2026-10-02 第二个报告起</b>：这里再加<b>一条</b>被 {@link #DEBUG_HOLD_TICKS} 门控的
	 * 诊断日志（每 5 tick 一行）——它只写日志，仍然不判任何玩法状态（下面的负向断言照旧：
	 * 不许出现 {@code ToolEnergy} / {@code canAfford} / {@code addWear} / {@code setDeltaMovement}）。
	 * 日志里带 {@code side=client|server}：两端各跑一遍，正好用来对照"客户端看到的按住时长"
	 * 与"服务端松手时算出的按住时长"是不是同一个数。</p>
	 */
	@Override
	public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
		// 有意为空：见本方法的 Javadoc（封顶之后与 2 秒那一刻完全等价）。
		// 唯一的例外是被门控的诊断日志（不读写任何玩法状态）。
		if (DEBUG_HOLD_TICKS && remainingUseDuration % 5 == 0) {
			logHold("onUseTick", level, living, stack, MAX_USE_DURATION - remainingUseDuration,
				remainingUseDuration);
		}
	}

	/**
	 * <b>松手</b>：点按 / 长按的<b>唯一判定与扣款处</b>（含冷却、能量、耐久记账）。
	 *
	 * <p>{@code timeLeft} 是原版给的"剩余使用时长"，按住 tick 数 =
	 * {@code getUseDuration − timeLeft}（与翠玉弓 {@code JadeTopazBowModelRegistration} 算
	 * {@code pull} 的同一个口径）。<b>同一 tick 按下抬起</b>时 {@code timeLeft} 等于
	 * {@code MAX_USE_DURATION} ⇒ 按住 0 tick ⇒ 点按，照投。</p>
	 */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (level.isClientSide || !(entity instanceof Player player)) {
			return; // 实体由服务端生成（否则两端各造一个）
		}
		int heldTicks = heldTicks(stack, entity, timeLeft);
		boolean longHold = isLongHold(heldTicks);
		// 诊断（默认关；开关与判读法见 DEBUG_HOLD_TICKS）。放在任何 return 之前：
		// 冷却中/能量不够的那一发也要能看见读数，否则"根本没投出去"与"按太久"分不清。
		logHold("releaseUsing", level, entity, stack, heldTicks, timeLeft);

		if (player.getCooldowns().isOnCooldown(this)) {
			// 裁定 D13：冷却中 ⇒ 不投掷、不扣耐久（连能量也不扣）。冷却圈画在原版 Cooldowns 上，
			// 就是给玩家的反馈；这里不再另发一条文案。
			return;
		}
		// ★ 技能携带判定（作者 2026-10-02 第二次 + 第四次裁定）：**投掷那一刻读一次技能键**。
		// 两个维度互相独立：轨迹由点按/长按决定（longHold），效果由技能键决定（下面两个布尔值）。
		// 槽位由 skillSlot(...) 从这条栈自己的技能表现算 ⇒ 与 CoeItems 的登记顺序同源。
		boolean pierceHeld = skillKeyHeld(player, stack, AllSkills.PIERCE);
		boolean orbitHeld = skillKeyHeld(player, stack, AllSkills.ORBIT);
		// ⚠ **键一与键二互斥**（作者第四次裁定原话："技能键 1 和 2 不能同时释放"）：
		// 两个都按住 ⇒ **两个都不生效**（执行会话按"明确、可预期"裁定，不选"先按下的那个"）。
		boolean pierceSkill = pierceHeld && !orbitHeld;
		boolean orbitSkill = orbitHeld && !pierceHeld;
		// 技能日志（默认关；见 DEBUG_HOLD_TICKS）：这一发到底带没带技能、带了哪个、多少钱。
		logSkills(level, player, stack, pierceHeld, orbitHeld, pierceSkill, orbitSkill);
		// 唯一一处"算总消耗"：模式消耗 + **按住的**技能的附加费（没按的技能一分钱不收）。
		int cost = throwCost(stack, player, longHold, pierceSkill, orbitSkill);
		if (!ToolEnergy.canAfford(player, stack, cost)) {
			// ★ 两条提示分开（作者 2026-10-02 第五次裁定）：
			//   · 这一发**没带技能**（普通点按/长按）⇒ 「由于能量不足无法使用回旋镖」；
			//   · 这一发**带了技能**（键一或键二生效）⇒ 「由于能量不足无法释放技能」。
			// 两者都走既有的工具能量提示出口（服务端包 + 本模组自绘图层），不自建第二套。
			ToolEnergy.sendLowEnergy(player, stack,
				pierceSkill || orbitSkill ? ToolEnergy.LOW_ENERGY_SKILL_KEY : ToolEnergy.LOW_ENERGY_USE_KEY);
			return; // 能量不够 ⇒ 不投掷，且**不扣耐久**
		}
		EntityType<? extends AbstractBoomerangEntity> type = entityType.get();
		AbstractBoomerangEntity boomerang = type.create(level);
		if (boomerang == null) {
			return;
		}
		InteractionHand hand = player.getUsedItemHand();
		// 先扣能，再取镖的那一份 ⇒ 镖上带的能量就是"扣过投掷费"的账
		ToolEnergy.consume(player, stack, cost);
		// ★ 投掷后的能量消耗提示（作者第五次裁定第 4 条）：**技能生效才有**，
		// 走与工具/弓完全相同的那个唯一出口（ToolEnergy#sendRemainingEnergyWithMedallion），
		// 并带上本次实际消耗（模式 + 技能）——不许自建第二套提示。
		if (pierceSkill || orbitSkill) {
			ToolEnergy.sendRemainingEnergyWithMedallion(player, stack,
				IMedallion.findBoundMedallion(player, stack), cost);
		}
		boomerang.setOwner(player);
		boomerang.setItemStack(stack);
		// ★ 把"这一发带不带"写在实体上（唯一写入口）：额度与环绕波生成都读它。
		// 记在实体上而不是栈上 —— 一次投掷一个实体，天然"每次投掷各一份"。
		boomerang.setCarriedSkills(pierceSkill, orbitSkill);
		boomerang.setSlot(hand == InteractionHand.MAIN_HAND
			? player.getInventory().selected
			: Inventory.SLOT_OFFHAND);
		Vec3 spawn = new Vec3(player.getX(), player.getEyeY() - 0.1D, player.getZ());
		boomerang.setPos(spawn.x, spawn.y, spawn.z);
		if (longHold) {
			// 长按：花瓣曲线（§3.4）。投掷那一刻的水平朝向就是曲线基准角 ψ
			// （数学角 atan2(朝向.z, 朝向.x) ⇒ 与需求 §3.4.3 的 x = P.x + r·cos(ψ+φ) 逐字同形）。
			Vec3 look = player.getLookAngle();
			boomerang.startPetalFlight(spawn, Math.atan2(look.z, look.x));
		} else {
			// 点按：**逐字沿用现状**（直线 + 1.5 初速 + 1.0 散布；不准改，改动就在这一行）。
			boomerang.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
				THROW_SPEED, THROW_INACCURACY);
		}
		// 耐久：只把本次投掷的损耗**记到实体上**（−2 / −5），飞行期间一次都不写回物品
		// （需求 §3.8：见底不当场归零）；挖方块/命中生物再各追加 −1。
		boomerang.addFlightWear(tier.throwWear(longHold));
		level.addFreshEntity(boomerang);

		// 环绕技能（需求 §3.6）：投掷时按需挂上 L 枚环绕波（L = 有效技能等级）。
		// ★ 口径（作者 2026-10-02 第二次裁定，**推翻批 4 的"点按与长按都生成"**）：
		//   **只有这一次投掷携带了环绕技能才生成** —— 判据在实体侧的第一道门
		//   （AbstractBoomerangEntity#spawnOrbitWaves 的 `if (!orbitSkillCarried) return;`），
		//   而 orbitSkillCarried 就是上面 setCarriedSkills(...) 写进去的"投掷那一刻按住键二没有"。
		//   于是：不按键 ⇒ 一枚都不生成（也一分钱不扣）；按住键二 ⇒ 点按与长按都生成。
		// ---------------------------------------------------------------------------------
		// RULING LANDED: KEY-GATED (author 2026-10-02, question 1a answered in the second
		// clarification). The old wording here was "both modes, no switch / PENDING RULING";
		// that whole口径 is VOID. pierce + orbit are effects THIS throw carries while their
		// key is held at the moment of the throw -- no active release, no kernel path, no
		// skill cooldown. Keys 1 and 2 are mutually exclusive: both held => neither applies.
		// Gate: `boomerang-skill-gating` (section 29q) pins every line of that shape.
		// ---------------------------------------------------------------------------------
		// ⚠ 调用点仍然**在 if 之外**（保持"唯一生成入口 + 唯一门"的形状）：门在实体里，
		//   物品侧不再复制第二份判据。
		// 放在 addFreshEntity 之后：环绕波第一 tick 就要按 UUID 在世界上找到这枚锚点。
		// 出手方向：点按 = 镖刚拿到的速度向量；长按 = 投掷那一刻的准心方向
		// （花瓣段第一 tick 还没有位移，getDeltaMovement() 还是零）。
		// ⚠ 数量/半径/角速度/伤害一律在实体侧从 BoomerangSkillConfigs 读（同一份数值口径），
		// 本行只负责"投出去了，把技能挂上"。
		boomerang.spawnOrbitWaves(stack,
			longHold ? player.getLookAngle() : boomerang.getDeltaMovement());

		// 手上那一格清空：镖在回程交还时才不会凭空多出一把
		player.setItemInHand(hand, ItemStack.EMPTY);
		player.getCooldowns().addCooldown(this, tier.cooldownTicks(longHold));
		player.awardStat(Stats.ITEM_USED.get(this));
	}

	/**
	 * 按住 tick 数 = {@code getUseDuration − 松手时的剩余时长}（钳到 ≥ 0；与弓的 pull 同一算法）。
	 *
	 * <p><b>唯一公式、两端同一处</b>（作者 2026-10-02 第二个报告 (b)）。{@code timeLeft} 是原版交给
	 * {@link #releaseUsing} 的"剩余使用时长"：{@code LivingEntity#releaseUsingItem}
	 * 用 {@code this.getUseItemRemainingTicks()} 调 {@code ItemStack#releaseUsing}
	 * （{@code LivingEntity.java:3303-3309} → {@code ItemStack.java:707-708}），
	 * 服务端那条链的入口是 {@code ServerGamePacketListenerImpl#handlePlayerAction}
	 * 的 {@code RELEASE_USE_ITEM} 分支（{@code ServerGamePacketListenerImpl.java:1095-1097}）。
	 * 客户端在 {@link #releaseUsing} 第一行就 {@code return}（{@code level.isClientSide}）
	 * ⇒ <b>实际投掷只用服务端这一份</b>，不存在"客户端算一套、服务端算一套"。</p>
	 *
	 * <p><b>核查过的、两个只会"多算"的窗口</b>（它们是"短按被判成长按"的全部已知候选；
	 * 真因要靠 {@link #DEBUG_HOLD_TICKS} 的实机读数才能定论）：</p>
	 * <ol>
	 *   <li><b>客户端的上报节拍</b>：{@code Minecraft#handleKeybinds} 每<b>客户端 tick</b>
	 *       才跑一次（原版 {@code Minecraft.java:2016-2018}：{@code if (player.isUsingItem()) {
	 *       if (!keyUse.isDown()) gameMode.releaseUsingItem(player); }}）⇒ 松手发生在两个 tick
	 *       边界之间时，RELEASE 包要等到<b>下一个边界</b>才发；而服务端的 {@code useItemRemaining}
	 *       从<b>它自己处理 USE 包那一 tick</b>开始倒数（{@code LivingEntity#tick} →
	 *       {@code updatingUsingItem} → {@code --useItemRemaining}，
	 *       {@code LivingEntity.java:2453-2455 / 3151-3163}）。两条链路各带 0..1 tick 的量化
	 *       与包处理偏移 ⇒ 本式算出的值相对"真实按住时长"会有 <b>−2..+3 tick</b> 的散布
	 *       （即 0.5 s 的分界实际落在约 0.35–0.5 s 的物理按压上）。</li>
	 *   <li><b>界面打开时完全不跑按键处理</b>：{@code Minecraft.java:1847} 是
	 *       {@code if (this.overlay == null && this.screen == null) { … this.handleKeybinds(); }}——
	 *       背包/聊天/暂停菜单（overlay）开着的期间，RELEASE 包<b>根本不会发</b>；
	 *       关掉界面后才补发 ⇒ 那一整段（可能几十到几百 tick）会被算进按住时长 ⇒
	 *       <b>必然判成长按</b>。这是"偶尔一次异常长大"的唯一已知来源。</li>
	 * </ol>
	 *
	 * <p>⚠ <b>负向要求（关卡 {@code boomerang-use-state} 钉着，别改）</b>：不许改用原版那个受
	 * {@code isUsingItem()} 门控的读取器（源码里的裸名以 {@code getTicksUsingItem} 开头；
	 * 它的定义见 {@code LivingEntity.java:3299-3301}——同一个减式，但多一层 {@code isUsingItem()}
	 * 门槛，标志短暂为假时会把时长读成 0）；也不许自己维护"开始使用的时间戳"。
	 * <b>一个数只能有一个来源，本方法就是那个来源。</b></p>
	 */
	public static int heldTicks(ItemStack stack, LivingEntity entity, int timeLeft) {
		return Math.max(0, stack.getUseDuration(entity) - timeLeft);
	}

	/**
	 * 按住时长的<b>诊断日志</b>（被 {@link #DEBUG_HOLD_TICKS} 门控；默认一个字节都不打）。
	 *
	 * <p>形态与 {@code WaveDiag} 一致：前缀 {@value #DEBUG_TAG} 只有一处字面量，走
	 * {@code CoeCore.LOGGER} 这个既有日志出口。<b>它不读写任何玩法状态</b>——
	 * 参数全是现算的读数（按住时长、剩余时长、{@code getUseDuration}、姿态枚举、
	 * {@code isUsingItem()}、模式判定），因此不可能影响投掷。</p>
	 *
	 * @param where    调用点标记（{@code onUseTick} / {@code releaseUsing}）
	 * @param level    用来标 {@code side=client|server}（两端各跑一遍，正好互相对照）
	 * @param entity   使用者（读 {@code isUsingItem()}）
	 * @param stack    手上那一份（读 {@code getUseDuration} / {@code getUseAnimation}）
	 * @param heldTicks 现算的按住时长
	 * @param timeLeft  现算时用的"剩余使用时长"
	 */
	private static void logHold(String where, Level level, LivingEntity entity, ItemStack stack,
								int heldTicks, int timeLeft) {
		if (!DEBUG_HOLD_TICKS) {
			return;
		}
		CoeCore.LOGGER.info(DEBUG_TAG
				+ "诊断 {} side={} heldTicks={} timeLeft={} useDuration={} useAnim={} isUsingItem={} longHold={}",
			where, level.isClientSide ? "client" : "server", heldTicks, timeLeft,
			stack.getUseDuration(entity), stack.getUseAnimation(), entity.isUsingItem(),
			heldTicks >= HOLD_THRESHOLD_TICKS);
	}

	/**
	 * <b>技能携带的诊断日志</b>（作者 2026-10-02 第二次裁定第 5 条："让作者能看出这次投掷到底
	 * 带没带技能、带了哪个、额度是多少"）。
	 *
	 * <p>与 {@link #logHold} 同一个开关、同一个前缀、同一个出口（{@code CoeCore.LOGGER}）。
	 * 它打印：两个技能键各自的<b>槽位</b>与<b>按住状态</b>、这一发的模式（点按/长按）、
	 * 以及这一发携带的技能会给的额度/数量（{@code 3L/5L} 或 {@code L} 枚）——
	 * 后者是<b>只读的预览</b>（真正的额度在实体侧 {@code ensurePierceQuota} 里算），
	 * 用来让日志一眼能对上"我按了键二，它说带了环绕、要生成 L 枚"。</p>
	 */
	private void logSkills(Level level, Player player, ItemStack stack,
						   boolean pierceHeld, boolean orbitHeld,
						   boolean pierceSkill, boolean orbitSkill) {
		if (!DEBUG_HOLD_TICKS || level.isClientSide) {
			return;
		}
		// 用实例重载取等级 ⇒ 与 throwCost 里那一处**同一个值**（同一份基准 + 同一份附魔读数），
		// 不会出现"收 20L 的钱、日志说 3L 的额度"。
		int skillLevel = effectiveSkillLevel(stack);
		CoeCore.LOGGER.info(DEBUG_TAG
				+ "诊断 技能 side=server pierceSlot={} pierceHeld={} orbitSlot={} orbitHeld={} bothHeld={} effective(pierce={} orbit={}) level={} pierceMobs={} pierceBlocks={} orbitCount={}",
			skillSlot(stack, AllSkills.PIERCE), pierceHeld,
			skillSlot(stack, AllSkills.ORBIT), orbitHeld,
			pierceHeld && orbitHeld,
			pierceSkill, orbitSkill,
			skillLevel,
			pierceSkill ? BoomerangSkillConfigs.pierceMobQuota(skillLevel) : 0,
			pierceSkill ? BoomerangSkillConfigs.pierceBlockQuota(skillLevel) : 0,
			orbitSkill ? BoomerangSkillConfigs.orbitCount(skillLevel) : 0);
	}

	/** 模式判据（<b>唯一一处</b>）：按住 {@code >=} {@link #HOLD_THRESHOLD_TICKS} ⇒ 长按（含等号）。 */
	public static boolean isLongHold(int heldTicks) {
		return heldTicks >= HOLD_THRESHOLD_TICKS;
	}

	/**
	 * 蓄力进度用的 tick（钳在 {@link #HOLD_CHARGE_CAP_TICKS} 上）—— <b>只喂姿态</b>。
	 *
	 * <p>客户端握手模型"往里收"的进度 = {@code chargeTicks(heldTicks) / HOLD_CHARGE_CAP_TICKS}
	 * （见 {@code BoomerangChargeClient}）；轨迹/消耗/耐久都不读它。</p>
	 */
	public static int chargeTicks(int heldTicks) {
		return Math.min(Math.max(0, heldTicks), HOLD_CHARGE_CAP_TICKS);
	}

	/**
	 * <b>本次投掷的总能量消耗（唯一一处）</b>：模式消耗 + <b>本次携带的</b>技能附加费。
	 *
	 * <p>批 2 只有模式消耗那一项（点按 10/9/8/8、长按 50/45/40/40）。批 3 把<b>穿刺</b>的附加费
	 * 加进来（{@code 20×等级}，需求 §3.7），批 4 再把<b>环绕</b>的（{@code 15×等级}）加进同一行。
	 * 这样"判合计"与"扣合计"永远在同一处（需求 §3.7 的 ⚠，也是陷阱清单 #7）：
	 * 全仓 {@code pierceEnergyCost(} 与 {@code orbitEnergyCost(} 各<b>只有这一次调用</b>，
	 * 关卡 {@code boomerang-pierce-skill} / {@code boomerang-orbit-skill} 钉着它。</p>
	 *
	 * <p>⚠ <b>签名在批 3 多了一个 {@code stack}</b>：技能等级要读这条栈上的附魔
	 * （技艺提升 / 技艺回溯），而等级又决定附加费 —— 少一个入参就只能去别处读，
	 * 那正是"同一件事两处口径"的开端。调用点只有 {@link #releaseUsing} 一处。</p>
	 *
	 * <p>⚠ <b>2026-10-02 第二次裁定又多了两个 {@code boolean}</b>：技能不再"只要投掷就生效"，
	 * 而是"<b>投掷那一刻按住技能键才生效</b>"（作者原话要点见类注释第七节）。两个开关与
	 * 模式（{@code longHold}）是<b>互相独立</b>的维度：</p>
	 * <table border="1">
	 *   <caption>2×2：能量账（{@code L} = 有效技能等级）</caption>
	 *   <tr><th>组合</th><th>这次投掷带什么</th><th>扣多少</th></tr>
	 *   <tr><td>点按 + 不按技能键</td><td>直线，无技能</td><td>{@code 点按消耗}</td></tr>
	 *   <tr><td>点按 + 键一</td><td>直线 + 穿刺额度 {@code 3L/5L}</td>
	 *       <td>{@code 点按消耗 + 20L}</td></tr>
	 *   <tr><td>点按 + 键二</td><td>直线 + {@code L} 枚环绕波</td>
	 *       <td>{@code 点按消耗 + 15L}</td></tr>
	 *   <tr><td>点按 + 两个键</td><td>直线 + 穿刺 + 环绕</td>
	 *       <td>{@code 点按消耗 + 20L + 15L}</td></tr>
	 *   <tr><td>长按 + 不按技能键</td><td>花瓣（自带无限制破坏/伤害，<b>不吃额度</b>）</td>
	 *       <td>{@code 长按消耗}</td></tr>
	 *   <tr><td>长按 + 键一</td><td>花瓣 + 穿刺额度（见报告：花瓣段本来就不吃额度）</td>
	 *       <td>{@code 长按消耗 + 20L}</td></tr>
	 *   <tr><td>长按 + 键二</td><td>花瓣 + {@code L} 枚环绕波</td>
	 *       <td>{@code 长按消耗 + 15L}</td></tr>
	 *   <tr><td>长按 + 两个键</td><td>花瓣 + 穿刺 + 环绕</td>
	 *       <td>{@code 长按消耗 + 20L + 15L}</td></tr>
	 * </table>
	 * <p>⇒ <b>没按的技能一分钱不收</b>（"不按住技能键 ⇒ 抛出去是没有任何技能释放的"，
	 * 能量自然也不该扣）。</p>
	 *
	 * @param stack       投掷的那一把镖（读附魔取有效等级）
	 * @param longHold    {@code true} = 长按（花瓣曲线）
	 * @param pierceSkill 投掷那一刻是否按住键一（= 这一发携带穿刺）
	 * @param orbitSkill  投掷那一刻是否按住键二（= 这一发携带环绕）
	 */
	public int throwCost(ItemStack stack, Player player, boolean longHold,
						 boolean pierceSkill, boolean orbitSkill) {
		// 模式消耗（档位表）+ 按住的技能各自的附加费。player 目前不参与计算，
		// 留着是因为"谁投的"将来可能进折扣/状态判定。
		// ⚠ 技能等级**只读一次**（effectiveSkillLevel(stack) 的唯一调用点就在这里），
		// 两个附加费共用它 ⇒ 不会出现"按一个等级收费、按另一个等级给效果"。
		int level = effectiveSkillLevel(stack);
		int cost = tier.throwCost(longHold);
		if (pierceSkill) {
			cost += BoomerangSkillConfigs.pierceEnergyCost(level);
		}
		if (orbitSkill) {
			cost += BoomerangSkillConfigs.orbitEnergyCost(level);
		}
		return cost;
	}

	// ========== 技能键（作者 2026-10-02 第二次裁定：按住才有，且只在投掷那一刻读一次） ==========

	/**
	 * <b>某个技能在本把镖上占用的按键槽位</b>（<b>现算</b>，不写字面量 0/1）。
	 *
	 * <p>口径与 {@code CoeSkillProvider#convert} <b>逐字同源</b>：{@code 槽位 = 该技能在自己
	 * SkillType 里的下标}（原话见 {@code SkillsComponent#getDataSkills} 的 Javadoc：
	 * 「顺序 = 物品绑定技能时的顺序，槽位 0 开始…槽位 0=键一、槽位 1=键二」）。</p>
	 *
	 * <p>本把镖的登记顺序在 {@code CoeItems.java:819-820}：
	 * {@code .addSkills(AllSkills.PIERCE, …)} <b>先</b>、{@code .addSkills(AllSkills.ORBIT, …)} 后
	 * ⇒ 今天 <b>穿刺 = 槽位 0（键一 = 默认左 Shift）/ 环绕 = 槽位 1（键二 = 默认 R）</b>。
	 * 现算而不是写死这两个数字的理由：将来若调整登记顺序（或某把镖少一个技能），
	 * 写死的映射会<b>静默错位</b>（按了键一却给环绕），而现算永远跟着真源走。</p>
	 *
	 * @return 槽位下标（0 起）；该技能不在这条栈上、或读不到技能组件时返回 {@code -1}
	 *         （调用方一律按"没按"处理）
	 */
	public static int skillSlot(ItemStack stack, DataSkill skill) {
		if (skill == null || skill.id == null || stack.isEmpty()) {
			return -1;
		}
		SkillsComponent holder = SkillItemStack.of(stack).getSkillsHolder();
		if (holder == null) {
			return -1;
		}
		List<DataSkill> useSkills = holder.getDataSkills(SkillType.USE_SKILL);
		for (int slot = 0; slot < useSkills.size(); slot++) {
			DataSkill candidate = useSkills.get(slot);
			if (candidate != null && skill.id.equals(candidate.id)) {
				return slot;
			}
		}
		return -1;
	}

	/**
	 * <b>投掷那一刻这个技能键按住没有</b>（作者 2026-10-02 第二次裁定）。
	 *
	 * <p><b>走既有的键位通道，不自己造一套</b>：{@code PlayerPressedKeys.isPressed(player, slot)}
	 * —— 与装备技能读取按住状态的是<b>同一个入口</b>
	 * （{@code ArmorSkillRuntime:181/194} 与 {@code CoeSkillRelease:73} 都用它）；
	 * 它由客户端的按键包写入，是<b>服务端权威</b>的（{@code PlayerPressedKeys.java:173-175}）。
	 * 客户端侧填这份状态的是 {@code CoeSkillClient#isSlotPressed(slot)}
	 * （工具槽 0/1/2 = 物理键一/二/三，与装备模式开关<b>无关</b>）。</p>
	 *
	 * <p>⚠ 这里<b>只读一次</b>（在 {@link #releaseUsing} 里）——投掷之后再按/松技能键不影响
	 * 已经飞出去的那一发（判据被写在实体上）。</p>
	 */
	public static boolean skillKeyHeld(Player player, ItemStack stack, DataSkill skill) {
		if (!(player instanceof ServerPlayer server)) {
			return false; // 只在服务端判定（客户端那份 releaseUsing 已在前一行返回）
		}
		int slot = skillSlot(stack, skill);
		return slot >= 0 && PlayerPressedKeys.isPressed(server, slot);
	}


	// ========== 技能等级（2026-10-02 批 3：穿刺；读取点唯一） ==========

	/**
	 * <b>本把镖的有效技能等级</b>（<b>唯一读取点</b>；物品侧与实体侧共用它）。
	 *
	 * <p>口径 = {@code SkillEnergyCost.effectiveLevel(stack, 基准, MAX_SKILL_LEVEL)}：
	 * {@code min(基准 + 技艺提升 − 技艺回溯, 5)} 且不低于 1（两个附魔 3 级及以上一律按 2 计）。</p>
	 *
	 * <p>谁用它：{@link #throwCost}({@code 20×等级}) 与实体的穿透额度
	 * （{@code AbstractBoomerangEntity#pierceMobQuota/pierceBlockQuota}，{@code 3L/5L}）。
	 * 两边都<b>现读</b>同一份栈 ⇒ 不会出现"收 20L 的钱、给 3(L-1) 的额度"这种漂移。</p>
	 *
	 * <p>⚠ 基准等级来自档位（{@link BoomerangTier#baseSkillLevel()}：1/2/3/3）——
	 * 与护甲的雷鸣=4 <b>不同</b>，这里绝不引用护甲那张表。</p>
	 */
	public static int effectiveSkillLevel(ItemStack stack, int baseLevel) {
		return SkillEnergyCost.effectiveLevel(stack, baseLevel, BoomerangSkillConfigs.MAX_SKILL_LEVEL);
	}

	/** 本实例（按自己的档位取基准等级）的有效技能等级；见 {@link #effectiveSkillLevel(ItemStack, int)}。 */
	public int effectiveSkillLevel(ItemStack stack) {
		return effectiveSkillLevel(stack, tier.baseSkillLevel());
	}

	// ========== 耐久：本档上限 + 读写接口（2026-10-02 批 1） ==========

	/** 这一档的耐久上限（读 {@link BoomerangTier#durability()}；它不是从栈上读的）。 */
	public int getMaxDurability() {
		return tier.durability();
	}

	/**
	 * 剩余耐久 = 耐久上限 − 原版 {@code DAMAGE} 组件（{@code damage} 即"已扣掉的量"）。
	 *
	 * <p>读的是栈上的 {@code MAX_DAMAGE}/{@code DAMAGE}（原版 {@code ItemStack#getMaxDamage} →
	 * {@code Item#getMaxDamage(ItemStack)}），所以它同时覆盖"老存档里没有该组件的旧镖"——
	 * 那些栈按物品自身的组件默认值补齐（见报告 §①，不改写任何存档）。</p>
	 */
	public int getDurability(ItemStack stack) {
		return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
	}

	/**
	 * 把剩余耐久写成 {@code remaining}（钳到 {@code [0, 上限]}），<b>直接写 {@code DAMAGE} 组件</b>。
	 *
	 * <p>⚠ 这是本模组唯一的耐久写入口，<b>刻意不用原版 {@code hurtAndBreak}</b>：后者在
	 * {@code damage >= maxDamage} 时会当场把栈打成空并抛 {@code ITEM_BREAK} 事件，
	 * 做不到需求 §3.8「见底时不立即归零 / 不当场销毁」。</p>
	 */
	public void setDurability(ItemStack stack, int remaining) {
		int max = stack.getMaxDamage();
		stack.setDamageValue(max - Mth.clamp(remaining, 0, max));
	}

	/**
	 * 累加一次磨损（{@code amount} 为本次要扣掉的点数，负数按 0 处理），返回写回后的剩余耐久。
	 *
	 * <p>它<b>只负责"把损耗记到栈上"</b>，不判断"该不该爆"。需求 §3.8 的批量结算口径是：
	 * 飞行期间把损耗<b>累计在实体上</b>（不逐次调用它，否则耐久会被提前写到 0），
	 * 回到玩家身上时再调用一次；若累计损耗 ≥ 剩余耐久，调用方负责让镖<b>爆掉</b>
	 * （销毁物品，而不是留下一个 0 耐久的镖）。调用点在
	 * {@code AbstractBoomerangEntity#settleWear}（唯一一处）。</p>
	 */
	public int addWear(ItemStack stack, int amount) {
		setDurability(stack, getDurability(stack) - Math.max(0, amount));
		return getDurability(stack);
	}

	/**
	 * <b>耐久条照原版画</b>（<b>作者 2026-10-03 澄清</b>：他要的就是这一条）。
	 *
	 * <p>原版默认<b>就是</b> {@code return stack.isDamaged()}（{@code Item#isBarVisible} 的默认
	 * 实现，已在 1.21.1 的 NeoForge 合并源码里逐行核过）。<b>刻意不写行号</b>：本仓旧注释里
	 * 那种 {@code Item.java:160-162} 式引用会随 NeoForge/映射版本漂移 —— 本轮实查的那份是
	 * 21.1.248，那一句落在 185-186 行，与旧引的 160-162 根本不是同一套行号。本方法
	 * <b>回落它</b>：{@code super.isBarVisible(stack)}（{@link BoomerangItem} 直接继承
	 * {@link Item}，故 {@code super} 那一条就是原版默认）⇒ 物品格下方按
	 * {@code damage / maxDamage} 画原版耐久条：<b>满耐久不画、扣过耐久才画</b>（原版语义，
	 * 与 {@code JadeTopazBowItem} 之类普通可损坏物品完全一致）。</p>
	 *
	 * <p><b>⛔ 批 1 的旧实现与旧理由都已作废</b>（留档，别改回去）：那时本方法恒
	 * {@code return false;}，理由写的是「本模组口径是<b>只留能量条</b>」。作者 2026-10-03 澄清
	 * 「<b>耐久条！说错了</b>」—— 他要看的就是镖的<b>耐久</b>条；而那条旧理由本身也站不住：
	 * 本模组<b>没有</b>"物品格下方的能量条"这种东西（能量条只画在 tooltip 里，见
	 * {@link #energyGradientStops} 与 {@code EnergyTooltipHandler}），所以
	 * "关掉耐久条 = 只留能量条"这个交换<b>根本不存在</b>，它当时的唯一效果就是让镖
	 * 连原版耐久条都不显示。</p>
	 *
	 * <p>⚠ 这里<b>不</b>改用 {@code getBarWidth}/{@code getBarColor} 把原版条挪去画能量比例：
	 * 作者要的是<b>耐久</b>条，"把原版条改成能量条"是另一个需求、没有任何依据。
	 * 关卡 {@code boomerang-durability-batch1}（§29j-2）钉着本方法体的形状（必须回落原版判定、
	 * 不得再恒 {@code false}、不得动那两个画条方法）。</p>
	 */
	@Override
	public boolean isBarVisible(ItemStack stack) {
		return super.isBarVisible(stack);
	}

	/**
	 * 耐久读数：<b>只加一行</b>，用原版自己的 {@code item.durability} 翻译键（中英都有，零新增语言键）。
	 *
	 * <p>⚠ 刻意不动 tooltip 布局：技能区 / 能量区 / 绑定行都由 {@code ClientEvents#onItemTooltip}
	 * 在 index 1 起往下插（本行由原版在 {@code getTooltipLines} 里追加，落在末尾）。</p>
	 */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.durability", getDurability(stack), stack.getMaxDamage())
			.withStyle(ChatFormatting.GRAY));
	}

	/**
	 * <b>只有本模组命名空间的附魔能上这把镖</b>（作者 2026-10-02 批 1：加耐久 ⇒ 物品变可附魔）。
	 *
	 * <p>为什么需要它：原版 {@code Item#isEnchantable} 的判据是「堆叠 1 && 有 {@code MAX_DAMAGE}」
	 * （{@code Item.java:292-294}），所以加了耐久之后四把镖自动变成可附魔物；而本模组口径只要
	 * 「技艺提升 / 技艺回溯」这一条线（需求 §3.7），不要原版耐久类附魔（耐久 / 经验修补）——
	 * 它们正是"用不坏"的那两条，会把本轮新加的耐久设计当场抹平。</p>
	 *
	 * <p>判据分两层，<b>不重复登记名单</b>：</p>
	 * <ol>
	 *   <li>本层：附魔必须是<b>本模组命名空间</b>（{@link CoeCore#REGISTRY_NAMESPACE}）的
	 *       ⇒ 原版与第三方附魔一律被挡；</li>
	 *   <li>下一层：交给默认实现（{@code IItemExtension#supportsEnchantment}，判据 = 该附魔 JSON 的
	 *       {@code supported_items}）⇒ <b>"到底哪几条本模组附魔可用"仍然只有一处真源</b>，
	 *       即 {@code #createoreexpansion:skill_boostable}（批 1 已加进四把镖）。
	 *       这正是 {@code AnvilEnchantmentGuard} 注释里记着的教训：别在本类里再抄一份
	 *       "附魔 → 允许物品"的表，否则它会与标签漂移。</li>
	 * </ol>
	 *
	 * <p>覆盖到的入口（各自都直接调它）：铁砧逐条校验（NeoForge 对 {@code AnvilMenu} 的补丁）、
	 * 附魔台候选（{@code EnchantmentHelper#getAvailableEnchantmentResults} 走
	 * {@code isPrimaryItemFor} → 默认实现回头调本方法）、{@code /enchant} 命令、随机战利品附魔。
	 * ⚠ 创造模式铁砧仍然放行任意附魔（NeoForge 在 {@code AnvilMenu} 里对
	 * {@code instabuild} 直接置真，是刻意语义，本类不干预）。</p>
	 */
	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		ResourceKey<Enchantment> key = enchantment.unwrapKey().orElse(null);
		if (key == null || !CoeCore.REGISTRY_NAMESPACE.equals(key.location().getNamespace())) {
			return false;
		}
		return super.supportsEnchantment(stack, enchantment);
	}

	/**
	 * <b>能量条/能量文案的渐变色标 = 同档护甲套那一套</b>（左→右；作者 2026-10-03 小修）。
	 *
	 * <p>色标<b>不再由本类拼</b>：唯一一行就是问护甲那张表
	 * {@link ArmorEnergyColors#stopsOf(com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet)}
	 * （档 → 套的对应关系在 {@link BoomerangTier#armorSet()}，与四套同名一一对应）。
	 * 于是回旋镖与护甲的条<b>同源同款</b>：同一条渲染路径
	 * （{@code BarTooltipRender.energyGradient(int,int,int,List<Color>)}，
	 * 护甲 tooltip 走的就是它），同一条色标表，<b>连色标条数也一致</b>
	 * （翠玉 2 / 宝石 2 / 星界 <b>4</b> / 雷鸣 2）。</p>
	 *
	 * <p><b>两条都已作废的旧形状</b>（留档，别改回去）：</p>
	 * <ol>
	 *   <li>更早的 {@link EnergyGradientTool} 是零方法标记，两个消费方各自写死翠玉之弓那条
	 *       黄绿渐变 ⇒ <b>四把镖共用同一条颜色</b>（2026-10-02 作者报的 bug）；</li>
	 *   <li>随后的修法是本方法返回 {@code List.of(tier.color().light, tier.color().dark)}
	 *       ——四把分开了，但那仍是<b>回旋镖自己拼的两色标渐变</b>（{@code ToolEnergyColorConfig}
	 *       的亮/暗两端），与护甲套经过作者逐套指定的配色（星界四段、宝石蓝→红…）不一样。
	 *       作者 2026-10-03 原话否掉的就是它：「<i>能量条的样式和颜色应该与那个装备一样，
	 *       而不是你自己造出一个新的渐变</i>」。</li>
	 * </ol>
	 *
	 * <p>⇒ 本方法体<b>只许</b>是下面这一行：出现字面色（{@code new Color(0x…)}）、出现
	 * {@code tier.color()} 或 {@code ToolEnergyColorConfig}，都等于又造了第二个色标来源。
	 * 关卡 {@code boomerang-bar-colour}（§29i）钉着这一行的形状、档→套的双射、以及"回旋镖侧
	 * 一个 {@code 0x} 字面色都没有"。</p>
	 */
	@Override
	public List<Color> energyGradientStops(ItemStack stack) {
		return ArmorEnergyColors.stopsOf(tier.armorSet());
	}

	/**
	 * 需求 10：恒 0 —— 镖不是普通工具（拿在手上挖不动任何东西）。
	 *
	 * <p>⚠ 这一条与"镖能挖方块"<b>不冲突</b>：挖掘发生在实体侧，用的是
	 * {@link BoomerangTier#digSpeed()}（那一档自己的数字），不是从物品读的。</p>
	 */
	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return 0.0F;
	}

	/**
	 * 「这把镖算不算该方块的正确工具」——决定 {@code destroyBlock} 掉不掉落物，
	 * 以及原版挖掘进度里的 {@code i = 30/100}。
	 *
	 * <p>判据与原版 {@code Tier#getIncorrectBlocksForDrops} <b>逐字同源</b>：方块得是镐类，
	 * 且不在本档的 {@code INCORRECT_FOR_*_TOOL} 里（{@link BoomerangTier#incorrectBlocks()}，
	 * 与 {@code AllTiers} 用同一批标签）。</p>
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(tier.incorrectBlocks());
	}
}
