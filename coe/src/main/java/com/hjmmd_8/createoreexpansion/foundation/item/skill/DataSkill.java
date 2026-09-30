package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
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
    /**
     * 技能注册 id —— <b>序列化的真源</b>（2026-09-30 技能换核第 4 阶段加入）。
     *
     * <p>此前 id 只能靠 {@code AllSkills.getId(skill)} 反查，因此数据层被迫持有技能实例
     * （旧内核形状）。现在 id 直接存在这里：{@link #toString()} 与
     * {@link #fromString(String)} 都只依赖它，技能执行与注册已全部在 Skiller 新内核，
     * 本类退化为纯数据载体。</p>
     */
    public ResourceLocation id;
    public ItemSkill skill;
    public CompoundTag nbt;
    public SkillConfig config;

    public DataSkill(ItemSkill skill, SkillConfig config, CompoundTag nbt) {
        this(AllSkills.getId(skill), skill, config, nbt);
    }

    public DataSkill(ResourceLocation id, ItemSkill skill, SkillConfig config, CompoundTag nbt) {
        this.id = id;
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
        return new DataSkill(id, skill, config, copiedNbt);
    }

    public String toString() {
        ResourceLocation key = id != null ? id : AllSkills.getId(skill);
        if (key == null) return "";
        if (nbt == null) return key.toString();
        return key.toString() + nbt.toString();
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
            if (id == null) return null;
            ItemSkill skill = AllSkills.get(id);

            CompoundTag nbt = pair.getSecond();

            // 恢复注册时的配置：优先按 NBT 中的等级取对应配置（一技能多等级）
            AllSkills.RegisteredDataSkill registered = AllSkills.getData(id);
            int level = nbt.getInt("Level");
            SkillConfig config = registered != null ? registered.configForLevel(level) : null;

            // 2026-09-30 第 4 阶段：id 是序列化真源，因此即使查不到技能实例
            // （例如该技能尚未重新注册）也保留这条数据，不再整条丢弃。
            return new DataSkill(id, skill, config, nbt);
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
