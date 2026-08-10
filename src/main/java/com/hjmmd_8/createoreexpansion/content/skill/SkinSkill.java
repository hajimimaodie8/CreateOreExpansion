package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.SkinConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.TypedItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.HitSkillContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Objects;

public class SkinSkill implements ItemSkill, TypedItemSkill<HitSkillContext> {

    private float dropChance;
    private int energyCost;

    @Override
    public SkillType getType() {
        return SkillType.HIT_SKILL;
    }

    @Override
    public void releaseTyped(HitSkillContext context, DataSkill data) {
        if (context.target().level().isClientSide()) return;
        var stack = getLoot(context);
        if (stack.isEmpty()) return;
        LivingEntity entity = context.target();
        Level level = entity.level();

        level.addFreshEntity(new ItemEntity(
                level, entity.getX(), entity.getY() + 0.2, entity.getZ(), stack));
    }

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

        List<ItemStack> drops = table.getRandomItems(params);

        // 概率抛弃
        drops.removeIf(stack -> entity.getRandom().nextFloat() > dropChance);

        return drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst();
    }

    public void load(SkinConfig config) {
        this.dropChance = config.dropChance;
        this.energyCost = config.energyCost;
    }

    @Override
    public int getCost() {
        return energyCost;
    }
}
