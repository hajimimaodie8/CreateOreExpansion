package com.hjmmd_8.createoreexpansion.content.skill.context;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

public class UseOnBlockContext extends UseItemContext<UseItemOnBlockEvent> {
    public UseOnBlockContext(UseItemOnBlockEvent useItemOnBlockEvent) {
        super(useItemOnBlockEvent);
    }

    @Override
    public Player getPlayer() {
        return event().getPlayer();
    }
}
