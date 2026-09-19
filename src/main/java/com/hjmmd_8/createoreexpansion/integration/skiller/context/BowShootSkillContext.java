package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 弓射击技能上下文（新内核版）——弓在松手射击时构造，携带射手与弓。
 *
 * <p>与旧 {@code content/skill/context/BowShootContext} 一一对应（那边是
 * {@code super(null)} 的 {@code UseItemContext<Event>}，因为它没有对应的 NeoForge 事件）。
 * 注意"箭命中"那一段不在这里：命中时由旧的 {@code JadeTopazBowEventHandler} 读箭上的标记、
 * 从旧注册表取技能并调它的 {@code applyTo}，迁移期这条第二段完全不动。</p>
 *
 * @since 1.0.0
 */
public class BowShootSkillContext implements SkillContext {

    private final Player player;
    private final ItemStack bow;

    public BowShootSkillContext(Player player, ItemStack bow) {
        this.player = player;
        this.bow = bow == null ? ItemStack.EMPTY : bow;
    }

    /** 本次射击所用的弓（技能往里写"本次携带什么技能/等级"的标记）。 */
    public ItemStack bow() {
        return bow;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
