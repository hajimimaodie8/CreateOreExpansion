package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * <b>变器攻击态能产出的 8 种魔素，以及「展示框里放什么算哪一种」的规则表</b>
 * （2026-10-03 需求 <b>cews-ess</b> §3.1）。
 *
 * <p><b>为什么是枚举 + 每常量体覆写</b>：8 种魔素各带一条"入选规则"，而其中<b>恰好一种</b>
 * （{@link #POISON}）的规则与另外七种<b>形状不同</b>——药水是「同一个物品 + 不同数据组件」，
 * 物品标签装不下（见 {@link #matchesComponent}）。这正是本仓
 * {@link TransmuterMode} 用的那个套路（枚举带行为 = 策略模式）：<b>每条规则自报</b>，
 * 调用方只问一句 {@link #resolve}，<b>不许在别处再写一份 if/else 链</b>。</p>
 *
 * <p><b>声明序 = 优先级（先命中者胜）</b>：8 个标签之间<b>不保证互斥</b>（将来往两个标签里放
 * 同一个物品是允许的），所以必须有一个固定顺序，而"顺序"这件事<b>只有这一处定义</b>——
 * 就是下面的声明序。它与 {@link WaveTrailStyle} 里那 8 个魔素的<b>声明序逐字一致</b>
 * （WATER → FIRE → EARTH → WIND → ICE → LIGHTNING → POISON → ARCANE），
 * 因此全仓只有<b>一种</b>魔素顺序，不必再维护第二张顺序表
 * （关卡 {@code check-transmuter-essence.ps1} 正向钉着这条对应关系）。</p>
 *
 * <p><b>为什么标签 id 声明在本类、而不是 {@code core} 的 {@code AllTags}</b>：本组标签是
 * <b>CEWS 自己的</b>选择器语义（"变器正上方展示框里放什么"），与 {@code AllTags} 里那些
 * 跨层共用的材料/系列标签不是一回事；声明在这里可以让"规则 + 它读的标签"同处一地
 * （唯一真源），也避免让最底层的 {@code core} 反过来知道某一层的玩法。
 * 数据侧是 <b>{@code cews} 模块里的手写 JSON</b>（{@code data/createoreexpansion/tags/item/essence_*.json}），
 * 不走 datagen —— 其中 {@link #ARCANE} 引用的是 {@code transmutation}（另一个 mod）的物品，
 * 而 {@code core} 是最底层、datagen 阶段未必拿得到它的 Item；手写 JSON 还能用
 * {@code "required": false} 的可选条目，使<b>缺那个 mod 时不报错、只是不认</b>。</p>
 *
 * <p><b>幂等与容错</b>：{@link #resolve} 对空栈、认不出的物品一律返回 {@code null}
 * （= "没有魔素"），从不抛异常——调用方据此<b>不点燃</b>（空框同义）。</p>
 */
public enum TransmuterEssence {

	/** 魔素·水：水桶 / 海洋之心 / 鹦鹉螺壳 / 海晶碎片 / 海晶砂粒。 */
	WATER(WaveTrailStyle.WATER, "essence_water"),
	/** 魔素·火：烈焰棒 / 烈焰粉 / 岩浆膏 / 岩浆块 / 岩浆桶。 */
	FIRE(WaveTrailStyle.FIRE, "essence_fire"),
	/** 魔素·地：仙人掌 / 泥土 / 砂土 / 缠根泥土 / 草方块 / 沙子 / 沙砾。 */
	EARTH(WaveTrailStyle.EARTH, "essence_earth"),
	/** 魔素·风：旋风棒 / 风弹。 */
	WIND(WaveTrailStyle.WIND, "essence_wind"),
	/** 魔素·冰：细雪桶 / 雪块 / 冰 / 浮冰 / 蓝冰。 */
	ICE(WaveTrailStyle.ICE, "essence_ice"),
	/** 魔素·雷：避雷针 / 本模组的强化避雷针。 */
	LIGHTNING(WaveTrailStyle.LIGHTNING, "essence_lightning"),
	/**
	 * 魔素·毒：蜘蛛眼 / 发酵蜘蛛眼，<b>外加"含中毒效果的药水"</b>——后者标签装不下，
	 * 由 {@link #matchesComponent} 那一层负责（见该方法的 javadoc）。
	 */
	POISON(WaveTrailStyle.POISON, "essence_poison") {
		@Override
		protected boolean matchesComponent(ItemStack stack) {
			return carriesPoison(stack);
		}
	},
	/**
	 * 魔素·异：{@code transmutation:transmutation_fluid_bucket}（嬗化液桶，属另一个 mod）。
	 * 该条目在 JSON 里是 {@code "required": false} ⇒ <b>没装那个 mod 时不报错，只是不认</b>。
	 */
	ARCANE(WaveTrailStyle.ARCANE, "essence_arcane");

	/**
	 * 本魔素在展示框里的入选标签（命名空间恒 {@link CoeCore#REGISTRY_NAMESPACE}）。
	 *
	 * <p>id 与 JSON 文件名逐字对应（{@code essence_water} → {@code tags/item/essence_water.json}），
	 * 关卡钉着"8 个文件都在、且目录是单数 {@code tags/item/}"。</p>
	 */
	private final TagKey<Item> tag;

	/**
	 * 本规则命中的<b>魔素本体</b>——就是 {@link WaveTrailStyle} 里对应的那个值。
	 *
	 * <p><b>为什么在这里再写一遍枚举名（显式传参），而不是 {@code WaveTrailStyle.valueOf(name())}</b>：
	 * 显式传参让"这条规则产出哪种魔素"在声明处<b>一眼可读</b>，且 {@link WaveTrailStyle} 的常量
	 * 顺序被别人重排时不会静默改指到另一种魔素（按名字查则只靠字符串约定）。
	 * 代价是枚举名写了两遍——那是<b>映射数据</b>，不是被复制的一份逻辑；
	 * 两侧的一一对应由关卡正向断言（名字与顺序都必须一致），漂移照样会变红。</p>
	 */
	private final WaveTrailStyle essence;

	TransmuterEssence(WaveTrailStyle essence, String tagPath) {
		this.tag = TagKey.create(Registries.ITEM, CoeCore.modLoc(tagPath));
		this.essence = essence;
	}

	/** 本魔素的入选标签。 */
	public final TagKey<Item> tag() {
		return tag;
	}

	/** 本规则命中的魔素（{@link WaveTrailStyle} 里的对应值）。 */
	public final WaveTrailStyle essence() {
		return essence;
	}

	/**
	 * <b>本魔素的入选判定</b>：物品标签为主，{@link #matchesComponent} 是"标签装不下的那一层"的
	 * 覆写点。
	 *
	 * <p><b>为什么是 {@code final} 的模板方法</b>：8 条规则共用的"先看标签"这一点<b>只写一次</b>
	 * （{@code stack.is(tag)}），各常量只覆写自己那点差异——否则每个带特例的常量都要把
	 * {@code stack.is(tag)} 抄一遍，抄出来的副本迟早走样。</p>
	 */
	public final boolean matches(ItemStack stack) {
		return stack.is(tag) || matchesComponent(stack);
	}

	/**
	 * <b>标签装不下、只能按数据组件判的那一层</b>（默认：没有这一层）。
	 *
	 * <p><b>为什么 {@link #POISON} 必须单开这一层</b>：1.21 之后"药水"不是若干种物品，而是
	 * <b>同一个物品</b>（{@code minecraft:potion} / {@code splash_potion} / {@code lingering_potion}）
	 * 配不同的 {@link DataComponents#POTION_CONTENTS 药水内容组件} ⇒
	 * <b>"含中毒效果的药水"这个集合无法用物品标签表达</b>。本层就负责读组件：
	 * 见 {@link #carriesPoison}。三种药水形态（普通/喷溅/滞留）都算。</p>
	 *
	 * @param stack 展示框里的物品（非空；调用方已滤空）
	 * @return true = 按组件判定也命中本魔素
	 */
	protected boolean matchesComponent(ItemStack stack) {
		return false;
	}

	/**
	 * <b>「物品 → 魔素」的唯一解析入口</b>：按声明序逐条问，<b>先命中者胜</b>。
	 *
	 * @param stack 展示框里的物品（可为空栈 / {@code null}）
	 * @return 命中的魔素；<b>空栈、以及认不出的物品一律 {@code null}</b>
	 *         （调用方据此"不点燃"，与"空展示框"同义）
	 */
	@Nullable
	public static WaveTrailStyle resolve(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return null;
		for (TransmuterEssence rule : values())
			if (rule.matches(stack))
				return rule.essence;
		return null;
	}

	/**
	 * 这一堆里是否含<b>中毒效果</b>的药水（普通 / 喷溅 / 滞留三种形态都算）。
	 *
	 * <p>判据取 {@link MobEffectInstance#is}（按 {@code Holder<MobEffect>} 比对），
	 * <b>不按物品 id</b>——那正是标签装不下的原因。读不到
	 * {@link DataComponents#POTION_CONTENTS}（不是药水 / 无水袋药水）⇒ {@code false}。</p>
	 */
	private static boolean carriesPoison(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null)
			return false;
		for (MobEffectInstance effect : contents.getAllEffects())
			if (effect.is(MobEffects.POISON))
				return true;
		return false;
	}
}
