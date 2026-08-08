package com.hjmmd_8.createoreexpansion.mixin;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.DestroyBlockContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
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

        if (!skillStack.hasSkill(SkillType.EXCAVATION_SKILL)) return;

        ExcavationSkillContext context = new DestroyBlockContext(this.level, blockPos, stack, this.player);
        skillStack.getSkillsHolder().releaseSkills(SkillType.EXCAVATION_SKILL, context);
    }
}
