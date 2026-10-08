package com.hjmmd_8.createoreexpansion.content.grinding.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/**
 * <b>角磨床盖侧放置提示</b>（COE 批 22）——<b>纯提示，什么都不拦、什么都不写</b>。
 *
 * <h2>为什么还留着提示</h2>
 * <p>批 22 把"盖那一格"改成真的有一个占位方块（{@code GrinderCoverPlaceholderBlock}），
 * 于是放置链在 {@code BlockPlaceContext#canPlace()} 处就返回 FAIL——这是原版标准的"那一格
 * 有方块，放不进去"，玩家看到的只是"没放上去"。但那个占位方块是<b>看不见</b>的，
 * "明明什么都没有却放不进去"确实需要一句解释，所以保留动作栏提示（作者没说要删，
 * 而批 21 加的那条语言键按"不许删键"的口径继续留着）。</p>
 *
 * <h2>为什么触发点从 {@code BlockEvent.EntityPlaceEvent} 换成 {@link UseItemOnBlockEvent}</h2>
 * <p><b>批 21 那条监听现在已经是死代码</b>（这不是估计，是源码事实）：
 * {@code ItemStack#useOn} 在服务端直接 {@code return CommonHooks.onPlaceItemIntoWorld(context)}；
 * {@code CommonHooks#onPlaceItemIntoWorld} 先跑 {@code itemstack.getItem().useOn(context)}，
 * <b>只有返回值 {@code consumesAction()} 为真时才发 {@code BlockEvent.EntityPlaceEvent}</b>。
 * 占位方块一到位，{@code BlockItem#place} 的第二句 {@code !context.canPlace()} 就为真并
 * 返回 {@code InteractionResult.FAIL} ⇒ {@code consumesAction()} 为 false ⇒ 事件<b>根本不发</b>。
 * 也就是说批 21 的取消守卫对本批要防的那件事已经完全看不见了。</p>
 *
 * <p>换成 {@link UseItemOnBlockEvent} 的 {@code ITEM_AFTER_BLOCK} 阶段：
 * {@code ItemStack#useOn} 的第一句就发这个事件，而
 * {@code ServerPlayerGameMode#useItemOn} 只有在<b>方块自己的 useItemOn / useWithoutItem
 * 都没有吃掉这次点击</b>之后才会走到 {@code stack.useOn(...)} ⇒ 事件发生时正是
 * "马上要把这个物品放到世界上"的那一刻。</p>
 *
 * <h2>落点怎么算（与原版逐字同一套算法）</h2>
 * <p>不自己拼方向：直接构造 {@link BlockPlaceContext}，读它的 {@code getClickedPos()}
 * ——那就是原版 {@code BlockItem#place} 接下来真正会用的落点（{@code replaceClicked} 为真时
 * 是点击格本身，否则是点击格沿点击面外推的那一格）。自己重写这套推演迟早会与原版分家。</p>
 *
 * <h2>为什么这样做不会有假提示</h2>
 * <p>只要落点是"某台已开盖角磨床的盖板那一格"，那一格就被占位方块占着
 * （{@code replaceable == false}）⇒ 这次放置<b>必然</b>失败。所以这条提示在它触发的每一种
 * 情况下都是真话。反过来，那些"点击被别的东西吃掉"的情况（例如空手/持物右键角磨床本体、
 * 右键箱子）根本走不到本事件，也就不会误报——这正是批 21 的
 * {@code RightClickBlock} 类方案做不到的（它发在"谁来处理这次点击"决定<b>之前</b>）。</p>
 *
 * <h2>modid 与位置</h2>
 * <p>住在 {@code :coe}（第一层，永远在场）。{@code @EventBusSubscriber} 的 {@code modid}
 * 必须是本类所在 mod 文件的 id（{@link CoeCore#MOD_ID}）——这条错配会<b>静默不注入</b>
 * （无警告、无报错、编译全绿）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class GrinderCoverPlacementHint {

	private GrinderCoverPlacementHint() {
	}

	/**
	 * 服务端、{@code ITEM_AFTER_BLOCK} 阶段：即将把一个 {@link BlockItem} 放到
	 * "已开盖角磨床的盖板那一格"时，给持有者一条动作栏提示。
	 *
	 * <p>不取消事件、不写世界、不改返回值——放置该失败还是失败（它本来就失败）。</p>
	 */
	@SubscribeEvent
	public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
		if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
			// 只认"物品即将落进世界"这一阶段：BLOCK 阶段是方块自己的交互，ITEM_BEFORE_BLOCK 更早。
			return;
		}
		Level level = event.getLevel();
		if (level.isClientSide) {
			// 事件两侧都发；世界状态与提示都只由服务端负责（与既有 cannot_open_cover 同一条通道）。
			return;
		}
		if (!(event.getItemStack().getItem() instanceof BlockItem)) {
			// 提示文案说的是"无法放置方块"；桶之类不算（它们也放不进被占的格子，但提示会对不上）。
			return;
		}
		BlockPos landing = new BlockPlaceContext(event.getUseOnContext()).getClickedPos();
		if (!PowerAngleGrinderBlock.isOpenCoverCell(level, landing)) {
			return;
		}
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}
		player.displayClientMessage(
			Component.translatable("createoreexpansion.msg.cannot_place_on_open_cover"), true);
	}
}
