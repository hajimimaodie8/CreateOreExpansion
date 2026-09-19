package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillMigrationGate;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationContextFactory;
import com.leaf.skiller.api.registry.SkillerBuiltInRegistries;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.provider.SkillProviders;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Skiller 内核接线入口（mod 事件总线）。
 *
 * <p>由 {@code CreateOreExpansion} 在构造器里调 {@link #register(IEventBus)} 挂上。
 * 本类只做两件事：<b>注册</b>（上下文工厂 / 资源 / Provider / 技能条目）与
 * <b>启动自检日志</b>。</p>
 *
 * <p><b>注册顺序</b>（Skiller 契约要求）：先让 {@link SkillerBuiltInRegistries} 完成类加载
 * （五张自定义注册表在静态块里建立），再往注册表里登记条目。</p>
 *
 * <p><b>渐进迁移</b>：{@code skiller:skill} 注册表里<b>没注册</b>的技能仍然归旧框架
 * （{@code foundation/item/skill}）管；每迁移一个技能就在这里补一条注册，并通过
 * {@link SkillMigrationGate} 让旧框架跳过它，避免新旧双重生效。</p>
 *
 * @since 1.0.0
 */
public final class SkillerIntegration {

    /** 一次性初始化（Provider 注册、迁移闸门接线）只做一次——RegisterEvent 会为每张注册表各触发一次。 */
    private static boolean initialized;

    private SkillerIntegration() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 在模组构造器里调用：挂上注册监听与启动自检。 */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SkillerIntegration::onRegister);
        modEventBus.addListener(SkillerIntegration::onCommonSetup);
    }

    private static void onRegister(RegisterEvent event) {
        SkillerBuiltInRegistries.init();

        // ── 一次性：Provider 与迁移闸门（与具体注册表无关，只做一次）──────────────
        if (!initialized) {
            initialized = true;
            // 技能候选来源：读取本模组旧组件并转成新内核的技能组件
            SkillProviders.register(new CoeSkillProvider());
            // 迁移闸门：已在 skiller:skill 注册表里的技能，旧框架必须跳过（否则双重生效）
            SkillMigrationGate.setCheck(id -> SkillerBuiltInRegistries.SKILLS.containsKey(id));
        }

        ResourceKey<? extends Registry<?>> registryKey = event.getRegistryKey();

        // ── 上下文工厂：每个触发场景一个（技能自己的上下文由它的工厂创建）──────────
        // 注意：NeoForge 的 RegisterEvent 只提供 Supplier 重载，对象在注册时实例化一次。
        if (SkillerRegistries.CONTEXT_FACTORY.equals(registryKey)) {
            event.register(SkillerRegistries.CONTEXT_FACTORY,
                    ExcavationContextFactory.ID, ExcavationContextFactory::new);
            // TODO(W4)：注册 HIT（受击）/ USE（右键）/ BOW（弓箭两段式）三套上下文工厂
            logRegistry("skill_context_factory", SkillerBuiltInRegistries.CONTEXT_FACTORIES.keySet().size());
            return;
        }

        // ── 技能资源：本模组技能消耗的是工具能量（含凝能佩兜底）────────────────────
        if (SkillerRegistries.SKILL_RESOURCE.equals(registryKey)) {
            event.register(SkillerRegistries.SKILL_RESOURCE,
                    CoeToolEnergyResource.ID, CoeToolEnergyResource::new);
            logRegistry("skill_resource", SkillerBuiltInRegistries.SKILL_RESOURCES.keySet().size());
            return;
        }

        // ── 技能条目：迁移一个注册一个（id 一律 createoreexpansion:xxx，翻译键零改动）──
        if (SkillerRegistries.SKILL.equals(registryKey)) {
            // TODO(W4)：shatter / channel / grade / fell / skin / plunder / hoe / bow_curse / bow_disarm
            // 每个都用 new ItemSkillRegistration<>(CoeSkillTypes.X, <该技能上下文工厂的 KEY>, <技能实现>)
            logRegistry("skill", SkillerBuiltInRegistries.SKILLS.keySet().size());
            return;
        }
    }

    /** 注册自检日志：确认 Skiller 的自定义注册表真的收到了 RegisterEvent，以及当前条目数。 */
    private static void logRegistry(String name, int size) {
        CreateOreExpansion.LOGGER.info("[Skiller] registry event: {} -> entries={}", name, size);
    }

    /**
     * 启动自检：注册是否真的进了 Skiller 的五张表。
     *
     * <p>{@code RegisterEvent} 只对 FML 认识的自定义注册表触发；如果 Skiller 的注册表
     * 没被纳入这一轮注册，这里的数字会是 0 —— 那是必须立刻发现的错误，所以专门打一条日志。</p>
     */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        int contextFactories = SkillerBuiltInRegistries.CONTEXT_FACTORIES.keySet().size();
        int resources = SkillerBuiltInRegistries.SKILL_RESOURCES.keySet().size();
        int skills = SkillerBuiltInRegistries.SKILLS.keySet().size();
        CreateOreExpansion.LOGGER.info(
                "[Skiller] 内核接线自检：上下文工厂={}（本模组应 ≥1）、技能资源={}（应 ≥1）、技能条目={}（W4 起逐个增加）",
                contextFactories, resources, skills);
        if (resources < 2) {
            CreateOreExpansion.LOGGER.error(
                    "[Skiller] 技能资源注册似乎没生效（当前 {} 个，预期至少包含 skiller:empty 与本模组的 tool_energy）",
                    resources);
        }
    }
}
