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

        Wave(ModConfigSpec.Builder builder) {
            builder.push("wave");
            requireCarriedType = builder
                    .comment("变体波是否只能执行它携带的配方类型（= 变器半径内确实读到对应加工机）",
                            "true（默认）：拆掉机器后波不再能做那类配方，与“读取周围机器并整合能力”的设计一致",
                            "false：回到旧的全库检索行为（任何变体波都能做任何配方，不推荐）")
                    .define("requireCarriedType", true);
            builder.pop();
        }
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
    }
}
