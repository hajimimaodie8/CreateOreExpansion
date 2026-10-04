package com.hjmmd_8.createoreexpansion.integration.skiller.resource;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.SkillResource;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

/**
 * <b>技能资源（能量池）的公共基类</b>——把"一个能量池接成 Skiller 资源"的<b>固定骨架</b>收在这里，
 * 子类只需要回答三个问题：<b>有多少</b>、<b>够不够</b>、<b>怎么扣</b>。
 *
 * <h2>为什么要这个基类（用户 2026-10-01：「能继承就继承，别复制粘贴一堆」）</h2>
 * <p>改造前 {@link CoeToolEnergyResource} 与 {@link CoeArmorEnergyResource} 是<b>逐方法同形</b>的两份
 * 实现：各自的 {@code ID}/{@code KEY} 样板、各自的 {@code getAmount}/{@code canConsume}/{@code consume}、
 * 各自写一遍"扣失败打日志 + 扣成功 {@code setChanged()}"。两份都要改时极容易漏一处。
 * 现在这套骨架只有一份，子类只剩业务三问。</p>
 *
 * <h2>骨架里被固定的四件事（子类不要再各写一遍）</h2>
 * <ol>
 *     <li>{@code key()} —— 由构造参数里的 path 生成 {@code ID}/{@code KEY}（注册名唯一来源）；</li>
 *     <li>{@code getAmount(null)} 恒为 0（省掉每个子类的 null 判断）；</li>
 *     <li>{@code canConsume(amount <= 0)} 恒为 true（零消耗永远付得起）；</li>
 *     <li>{@code consume} 的<b>反馈</b>：先 {@code pay(...)}，失败打一条 warn（"扣费静默失败"最难查），
 *         成功后 {@code player.getInventory().setChanged()}（漏了它的症状是：能量条不刷新、
 *         护目镜/饰品的"剩余能量"读数整片消失）。</li>
 * </ol>
 *
 * <h2>写成扩展的姿势（给后来的开发者）</h2>
 * <pre>{@code
 * public class MyEnergyResource extends AbstractEnergySkillResource {
 *     public MyEnergyResource() { super("my_energy"); }   // 注册 id = createoreexpansion:my_energy
 *     @Override protected int amountOf(Player p) { ... }           // 池子里还有多少
 *     @Override protected boolean canPay(Player p, int amount) { } // 够不够（全有或全无由你定）
 *     @Override protected boolean pay(Player p, int amount) { }    // 真扣（成功返回 true）
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public abstract class AbstractEnergySkillResource implements SkillResource {

    /** 注册路径（完整 id = {@code createoreexpansion:<path>}）。 */
    private final String path;

    /** 完整资源 id。 */
    private final ResourceLocation id;

    /** 该资源在 {@code skiller:skill_resource} 注册表中的键。 */
    private final ResourceKey<SkillResource> key;

    /**
     * @param path 注册路径（不带命名空间，例如 {@code tool_energy} / {@code armor_energy}）
     */
    protected AbstractEnergySkillResource(String path) {
        this.path = path;
        this.id = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, path);
        this.key = ResourceKey.create(SkillerRegistries.SKILL_RESOURCE, id);
    }

    /** 注册路径（子类若要暴露 {@code PATH} 常量，从这里取，别再手写字符串）。 */
    public final String path() {
        return path;
    }

    /** 完整资源 id。 */
    public final ResourceLocation id() {
        return id;
    }

    @Override
    public final ResourceKey<SkillResource> key() {
        return key;
    }

    /** 池子里的可用量；{@code player == null} 由基类挡掉，子类不必再判。 */
    protected abstract int amountOf(Player player);

    /** 够不够付 {@code amount}（"四件平摊、全有或全无"这类口径由子类决定）。 */
    protected abstract boolean canPay(Player player, int amount);

    /**
     * 真扣。<b>返回是否成功</b>；失败时基类会打一条 warn（{@code canPay} 已过仍失败说明有 bug）。
     *
     * @return true = 已扣款（基类会补物品栏同步反馈）
     */
    protected abstract boolean pay(Player player, int amount);

    /** 扣款失败时的日志（子类可覆写补充上下文，例如"哪件工具"）。 */
    protected void logPayFailure(Player player, int amount) {
        CoeCore.LOGGER.warn("[Skiller] {} 扣减失败：player={}, amount={}",
            path, player.getName().getString(), amount);
    }

    @Override
    public final int getAmount(@Nullable Player player) {
        return player == null ? 0 : amountOf(player);
    }

    @Override
    public final boolean canConsume(@Nullable Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        return player != null && canPay(player, amount);
    }

    /**
     * <b>模板方法</b>：扣款 → 失败记账 / 成功反馈 → 交给子类的钩子。
     *
     * <p>注意 Skiller 的 {@link SkillResource#consume(Player, int)} 返回 void，失败状态由
     * {@link #canConsume} 前置把关（骨架统一在这里兜底记账）。子类<b>不要覆写本方法</b>，
     * 需要额外反馈请覆写 {@link #afterPaid(Player, int)}（继承的正确用法：覆写钩子，而不是重写骨架）。</p>
     */
    @Override
    public void consume(@Nullable Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        if (!pay(player, amount)) {
            logPayFailure(player, amount);
            return;
        }
        // 玩家能看见的反馈：强制物品栏同步（客户端立刻看到能量变化）
        player.getInventory().setChanged();
        afterPaid(player, amount);
    }

    /**
     * 扣款成功后的<b>附加</b>反馈（钩子；默认什么都不做）。
     *
     * <p>例子：工具能源在这里播报"剩余能量"读数（护目镜限定）。骨架已经做完了
     * "扣成功 ⇒ 物品栏同步"，钩子里只做自己特有的部分。</p>
     */
    protected void afterPaid(Player player, int amount) {
        // 默认无附加反馈
    }
}
