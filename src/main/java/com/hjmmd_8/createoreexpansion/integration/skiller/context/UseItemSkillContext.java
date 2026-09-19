package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用类技能上下文（新内核版）——右键触发场景。
 *
 * <p>旧框架把右键分成两个上下文（{@code UseOnBlockContext} / {@code RightClickItemContext}），
 * 都实现同一个 {@code UseItemContext<?>}。新内核的上下文由技能自己的
 * {@link com.leaf.skiller.foundation.skill.config.SkillContextFactory} 创建，一个技能只认一种，
 * 所以这里合并成一个上下文、把两种事件都带上，由技能自己判断该处理哪一种
 * （例如 {@code hoe} 只处理"右键方块"这一种，旧实现就是
 * {@code if (!(context instanceof UseOnBlockContext blockContext)) return;}）。</p>
 *
 * @since 1.0.0
 */
public class UseItemSkillContext implements SkillContext {

    @Nullable
    private final UseItemOnBlockEvent useOnBlockEvent;
    @Nullable
    private final PlayerInteractEvent.RightClickItem rightClickItemEvent;
    private final Player player;
    private final ItemStack stack;

    /**
     * 本次释放的临时数据：{@code consumeResource} 里算好的结果（例如解析出来的优先级）
     * 供随后的 {@code release} 复用。新内核会为每个技能实例各建一个上下文对象，
     * 所以它天然是"本次释放"的作用域。
     */
    private final Map<String, Object> scratch = new HashMap<>();

    public UseItemSkillContext(UseItemOnBlockEvent event) {
        this.useOnBlockEvent = event;
        this.rightClickItemEvent = null;
        this.player = event.getPlayer();
        this.stack = event.getItemStack();
    }

    public UseItemSkillContext(PlayerInteractEvent.RightClickItem event) {
        this.useOnBlockEvent = null;
        this.rightClickItemEvent = event;
        this.player = event.getEntity();
        this.stack = event.getItemStack();
    }

    /** 右键方块事件；不是这条路径时为 null。 */
    @Nullable
    public UseItemOnBlockEvent useOnBlockEvent() {
        return useOnBlockEvent;
    }

    /** 右键空气/物品事件；不是这条路径时为 null。 */
    @Nullable
    public PlayerInteractEvent.RightClickItem rightClickItemEvent() {
        return rightClickItemEvent;
    }

    /** 触发这次技能的主手物品。 */
    public ItemStack itemStack() {
        return stack;
    }

    /** 写入本次释放的临时数据。 */
    public void putScratch(String key, Object value) {
        scratch.put(key, value);
    }

    /** 读取本次释放的临时数据；没有或类型不符时返回 null。 */
    @Nullable
    public <T> T getScratch(String key, Class<T> type) {
        Object value = scratch.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
