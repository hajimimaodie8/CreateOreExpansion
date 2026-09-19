package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillMigrationGate;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.AreaAoeItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.SkinItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.FellingItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeAreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeFellingStrategy;
import com.leaf.skiller.api.registry.SkillerBuiltInRegistries;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.provider.SkillProviders;
import com.leaf.skiller.foundation.skill.ItemSkillRegistration;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
            event.register(SkillerRegistries.CONTEXT_FACTORY,
                    HitContextFactory.ID, HitContextFactory::new);
            // TODO(W4)：注册 USE（右键）/ BOW（弓箭两段式）两套上下文工厂
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

        // ── 技能策略：把"选哪些方块/实体"的计算注册成可被技能引用的策略对象 ──────────
        if (SkillerRegistries.STRATEGY.equals(registryKey)) {
            event.register(SkillerRegistries.STRATEGY,
                    CoeAreaAoeStrategy.ID, CoeAreaAoeStrategy::new);
            event.register(SkillerRegistries.STRATEGY,
                    CoeFellingStrategy.ID, CoeFellingStrategy::new);
            return;
        }

        // ── 技能条目：迁移一个注册一个（id 一律 createoreexpansion:xxx，翻译键零改动）──
        if (SkillerRegistries.SKILL.equals(registryKey)) {
            registerExcavationSkills(event);
            registerFellingSkill(event);
            registerHitSkills(event);
            logRegistry("skill", SkillerBuiltInRegistries.SKILLS.keySet().size());
            return;
        }
    }

    /**
     * 注册受击系技能：{@code skin}（剥取）。
     *
     * <p>{@code plunder}（夺取）随后补上（同一批）。触发点是
     * {@code content/skill/handler/HurtLivingEntityHandler}（已经接过新内核）。</p>
     */
    private static void registerHitSkills(RegisterEvent event) {
        for (String path : new String[]{"skin"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, path);
            event.register(SkillerRegistries.SKILL, id,
                    () -> new ItemSkillRegistration<HitSkillContext>(
                            CoeSkillTypes.HIT, HitContextFactory.KEY, new SkinItemSkill()));
        }
    }

    /**
     * 注册「范围挖掘」一族：{@code shatter}（开岩）/ {@code channel}（引渠）/ {@code grade}（平场）。
     *
     * <p>三者共用 {@link AreaAoeItemSkill} 与 {@link CoeAreaAoeStrategy}，差异全在各自的等级配置
     * （{@code SkillAoeConfigs}）。上下文工厂统一用 {@link ExcavationContextFactory#KEY}。</p>
     *
     * <p><b>id 必须与旧 {@code common/AllSkills.java} 里注册的一字不差</b>：翻译键
     * （{@code skill.createoreexpansion.<path>}）与物品上的技能组成由它决定，写错就变成
     * "技能既不旧也不新"（旧框架按迁移闸门跳过、新内核又查不到）。</p>
     */
    private static void registerExcavationSkills(RegisterEvent event) {
        for (String path : new String[]{"shatter", "channel", "grade"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, path);
            event.register(SkillerRegistries.SKILL, id,
                    () -> new ItemSkillRegistration<ExcavationSkillContext>(
                            CoeSkillTypes.EXCAVATION, ExcavationContextFactory.KEY, new AreaAoeItemSkill()));
        }
    }

    /**
     * 注册砍伐 {@code fell}（斧头连锁砍树）。
     *
     * <p>它同样属于 {@code EXCAVATION} 族（旧 {@code FellingSkill#getType} 返回
     * {@code SkillType.EXCAVATION_SKILL}），因此**不需要**新增任何 mixin 触发点：
     * 挖掘触发处已有的那一次 {@code CoeSkillRelease.release(player, CoeSkillTypes.EXCAVATION, env)}
     * 会把它一起带上。</p>
     *
     * <p><b>id 必须是 {@code createoreexpansion:fell}</b>——与旧 {@code AllSkills.FELL} 一字不差：
     * 翻译键（{@code skill.createoreexpansion.fell}）与物品上的技能绑定都靠它。</p>
     */
    private static void registerFellingSkill(RegisterEvent event) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "fell");
        event.register(SkillerRegistries.SKILL, id,
                () -> new ItemSkillRegistration<ExcavationSkillContext>(
                        CoeSkillTypes.EXCAVATION, ExcavationContextFactory.KEY, new FellingItemSkill()));
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
