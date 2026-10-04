package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * <b>回旋镖开箱取物</b>（2026-10-03 行为零变化拆分，从 {@code AbstractBoomerangEntity} 逐字搬出）。
 *
 * <p>撞到容器方块时：判据 {@link #containerAt(AbstractBoomerangEntity, BlockPos)} →
 * 按投掷者填战利品表 {@link #unpackLootTables(AbstractBoomerangEntity, BlockPos, Player)} →
 * 逐槽搬空 {@link #lootContainer(AbstractBoomerangEntity, Container)} →
 * 每份物品上船 {@link #carry(AbstractBoomerangEntity, ItemStack)}。
 * "挖不挖容器方块本身"由 {@link #perPlayerLoot(AbstractBoomerangEntity, BlockPos)} 决定，
 * 破坏走 {@link BoomerangMining#mineBlock(AbstractBoomerangEntity, BlockPos)}。</p>
 *
 * <p>全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 *
 * <h2>九、批 7（2026-10-03）：开箱取物 —— 撞到容器方块就把它搬空（作者需求）</h2>
 * <p><b>作者要求</b>：野外探险时，镖应该能"<b>把箱子里面的所有物品都戴到自己身上，多出来的物品
 * 变成掉落物，掉落在自己旁边</b>"（Quark 没有这个功能）。</p>
 *
 * <p><b>触发</b>：{@link #containerAt(BlockPos)} 非空 —— 即去程命中判定
 * （{@link BoomerangImpact#checkImpact()} → {@link BoomerangImpact#onHitBlock(BlockPos)}）撞到的那个方块是容器。
 * <b>点按与长按共用这一条</b>（两种模式的命中判定本来就是同一处，批 3 起就是）。</p>
 *
 * <p><b>容器怎么判（唯一判据处 {@link #containerAt(BlockPos)}，三道闸门）</b>：</p>
 * <ol>
 *   <li><b>机器闸门</b>（{@link #NEVER_TOUCH_NAMESPACES}）：Create 的机器与本模组自己的机器
 *       （充能器等）一律<b>连碰都不碰</b> —— 作者明确要求"不动我们自己的机器"。按<b>注册命名空间</b>判，
 *       绝不 import 可选模组的类（AGENTS.md 红线）；
 *   <br>⚠ 实测口径：Create 全仓<b>只有</b> {@code foundation.blockEntity.ItemHandlerContainer}
 *       一个类实现原版 {@code Container}，而它不是任何一种方块实体；本模组全仓 0 个
 *       ⇒ 下面第 ③ 道闸门<i>今天已经</i>把机器全挡掉了，这道命名空间闸门是<b>冗余的防御</b>；</li>
 *   <li><b>大箱子合并</b>：{@code ChestBlock#getContainer(..., true)} —— 箱子/陷阱箱走原版合并器，
 *       两半<b>一起</b>取（{@code ChestBlockEntity} 只暴露自己那一半）；</li>
 *   <li><b>方块容器</b>：方块实体 {@code instanceof Container}
 *       （木桶 / 潜影盒 / 漏斗 / 发射器 / 投掷器 / 熔炉 / 烟熏炉 / 高炉 / 酿造台 / 合成器 …，
 *       以及别的模组实现了 {@code Container} 的方块）。
 *       <br>⚠ <b>末影箱天然不在内</b>（{@code EnderChestBlockEntity} 只 implements
 *       {@code LidBlockEntity}，本仓从 MC 源码核对过）。</li>
 * </ol>
 *
 * <p><b>物品怎么搬（{@link #lootContainer(Container)} + {@link #carry(ItemStack)}，零新机制）</b>：
 * 逐槽 {@code removeItemNoUpdate} 全取 ⇒ 每份物品生成一个<b>既有</b> {@link ItemEntity} 再
 * {@code startRiding(this)} 上船 —— 与 {@link BoomerangPickup#pickUpItems()} 走的是<b>同一条承载路径</b>
 * （原版乘客链 + {@link BoomerangPickup#canCarry(Entity)}）。<b>不吃穿刺额度</b>（作者默认值，见报告）。</p>
 *
 * <p><b>溢出去哪</b>：掉落物本来就是乘客 ⇒ 回到玩家手里时走的仍是
 * {@link BoomerangTails#finishFlight(boolean)} → {@link BoomerangTails#handPassengersToPlayer(Player)}
 * （{@code stopRiding} + 清拾取延迟 + {@code playerTouch}）。
 * {@code ItemEntity#playerTouch} 只在 {@code inventory.add(...)} 成功时才消失 ⇒
 * <b>装得下进背包、装不下的留在原地 = 玩家旁边</b>（镖是贴到主人身上才交还的），
 * <b>绝不会掉回箱子那儿</b>；镖爆掉时也照样先交给玩家（批 2 的既有语义）。</p>
 *
 * <h2>十、批 7 第二轮（2026-10-03 同日改口径）：主动填表 → 取空 → 连箱子方块一起挖走</h2>
 * <p><b>作者第二轮要求（三条）</b>：① 战利品箱子是"玩家主动打开箱子那一瞬间"才刷出物品的，
 * 所以镖必须<b>自己主动刷新箱子里的物品</b>；② 把箱子的物品<b>连带被挖掘掉掉落的箱子本身</b>
 * 都吸回来；③ 若玩家装了"不同玩家打开箱子时刷新的物品互相独立"的特殊战利品箱子模组，
 * 那就<b>不要把箱子挖掉</b>（只取物）。</p>
 *
 * <p><b>改动一：取物前主动填一次战利品表（以投掷者本人为玩家）</b>
 * —— {@link #unpackLootTables(BlockPos, Player)}（大箱子两半都填，源码依据全在它的注释里）。
 * ⚠ 顺带更正本功能上一轮的一条<b>错误判断</b>：批 7 的注释写"带 LootTable 的容器此刻还是空的、
 * 镖什么都取不到"——<b>不精确</b>。1.21.1 里 {@code RandomizableContainerBlockEntity#removeItemNoUpdate}
 * /{@code getItem}/{@code isEmpty} <b>各自</b>都会先调 {@code unpackLootTable(null)}
 * （{@code mcsrc-all/.../RandomizableContainerBlockEntity.java:49-88}），所以旧代码其实取得到东西；
 * 真正的问题是<b>填表用的是 {@code null} 玩家</b>（没有幸运值、没有 {@code THIS_ENTITY}、
 * 也不触发 {@code GENERATE_LOOT}）⇒ 拿到的<b>不是"他那一份"</b>。本轮的改动因此是
 * "把隐式的、没玩家的填表换成显式的、按投掷者填"，而不是"从取不到变成取得到"。</p>
 *
 * <p><b>改动二：取空之后把容器方块本身也挖掉</b>
 * —— 走<b>既有唯一挖掘入口</b> {@link BoomerangMining#mineBlock(BlockPos)}（临时换主手 + {@code gameMode.destroyBlock}
 * + {@code finally} 还原），掉落的箱子方块由既有吸附（{@link BoomerangPickup#pickUpItems()}，
 * 每个服务端 tick 一次、就在同一 tick 的末尾）带走 ⇒ 与掉落物同一条链。</p>
 * <ul>
 *   <li><b>顺序铁律"先取空、再挖"</b>：反过来的话，{@code ChestBlock#onRemove} →
 *       {@code Containers.dropContentsOnDestroy}（{@code mcsrc-all/net/minecraft/world/Containers.java:51-58}）
 *       会把<b>还在容器里</b>的东西全撒到地上；而且那条路走的是 {@code getItem(...)}，
 *       对还没填过表的容器又会{@code unpackLootTable(null)} —— 等于把战利品<b>按"没有玩家"</b>roll 一遍，
 *       直接推翻改动一。取空的容器再被破坏时 {@code dropContents} 只会拿到 27 个空槽。</li>
 *   <li><b>挖不动就不挖</b>（硬度/挖掘等级没过、权限不允许）：物品<b>已经取走了</b>，方块留在原地 ——
 *       如实报告这个组合行为，不额外补偿。</li>
 *   <li><b>大箱子只挖命中的那一半</b>：{@link BoomerangMining#mineBlock(BlockPos)} 只作用在镖撞到的那个坐标上，
 *       另一半会由原版 {@code updateShape} 变回单箱（内容已被一起取空）⇒ 世界上剩一个空箱子。
 *       要不要"两半都挖掉"需求没写，取最小偏差（见报告）。</li>
 * </ul>
 *
 * <p><b>改动三：每玩家战利品模组的容器 ⇒ 只取不挖</b>
 * —— {@link #PER_PLAYER_LOOT_NAMESPACES}（{@code lootr}）+ 唯一判据 {@link #perPlayerLoot(BlockPos)}；
 * 这类容器照旧取物（投掷者自己那份），但 {@code onHitBlock} 里那道
 * {@code if (!perPlayerLoot(pos)) { destroyed = mineBlock(pos); }} 让它<b>绝不</b>被挖掉。
 * ⚠ <b>语义变更留档</b>：批 7 那张表把 {@code lootr} 与机器并列、含义是"完全不碰"，
 * 现在拆成两张表（{@link #NEVER_TOUCH_NAMESPACES} = 完全不碰的机器 /
 * {@link #PER_PLAYER_LOOT_NAMESPACES} = 只取不挖），旧名 {@code NEVER_LOOT_NAMESPACES} 已删。
 * 判据只读方块注册命名空间（一个字符串，绝不 import 可选模组类）；它的脆弱性与扩展方式
 * 逐条写在 {@link #PER_PLAYER_LOOT_NAMESPACES} 的注释里。</p>
 *
 * <p><b>改动四：同一次命中只记一次耐久</b>
 * —— 取物 −1 与"挖掉一个方块 −1"不再各记一笔（作者裁定"不重复扣"）：{@code mineBlock} 负责
 * "真的挖掉了"那一笔，{@link BoomerangImpact#onHitBlock(BlockPos)} 只在 {@code took && !destroyed}
 * （取到了、却没挖掉：挖不动，或命中每玩家战利品模组）时补记取物那一笔。</p>
 */
final class BoomerangContainerLoot {

	private BoomerangContainerLoot() {
	}

	/**
	 * <b>完全不许碰的方块命名空间</b>（"不抽取、不破坏、什么都不做"；{@link #containerAt(BlockPos)} 的第一道闸门）。
	 *
	 * <ul>
	 *   <li>{@code create} —— Create 的机器方块（作者："不动我们自己的机器"）；</li>
	 *   <li>{@link CoeCore#REGISTRY_NAMESPACE} —— 本模组自己的机器（充能器等，同一句要求）。</li>
	 * </ul>
	 *
	 * <p>⚠ <b>本表 2026-10-03 第二轮改过名与语义</b>：它原来叫 {@code NEVER_LOOT_NAMESPACES}、
	 * 里面<b>混着</b> {@code lootr}，含义是"命名空间 ⇒ 完全不碰"。作者第二轮把
	 * {@code lootr} 这一类单独拎出来（见 {@link #PER_PLAYER_LOOT_NAMESPACES}：取物但<b>不</b>破坏），
	 * 于是这张表只剩"真的一个字都不碰"的机器命名空间 —— 名字随之改成
	 * {@code NEVER_TOUCH_NAMESPACES}（旧名留着会让人以为 {@code lootr} 还在里面）。</p>
	 *
	 * <p>⚠ <b>当下这是冗余的防御</b>：命名空间为 {@code create} / 本模组的方块实体<b>没有一个</b>
	 * 实现原版 {@code Container}（Create 全仓只有 {@code foundation.blockEntity.ItemHandlerContainer}
	 * 一个类实现它、且不是方块实体；本模组 0 个）⇒ 第三道闸门已经挡住它们。留着它是为了让
	 * "不动我们自己的机器"这条要求在<b>将来某个机器真的实现了 Container 时</b>也自动成立。</p>
	 */
	private static final Set<String> NEVER_TOUCH_NAMESPACES =
		Set.of("create", CoeCore.REGISTRY_NAMESPACE);

	/**
	 * <b>"每个玩家各自一份战利品"的模组容器命名空间</b>（作者 2026-10-03 第三轮要求：
	 * 这类容器<b>照旧取物、但绝不挖掉方块</b>）。
	 *
	 * <p>为什么不能共用心智：这类模组的容器按"谁打开"给谁现生成一份自己的战利品
	 * （{@code lootr} 就是），所以它<b>不是</b> {@link #NEVER_TOUCH_NAMESPACES} 那种"完全不碰"
	 * ——作者明确要求"照旧取物（拿到投掷者自己那份）、但别把箱子挖掉"。</p>
	 *
	 * <p><b>判据为什么只有一串命名空间字符串</b>：可选模组的类<b>不许进 {@code content/} 包</b>
	 * （AGENTS.md 红线；{@code lootr} 连可选依赖都不是），所以这里只能按方块的<b>注册命名空间</b>判。
	 * <br>⚠ <b>这条判据天生脆弱，如实记下来</b>：</p>
	 * <ul>
	 *   <li><b>漏判</b>：某个这类模组若用了别的命名空间、或把容器做成别的形态
	 *       （不是方块实体 / 不给原版 {@code Container} 接口 / 方块注册在别人的命名空间下，
	 *       例如整合包用 KubeJS 之类把方块挪到自定义 ns），这里会判不出来 ⇒
	 *       它会走进"普通容器"那一支（被取空<b>并且</b>被挖掉）；</li>
	 *   <li><b>误判</b>：命名空间里任何一个普通方块容器只要实现了 {@code Container}，
	 *       也会被当成"只取不挖"（比破坏它更安全，是刻意选的失败方向）。</li>
	 * </ul>
	 * <p><b>将来怎么扩展（只有这一处要动）</b>：往这个集合里加命名空间；
	 * 若某个模组需要更细的判据（例如同一个命名空间里只有部分方块是"每玩家"），
	 * 就把判据从"命名空间集合"升级成"一个具名的判据方法"（现在的调用点只有
	 * {@link #perPlayerLoot(BlockPos)} 一处，改它不影响别处）。</p>
	 */
	private static final Set<String> PER_PLAYER_LOOT_NAMESPACES =
		Set.of("lootr");

	/**
	 * ★ <b>"这个方块是不是可以开箱取物的容器"的唯一判据处</b>（作者需求 §2；不是就返回 {@code null}）。
	 *
	 * <p>三道闸门，顺序固定（口径与实测见类注释第九节）：</p>
	 * <ol>
	 *   <li>空方块 / {@link #NEVER_TOUCH_NAMESPACES} 里的命名空间 ⇒ 不是；</li>
	 *   <li>箱子 / 陷阱箱（{@code instanceof ChestBlock}）⇒ 交给原版
	 *       {@link ChestBlock#getContainer}
	 *       （{@code override = true}：镖是飞过去的，不理会"箱子上方被挡"这类开盖条件），
	 *       于是<b>大箱子两半一起被清空</b>；</li>
	 *   <li>其余：方块实体 {@code instanceof Container} ⇒ 就是它（木桶 / 潜影盒 / 漏斗 / 发射器 /
	 *       熔炉 / 酿造台 / 合成器 …，以及别的模组实现了 {@code Container} 的方块）。
	 *       <br>末影箱天然落空（{@code EnderChestBlockEntity} 不是 {@code Container}）；
	 *       {@code lootr} 那类"每玩家一份"的容器<b>仍然会被判成容器</b>（要取物），
	 *       只是随后<b>不挖它</b> —— 见 {@link #perPlayerLoot(BlockPos)}。</li>
	 * </ol>
	 *
	 * <p><b>不上锁判定</b>：{@code BaseContainerBlockEntity#canOpen(Player)} 只有锁判定、没有距离，
	 * 但它会<b>给玩家发"容器已上锁"提示音与消息</b>（{@code Container#stillValid} 那条路则是
	 * "玩家离容器 4 格内"，而镖本来就是在远处开箱的，用它等于把整个功能关掉）⇒ 这里两个都不用，
	 * 代价是<b>上锁的容器也会被搬空</b>（原版生存里没有天然上锁的容器，见报告"我定的部分"）。</p>
	 *
	 * @return 该位置的容器（可能是两半合并后的 {@code CompoundContainer}）；不是容器则 {@code null}
	 */
	static Container containerAt(AbstractBoomerangEntity host, BlockPos pos) {
		BlockState state = host.level().getBlockState(pos);
		if (state.isAir()) {
			return null;
		}
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (NEVER_TOUCH_NAMESPACES.contains(id.getNamespace())) {
			return null;
		}
		if (state.getBlock() instanceof ChestBlock chest) {
			return ChestBlock.getContainer(chest, state, host.level(), pos, true);
		}
		BlockEntity blockEntity = host.level().getBlockEntity(pos);
		return blockEntity instanceof Container container ? container : null;
	}

	/**
	 * ★ <b>"这个容器是不是每玩家各自一份战利品的模组容器"</b>（作者 2026-10-03 第三轮；
	 * 唯一消费点 = {@link BoomerangImpact#onHitBlock(BlockPos)} 里"挖不挖"的那一道闸门）。
	 *
	 * <p>判据 = 该方块<b>注册命名空间</b>（读的是方块注册表 id、一个纯字符串）落在
	 * {@link #PER_PLAYER_LOOT_NAMESPACES} 里。这类容器<b>照旧取物</b>（投掷者自己那份），
	 * 但<b>绝不挖掉方块</b>——它的战利品是"每个玩家一份"，把方块拆了等于毁掉别人的那一份。</p>
	 *
	 * <p>⚠ 判据本身为什么脆弱、将来往哪儿扩展：见 {@link #PER_PLAYER_LOOT_NAMESPACES} 的注释。
	 * 本方法<b>只做判定、绝不破坏</b>（关卡 29s 有一条负向断言钉着"这个判据里没有挖方块"）。</p>
	 */
	static boolean perPlayerLoot(AbstractBoomerangEntity host, BlockPos pos) {
		BlockState state = host.level().getBlockState(pos);
		return PER_PLAYER_LOOT_NAMESPACES.contains(
			BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
	}

	/**
	 * ★ <b>主动把战利品表填一次</b>（作者 2026-10-03 第二轮要求；<b>必须早于逐槽取物</b>）。
	 *
	 * <p><b>为什么必须存在这一步</b>（1.21.1 源码依据，逐条可查）：</p>
	 * <ul>
	 *   <li>{@code RandomizableContainerBlockEntity}（箱子 / 木桶 / 潜影盒 / 发射器 … 的父类）
	 *       把"填表"这件事推迟到<b>第一次碰槽位</b>：{@code getItem} / {@code removeItemNoUpdate} /
	 *       {@code isEmpty} 都各自先调一次 {@code this.unpackLootTable(null)}
	 *       （{@code mcsrc-all/net/minecraft/world/level/block/entity/RandomizableContainerBlockEntity.java:49-88}）；</li>
	 *   <li>而原版"玩家打开箱子"那一刻走的是 {@code createMenu(...)} →
	 *       {@code this.unpackLootTable(playerInventory.player)}（同文件 :97-104），
	 *       <b>只有那一条路带得上玩家</b>；</li>
	 *   <li>{@code unpackLootTable(Player)} 里，玩家参数决定两件事：
	 *       {@code withLuck(player.getLuck())} 与 {@code THIS_ENTITY} 这个掉落上下文参数
	 *       （{@code mcsrc-all/net/minecraft/world/RandomizableContainer.java:81-100}），
	 *       另外还会给投掷者触发 {@code CriteriaTriggers.GENERATE_LOOT}
	 *       ——这正是"拿到的是<b>他的</b>那一份战利品"的含义。</li>
	 * </ul>
	 *
	 * <p><b>所以</b>：如果只靠 {@code removeItemNoUpdate} 那条隐式路，填表用的是
	 * {@code unpackLootTable(null)}（没有玩家、没有幸运值、没有"谁拿的"）。本方法在取物<b>之前</b>
	 * 显式按 {@code thrower} 填一次，把 {@code lootTable} 字段清掉
	 * （{@code unpackLootTable} 内部第一步就是 {@code setLootTable(null)}）⇒ 后面的逐槽取物
	 * 不会再填第二次。</p>
	 *
	 * <p><b>大箱子两半都要填</b>：{@link #containerAt(BlockPos)} 拿到的是
	 * {@code ChestBlock#getContainer(...)} 合并出来的 {@code CompoundContainer}，而
	 * {@code CompoundContainer} <b>不暴露</b>它的两半（{@code mcsrc-all/net/minecraft/world/CompoundContainer.java}
	 * 只有 getItem/removeItem 那套转发）⇒ 只能按"另一半在世界里的位置"各自进去填一次
	 * （另一半的方向 = 原版 {@code ChestBlock#getConnectedDirection(BlockState)}，它对 LEFT/RIGHT
	 * 两半各自指向对面），否则"大箱子的另一半"会留着一张没填的表，
	 * 到被挖时才由 {@code dropContents} 用 {@code null} 玩家 roll 出来。</p>
	 *
	 * @param thrower 投掷者本人（{@code getOwner()}；拿不到时是 {@code null}，退化成原版隐式行为）
	 */
	static void unpackLootTables(AbstractBoomerangEntity host, BlockPos pos, Player thrower) {
		unpackLootTableAt(host, pos, thrower);
		BlockState state = host.level().getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			unpackLootTableAt(host, pos.relative(ChestBlock.getConnectedDirection(state)), thrower);
		}
	}

	/** 单个方块位置上的战利品表填充（{@link #unpackLootTables(BlockPos, Player)} 的逐半实现）。 */
	static void unpackLootTableAt(AbstractBoomerangEntity host, BlockPos pos, Player thrower) {
		if (host.level().getBlockEntity(pos) instanceof RandomizableContainer container) {
			container.unpackLootTable(thrower);
		}
	}

	/**
	 * ★ <b>把一个容器搬空</b>（唯一搬运处）：逐槽全取 ⇒ 每份物品上船（{@link #carry(ItemStack)}）。
	 *
	 * <p>只做"扫槽 + 取走 + 标脏一次"；{@code removeItemNoUpdate} 逐槽取、最后
	 * {@code setChanged()} 只标脏一次（{@code removeItem} 会每槽标一次）。
	 * 战利品表的<b>主动填充不在这里</b>——它是调用方在<b>取物之前</b>做的
	 * （{@link #unpackLootTables(BlockPos, Player)}）。</p>
	 *
	 * <p><b>代价</b>：<b>本方法不记耐久</b>（2026-10-03 第二轮改）：容器事件现在可能既取物又挖方块，
	 * 耐久统在 {@link BoomerangImpact#onHitBlock(BlockPos)} 那一处记，<b>同一次命中只记一次</b>
	 * （挖掉了由 {@code mineBlock} 记，没挖掉才由那一处补记）。<b>不吃穿刺额度</b>：
	 * 容器走的仍是 {@code turnAroundIfNotPiercing(..., mayPierceThrough = false)} 那条"挖不动的方块"口径。</p>
	 *
	 * @return {@code true} = 至少取到了物品（调用方据此决定要不要补记耐久）
	 */
	static boolean lootContainer(AbstractBoomerangEntity host, Container container) {
		boolean took = false;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).isEmpty()) {
				continue;
			}
			ItemStack taken = container.removeItemNoUpdate(slot);
			if (taken.isEmpty()) {
				continue;
			}
			carry(host, taken);
			took = true;
		}
		if (took) {
			container.setChanged();
		}
		return took;
	}

	/**
	 * <b>把一份物品挂上本镖</b>（复用既有承载路径的<b>唯一</b>生成点）。
	 *
	 * <p>与 {@link BoomerangPickup#pickUpItems()} 同一条链：既有 {@link ItemEntity} + 原版 {@code startRiding}
	 * 乘客链 + 同一个 {@link AbstractBoomerangEntity#PICKUP_DELAY} 拾取延迟 ⇒ <b>零新机制</b>（不新增实体类型、贴图、模型、
	 * 渲染器，也不给镖加内部库存）。生成点取镖当前的位置：万一上船失败，同一 tick 末尾的
	 * {@link BoomerangPickup#pickUpItems()} 也会把它吸上来（两处用的是同一个 {@code canCarry}）。</p>
	 */
	static void carry(AbstractBoomerangEntity host, ItemStack stack) {
		ItemEntity item = new ItemEntity(host.level(), host.getX(), host.getY(), host.getZ(), stack);
		host.level().addFreshEntity(item);
		item.startRiding(host);
		item.setPickUpDelay(AbstractBoomerangEntity.PICKUP_DELAY);
	}
}
