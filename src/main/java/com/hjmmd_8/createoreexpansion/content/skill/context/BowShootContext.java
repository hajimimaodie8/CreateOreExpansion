package com.hjmmd_8.createoreexpansion.content.skill.context;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * 弓射击技能上下文 —— 弓在松手射击（releaseUsing）时构造，
 * 携带射手与弓物品，供 USE_SKILL 类型的弓技能释放使用。
 */
public class BowShootContext extends UseItemContext<Event> {

	private final Player player;
	private final ItemStack bow;

	public BowShootContext(Player player, ItemStack bow) {
		super(null); // 弓射击无对应 NeoForge 事件
		this.player = player;
		this.bow = bow;
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	public ItemStack bow() {
		return bow;
	}
}
