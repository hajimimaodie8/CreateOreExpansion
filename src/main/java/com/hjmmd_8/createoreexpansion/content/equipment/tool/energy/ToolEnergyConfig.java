package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

/**
 * 工具能量系统配置常量
 * 集中管理所有能量相关数值，避免魔法数字
 */
public final class ToolEnergyConfig {
    private ToolEnergyConfig() {}

    // ===== 能量上限配置 =====
    public static final int JADE_MAX_ENERGY = 600;
    public static final int TOPAZ_MAX_ENERGY = 800;
    public static final int SAPPHIRE_MAX_ENERGY = 1000;
    public static final int JADE_TOPAZ_BOW_MAX_ENERGY = 800;

    // ===== 技能消耗配置 =====
    public static final int SWORD_AXE_SKILL_COST = 100;
    public static final int PICKAXE_SHOVEL_SKILL_COST = 10;

    // ===== 能量阈值配置 =====
    /** 技能使用最低能量阈值（20%） */
    public static final float SKILL_USAGE_THRESHOLD = 0.2f;

    // ===== Tooltip进度条配置 =====
    public static final int TOOLTIP_BAR_SLOTS = 10;
    public static final char TOOLTIP_BAR_CHAR = '█';

    // ===== 工具颜色配置（亮色） =====
    public static final int JADE_COLOR_BRIGHT = 0x55FF55;
    public static final int TOPAZ_COLOR_BRIGHT = 0xFFFF55;
    public static final int SAPPHIRE_COLOR_BRIGHT = 0x55AAFF;

    // ===== 工具颜色配置（暗色） =====
    public static final int JADE_COLOR_DARK = 0x2A7F2A;
    public static final int TOPAZ_COLOR_DARK = 0x8A6D00;
    public static final int SAPPHIRE_COLOR_DARK = 0x1F4F8A;

    // ===== 翠玉之弓渐变色配置 =====
    public static final int BOW_COLOR_START = 0xFFFF00; // 黄
    public static final int BOW_COLOR_END = 0x00FF00;   // 绿

    // ===== 低能量状态颜色 =====
    public static final int LOW_ENERGY_COLOR = 0xFFFFFF; // 白色
    public static final int LOW_ENERGY_DARK = 0x808080;  // 灰色
}
