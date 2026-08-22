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
        // 添加Config配置项
        public final ModConfigSpec.BooleanValue TEST_CONFIG;
        /** 角磨床配置（角磨轮等级 / 转速） */
        public final Grinding GRINDING;

        public Common(ModConfigSpec.Builder builder) {
            // 使用builder添加配置项
            TEST_CONFIG = builder
                    // 类似于注释
                    .comment("A Test Config")
                    // 实际的名字和默认值
                    .define("test", false);
            GRINDING = new Grinding(builder);
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

    // 声明对应缓存
    public static boolean testConfig;
    public static int tier1MinRpm;
    public static float tier1Time;
    public static int tier2MinRpm;
    public static float tier2Time;
    public static int tier3MinRpm;
    public static float tier3Time;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 在加载Config后填充缓存
        testConfig = COMMON.TEST_CONFIG.get();
        tier1MinRpm = COMMON.GRINDING.tier1MinRpm.get();
        tier1Time = COMMON.GRINDING.tier1Time.get().floatValue();
        tier2MinRpm = COMMON.GRINDING.tier2MinRpm.get();
        tier2Time = COMMON.GRINDING.tier2Time.get().floatValue();
        tier3MinRpm = COMMON.GRINDING.tier3MinRpm.get();
        tier3Time = COMMON.GRINDING.tier3Time.get().floatValue();
    }
}
