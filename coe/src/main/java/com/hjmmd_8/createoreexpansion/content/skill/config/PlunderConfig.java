package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

/**
 * 夺取技能配置 —— 5 级共用（数值统一在 {@link PlunderConfigs} 定义）。
 *
 * 装备夺取有两种模式（由字段组合表达）：
 * <ul>
 *     <li><b>互斥权重模式</b>（Lv1~3）：按 {@link #plunderWeaponWeight} /
 *     {@link #plunderArmorWeight} / {@link #plunderBothWeight} 权重 roll，
 *     剩余权重为「空」，互斥择一执行；</li>
 *     <li><b>独立判定模式</b>（Lv4~5）：武器必夺（{@link #alwaysWeapon}），
 *     装备按 {@link #armorChance} 独立判定。</li>
 * </ul>
 *
 * 吸血：敌人额外扣除 {@link #lifestealAmount} 点血，自身回复等量；
 * 满血时改为伤害吸收（时长 {@link #absorptionTicks}、等级 {@link #absorptionAmplifier}）。
 */
public class PlunderConfig extends AutoSkillConfig {

    // ========== 装备夺取（互斥权重模式） ==========

    /** 夺武器权重（Lv1~3 用，与夺装备/两者/空 合计 100） */
    public int plunderWeaponWeight;
    /** 夺装备权重（Lv1~3 用） */
    public int plunderArmorWeight;
    /** 同时夺武器+装备权重（Lv1~3 用） */
    public int plunderBothWeight;

    // ========== 装备夺取（独立判定模式，Lv4~5 用） ==========

    /** 武器是否必夺（true 时武器 100% 夺取） */
    public boolean alwaysWeapon;
    /** 装备独立夺取概率（0~1） */
    public float armorChance;

    // ========== 处置方式 ==========

    /** 武器处置方式（DROP=掉落物 / PICKUP=收缴背包） */
    public String weaponDisposition;
    /** 装备处置方式（DROP=掉落物 / PICKUP=收缴背包） */
    public String armorDisposition;

    // ========== 吸血 / 吸收 ==========

    /** 吸血量（敌人扣血 = 自身回血） */
    public int lifestealAmount;
    /** 满血时伤害吸收 buff 时长（tick） */
    public int absorptionTicks;
    /** 满血时伤害吸收等级（0=吸收I, 1=吸收II, 2=吸收III, 3=吸收IV） */
    public int absorptionAmplifier;

    // ========== 通用 ==========

    /** 技能冷却秒数（创造模式无冷却） */
    public int cooldownSeconds;
    public int energyCost;

    public PlunderConfig(int plunderWeaponWeight, int plunderArmorWeight, int plunderBothWeight,
                         boolean alwaysWeapon, float armorChance,
                         String weaponDisposition, String armorDisposition,
                         int lifestealAmount, int absorptionTicks, int absorptionAmplifier,
                         int cooldownSeconds, int energyCost) {
        this.plunderWeaponWeight = plunderWeaponWeight;
        this.plunderArmorWeight = plunderArmorWeight;
        this.plunderBothWeight = plunderBothWeight;
        this.alwaysWeapon = alwaysWeapon;
        this.armorChance = armorChance;
        this.weaponDisposition = weaponDisposition;
        this.armorDisposition = armorDisposition;
        this.lifestealAmount = lifestealAmount;
        this.absorptionTicks = absorptionTicks;
        this.absorptionAmplifier = absorptionAmplifier;
        this.cooldownSeconds = cooldownSeconds;
        this.energyCost = energyCost;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("PlunderWeaponWeight", () -> plunderWeaponWeight, value -> plunderWeaponWeight = value),
                ofInt("PlunderArmorWeight", () -> plunderArmorWeight, value -> plunderArmorWeight = value),
                ofInt("PlunderBothWeight", () -> plunderBothWeight, value -> plunderBothWeight = value),
                ofBool("AlwaysWeapon", () -> alwaysWeapon, value -> alwaysWeapon = value),
                ofFloat("ArmorChance", () -> armorChance, value -> armorChance = value),
                ofStr("WeaponDisposition", () -> weaponDisposition, value -> weaponDisposition = value),
                ofStr("ArmorDisposition", () -> armorDisposition, value -> armorDisposition = value),
                ofInt("LifestealAmount", () -> lifestealAmount, value -> lifestealAmount = value),
                ofInt("AbsorptionTicks", () -> absorptionTicks, value -> absorptionTicks = value),
                ofInt("AbsorptionAmplifier", () -> absorptionAmplifier, value -> absorptionAmplifier = value),
                ofInt("Cooldown", () -> cooldownSeconds, value -> cooldownSeconds = value),
                ofInt("Cost", () -> energyCost, value -> energyCost = value)
        );
    }
}
