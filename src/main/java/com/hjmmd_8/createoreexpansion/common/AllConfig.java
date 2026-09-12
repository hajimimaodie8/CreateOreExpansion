package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class AllConfig {
    // 获取Builder
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;
    static final Common COMMON;

    static {
        // 实例化 Common，只有这一种注册config的原版方式可以被create的神秘小代码识别
        var pair = BUILDER.configure(Common::new);
        SPEC = pair.getRight();
        COMMON = pair.getLeft();
    }

    public static class Common {
        /** 角磨床配置（角磨轮等级 / 转速） */
        public final Grinding GRINDING;
        /** 应力充能器配置（储存模式最高充能层数） */
        public final Charger CHARGER;
        /** 能量波配置（变体波的配方类型门槛） */
        public final Wave WAVE;

        public Common(ModConfigSpec.Builder builder) {
            GRINDING = new Grinding(builder);
            CHARGER = new Charger(builder);
            WAVE = new Wave(builder);
        }
    }

    /** 角磨床配置：低/中/高三级角磨轮的最低工作转速与最低转速下的加工耗时。
     * 转速插值上限跟随 Create 的 maxRotationSpeed（可调）。 */
    public static class Grinding {
        public final ModConfigSpec.IntValue tier1MinRpm;
        public final ModConfigSpec.DoubleValue tier1Time;
        public final ModConfigSpec.IntValue tier2MinRpm;
        public final ModConfigSpec.DoubleValue tier2Time;
        public final ModConfigSpec.IntValue tier3MinRpm;
        public final ModConfigSpec.DoubleValue tier3Time;

        Grinding(ModConfigSpec.Builder builder) {
            builder.push("grinding");
            tier1MinRpm = builder
                    .comment("一级角磨轮（铁/金/黄铜/锌）最低工作转速（RPM），低于此转速不加工")
                    .defineInRange("tier1MinRpm", 64, 1, 1024);
            tier1Time = builder
                    .comment("一级角磨轮在最低转速下单个物品的加工耗时（秒），转速越高耗时线性降低")
                    .defineInRange("tier1Time", 10.0, 0.5, 120.0);
            tier2MinRpm = builder
                    .comment("二级角磨轮（翡翠/钻石/黄玉）最低工作转速（RPM），低于此转速不加工")
                    .defineInRange("tier2MinRpm", 96, 1, 1024);
            tier2Time = builder
                    .comment("二级角磨轮在最低转速下单个物品的加工耗时（秒），转速越高耗时线性降低")
                    .defineInRange("tier2Time", 5.0, 0.5, 120.0);
            tier3MinRpm = builder
                    .comment("三级角磨轮（蓝宝石/星辉石/下界合金）最低工作转速（RPM），低于此转速不加工")
                    .defineInRange("tier3MinRpm", 128, 1, 1024);
            tier3Time = builder
                    .comment("三级角磨轮在最低转速下单个物品的加工耗时（秒），转速越高耗时线性降低")
                    .defineInRange("tier3Time", 3.0, 0.5, 120.0);
            builder.pop();
        }
    }

    /** 可生长水晶生长参数已迁至 {@code content.crystal.CrystalGrowthConfigs}（模组内统一修改点）。 */

    /**
     * 应力充能器配置：<b>储存模式的最高充能层数</b>（每层 = 攒满一个发射间隔 = 右键/红石释放一次能量波）。
     *
     * <p>层数只决定<b>储存容量</b>：不影响单波等级（蓝宝石按转速分档、星辉石按手动等级槽）、
     * 也不影响发射间隔；装满后机器停止充能（多余充能丢弃），等释放腾出空间。</p>
     */
    public static class Charger {
        public final ModConfigSpec.IntValue sapphireMaxLayers;
        public final ModConfigSpec.IntValue stellarstoneMaxLayers;

        Charger(ModConfigSpec.Builder builder) {
            builder.push("charger");
            sapphireMaxLayers = builder
                    .comment("蓝宝石应力充能器储存模式的最高充能层数（每层 = 一次可释放的能量波）")
                    .defineInRange("sapphireMaxLayers", 10, 1, 1000);
            stellarstoneMaxLayers = builder
                    .comment("星辉石应力充能器储存模式的最高充能层数（默认 20；只影响容量，不影响单波等级）")
                    .defineInRange("stellarstoneMaxLayers", 20, 1, 1000);
            builder.pop();
        }
    }

    /**
     * 能量波配置：<b>变体波的配方类型门槛</b>。
     *
     * <p>变器把"读到的加工机 → 配方类型"整合进波（见 {@code StellarWaveMachineCatalog}）。
     * 打开本开关后，波<b>只能执行它携带到的类型</b>——把机器拆掉，下一发波就不再具备那类配方
     * （老毛病：全库检索会照做，因为执行侧原本只按材料匹配、不看类型）。</p>
     *
     * <p>关掉它 = 回到旧的全库行为（任何变体波都能执行任何可执行配方族），仅供整合包做兼容排查。</p>
     */
    public static class Wave {
        public final ModConfigSpec.BooleanValue requireCarriedType;
        public final ModConfigSpec.EnumValue<PayloadRelease> payloadRelease;
        public final ModConfigSpec.BooleanValue consumeHeldItemAux;
        public final ModConfigSpec.IntValue maxPayloadItems;
        public final ModConfigSpec.IntValue maxPayloadKinds;
        public final ModConfigSpec.IntValue maxPayloadFluidMb;
        public final ModConfigSpec.IntValue maxPayloadEnergyFe;
        public final ModConfigSpec.BooleanValue refillPayloadOnHit;

        Wave(ModConfigSpec.Builder builder) {
            builder.push("wave");
            requireCarriedType = builder
                    .comment("变体波是否只能执行它携带的配方类型（= 变器半径内确实读到对应加工机）",
                            "true（默认）：拆掉机器后波不再能做那类配方，与“读取周围机器并整合能力”的设计一致",
                            "false：回到旧的全库检索行为（任何变体波都能做任何配方，不推荐）")
                    .define("requireCarriedType", true);
            payloadRelease = builder
                    .comment("波的剩余载荷（没用完的辅料）去哪儿——三种口径：",
                            "NEAREST_CONTAINER（默认，用户 2026-09-11 拍板）：存进【击中方块周围、变器读取半径之内】",
                            "  最近的“可存容器”（按距离由近到远逐个试，装满一个接着下一个；工作盆与正在加工的那个方块不算）",
                            "SOURCE_CONTAINER：还回实际取料的那几个容器（箱子里没用完的辅料原样回到原箱）",
                            "DROP_AT_DISSIPATION：全部爆落在消散点（最旧的行为；“想亲眼看着掉出来”时用）",
                            "三种口径都不会把余料放进正在加工的工作盆/方块（2026-09 实测反馈的那条）")
                    .defineEnum("payloadRelease", PayloadRelease.NEAREST_CONTAINER);
            consumeHeldItemAux = builder
                    .comment("执行“手持物类配方”（机械手部署 deploying / 物品应用 item_application）时是否消耗辅料",
                            "true（默认）：消耗——波没有“手”，每一击都要把辅料从容器/载荷里物化出来，",
                            "  否则会抓着一份辅料无限盖章、箱子里的原料永远不少（序列组装必须靠它吃齿轮/铁粒）",
                            "false：按实物机械手的口径，手持物不消耗（旧行为；会让序列组装变成无消耗）")
                    .define("consumeHeldItemAux", true);
            maxPayloadItems = builder
                    .comment("波一次最多携带的物品总数（件）——【取料是“傻抽”：范围内有多少抽多少，抽到上限为止，",
                            "不看这次要做什么加工、也不按配方需要挑种类，用户 2026-09 明确要求保持这样】",
                            "取料顺序：先每种各 1 件（铺开种类），还没到本上限时再从“剩余最多的那种”继续各抽 1 件",
                            "（用户口径：4 种 → 先各 1 件，再从还有存货的那组补到上限）")
                    .defineInRange("maxPayloadItems", 5, 1, 64);
            maxPayloadKinds = builder
                    .comment("波一次最多携带的物品种类数（种）——超过本数的种类不会被带走（同样是傻抽，不做需求筛选）")
                    .defineInRange("maxPayloadKinds", 5, 1, 64);
            maxPayloadFluidMb = builder
                    .comment("波一次最多携带的流体量（mB）——只取扫描到的第一种流体，抽到上限为止",
                            "默认 2000（= 2 B，用户 2026-09 拍板；旧值 500 mB）")
                    .defineInRange("maxPayloadFluidMb", 2000, 0, 100000);
            maxPayloadEnergyFe = builder
                    .comment("波一次最多携带的电量（FE）",
                            "-1（默认，自动）：上限 = CC&A 充电配方里最贵那条的耗电量（用户 2026-09 口径）；",
                            "  未安装 CC&A / 没有此类配方时回退为“不设上限”（保持旧行为：抽干可抽取储能）",
                            ">= 0：固定上限（想把电量载荷压得更小或放大时直接写数字）",
                            "上限只限制“取多少”，不改变取料方式（依旧是范围内有多少抽多少、抽到上限为止）")
                    .defineInRange("maxPayloadEnergyFe", -1, -1, 100_000_000);
            refillPayloadOnHit = builder
                    .comment("波命中目标后，是否再在命中点周围自动补一次料",
                            "false（默认，用户 2026-09-11 拍板）：载荷只在变器穿波那一刻取一次",
                            "  （取自变器读取半径内的容器 = 玩家自己摆的箱子），波不在命中点自作主张搬东西",
                            "true：恢复旧行为——命中后按“变器读取半径”在命中点周围再补一次物品/流体/电量")
                    .define("refillPayloadOnHit", false);
            builder.pop();
        }
    }

    /** 波剩余载荷的处置口径（见 {@code [wave] payloadRelease}）。 */
    public enum PayloadRelease {
        /** 存进"击中方块周围、变器读取半径之内"最近的可存容器（默认）。 */
        NEAREST_CONTAINER,
        /** 还回实际取料的那些容器（原箱）。 */
        SOURCE_CONTAINER,
        /** 全部爆落在消散点（最旧的行为）。 */
        DROP_AT_DISSIPATION
    }

    // 声明对应缓存
    public static int tier1MinRpm;
    public static float tier1Time;
    public static int tier2MinRpm;
    public static float tier2Time;
    public static int tier3MinRpm;
    public static float tier3Time;
    /** 蓝宝石充能器最高充能层数（初值 = 配置默认值，配置加载后覆盖；避免加载前取到 0） */
    public static int sapphireMaxStoreLayers = 10;
    /** 星辉石充能器最高充能层数（初值 = 配置默认值 20，配置加载后覆盖） */
    public static int stellarstoneMaxStoreLayers = 20;
    /** 变体波是否只执行"携带到的"配方类型（初值 = 默认 true，配置加载后覆盖） */
    public static boolean waveRequireCarriedType = true;
    /** 波剩余载荷的处置口径（初值 = 默认 NEAREST_CONTAINER：存进击中方块周围、变器范围内的最近可存容器） */
    public static PayloadRelease wavePayloadRelease = PayloadRelease.NEAREST_CONTAINER;
    /** 手持物类配方（deploying/item_application）是否消耗辅料（初值 = 默认 true；false = 按实物机械手口径不消耗） */
    public static boolean waveConsumeHeldItemAux = true;
    /** 波载荷物品总数上限（件） */
    public static int waveMaxPayloadItems = 5;
    /** 波载荷物品种类上限（种） */
    public static int waveMaxPayloadKinds = 5;
    /** 波载荷流体上限（mB；默认 2000 = 2 B） */
    public static int waveMaxPayloadFluidMb = 2000;
    /** 波载荷电量上限（FE；-1 = 自动：CC&A 充电配方里最贵那条的耗电量；其余 = 固定上限） */
    public static int waveMaxPayloadEnergyFe = -1;
    /** 波命中目标后是否再在命中点自动补料（初值 = 默认 false：只在变器穿波时取一次） */
    public static boolean waveRefillPayloadOnHit = false;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 在加载Config后填充缓存
        tier1MinRpm = COMMON.GRINDING.tier1MinRpm.get();
        tier1Time = COMMON.GRINDING.tier1Time.get().floatValue();
        tier2MinRpm = COMMON.GRINDING.tier2MinRpm.get();
        tier2Time = COMMON.GRINDING.tier2Time.get().floatValue();
        tier3MinRpm = COMMON.GRINDING.tier3MinRpm.get();
        tier3Time = COMMON.GRINDING.tier3Time.get().floatValue();
        sapphireMaxStoreLayers = Math.max(1, COMMON.CHARGER.sapphireMaxLayers.get());
        stellarstoneMaxStoreLayers = Math.max(1, COMMON.CHARGER.stellarstoneMaxLayers.get());
        waveRequireCarriedType = COMMON.WAVE.requireCarriedType.get();
        wavePayloadRelease = COMMON.WAVE.payloadRelease.get();
        waveConsumeHeldItemAux = COMMON.WAVE.consumeHeldItemAux.get();
        waveMaxPayloadItems = Math.max(1, COMMON.WAVE.maxPayloadItems.get());
        waveMaxPayloadKinds = Math.max(1, COMMON.WAVE.maxPayloadKinds.get());
        waveMaxPayloadFluidMb = Math.max(0, COMMON.WAVE.maxPayloadFluidMb.get());
        waveMaxPayloadEnergyFe = COMMON.WAVE.maxPayloadEnergyFe.get();
        waveRefillPayloadOnHit = COMMON.WAVE.refillPayloadOnHit.get();
    }
}
