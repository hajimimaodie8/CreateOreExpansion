package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.SkinConfig;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 剥取技能（新内核版）——剑类攻击时额外掉落（按稀有度反选战利品）。
 *
 * <p>与旧 {@code content/skill/SkinSkill} 同源，逐条对应：</p>
 * <table border="1">
 *   <caption>与旧实现的差异</caption>
 *   <tr><th>旧实现</th><th>新实现</th></tr>
 *   <tr><td>技能键判定</td><td><b>去掉</b>（新路由只对按下的槽位调用）</td></tr>
 *   <tr><td>流程：概率判定 → 数量 roll → {@code tryConsume} → 取战利品 → 生成掉落</td>
 *       <td>拆成 <b>两段</b>：概率判定与数量 roll 放 {@link #consumeResource}（随后立刻扣能），
 *           取战利品与生成掉落放 {@link #release}。随机判定只滚一次，结果通过
 *           {@link HitSkillContext#putScratch} 传给 release —— 否则"扣能时的随机"和
 *           "掉落时的随机"会不一致</td></tr>
 *   <tr><td>冷却判定在 {@code HurtLivingEntityHandler}，释放成功后 {@code startTicks}</td>
 *       <td>判定移到本技能（{@code consumeResource} 与 {@code release} <b>两处同判</b>），
 *           生成掉落后 {@code startTicks}；时长仍按旧口径
 *           （{@link CoeSkillSupport#cooldownTicks}）</td></tr>
 * </table>
 *
 * <p>旧 {@code SkinSkill} 挂的 {@code EntityStrategy} 只用于客户端实体描边预览
 * （它的 {@code calculate} 恒返回空集合），因此新实现不实现 {@code StrategySkill}：
 * 迁移期预览仍由旧渲染器提供（旧技能条目还在 {@code AllSkills} 里），W5 再统一补策略与渲染器。</p>
 *
 * @since 1.0.0
 */
public class SkinItemSkill implements ItemSkill<HitSkillContext> {

    /** 本次释放 roll 出来的掉落数量（由 consumeResource 写入、release 读取） */
    private static final String SCRATCH_DROP_COUNT = "coe:skin_drop_count";

    @Override
    public void release(HitSkillContext context, ISkillInstance<HitSkillContext> instance) {
        LivingEntity target = context.target();
        Player player = context.getPlayer();
        if (target == null || player == null || target.level().isClientSide()) {
            return;
        }
        // 概率判定没通过时不会写 scratch（也没扣能）→ 这里直接退出
        Integer count = context.getScratch(SCRATCH_DROP_COUNT, Integer.class);
        if (count == null || count <= 0) {
            return;
        }
        if (CoeSkillSupport.onCooldown(player, player.getMainHandItem())) {
            return;
        }

        ItemStack loot = getLoot(context, target);
        if (loot.isEmpty()) {
            return;
        }
        Level level = target.level();
        for (int i = 0; i < count; i++) {
            level.addFreshEntity(new ItemEntity(
                    level, target.getX(), target.getY() + 0.2, target.getZ(), loot.copy()));
        }

        // 真正产出过才进冷却（与旧 HurtLivingEntityHandler 一致）
        SkinConfig config = CoeSkillSupport.configForLevel(
                player.getMainHandItem(), instance, SkinConfig.class);
        int cooldownSeconds = config == null ? 0 : config.cooldownSeconds;
        int ticks = CoeSkillSupport.cooldownTicks(player.getMainHandItem(), cooldownSeconds);
        if (ticks > 0) {
            ToolSkillCooldown.startTicks(player, player.getMainHandItem(), ticks);
        }
    }

    /**
     * 概率判定 + 掉落数量 roll + 扣能（顺序与旧 {@code SkinSkill#release} 的前三步一致：
     * 概率不足 → 不扣能；概率过了 → 立刻扣能，之后数量为 0 也不会退）。
     */
    @Override
    public void consumeResource(HitSkillContext context, Consumable consumable,
                                ISkillInstance<HitSkillContext> instance) {
        LivingEntity target = context.target();
        Player player = context.getPlayer();
        if (target == null || player == null || target.level().isClientSide()) {
            return;
        }
        ItemStack sword = player.getMainHandItem();
        SkinConfig config = CoeSkillSupport.configForLevel(sword, instance, SkinConfig.class);
        if (config == null) {
            return;
        }
        // 冷却中：不消耗（release 也会同样退出，口径必须一致）
        if (CoeSkillSupport.onCooldown(player, sword)) {
            return;
        }
        // 1. 掉落概率判定（不足则不触发、不扣能量）
        if (target.getRandom().nextFloat() > config.dropChance) {
            return;
        }
        // 2. 掉落数量判定（把结果留给 release，随机只滚这一次）
        int count = rollDropCount(config, target);
        context.putScratch(SCRATCH_DROP_COUNT, count);

        // 3. 真正生效前消耗能量
        int cost = CoeSkillSupport.cost(sword, config.energyCost,
                CoeSkillSupport.effectiveLevel(sword, instance));
        CoeSkillSupport.consume(player, sword, consumable, cost);
    }

    /** 按掉落数量权重 roll 出本次掉落数量（旧 {@code SkinSkill#rollDropCount}）。 */
    private static int rollDropCount(SkinConfig config, LivingEntity target) {
        int total = config.drop0Weight + config.drop1Weight + config.drop2Weight + config.drop3Weight;
        if (total <= 0) {
            return 1; // 未配置分布时兜底掉 1 个
        }
        int roll = target.getRandom().nextInt(total);
        if (roll < config.drop0Weight) return 0;
        roll -= config.drop0Weight;
        if (roll < config.drop1Weight) return 1;
        roll -= config.drop1Weight;
        if (roll < config.drop2Weight) return 2;
        return 3;
    }

    /**
     * 剥取战利品：按「稀有度反选」——采样统计战利品表中每种物品的出现频率，
     * 频率越低（越稀有）权重越高。
     *
     * <p>与旧 {@code SkinSkill#getLoot} 同一实现（64 次采样 + 1/频率 加权）。</p>
     */
    private static ItemStack getLoot(HitSkillContext context, LivingEntity target) {
        if (target.level().isClientSide()) {
            return ItemStack.EMPTY;
        }
        ResourceKey<LootTable> lootTableId = target.getLootTable();
        LootTable table = Objects.requireNonNull(target.level().getServer())
                .reloadableRegistries().getLootTable(lootTableId);

        DamageSource damageSource = target.level().damageSources().playerAttack(context.getPlayer());
        LootParams params = new LootParams.Builder((ServerLevel) target.level())
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                .create(LootContextParamSets.ENTITY);

        Map<Item, Integer> frequency = new HashMap<>();
        int samples = 64;
        for (int i = 0; i < samples; i++) {
            for (ItemStack drop : table.getRandomItems(params)) {
                frequency.merge(drop.getItem(), 1, Integer::sum);
            }
        }
        if (frequency.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(frequency.entrySet());
        double totalWeight = 0;
        Map<Item, Double> weights = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : entries) {
            double weight = 1.0 / entry.getValue();
            weights.put(entry.getKey(), weight);
            totalWeight += weight;
        }

        double roll = target.getRandom().nextDouble() * totalWeight;
        for (Map.Entry<Item, Integer> entry : entries) {
            roll -= weights.get(entry.getKey());
            if (roll <= 0) {
                return new ItemStack(entry.getKey());
            }
        }
        return new ItemStack(entries.get(entries.size() - 1).getKey());
    }
}
