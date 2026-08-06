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

        public Common(ModConfigSpec.Builder builder) {
            // 使用builder添加配置项
            TEST_CONFIG = builder
                    // 类似于注释
                    .comment("A Test Config")
                    // 实际的名字和默认值
                    .define("test", false);
        }
    }

    // 声明对应缓存
    public static boolean testConfig;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 在加载Config后填充缓存
        testConfig = COMMON.TEST_CONFIG.get();
    }
}
