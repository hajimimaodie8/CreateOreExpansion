package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.leaf.skiller.content.skill.SkillComponent;
import com.leaf.skiller.foundation.SkillResource;
import com.leaf.skiller.foundation.context.SkillContext;
import com.leaf.skiller.foundation.provider.SkillProviders;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.SkillBundle;
import com.leaf.skiller.foundation.skill.SkillType;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import com.leaf.skiller.foundation.skill.config.SkillContextFactory;
import com.leaf.skiller.server.PlayerPressedKeys;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端技能释放路由：把「当前按下的技能键 + 本次触发场景」交给新内核执行。
 *
 * <h2>为什么不直接用 Skiller 的 {@code SkillReleaser}</h2>
 * <p>{@code SkillReleaser} 从 {@code ServerSkillCache} 取技能组件，而后者<b>只在该玩家
 * 按过一次"启用技能"开关后</b>才有内容（{@code SkillTogglePacket} → {@code onToggle}）。
 * 本模组既有玩法是<b>随时可用</b>（没有总开关），因此这里改为每次触发时即时收集
 * （{@link CoeSkillProvider} 只读主手物品的一个数据组件，开销可忽略）。换来的好处是：
 * 玩家换工具、技能变化都立刻生效，不存在"缓存过期"这一类问题。</p>
 *
 * <h2>为什么不调 {@code SkillBundle#releaseSkills}</h2>
 * <p>新内核的 {@code releaseSkills} 把「累加 → 校验 → 落账」<b>整段</b>包在
 * {@code if (!player.isCreative())} 里——创造模式下技能完全不耗能。这与本模组换核前的
 * 玩法相反（旧 {@code ToolEnergy.tryConsume} 无论创造与否都扣能），而且用户要求这个
 * 行为<b>可由玩家在游戏内开关</b>。{@code SkillBundle} 属内核、<b>不许改</b>，
 * 所以本类把那段编排<b>照抄过来</b>，只在"要不要消耗"这一个判据上换成
 * {@link SkillSettings#consumeInCreative}（详见 {@link #releaseBundle}）。</p>
 *
 * <h2>按键来源</h2>
 * <p>按键状态取自 {@link PlayerPressedKeys}——它由客户端的按键包写入，是
 * <b>服务端权威</b>的。这正好修掉本模组的一个既有缺陷：旧 {@code AllKeys} 是纯客户端
 * 对象，专用服务器上 {@code isPressed()} 恒为 false，技能键永远不触发。</p>
 *
 * @since 1.0.0
 */
public final class CoeSkillRelease {

    private CoeSkillRelease() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 释放该玩家当前按下的按键槽位里、指定类型的所有已迁移技能。
     *
     * @param player 触发者（服务端玩家）
     * @param type   本次触发的技能类型（挖掘 / 受击 / 使用）
     * @param env    上下文环境（事件或 extraData）
     * @return 是否有任一槽位成功释放；没有任何按下/绑定/已迁移技能时返回 false
     */
    public static boolean release(ServerPlayer player, SkillType type, SkillContextEnvironment env) {
        if (player == null || type == null || env == null) {
            return false;
        }
        // 整次释放只读一次开关：同一次触发里的多个槽位必须用同一个判据，
        // 不能在循环里反复查存档（也更省）。
        boolean consumeInCreative = SkillSettings.consumeInCreative(player.serverLevel());
        SkillComponent component = SkillProviders.collectAllSkills(player);
        boolean released = false;
        for (Map.Entry<Integer, SkillBundle> binding : component.bindings().entrySet()) {
            Integer slot = binding.getKey();
            if (slot == null || !PlayerPressedKeys.isPressed(player, slot)) {
                continue;
            }
            if (releaseBundle(player, binding.getValue(), type, env, consumeInCreative)) {
                released = true;
            }
        }
        return released;
    }

    /**
     * 便捷重载：直接把一个 NeoForge 事件包成环境。
     *
     * <p>注意事件类型要与技能自己的 {@code SkillContextFactory} 对得上；对不上时
     * 工厂会回落到 extraData / 尽力构造（见各工厂的 {@code createDefault}）。</p>
     */
    public static boolean release(ServerPlayer player, SkillType type, Event triggerEvent) {
        if (player == null || triggerEvent == null) {
            return false;
        }
        return release(player, type, SkillContextEnvironment.withEvent(player, player.level(), triggerEvent));
    }

    /**
     * 为单个技能包执行「建上下文 → 累加资源 → 校验 → 落账 → 释放」。
     *
     * <p>这是内核 {@code SkillBundle#releaseSkills(SkillType, SkillContextEnvironment)} 的
     * <b>本模组复刻版</b>，逐条对应关系如下（<b>只有第 3 条不同</b>）：</p>
     * <ol>
     *     <li><b>取实例</b>：内核从 {@code skillData.get(type)} 取；这里从
     *         {@link SkillBundle#getAllData()} 按 {@code skill().getType() == type} 过滤
     *         （{@code SkillTypeFactory} 保证同 id 同实例，所以 {@code ==} 成立）。
     *         该 type 一个实例都没有时<b>返回 false</b>——内核这里返回 true
     *         （"没有技能也算成功"），但调用方靠它判断"这次触发到底有没有做事"，
     *         返回 true 会产生假的成功日志/分支。</li>
     *     <li><b>上下文每实例一份</b>：内核用 {@code contexts.put(instance, factory.create(env, instance))}，
     *         这里同样是<b>每个实例各建一个上下文</b>，且用
     *         {@link LinkedHashMap} 保持与实例列表同序。技能的 scratch
     *         （{@code putScratch/getScratch}，同一实例的 {@code consumeResource} 与
     *         {@code release} 之间传递"本次释放"的结果）依赖于此：若所有实例共用一个上下文，
     *         多个技能会互相踩 scratch。工厂拿不到（未注册）或 create 返回 null 的实例
     *         <b>跳过</b>，不消耗也不释放。</li>
     *     <li><b>要不要消耗</b>：内核写死 {@code !player.isCreative()}；这里换成
     *         {@code !player.isCreative() || consumeInCreative}。
     *         {@code consumeInCreative == false} 时<b>整段校验与落账都跳过</b>，
     *         但<b>仍然调用 {@code consumeResource}</b>——技能靠它算随机数/优先级/选目标等
     *         scratch，不调的话 {@code release} 里读不到结果（会有空指针或行为错乱）。</li>
     *     <li><b>累加</b>：内核按 {@code instance.getResource().key()} 做
     *         {@code computeIfAbsent} 共享一个 {@code DelayConsumable}（同一资源只累加一份），
     *         这里完全一致。</li>
     *     <li><b>全有或全无</b>：内核先对全部 consumable 调 {@code canConsume()}，
     *         任一失败立刻返回 false（<b>既没扣也没释放</b>——注意累加已经发生，
     *         但因为没 apply 所以玩家实际资源没变）；通过后再统一 {@code apply()}。
     *         这里完全一致（含"提前 return 会跳过后续释放"这一点）。</li>
     *     <li><b>释放</b>：内核最后逐个 {@code instance.release(context)} 并返回 true；一致。</li>
     * </ol>
     *
     * @param player            触发者
     * @param bundle            该按键槽位上的技能包
     * @param type              本次触发的技能类型
     * @param env               上下文环境
     * @param consumeInCreative 创造模式下是否也消耗（{@link SkillSettings} 的值，整次释放读一次）
     * @return 该包里是否有该类型的技能被真正释放
     */
    @SuppressWarnings("unchecked")
    private static boolean releaseBundle(ServerPlayer player, SkillBundle bundle, SkillType type,
                                         SkillContextEnvironment env, boolean consumeInCreative) {
        if (bundle == null) {
            return false;
        }

        // 1) 该包里属于本次 type 的实例（内核读 skillData.get(type)，这里按类型过滤同一个表）
        List<ISkillInstance<SkillContext>> candidates = new ArrayList<>();
        for (ISkillInstance<?> raw : bundle.getAllData()) {
            if (raw == null || raw.skill() == null || raw.skill().getType() != type) {
                continue;
            }
            candidates.add((ISkillInstance<SkillContext>) (ISkillInstance<?>) raw);
        }
        if (candidates.isEmpty()) {
            return false; // 不能返回 true：调用方据此判断"本次触发有没有真正释放"
        }

        // 2) 每个实例一份上下文（技能 scratch 的作用域；共用一个会互相踩）
        Map<ISkillInstance<SkillContext>, SkillContext> contexts = new LinkedHashMap<>();
        List<ISkillInstance<SkillContext>> instances = new ArrayList<>(candidates.size());
        for (ISkillInstance<SkillContext> instance : candidates) {
            SkillContextFactory<SkillContext> factory = instance.skill().getFactory();
            if (factory == null) {
                continue; // 工厂未注册：这个实例没法建上下文，跳过（不消耗也不释放）
            }
            SkillContext context = factory.create(env, instance);
            if (context == null) {
                continue; // 该触发场景建不出上下文（例如事件类型对不上）
            }
            contexts.put(instance, context);
            instances.add(instance);
        }
        if (instances.isEmpty()) {
            return false;
        }

        // 3) 累加资源（无论耗不耗都要做：技能靠 consumeResource 写 scratch 来算结果）
        boolean consume = !player.isCreative() || consumeInCreative;
        Map<ResourceKey<SkillResource>, SkillResource.DelayConsumable> consumables = new HashMap<>();
        for (ISkillInstance<SkillContext> instance : instances) {
            SkillResource resource = instance.getResource();
            if (resource == null || resource.key() == null) {
                continue; // 防御：资源缺失的实例无法计价（内核此处会 NPE）
            }
            ResourceKey<SkillResource> key = resource.key();
            consumables.computeIfAbsent(key, k -> resource.getDelayConsumable(player));
            instance.consumeResource(contexts.get(instance), consumables.get(key));
        }

        if (consume) {
            // 4) 全有或全无：先全部校验，再全部落账
            for (SkillResource.DelayConsumable consumable : consumables.values()) {
                if (!consumable.canConsume()) {
                    return false; // 能量不够：不扣不释放
                }
            }
            for (SkillResource.DelayConsumable consumable : consumables.values()) {
                consumable.apply();
            }
        }
        // consume == false：跳过上面的校验与落账，但仍已完成第 3 步的 consumeResource

        // 5) 释放
        for (ISkillInstance<SkillContext> instance : instances) {
            instance.release(contexts.get(instance));
        }
        return true;
    }
}
