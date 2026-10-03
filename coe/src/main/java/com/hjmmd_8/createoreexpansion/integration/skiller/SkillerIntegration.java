package com.hjmmd_8.createoreexpansion.integration.skiller;


import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.BowContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.BowShootSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.UseItemContextFactory;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.UseItemSkillContext;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillRuntime;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.AreaAoeItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.BloodPactItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.EquipmentSkillStub;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.BowShootItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.HoeItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.PlunderItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.SkinItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.FellingItemSkill;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeAreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.integration.skiller.strategy.CoeEntityStrategy;
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

        // ── 一次性：Provider（与具体注册表无关，只做一次）────────────────────
        if (!initialized) {
            initialized = true;
            // 技能候选来源：读取本模组旧组件并转成新内核的技能组件（槽位 0/1/2 = 主手）
            SkillProviders.register(new CoeSkillProvider());
            // 装备技能来源（槽位 3/4/5 = 完整穿着的那一套；2026-10-01 用户裁定）：
            // 必须与工具段分开，否则按一次键会同时释放两个技能（能量双扣）。见 ArmorSkillProvider。
            SkillProviders.register(new ArmorSkillProvider());
            // 2026-09-30 第 4 阶段：迁移闸门（SkillMigrationGate）已删除 ——
            // 旧框架的执行入口（SkillsComponent#releaseSkills/releaseSkillAt）与两个旧触发点
            // 一并拔掉了，没有"旧路径可能重复执行"的窗口，闸门不再需要。
        }

        ResourceKey<? extends Registry<?>> registryKey = event.getRegistryKey();

        // ── 上下文工厂：每个触发场景一个（技能自己的上下文由它的工厂创建）──────────
        // 注意：NeoForge 的 RegisterEvent 只提供 Supplier 重载，对象在注册时实例化一次。
        if (SkillerRegistries.CONTEXT_FACTORY.equals(registryKey)) {
            event.register(SkillerRegistries.CONTEXT_FACTORY,
                    ExcavationContextFactory.ID, ExcavationContextFactory::new);
            event.register(SkillerRegistries.CONTEXT_FACTORY,
                    HitContextFactory.ID, HitContextFactory::new);
            event.register(SkillerRegistries.CONTEXT_FACTORY,
                    UseItemContextFactory.ID, UseItemContextFactory::new);
            event.register(SkillerRegistries.CONTEXT_FACTORY,
                    BowContextFactory.ID, BowContextFactory::new);
            logRegistry("skill_context_factory", SkillerBuiltInRegistries.CONTEXT_FACTORIES.keySet().size());
            return;
        }

        // ── 技能资源：本模组技能消耗的是工具能量（含凝能佩兜底）────────────────────
        if (SkillerRegistries.SKILL_RESOURCE.equals(registryKey)) {
            event.register(SkillerRegistries.SKILL_RESOURCE,
                    CoeToolEnergyResource.ID, CoeToolEnergyResource::new);
            // 护甲能量（2026-10-01）：四套护甲的储能池，装备技能实例的 "resource" 指向它
            event.register(SkillerRegistries.SKILL_RESOURCE,
                CoeArmorEnergyResource.ID, CoeArmorEnergyResource::new);
            logRegistry("skill_resource", SkillerBuiltInRegistries.SKILL_RESOURCES.keySet().size());
            return;
        }

        // ── 技能策略：把"选哪些方块/实体"的计算注册成可被技能引用的策略对象 ──────────
        if (SkillerRegistries.STRATEGY.equals(registryKey)) {
            event.register(SkillerRegistries.STRATEGY,
                    CoeAreaAoeStrategy.ID, CoeAreaAoeStrategy::new);
            event.register(SkillerRegistries.STRATEGY,
                    CoeFellingStrategy.ID, CoeFellingStrategy::new);
            // 实体策略：只服务客户端描边预览（skin / plunder 靠它换来"对着生物有预选框"）
            event.register(SkillerRegistries.STRATEGY,
                    CoeEntityStrategy.ID, CoeEntityStrategy::new);
            return;
        }

        // ── 技能条目：迁移一个注册一个（id 一律 createoreexpansion:xxx，翻译键零改动）──
        if (SkillerRegistries.SKILL.equals(registryKey)) {
            registerExcavationSkills(event);
            registerFellingSkill(event);
            registerHitSkills(event);
            registerUseSkills(event);
            registerArmorSkills(event);
            logRegistry("skill", SkillerBuiltInRegistries.SKILLS.keySet().size());
            return;
        }
    }

    /**
     * 注册<b>装备（护甲）技能</b>的技能条目。
     *
     * <p><b>为什么装备技能也必须在这里登记</b>：{@code skiller:skill} 是<b>数据驱动白名单</b> ——
     * 实例反序列化（{@code NbtSkillInstanceFactory#createFromData}）第一步就是
     * {@code SKILLS.get(skillId)}，查不到直接返回 null。不登记的症状是"看着全接好了、实则一直没有绑定"：
     * 客户端不会为槽位 3/4/5 轮询按键（技能按不出来），HUD 也没有技能行
     * （2026-10-01 用户实测到的就是这个现象）。</p>
     *
     * <p><b>为什么挂 USE 类型 + 空壳实现</b>：内核的注册条目必须带"类型 + 上下文工厂 + 技能实现"
     * 三件套。装备技能的执行在 {@code ArmorSkillRuntime}（长按语义），这两个方法与策略都不会被调用
     * （内核释放路径显式跳过装备段槽位，见 {@code CoeSkillRelease}）。挂 USE 是因为"按键触发的
     * 主动效果"在语义上最接近；空壳实现见 {@link EquipmentSkillStub}（所有装备技能共用同一个单例，不必一技能一类）。</p>
     */
    private static void registerArmorSkills(RegisterEvent event) {
        event.register(SkillerRegistries.SKILL, ArmorSkillRuntime.FALL_GUARD_ID,
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 蓄能疾骋（翠玉套槽位 2）：执行体同样住在 ArmorSkillRuntime，与虚衡坠护共用同一个无操作壳。
        event.register(SkillerRegistries.SKILL, ArmorSkillRuntime.CHARGE_DASH_ID,
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 绝境守护（宝石套槽位 1）：被动触发的执行体住在 LastStandHandler，长按段位住在
        // ArmorSkillRuntime，与上面两条共用同一个无操作壳。
        event.register(SkillerRegistries.SKILL, ArmorSkillRuntime.LAST_STAND_ID,
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 临域充力（宝石套槽位 3，基准等级 1 —— 用户 2026-10-01 更正：原先是槽位 2 / 基准 2）：
        // 执行体住在 ArmorSkillRuntime + FieldChargeRuntime，与上面几条共用同一个无操作壳。
        // 不登记的症状是"客户端不为该槽位轮询按键、HUD 也没有那一行"。
        event.register(SkillerRegistries.SKILL, ArmorSkillRuntime.FIELD_CHARGE_ID,
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 衡元择势（星界套槽位 1，基准等级 2 —— 用户 2026-10-02 星界轮）：融合蓄能疾骋 + 绝境守护，
        // 执行体住在 ArmorSkillRuntime + LastStandHandler（共用的高额伤害被动），同一个无操作壳。
        event.register(SkillerRegistries.SKILL, ArmorSkillRuntime.BALANCE_CHOICE_ID,
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 星芒嬗震（星界套槽位 3，基准等级 1 —— 用户 2026-10-02 当日第二版更正）：执行体住在
        // ArmorSkillRuntime + StarShockRuntime（发的是**既有**能量波实体），同一个无操作壳。
        // 与上面几条一样，登记的目的**只是**让 instance 反序列化能过白名单
        // （以及客户端据此为槽位 5 轮询按键）；真正的执行永远不会走内核那两条方法。
        event.register(SkillerRegistries.SKILL, ArmorSkillRuntime.STAR_SHOCK_ID,
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
    }

    /**
     * 注册受击系技能：{@code skin}（剥取）、{@code plunder}（夺取）与 {@code blood_pact}（血契置换）。
     *
     * <p>触发点是 {@code content/skill/handler/HurtLivingEntityHandler}（已经接过新内核）。</p>
     */
    private static void registerHitSkills(RegisterEvent event) {
        event.register(SkillerRegistries.SKILL, skillId("skin"),
                () -> new ItemSkillRegistration<HitSkillContext>(
                        CoeSkillTypes.HIT, HitContextFactory.KEY, new SkinItemSkill()));
        event.register(SkillerRegistries.SKILL, skillId("plunder"),
                () -> new ItemSkillRegistration<HitSkillContext>(
                        CoeSkillTypes.HIT, HitContextFactory.KEY, new PlunderItemSkill()));
        // 血契置换（coe-pact 批 1 登记条目；批 2 已把执行体填进 BloodPactItemSkill）。
        // 为什么批 1 就得先有这条登记：skiller:skill 是数据驱动白名单，
        //   漏了 ⇒ 物品上这个技能实例反序列化返回 null（症状是"看着接好了、其实没绑上"），
        //   而且 tools/check-skill-registry-parity.ps1 要求 AllSkills 与内核白名单逐字对齐。
        // 为什么不在内核里执行：需求 §5.1 #7 把交换接在受击链上；触发事件
        //   LivingIncomingDamageEvent 发生在扣血之前 ⇒ 天然满足"未受到攻击之前"的血量口径。
        // ⚠ 不另造共用空壳类：tools/check-armor-sets.ps1 断言 integration/skiller/skill 下
        //   只有一个 *Stub*.java（EquipmentSkillStub.java），所以血契置换用自己的实现槽位。
        // ⚠ 冷却走按技能记的 PerSkillCooldown（作者 2026-10-03 裁定 B）；
        //   两把剑的绑定仍属批 3 ⇒ 现在没有任何物品携带它。
        event.register(SkillerRegistries.SKILL, skillId("blood_pact"),
                () -> new ItemSkillRegistration<HitSkillContext>(
                        CoeSkillTypes.HIT, HitContextFactory.KEY, new BloodPactItemSkill()));
    }

    /** 技能条目 id：{@code createoreexpansion:<path>}（必须与旧 {@code AllSkills} 一字不差）。 */
    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, path);
    }

    /**
     * 注册使用系（右键）技能：{@code hoe}（锄头四优先级）、{@code pierce} / {@code orbit}
     * （回旋镖穿刺 + 环绕）、弓类两条（{@code bow_curse} / {@code bow_disarm}）。
     *
     * <p>触发点是 {@code content/skill/handler/UseItemHandler}（已经接过新内核）。
     * 弓类两个技能属于"松手射出 → 箭命中"的两段式，不走右键瞬间，另行处理。</p>
     */
    private static void registerUseSkills(RegisterEvent event) {
        event.register(SkillerRegistries.SKILL, skillId("hoe"),
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, new HoeItemSkill()));
        // 穿刺（回旋镖 · 2026-10-02 批 3）：**执行不在内核里** —— 它是镖自己的命中判定
        // （AbstractBoomerangEntity#onHitEntity/onHitBlock 吃掉额度、决定穿过还是掉头），
        // 内核侧登记的目的只有两个，与装备技能那 6 条逐条同形：
        //   1) skiller:skill 是数据驱动白名单，漏了 ⇒ 物品上这个技能实例反序列化返回 null；
        //   2) 客户端据此知道主手有一个 USE 族技能。
        // ⚠ 因为 1) 的"USE 族"身份，右键投掷会被 UseItemHandler 当成一次技能释放
        // （那就是"投一次扣两次能量"）—— 所以那里按**类型**（BoomerangItem）开了豁免，
        // 理由与弓的同形，见 UseItemHandler#release 的注释（裁定 D11）。
        event.register(SkillerRegistries.SKILL, skillId("pierce"),
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 环绕（回旋镖 · 2026-10-02 批 4）：与上面那条**逐条同形** —— 执行不在内核里
        // （生成在 BoomerangItem#releaseUsing → AbstractBoomerangEntity#spawnOrbitWaves，
        // 伤害/挖掘在环绕波自己的既有命中链上），这里登记的目的只有两个：
        //   1) skiller:skill 是数据驱动白名单，漏了 ⇒ 物品上这个技能实例反序列化返回 null；
        //   2) 客户端据此知道主手还有第二个 USE 族技能（HUD 那一行）。
        // ⚠ 同样是 USE 族 ⇒ 右键投掷会被 UseItemHandler 当成一次技能释放；那里按**类型**
        // （BoomerangItem）开的豁免**同时覆盖穿刺与环绕**（判据是类、不是技能 id），
        // 所以这里不需要（也不许）再加一条按 id 的白名单。
        event.register(SkillerRegistries.SKILL, skillId("orbit"),
                () -> new ItemSkillRegistration<UseItemSkillContext>(
                        CoeSkillTypes.USE, UseItemContextFactory.KEY, EquipmentSkillStub.INSTANCE));
        // 弓的两个技能：只迁"松手射击"这一段（耗能/冷却/写标记），"箭命中"那段仍在旧 handler 上
        event.register(SkillerRegistries.SKILL, skillId("bow_curse"),
                () -> new ItemSkillRegistration<BowShootSkillContext>(
                        CoeSkillTypes.USE, BowContextFactory.KEY, BowShootItemSkill.CURSE));
        event.register(SkillerRegistries.SKILL, skillId("bow_disarm"),
                () -> new ItemSkillRegistration<BowShootSkillContext>(
                        CoeSkillTypes.USE, BowContextFactory.KEY, BowShootItemSkill.DISARM));
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
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, path);
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "fell");
        event.register(SkillerRegistries.SKILL, id,
                () -> new ItemSkillRegistration<ExcavationSkillContext>(
                        CoeSkillTypes.EXCAVATION, ExcavationContextFactory.KEY, new FellingItemSkill()));
    }

    /** 注册自检日志：确认 Skiller 的自定义注册表真的收到了 RegisterEvent，以及当前条目数。 */
    private static void logRegistry(String name, int size) {
        CoeCore.LOGGER.info("[Skiller] registry event: {} -> entries={}", name, size);
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
        CoeCore.LOGGER.info(
                "[Skiller] 内核接线自检：上下文工厂={}（本模组应 ≥1）、技能资源={}（应 ≥1）、技能条目={}（W4 起逐个增加）",
                contextFactories, resources, skills);
        if (resources < 2) {
            CoeCore.LOGGER.error(
                    "[Skiller] 技能资源注册似乎没生效（当前 {} 个，预期至少包含 skiller:empty 与本模组的 tool_energy）",
                    resources);
        }
    }
}
