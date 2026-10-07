package com.hjmmd_8.createoreexpansion.mixin;

import com.hjmmd_8.createoreexpansion.integration.skiller.settings.SkillCreativeSwitch;
import com.leaf.skiller.foundation.skill.SkillBundle;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把内核 {@link SkillBundle} 里写死的「创造模式豁免」改成<b>可开关</b>——<b>用注入，不改内核</b>。
 *
 * <h2>作者裁定（2026-10-07，逐字）</h2>
 * <p>「<b>不要直接改那个 skiller 内核，你要改的话，你可以用注入之类的方式</b>」</p>
 *
 * <h2>要改的是什么（内核源码 :223 与 :304）</h2>
 * <p>{@code SkillBundle} 的两个 {@code releaseSkills} 重载都把
 * 「累加 → 校验 → 落账」<b>整段</b>包在 {@code if (!player.isCreative())} 里
 * （源码副本 {@code _ref/Skiller/.../SkillBundle.java} 第 223 行与第 304 行；
 * 内嵌 jar 里的字节码用 {@code javap} 核过：两个方法各<b>恰好一处</b>
 * {@code invokevirtual net/minecraft/world/entity/player/Player.isCreative:()Z}）。
 * 于是创造模式下技能完全不耗能，而作者要求这个行为可由玩家在游戏内开关。</p>
 *
 * <h2>注入怎么做（两处 {@code @Redirect} + 一处 {@code @Inject}）</h2>
 * <ul>
 *   <li><b>两处 {@code @Redirect}</b>：把 {@code Player#isCreative()} 的调用点换成本模组
 *       {@link SkillCreativeSwitch#consumeFor(Player)} —— 表达式与内核原来那句相比只多一个
 *       {@code || 开关值}，开关为 {@code false}（缺省）时<b>逐位等价</b>。
 *       两处各按<b>完整方法描述符</b>定向，绝不用裸方法名（裸名会同时命中两个重载，
 *       将来内核加重载会变成静默的第三种落点）。</li>
 *   <li><b>一处 {@code @Inject}</b>（{@code <init>()V} 的 TAIL）：只是把
 *       {@link SkillCreativeSwitch#kernelInjected()} 置真，作为「注入真的落到了
 *       {@code META-INF/jarjar/} 里那个嵌套 jar 的类上」的<b>运行期证据</b>。
 *       {@code SkillBundle.EMPTY = new SkillBundle()} 是它的静态初始化，
 *       而静态初始化又由 {@code AllDataComponents.SKILL_COMPONENT → SkillComponent.CODEC →
 *       SkillBundle.CODEC} 在 skiller 的 {@code @Mod} 构造器里触发 —— 所以这条标记在
 *       启动期（早于 {@code FMLCommonSetupEvent}）一定会跑一次。</li>
 * </ul>
 *
 * <h2>为什么注入是必要的（不是"可以绕开"）</h2>
 * <p>那两处 {@code if} 把 {@code instance.consumeResource(..)} 也包在里面
 * （源码 :229 与 :310），所以<b>从资源侧绕不过去</b>：创造模式下内核连
 * {@code consumeResource} 都不调，自定义 {@code SkillResource} 也就没有任何被问到的机会。
 * 内核也没有留下任何可被外部塞值的钩子（没有配置项、没有 {@code BooleanSupplier} 字段），
 * 所以"反射改一个静态字段"这条路同样不存在。⇒ 能用的注入只有字节码层面这一种，
 * 也就是本类。</p>
 *
 * <h2>内嵌 jar 里的类能不能被 mixin 变换（本批的核心可行性问题）</h2>
 * <p>能。判据与证据：</p>
 * <ol>
 *   <li>skiller 由 {@code :coe} 的 {@code jarJar(implementation("com.leaf:skiller:1.0.0"))}
 *       嵌在 {@code META-INF/jarjar/skiller-1.0.0.jar}（第 1 层；整包形态下在深度 2）。
 *       JarJar 解出来的嵌套 jar 由 FML 登记成<b>同一个层</b>的 mod 文件，
 *       因此它的类与 {@code :coe} 的类<b>同一个 {@code TransformingClassLoader}</b> 加载 ——
 *       而 Mixin 的变换是按<b>类名</b>挂在那个 classloader 的 {@code loadClass} 上的，
 *       与"类来自哪个 jar"无关。</li>
 *   <li>反证同一件事的现成例子：skiller 自己就带一份 {@code skiller.mixins.json}
 *       （{@code BuiltInRegistriesMixin} / {@code MinecraftMixin}），
 *       说明"嵌套 jar 里的 mod 能注册 mixin 配置"这条路是通的；
 *       本类是相反方向（从 {@code :coe} 注入进嵌套 jar 的类），
 *       但两者走的是同一个 classloader 变换管线。</li>
 *   <li><b>实测</b>（本批 {@code runData} 日志）：{@code SkillerIntegration} 的启动自检打印
 *       「内核注入已生效」= true —— 那就是嵌套 jar 里的 {@code SkillBundle} 真的被变换过的
 *       运行期证据（{@code @Inject} / {@code @Redirect} 若没落上，
 *       {@code injectors.defaultRequire = 1} 会让它在类加载期直接报错，而不是静默通过）。</li>
 * </ol>
 *
 * @see SkillCreativeSwitch
 * @see com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease
 * @since 1.0.0
 */
@Mixin(SkillBundle.class)
public abstract class SkillBundleCreativeConsumeMixin {

    /**
     * 泛型重载 {@code releaseSkills(SkillType, T)}（擦除后第二参为 {@code SkillContext}）。
     *
     * <p>方法体里只有一处 {@code player.isCreative()}；把它换成本模组的判据。</p>
     */
    @Redirect(
            method = "releaseSkills(Lcom/leaf/skiller/foundation/skill/SkillType;Lcom/leaf/skiller/foundation/context/SkillContext;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isCreative()Z"))
    private boolean createoreexpansion$consumeForContext(Player player) {
        return SkillCreativeSwitch.consumeFor(player);
    }

    /**
     * 环境重载 {@code releaseSkills(SkillType, SkillContextEnvironment)}
     * —— 内核 {@code SkillReleaser} 实际调用的那一个。
     *
     * <p>方法体里同样只有一处 {@code player.isCreative()}。</p>
     */
    @Redirect(
            method = "releaseSkills(Lcom/leaf/skiller/foundation/skill/SkillType;Lcom/leaf/skiller/foundation/skill/config/SkillContextEnvironment;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isCreative()Z"))
    private boolean createoreexpansion$consumeForEnvironment(Player player) {
        return SkillCreativeSwitch.consumeFor(player);
    }

    /**
     * 注入落地的运行期标记（零行为；只写一个 volatile 静态布尔）。
     *
     * <p>选 {@code <init>()V} 而不是 {@code <clinit>}：无参构造器是 {@code SkillBundle.EMPTY}
     * 的初始化体，一定会随类的静态初始化跑一次，而 Mixin 对构造函数注入的支持比
     * {@code <clinit>} 更明确。</p>
     */
    @Inject(method = "<init>()V", at = @At("TAIL"))
    private void createoreexpansion$markKernelInjected(CallbackInfo ci) {
        SkillCreativeSwitch.markKernelInjected();
    }
}
