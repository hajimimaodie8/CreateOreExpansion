package com.hjmmd_8.createoreexpansion.foundation.item.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.function.*;

/**
 * 声明式 Config — 支持嵌套
 */
public abstract class AutoSkillConfig implements SkillConfig {

    public interface FieldMapping {
        String key();
        void load(CompoundTag tag);
        void save(CompoundTag tag);
    }

    protected abstract List<FieldMapping> mappings();

    @Override
    public void load(DataSkill data) {
        if (data.nbt == null || !data.nbt.contains("Config")) return;
        CompoundTag tag = data.nbt.getCompound("Config");
        mappings().forEach(m -> m.load(tag));
    }

    @Override
    public void accept(CompoundTag compoundTag) {
        CompoundTag tag = new CompoundTag();
        mappings().forEach(m -> m.save(tag));
        compoundTag.put("Config", tag);
    }

    // ========== 基础类型 ==========

    public static FieldMapping of(String key, Supplier<Integer> getter, IntConsumer setter) {
        return new FieldMapping() {
            public String key() { return key; }
            public void load(CompoundTag t) { if (t.contains(key)) setter.accept(t.getInt(key)); }
            public void save(CompoundTag t) { t.putInt(key, getter.get()); }
        };
    }

    public static FieldMapping ofFloat(String key, Supplier<Float> getter, Consumer<Float> setter) {
        return new FieldMapping() {
            public String key() { return key; }
            public void load(CompoundTag t) { if (t.contains(key)) setter.accept(t.getFloat(key)); }
            public void save(CompoundTag t) { t.putFloat(key, getter.get()); }
        };
    }

    public static FieldMapping ofStr(String key, Supplier<String> getter, Consumer<String> setter) {
        return new FieldMapping() {
            public String key() { return key; }
            public void load(CompoundTag t) { if (t.contains(key)) setter.accept(t.getString(key)); }
            public void save(CompoundTag t) { t.putString(key, getter.get()); }
        };
    }

    // ========== 嵌套 ==========

    /** 嵌套对象：内部也是 FieldMapping 列表 */
    public static FieldMapping nested(String key, List<FieldMapping> children) {
        return new FieldMapping() {
            public String key() { return key; }
            public void load(CompoundTag t) {
                CompoundTag nested = t.getCompound(key);
                children.forEach(m -> m.load(nested));
            }
            public void save(CompoundTag t) {
                CompoundTag nested = new CompoundTag();
                children.forEach(m -> m.save(nested));
                t.put(key, nested);
            }
        };
    }
}