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

        static <T> FieldMapping of(String key, Supplier<T> valueGetter, Consumer<T> valueSetter,
                                   Getter<T> getter, Putter<T> putter) {
            return new FieldMapping() {
                public String key() { return key; }

                public void load(CompoundTag tag) {
                    if (tag.contains(key)) {
                        valueSetter.accept(getter.get(tag, key));
                    }
                }

                public void save(CompoundTag tag) {
                    putter.put(tag, key, valueGetter.get());
                }
            };
        }

        interface Getter<T> {
            T get(CompoundTag tag, String key);
        }

        interface Putter<T> {
            void put(CompoundTag tag, String key, T value);
        }
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

    public static FieldMapping ofBool(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return of(key, getter, setter, CompoundTag::getBoolean, CompoundTag::putBoolean);
    }

    public static FieldMapping ofInt(String key, Supplier<Integer> getter, Consumer<Integer> setter) {
        return of(key, getter, setter, CompoundTag::getInt, CompoundTag::putInt);
    }

    public static FieldMapping ofFloat(String key, Supplier<Float> getter, Consumer<Float> setter) {
        return of(key, getter, setter, CompoundTag::getFloat, CompoundTag::putFloat);
    }

    public static FieldMapping ofStr(String key, Supplier<String> getter, Consumer<String> setter) {
        return of(key, getter, setter, CompoundTag::getString, CompoundTag::putString);
    }

    public static <T> FieldMapping of(String key, Supplier<T> valueGetter, Consumer<T> valueSetter,
                                      FieldMapping.Getter<T> getter, FieldMapping.Putter<T> putter) {
        return FieldMapping.of(key, valueGetter, valueSetter, getter, putter);
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