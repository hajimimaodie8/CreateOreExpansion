package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.SkinConfig;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.*;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.HitSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.EntityStrategy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
 * 剥取技能 —— 剑类攻击时额外掉落（剥取战利品）。
 *
 * 流程：先按掉落概率（dropChance）决定本次是否触发；触发后再按
 * 掉落数量分布（drop0~drop3 权重）决定掉几个。数值见 {@code SkinConfigs}。
 */
public class SkinSkill extends AbstractStrategySkill<Entity, EntityStrategy>
        implements ConfigSkill<HitSkillContext, SkinConfig> {

    private float dropChance;
    private int energyCost;
    private int cooldownSeconds;
    private int drop0Weight;
    private int drop1Weight;
    private int drop2Weight;
    private int drop3Weight;

    public SkinSkill(EntityStrategy strategy) {
        super(strategy);
    }

    @Override
    public SkillType getType() {
        return SkillType.HIT_SKILL;
    }

    @Override
    public void release(HitSkillContext context) {
        if (context.target().level().isClientSide()) return;

        LivingEntity entity = context.target();
        Level level = entity.level();
        Player player = context.player();
        if (player == null) return;

        // 1. 掉落概率判定（不足则不触发，不扣能量）
        if (entity.getRandom().nextFloat() > dropChance) return;

        // 2. 掉落数量判定（按权重 roll 出本次掉几个）
        int count = rollDropCount(entity);

        // 3. 真正生效前消耗能量
        if (!ToolEnergy.tryConsume(player, player.getMainHandItem(), this)) return;

        // 4. 从战利品表中随机取一个掉落物，复制 count 份生成
        ItemStack stack = getLoot(context);
        if (stack.isEmpty() || count <= 0) return;

        for (int i = 0; i < count; i++) {
            level.addFreshEntity(new ItemEntity(
                    level, entity.getX(), entity.getY() + 0.2, entity.getZ(), stack.copy()));
        }
    }

    /** 按掉落数量权重 roll 出本次掉落数量 */
    private int rollDropCount(LivingEntity entity) {
        int total = drop0Weight + drop1Weight + drop2Weight + drop3Weight;
        if (total <= 0) return 1; // 未配置分布时兜底掉 1 个
        int roll = entity.getRandom().nextInt(total);
        if (roll < drop0Weight) return 0;
        roll -= drop0Weight;
        if (roll < drop1Weight) return 1;
        roll -= drop1Weight;
        if (roll < drop2Weight) return 2;
        return 3;
    }

    /**
     * 剥取战利品：按「稀有度反选」—— 采样统计战利品表中每种物品的出现频率，
     * 出现频率越低（越稀有）的选中权重越高，实现「最稀有的物品最可能被剥取」。
     *
     * 例如凋零骷髅：骨头/煤炭常见（高频），凋零骷髅头稀有（低频）→ 大概率剥到头。
     *
     * @return 选中物品的 ItemStack；无掉落物时返回空
     */
    private ItemStack getLoot(HitSkillContext context) {
        LivingEntity entity = context.target();
        if (entity.level().isClientSide()) return ItemStack.EMPTY;

        ResourceKey<LootTable> lootTableId = entity.getLootTable();
        LootTable table = Objects.requireNonNull(entity.level().getServer())
                .reloadableRegistries().getLootTable(lootTableId);

        // 创建 damage_source 参数
        DamageSource damageSource = entity.level().damageSources().playerAttack(context.player());

        LootParams params = new LootParams.Builder((ServerLevel) entity.level())
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                .create(LootContextParamSets.ENTITY);

        // 1. 多次采样统计每种物品出现次数（次数越少 = 越稀有）
        Map<net.minecraft.world.item.Item, Integer> frequency = new HashMap<>();
        int samples = 64;
        for (int i = 0; i < samples; i++) {
            for (ItemStack drop : table.getRandomItems(params)) {
                frequency.merge(drop.getItem(), 1, Integer::sum);
            }
        }
        if (frequency.isEmpty()) return ItemStack.EMPTY;

        // 2. 按稀有度反选：权重 = 1 / 出现次数（稀有物品权重高）
        double totalWeight = 0;
        List<Map.Entry<net.minecraft.world.item.Item, Integer>> entries = new ArrayList<>(frequency.entrySet());
        Map<net.minecraft.world.item.Item, Double> weights = new HashMap<>();
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : entries) {
            double w = 1.0 / e.getValue();
            weights.put(e.getKey(), w);
            totalWeight += w;
        }

        double roll = entity.getRandom().nextDouble() * totalWeight;
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : entries) {
            roll -= weights.get(e.getKey());
            if (roll <= 0) {
                return new ItemStack(e.getKey());
            }
        }
        return new ItemStack(entries.getLast().getKey());
    }

    public void load(SkinConfig config, DataSkill data) {
        this.dropChance = config.dropChance;
        this.energyCost = config.energyCost;
        this.cooldownSeconds = config.cooldownSeconds;
        this.drop0Weight = config.drop0Weight;
        this.drop1Weight = config.drop1Weight;
        this.drop2Weight = config.drop2Weight;
        this.drop3Weight = config.drop3Weight;
    }

    /** 供外部（如冷却逻辑）读取本技能冷却秒数 */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public Class<SkinConfig> getConfigType() {
        return SkinConfig.class;
    }

    @Override
    public int getCost() {
        return energyCost;
    }
}
