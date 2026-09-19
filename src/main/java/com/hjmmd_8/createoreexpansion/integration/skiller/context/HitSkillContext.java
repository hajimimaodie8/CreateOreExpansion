package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 受击类技能上下文（新内核版）——持有触发这次技能的那次伤害事件。
 *
 * <p>字段与旧 {@code content/skill/context/LivingHurtContext} 一一对应
 * （旧的是 {@code record LivingHurtContext(LivingIncomingDamageEvent event)}），
 * 区别只是改为实现 {@link SkillContext}，并为 Skiller 的释放路径提供 {@link #getPlayer()}。</p>
 *
 * <p>把整棵事件留在上下文里（而不是只留目标与玩家），是为了让技能实现仍能读/改
 * 事件上的其它信息（例如是否取消、伤害值），与旧实现的能力一致。</p>
 *
 * @since 1.0.0
 */
public class HitSkillContext implements SkillContext {

    private final LivingIncomingDamageEvent event;

    /**
     * 本次释放的临时数据：{@code consumeResource} 里算好的结果（例如随机判定出来的掉落数量）
     * 供随后的 {@code release} 复用，避免"随机数滚两次"导致扣能与效果不一致。
     *
     * <p>放这里安全：新内核的 {@code SkillBundle.releaseSkills} 会为<b>每个技能实例各建一个</b>
     * 上下文对象（同一次释放内一一对应），所以它天然是"本次释放"的作用域。</p>
     */
    private final Map<String, Object> scratch = new HashMap<>();

    public HitSkillContext(LivingIncomingDamageEvent event) {
        this.event = event;
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

    /** 原始伤害事件（技能实现可能需要读写它）。 */
    public LivingIncomingDamageEvent event() {
        return event;
    }

    /** 被打的目标（旧 {@code HitSkillContext#target()}）。 */
    public LivingEntity target() {
        return event.getEntity();
    }

    /** 造成这次伤害的玩家；不是玩家造成时返回 null（与旧实现一致）。 */
    @Override
    @Nullable
    public Player getPlayer() {
        return event.getSource().getDirectEntity() instanceof Player player ? player : null;
    }
}
