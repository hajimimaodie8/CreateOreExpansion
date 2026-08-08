package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import com.mojang.datafixers.util.Pair;

public class DataSkill {
    public ItemSkill skill;
    public CompoundTag nbt;
    public int cost;

    public DataSkill(ItemSkill skill, CompoundTag nbt, int cost) {
        this.skill = skill;
        this.nbt = nbt;
        this.cost = cost;
    }

    public static DataSkill fromSkill(ItemSkill skill) {
        return new DataSkill(skill, null, skill.getCost());
    }

    public void modifyCost(SkillCostModifier modifier) {
        cost = modifier.modify(skill, skill.getCost());
    }

    public String toString() {
        if (nbt == null) return AllSkills.getId(skill).toString();
        return AllSkills.getId(skill).toString() + nbt.toString();
    }

    public static DataSkill fromString(String string) {
        try {
            var pair = parse(string);
            ItemSkill skill = AllSkills.get(pair.getFirst());
            CompoundTag nbt = pair.getSecond();
            if (skill == null) return null;
            int cost = skill.getCost();
            if (nbt != null && nbt.contains("Cost")) cost = nbt.getInt("Cost");
            return new DataSkill(skill, nbt, cost);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static Pair<ResourceLocation, CompoundTag> parse(String input) throws CommandSyntaxException {
        int brace = input.indexOf('{');
        if (brace == -1) {
            return Pair.of(ResourceLocation.tryParse(input), new CompoundTag());
        }

        String id = input.substring(0, brace);
        String nbt = input.substring(brace).replace('=', ':');

        return Pair.of(ResourceLocation.tryParse(id), TagParser.parseTag(nbt));
    }
}
