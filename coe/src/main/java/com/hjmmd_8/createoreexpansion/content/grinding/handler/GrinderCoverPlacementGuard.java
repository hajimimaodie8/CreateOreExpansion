package com.hjmmd_8.createoreexpansion.content.grinding.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * <b>角磨床盖侧放置守卫</b>（COE 批 21）：开盖状态下，禁止往"盖板那一格"放方块，
 * 并给出手持者一条动作栏提示。
 *
 * <h2>这条守卫补的是哪一半</h2>
 * <p>{@link PowerAngleGrinderBlock} 原来只有<b>开盖前</b>的一半保护：那一格有不可替换方块
 * ⇒ 拒绝开盖（{@code isCoverBlocked}）。反向的这一半一直缺着，而缺它的表现是：
 * 开盖后盖板翻起，那一格是<b>纯 AIR</b>（盖是同一个方块的模型部件，方块本身不占邻格、
 * 碰撞形状也不伸出去），于是玩家可以往里面正常放方块——盖合不回去/挡在盖位上。</p>
 *
 * <h2>为什么用 {@link BlockEvent.EntityPlaceEvent}（而不是别的三条路）</h2>
 * <ul>
 *   <li><b>为什么不是"那一格放个占位方块"</b>：会动存档与 blockstate，还要贴图模型；
 *       而且占位方块会反过来影响 {@code isCoverBlocked} 自己的开盖判定（自相矛盾）。</li>
 *   <li><b>为什么不是"机器自己的 useItemOn 判据"</b>：机器的 {@code useItemOn} 只在玩家
 *       点到<b>机器本体那一面</b>时才被调用；玩家点盖那一格的<b>别的相邻方块</b>时机器根本
 *       收不到回调。而且潜行右键机器本体时 {@code useItemOn} 会被整段跳过
 *       （{@code ServerPlayerGameMode} 的 "sneaky + 手非空 ⇒ 直接走物品放置"），
 *       所以那条路必然漏一半。</li>
 *   <li><b>本条事件给的是真实落点</b>：{@code event.getPos()} 就是"即将出现方块的那一格"
 *       （不是从点击面推算出来的），所以"只拦盖那一格"是精确的，不会误伤其它格。</li>
 *   <li><b>只在服务端派发</b>：{@code ItemStack#useOn} 里非客户端才走
 *       {@code CommonHooks.onPlaceItemIntoWorld}，事件就在那里发出 ⇒ 一次放置动作一次事件，
 *       <b>天然不需要同 tick 去重/冷却</b>；动作栏本身也是覆盖式显示。</li>
 *   <li><b>取消的语义</b>：NeoForge 在这条事件上回滚本次放置捕获到的全部方块快照，
 *       并且不扣物品（{@code CommonHooks} 在事件前已把物品数量还原）。</li>
 *   <li><b>多格放置同一条监听覆盖</b>：床 / 门那种一次多格的形态走
 *       {@code EntityMultiPlaceEvent}，它是本事件的子类，所以只要有一格命中盖侧，
 *       整次放置都会被取消。</li>
 * </ul>
 *
 * <h2>拦得住 / 拦不住（如实记录，避免以后误判）</h2>
 * <p><b>拦得住</b>：玩家经"使用物品"放置的<b>所有</b>形态——点任意相邻方块的面、
 * 潜行放置、床/门多格放置，以及其它实体走同一条路（Create 机械手、假玩家、
 * 末影人拿方块放）。非玩家实体同样会被取消，只是不给提示。</p>
 * <p><b>拦不住</b>：绕开"使用物品"这条路的直接写入——活塞把方块推进那一格、
 * {@code /setblock}、结构方块 / 拼图、蓝图加农炮、以及其它模组直接
 * {@code Level#setBlock}；桶倒流体也不算方块放置（NeoForge 对 {@code BucketItem}
 * 不捕获放置快照）。这些形态本条守卫<b>看不见</b>，也不做"放完再拆"的事后破坏
 * （那会连带毁掉玩家自己的方块与容器内容）。</p>
 *
 * <h2>modid 与位置</h2>
 * <p>住在 {@code :coe}（第一层，永远在场）。{@code @EventBusSubscriber} 的 {@code modid}
 * 必须是本类所在 mod 文件的 id（{@link CoeCore#MOD_ID}）——这条错配会<b>静默不注入</b>
 * （无警告、无报错、编译全绿）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class GrinderCoverPlacementGuard {

	private GrinderCoverPlacementGuard() {
	}

	/**
	 * 服务端放置事件：落点若是"已开盖角磨床的盖板那一格" ⇒ 取消本次放置 + 动作栏提示。
	 *
	 * <p>提示只发给 {@link Player}（非玩家实体照样取消，只是没有收件人）；
	 * 提示通道与 {@code createoreexpansion.msg.cannot_open_cover} 完全同形
	 * （{@code displayClientMessage(Component.translatable(key), true)} = 动作栏）。</p>
	 */
	@SubscribeEvent
	public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
		if (!PowerAngleGrinderBlock.isOpenCoverCell(event.getLevel(), event.getPos())) {
			return;
		}
		// 取消放置；服务端会回滚这次放置捕获到的方块快照，且不扣物品
		event.setCanceled(true);
		Entity entity = event.getEntity();
		if (entity instanceof Player player) {
			player.displayClientMessage(
				Component.translatable("createoreexpansion.msg.cannot_place_on_open_cover"), true);
		}
	}
}
