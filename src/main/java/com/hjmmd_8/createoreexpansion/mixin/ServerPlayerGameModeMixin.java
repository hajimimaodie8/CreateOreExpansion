package com.hjmmd_8.createoreexpansion.mixin;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.content.skill.context.DestroyBlockContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationContextFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;
    @Shadow
    protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z",
            ordinal = 0, shift = At.Shift.BEFORE))
    public void createoreexpansion$beforeRemoveBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        this.createOreExpansion$trigger(blockPos);
    }

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V",
            ordinal = 0, shift = At.Shift.BEFORE))
    public void createoreexpansion$beforeMineBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        this.createOreExpansion$trigger(blockPos);
    }

    @Unique
    private void createOreExpansion$trigger(BlockPos blockPos) {
        ItemStack stack = this.player.getMainHandItem();
        SkillItemStack skillStack = SkillItemStack.of(stack);
        SkillsComponent holder = skillStack.getSkillsHolder();
        // 非技能物品（无 SKILLS 组件）直接忽略，避免 NPE 干扰原版挖掘
        if (holder == null) return;
        List<DataSkill> skills = holder.getDataSkills(SkillType.EXCAVATION_SKILL);
        if (skills.isEmpty()) return;

        ExcavationSkillContext context = new DestroyBlockContext(this.level, blockPos, stack, this.player);
        boolean released = false;

        // 按技能键选槽位释放（与剑类双技能一致）：
        // 键一(Shift)→槽位0、键二(R)→槽位1、键三(G)→槽位2；未按技能键不触发
        if (AllKeys.SKILL_RELEASE.isPressed()) {
            released |= holder.releaseSkillAt(skillStack, SkillType.EXCAVATION_SKILL, 0, context);
        }
        if (AllKeys.SKILL_RELEASE_2.isPressed()) {
            released |= holder.releaseSkillAt(skillStack, SkillType.EXCAVATION_SKILL, 1, context);
        }
        if (AllKeys.SKILL_RELEASE_3.isPressed()) {
            released |= holder.releaseSkillAt(skillStack, SkillType.EXCAVATION_SKILL, 2, context);
        }

        // 新内核（Skiller）路径：已经迁移过去的技能由它执行。
        // 两条路径不会重复：迁移闸门（SkillMigrationGate）让旧路径跳过已注册进新内核的技能。
        // 按键槽位由内核按服务端权威按键状态自己挑（这里不再逐个 if）。
        // 挖掘注入点没有对应的 NeoForge 事件 → 用 noEvent + extraData 传现场信息
        // （绝不能调 env.getEvent()：无事件时它会抛 NPE）。
        com.leaf.skiller.foundation.skill.config.SkillContextEnvironment env =
                com.leaf.skiller.foundation.skill.config.SkillContextEnvironment
                        .noEvent(this.player, this.level)
                        .extraData(ExcavationContextFactory.KEY_POS, blockPos)
                        .extraData(ExcavationContextFactory.KEY_TOOL, stack)
                        .extraData(ExcavationContextFactory.KEY_ENTITY, this.player);
        CoeSkillRelease.release(this.player, CoeSkillTypes.EXCAVATION, env);

        // 剩余能量显示统一由 ToolEnergy.tryConsume（消耗时）处理：工具行 + 绑定凝能佩行
    }
}
