package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * <b>「系列特性」的唯一登记入口与运行时判定</b>（星辉石系列 / 雷鸣合金系列）。
 *
 * <p><b>系列特性是什么</b>（行为实现在 {@code MedallionEffectHandler}；本类只管"谁属于哪个系列"）：</p>
 * <ul>
 *   <li><b>星辉石系列</b>：掉落物不落虚空（无重力悬浮）、岩浆/嬗化液中不销毁、在岩浆或嬗化液里发光；
 *       嬗化液里正常沉底不悬浮（照搬 Create 暗影钢 / NoGravMagicalDohicky 的做法）；佩与工具免疫嬗乱。</li>
 *   <li><b>雷鸣合金系列</b>：岩浆中不销毁不燃烧且发光；被雷击豁免并把工具能量充满（含手持）。</li>
 * </ul>
 *
 * <p><b>为什么要有本类（用户 2026-09-15 要求"抽成一个特定函数，在物品后面加"）</b>：原本是
 * <b>两个机制各管一半</b>——运行时按"注册名里含不含 stellarstone / thunderite"猜，而
 * {@code stellarstone_items} / {@code thunderite_items} 两个标签由<b>手写数据文件</b>维护、
 * 里面<b>只列了"佩 + 5 件工具"</b>，材料（锭/粒/板/杆/线/碎块…）全靠命名约定兜底。
 * 后果：① 系列归属散在两处、加一件材料要记得改两处（容易漏）；② 整合包只能改手写文件，
 * 而"哪些是本模组自己的系列物品"在代码里读不出来。现在统一到<b>注册链登记</b>一处，
 * 标签由 datagen 生成（手写的那两份已删除）。</p>
 *
 * <p><b>现在怎么用</b>（Java 没有扩展方法，所以按"能拿到什么 builder"分两种写法，语义完全相同）：</p>
 * <ul>
 *   <li><b>方块</b>：{@code .transform(SeriesTraits.addStellarstoneTraits())} —— 真·链式命名调用
 *       （{@code BlockBuilder} 支持 {@code transform}），例如星辉石机壳那一行；</li>
 *   <li><b>物品</b>：链上写 {@code .tag(AllModItemTags.STELLARSTONE_ITEMS)}（{@code ItemBuilder}
 *       没有 {@code transform}，也无法从外部给它加方法）；需要"函数形态"时用
 *       {@link #addStellarstoneTraits(ItemBuilder)}（包裹式：{@code addStellarstoneTraits(builder).register()}）。</li>
 * </ul>
 *
 * <p><b>登记到哪儿</b>：物品写进<b>系列物品标签</b>、方块写进<b>系列方块标签</b>（后者供"方块物品"用：
 * 机壳这类方块的物品由 {@code BuilderTransformers.casing} 注册，拿不到 {@code ItemBuilder} 去挂物品标签）。
 * 运行时的 {@link #isStellarstone(ItemStack)} / {@link #isThunderite(ItemStack)} 三支都认：
 * <b>物品标签 ∪ 方块标签（BlockItem 查它的方块）∪ 注册名约定</b>。命名约定那支只为"漏登记的老条目"
 * 兜底——保证这次改造<b>不改变任何既有行为</b>。</p>
 */
public final class SeriesTraits {

	/** 星辉石系列的<b>方块</b>标签（方块物品的归属走它；见类注释）。 */
	public static final TagKey<Block> STELLARSTONE_BLOCKS = TagKey.create(Registries.BLOCK,
		ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "stellarstone_blocks"));

	/** 雷鸣合金系列的<b>方块</b>标签。 */
	public static final TagKey<Block> THUNDERITE_BLOCKS = TagKey.create(Registries.BLOCK,
		ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "thunderite_blocks"));

	/** 星辉石系列的注册名特征（命名约定兜底用）。 */
	private static final String STELLARSTONE_TOKEN = "stellarstone";

	/** 雷鸣合金系列的注册名特征（命名约定兜底用）。 */
	private static final String THUNDERITE_TOKEN = "thunderite";

	private SeriesTraits() {}

	// ================= 登记（注册链上用） =================

	/**
	 * <b>给方块登记星辉石系列特性</b>：用法 {@code .transform(SeriesTraits.addStellarstoneTraits())}。
	 *
	 * @param <B> 方块类型
	 * @param <P> builder 父类型
	 * @return 一元算子（把该方块写进 {@link #STELLARSTONE_BLOCKS}），可直接交给 {@code BlockBuilder#transform}
	 */
	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> addStellarstoneTraits() {
		return builder -> builder.tag(STELLARSTONE_BLOCKS);
	}

	/**
	 * <b>给方块登记雷鸣合金系列特性</b>：用法 {@code .transform(SeriesTraits.addThunderiteTraits())}。
	 *
	 * @param <B> 方块类型
	 * @param <P> builder 父类型
	 * @return 一元算子（把该方块写进 {@link #THUNDERITE_BLOCKS}）
	 */
	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> addThunderiteTraits() {
		return builder -> builder.tag(THUNDERITE_BLOCKS);
	}

	/**
	 * <b>给物品登记星辉石系列特性</b>（包裹式；链上等价的写法是 {@code .tag(AllModItemTags.STELLARSTONE_ITEMS)}）。
	 *
	 * <pre>{@code
	 * public static final ItemEntry<Item> STELLARSTONE_INGOT = addStellarstoneTraits(
	 *         CreateOreExpansion.REGISTRATE.item("stellarstone_ingot", Item::new)
	 *             .properties(...)
	 *             .model(...))
	 *     .register();
	 * }</pre>
	 *
	 * @param builder 物品注册 builder
	 * @param <T>     物品类型
	 * @param <P>     builder 父类型
	 * @return 同一个 builder（可继续链式调用）
	 */
	public static <T extends Item, P> ItemBuilder<T, P> addStellarstoneTraits(ItemBuilder<T, P> builder) {
		return builder.tag(AllModItemTags.STELLARSTONE_ITEMS);
	}

	/** <b>给物品登记雷鸣合金系列特性</b>（包裹式；链上等价写法 {@code .tag(AllModItemTags.THUNDERITE_ITEMS)}）。 */
	public static <T extends Item, P> ItemBuilder<T, P> addThunderiteTraits(ItemBuilder<T, P> builder) {
		return builder.tag(AllModItemTags.THUNDERITE_ITEMS);
	}

	// ================= 判定（运行时用） =================

	/** 该物品是否属于星辉石系列（物品标签 ∪ 方块标签 ∪ 注册名约定）。 */
	public static boolean isStellarstone(ItemStack stack) {
		return inSeries(stack, AllModItemTags.STELLARSTONE_ITEMS, STELLARSTONE_BLOCKS, STELLARSTONE_TOKEN);
	}

	/** 该物品是否属于雷鸣合金系列（物品标签 ∪ 方块标签 ∪ 注册名约定）。 */
	public static boolean isThunderite(ItemStack stack) {
		return inSeries(stack, AllModItemTags.THUNDERITE_ITEMS, THUNDERITE_BLOCKS, THUNDERITE_TOKEN);
	}

	/**
	 * 系列归属判定，三支按"越显式越优先"的顺序：物品标签 → 方块标签（方块物品）→ 注册名约定。
	 *
	 * <p>名称约定那一支限定在<b>本模组命名空间</b>内，避免误伤别的模组里恰好含这些词的物品。</p>
	 */
	private static boolean inSeries(ItemStack stack, TagKey<Item> itemTag, TagKey<Block> blockTag, String nameToken) {
		if (stack == null || stack.isEmpty())
			return false;
		if (stack.is(itemTag))
			return true;
		if (stack.getItem() instanceof BlockItem blockItem
			&& blockItem.getBlock()
				.defaultBlockState()
				.is(blockTag))
			return true;
		ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key != null && CreateOreExpansion.MOD_ID.equals(key.getNamespace())
			&& key.getPath()
				.contains(nameToken);
	}
}
