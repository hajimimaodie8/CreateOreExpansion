package com.hjmmd_8.createoreexpansion.content.skill.context;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class RightClickItemContext extends UseItemContext<PlayerInteractEvent.RightClickItem> {
    public RightClickItemContext(PlayerInteractEvent.RightClickItem rightClickItem) {
        super(rightClickItem);
    }
}
