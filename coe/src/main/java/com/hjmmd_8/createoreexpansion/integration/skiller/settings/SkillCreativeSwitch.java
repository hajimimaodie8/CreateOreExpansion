package com.hjmmd_8.createoreexpansion.integration.skiller.settings;

import net.minecraft.world.entity.player.Player;

/**
 * 「创造模式是否消耗」判据的<b>注入侧持有者</b>（内核不许依赖 COE，所以值放在这一侧）。
 *
 * <h2>为什么要有这个类</h2>
 * <p>内核 {@code com.leaf.skiller.foundation.skill.SkillBundle} 两处
 * （{@code releaseSkills(SkillType, SkillContext)} 与
 * {@code releaseSkills(SkillType, SkillContextEnvironment)}）把「累加 → 校验 → 落账」
 * <b>整段</b>写死在 {@code if (!player.isCreative())} 里。作者 2026-10-07 裁定：
 * <b>不许改内核本体</b>，要用注入的方式让它可开关。</p>
 *
 * <p>于是：{@link com.hjmmd_8.createoreexpansion.mixin.SkillBundleCreativeConsumeMixin}
 * 把那两处 {@code Player#isCreative()} 的调用点重定向到
 * {@link #consumeFor(Player)} —— 注入体读的就是本类的静态值。
 * 本类住在 {@code :coe}，所以<b>内核一个字节都不用改、也不依赖 COE</b>；
 * 反过来内核只要被注入，就必须读 COE 这一侧持有的值。</p>
 *
 * <h2>判据唯一实现</h2>
 * <p>{@link #consumeFor(Player, boolean)} 是「创造模式下这次要不要真扣」的<b>唯一实现</b>：
 * 既被注入进内核的那两处使用（{@link #consumeFor(Player)} 重载），
 * 也被 COE 自己的释放编排
 * （{@code CoeSkillRelease#releaseBundle}）使用 —— 两条路径同一个判据，不会各自漂移。</p>
 *
 * <h2>默认值 = 今天的行为</h2>
 * <p>初值就是 {@link SkillSettings#DEFAULT_CONSUME_IN_CREATIVE}
 * （{@code true} = 创造模式也消耗），与换核前旧 {@code ToolEnergy.tryConsume} 的手感一致，
 * 也是本模组今天的实际行为。缺省状态下注入<b>不改变任何一位行为</b>：
 * 内核原来在创造模式跳过那一整段，注入后依旧跳过（{@code !isCreative()} 为假、静态值又为真时
 * {@link #consumeFor(Player)} 返回 {@code true}，进而在创造模式下仍然跳过 —— 见下面的真值表）。</p>
 *
 * <table border="1">
 *   <caption>判据真值表（{@code consumeInCreative} = 开关值）</caption>
 *   <tr><th>玩家</th><th>开关</th><th>consumeFor</th><th>结果</th></tr>
 *   <tr><td>生存</td><td>任意</td><td>true</td><td>消耗（与内核原行为一致）</td></tr>
 *   <tr><td>创造</td><td>false（默认）</td><td>false</td><td>不消耗（与内核原行为一致）</td></tr>
 *   <tr><td>创造</td><td>true</td><td>true</td><td>消耗（开关打开时的新行为）</td></tr>
 * </table>
 *
 * <h2>值的来源与刷新</h2>
 * <p>真源仍是存档侧的服务端权威值（{@link SkillSettings}）。本类的静态字段是它的
 * <b>镜像</b>，由 {@link SkillSettings} 在<b>每一次读</b>（{@code consumeInCreative}）
 * 与<b>每一次写</b>（{@code setConsumeInCreative}）时推送 —— 因此不会漂移：
 * COE 每次释放技能都会先读一次存档（{@code CoeSkillRelease#release} 第 76 行），
 * 顺带就把镜像刷新了。</p>
 *
 * @see SkillSettings
 * @see com.hjmmd_8.createoreexpansion.mixin.SkillBundleCreativeConsumeMixin
 * @since 1.0.0
 */
public final class SkillCreativeSwitch {

    /**
     * 注入侧持有的开关值：{@code true} = 创造模式也消耗。
     *
     * <p>初值故意取 {@link SkillSettings#DEFAULT_CONSUME_IN_CREATIVE}
     * （而不是硬写 {@code true}/{@code false}）：默认值只有一个定义处，
     * 改默认值不会出现「COE 路径与内核路径默认值不一致」。</p>
     */
    private static volatile boolean consumeInCreative = SkillSettings.DEFAULT_CONSUME_IN_CREATIVE;

    /**
     * 内核注入是否真的落到类上了（由 mixin 在 {@code SkillBundle} 构造期标记）。
     *
     * <p>只用于启动自检日志：内核类住在 {@code META-INF/jarjar/} 的嵌套 jar 里，
     * 「有没有被注入」在静态关卡里看不出来，只能靠这条运行期证据。</p>
     */
    private static volatile boolean kernelInjected;

    private SkillCreativeSwitch() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 读注入侧持有的开关值（{@code true} = 创造模式也消耗）。 */
    public static boolean consumeInCreative() {
        return consumeInCreative;
    }

    /**
     * 推送开关值（由 {@link SkillSettings} 在读写存档时调用）。
     *
     * @param value 新值（{@code true} = 创造模式也消耗）
     */
    public static void push(boolean value) {
        consumeInCreative = value;
    }

    /**
     * 判据的唯一实现：这次释放要不要真扣（创造模式豁免与否）。
     *
     * <p>表达式与内核原来那句 {@code !player.isCreative()} 相比只多了一个
     * {@code || consumeInCreative}；{@code consumeInCreative == false} 时逐位等价。</p>
     *
     * @param player            触发者
     * @param consumeInCreative 本次释放采信的开关值（调用方整次释放只读一次存档）
     * @return true = 走「累加 → 校验 → 落账」整段
     */
    public static boolean consumeFor(Player player, boolean consumeInCreative) {
        return !player.isCreative() || consumeInCreative;
    }

    /**
     * {@link #consumeFor(Player, boolean)} 的注入侧重载：读本类当前持有的开关值。
     *
     * <p>内核被注入后调用的就是这个重载 —— 它拿不到「整次释放」的语义，
     * 只能读静态镜像；镜像由 {@link SkillSettings} 维持新鲜。</p>
     *
     * @param player 触发者（即内核那两处 {@code player.isCreative()} 的接收者）
     * @return true = 走「累加 → 校验 → 落账」整段
     */
    public static boolean consumeFor(Player player) {
        return consumeFor(player, consumeInCreative);
    }

    /** 由 mixin 在 {@code SkillBundle} 构造期调用：证明注入真的落到了嵌套 jar 里的类上。 */
    public static void markKernelInjected() {
        kernelInjected = true;
    }

    /** 内核注入是否已生效（启动自检用）。 */
    public static boolean kernelInjected() {
        return kernelInjected;
    }
}
