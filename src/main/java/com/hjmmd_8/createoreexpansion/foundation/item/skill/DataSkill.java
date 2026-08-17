package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import com.mojang.datafixers.util.Pair;

/**
 * 技能数据 - 一个技能实例在某个 ItemStack 上的运行数据。
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>{@link #skill} - 技能实例（同一注册技能的所有物品共享）</li>
 *     <li>{@link #config} - 技能配置（注册时从 AllSkills 挂载，反序列化时恢复）</li>
 *     <li>{@link #nbt} - 技能附加数据（等级、Config 快照、轮廓颜色等）</li>
 * </ul>
 *
 * <p>能量消耗统一以 {@link ItemSkill#getCost()} 为准，不在此类重复记录。</p>
 */
public class DataSkill {
    public ItemSkill skill;
    public CompoundTag nbt;
    public SkillConfig config;

    public DataSkill(ItemSkill skill, SkillConfig config, CompoundTag nbt) {
        this.skill = skill;
        this.config = config;
        this.nbt = nbt;
    }

    public CompoundTag getOrCreateNbt() {
        if (nbt == null) nbt = new CompoundTag();
        return nbt;
    }

    public <T extends SkillConfig> T getConfig(Class<T> type) {
        return type.cast(config);
    }

    /**
     * 复制此技能数据（NBT 深拷贝），用于在不同物品上以独立等级/颜色绑定同一技能，
     * 避免修改共享的注册实例。
     */
    public DataSkill copy() {
        CompoundTag copiedNbt = nbt != null ? nbt.copy() : null;
        return new DataSkill(skill, config, copiedNbt);
    }

    public String toString() {
        if (nbt == null) return AllSkills.getId(skill).toString();
        return AllSkills.getId(skill).toString() + nbt.toString();
    }

    /**
     * 从字符串反序列化技能数据。
     *
     * <p>字符串格式为 {@code skillId{NBT}}，其中 NBT 内的 {@code Config} 保存了技能配置。
     * 配置对象会从注册表恢复（按 NBT 中的 Level 取对应等级配置，支持一技能多等级），
     * 确保技能可正常释放。</p>
     */
    public static DataSkill fromString(String string) {
        try {
            var pair = parse(string);
            ResourceLocation id = pair.getFirst();
            ItemSkill skill = AllSkills.get(id);
            if (skill == null) return null;

            CompoundTag nbt = pair.getSecond();

            // 恢复注册时的配置：优先按 NBT 中的等级取对应配置（一技能多等级）
            AllSkills.RegisteredDataSkill registered = AllSkills.getData(id);
            int level = nbt.getInt("Level");
            SkillConfig config = registered != null ? registered.configForLevel(level) : null;

            return new DataSkill(skill, config, nbt);
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
