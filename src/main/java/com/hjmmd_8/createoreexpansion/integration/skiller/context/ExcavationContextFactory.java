package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import com.leaf.skiller.foundation.skill.config.SkillContextFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ExcavationSkillContext} 的上下文工厂（注册到 {@code skiller:skill_context_factory}）。
 *
 * <p>取信息的两条路：</p>
 * <ol>
 *     <li><b>NeoForge 事件</b>：{@link BlockEvent.BreakEvent}（服务端方块破坏事件）携带
 *         玩家 / 坐标 / 世界，直接构造成上下文；</li>
 *     <li><b>extraData</b>：旧触发点是 {@code mixin/ServerPlayerGameModeMixin}
 *         （注入 {@code destroyBlock}，<b>没有</b>对应 NeoForge 事件），此时环境由
 *         {@code SkillContextEnvironment.noEvent(...)} 构造，现场信息改走
 *         {@link SkillContextEnvironment#extraData(String, Object)} 的约定键
 *         {@link #KEY_POS} / {@link #KEY_TOOL} / {@link #KEY_ENTITY}。</li>
 * </ol>
 *
 * <p><b>空安全</b>：{@code SkillContextEnvironment.getEvent()} 在"无事件"时会抛
 * {@code NullPointerException}（内部 {@code eventClass} 为 null，`null.cast(...)`），
 * 事件类型不符时抛 {@code ClassCastException}；因此本工厂统一经 {@link #safeEvent} 取值并吞掉
 * 这两类异常，绝不把异常抛给释放路径。取不到玩家时返回 null（Skiller 的
 * {@code SkillBundle.releaseSkills(type, env)} 会为该实例放入 null 上下文）。</p>
 */
public class ExcavationContextFactory implements SkillContextFactory<ExcavationSkillContext> {

    /** 注册 id：{@code createoreexpansion:excavation_context} */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "excavation_context");

    /** 该工厂在 {@code skiller:skill_context_factory} 注册表中的键（W4 注册技能条目时用作 {@code ItemSkillRegistration} 的 factoryKey）。 */
    public static final ResourceKey<SkillContextFactory<ExcavationSkillContext>> KEY = createKey();

    /** extraData 约定键：破坏的方块坐标 */
    public static final String KEY_POS = "pos";
    /** extraData 约定键：使用的工具 */
    public static final String KEY_TOOL = "tool";
    /** extraData 约定键：破坏方块的实体（通常为玩家） */
    public static final String KEY_ENTITY = "entity";

    @Override
    @Nullable
    public ExcavationSkillContext create(SkillContextEnvironment env, ISkillInstance<ExcavationSkillContext> instance) {
        if (env == null) {
            return null;
        }
        Object event = safeEvent(env);
        if (event instanceof BlockEvent.BreakEvent breakEvent) {
            Level world = breakEvent.getLevel() instanceof Level level ? level : env.getLevel();
            Player player = breakEvent.getPlayer();
            if (world == null || player == null) {
                return null;
            }
            return new ExcavationSkillContext(world, breakEvent.getPos(), player.getMainHandItem(), player);
        }
        // 事件缺失/类型不符：回落到"尽力构造"（mixin 触发点走的就是这条路）
        return createDefault(env, instance == null ? 1 : instance.level());
    }

    @Override
    @Nullable
    public ExcavationSkillContext createDefault(SkillContextEnvironment env, int level) {
        if (env == null) {
            return null;
        }
        Player player = env.getPlayer();
        if (player == null) {
            return null;
        }
        Level world = env.getLevel() != null ? env.getLevel() : player.level();
        if (world == null) {
            return null;
        }
        BlockPos pos = extraData(env, KEY_POS, BlockPos.class);
        ItemStack tool = extraData(env, KEY_TOOL, ItemStack.class);
        LivingEntity entity = extraData(env, KEY_ENTITY, LivingEntity.class);
        return new ExcavationSkillContext(
                world,
                pos != null ? pos : BlockPos.ZERO,
                tool != null ? tool : player.getMainHandItem(),
                entity != null ? entity : player);
    }

    /** {@code env.getEvent()} 的防御性包装：无事件 / 事件类型不符都返回 null，不抛异常。 */
    @Nullable
    static Object safeEvent(SkillContextEnvironment env) {
        try {
            return env.getEvent();
        } catch (RuntimeException e) {
            // 无事件 -> NPE（eventClass == null）；类型不符 -> ClassCastException
            return null;
        }
    }

    /** {@code env.getExtraData(...)} 的防御性包装：extraData 为 null 或缺键都返回 null。 */
    @Nullable
    static <T> T extraData(SkillContextEnvironment env, String key, Class<T> type) {
        try {
            return env.getExtraData(key, type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillContextFactory<ExcavationSkillContext>> createKey() {
        // SkillerRegistries.CONTEXT_FACTORY 是 ResourceKey<Registry<SkillContextFactory<?>>>，
        // 这里把它收窄到本工厂自己的泛型实参（同一把注册表键，仅泛型不同）。
        return (ResourceKey<SkillContextFactory<ExcavationSkillContext>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.CONTEXT_FACTORY, ID);
    }
}
