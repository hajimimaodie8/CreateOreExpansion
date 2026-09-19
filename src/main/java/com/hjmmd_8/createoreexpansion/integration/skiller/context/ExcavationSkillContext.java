package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 挖掘类技能上下文（新内核版）。
 *
 * <p>字段与旧
 * {@link com.hjmmd_8.createoreexpansion.content.skill.context.DestroyBlockContext}
 * （{@code record DestroyBlockContext(Level level, BlockPos pos, ItemStack tool, LivingEntity entity)}）
 * 一一对应，只是改为实现 {@link SkillContext}，为 Skiller 的释放路径提供 {@link #getPlayer()}。</p>
 *
 * <p>旧触发点是 {@code mixin/ServerPlayerGameModeMixin}（注入 {@code destroyBlock}，
 * 没有对应的 NeoForge 事件），因此本上下文允许 {@code entity} 为 null：
 * 玩家取不到时 {@link #getPlayer()} 返回 null，调用方（{@code SkillBundle.releaseSkills(type, env)}）
 * 仍以 {@code env.getPlayer()} 为准，不会因此中断。</p>
 */
public class ExcavationSkillContext implements SkillContext {

    private final Level level;
    private final BlockPos pos;
    private final ItemStack tool;
    @Nullable
    private final LivingEntity entity;

    public ExcavationSkillContext(Level level, BlockPos pos, ItemStack tool, @Nullable LivingEntity entity) {
        this.level = level;
        this.pos = pos;
        this.tool = tool == null ? ItemStack.EMPTY : tool;
        this.entity = entity;
    }

    public Level level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    public ItemStack tool() {
        return tool;
    }

    @Nullable
    public LivingEntity entity() {
        return entity;
    }

    /** 旧语义：破坏方格的实体即玩家（{@code SkillsComponent.resolvePlayer} 的挖掘分支）。 */
    @Override
    @Nullable
    public Player getPlayer() {
        return entity instanceof Player player ? player : null;
    }
}
