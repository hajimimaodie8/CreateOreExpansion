package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import java.awt.*;

public enum ToolEnergyColorConfig {
    DEFAULT(0x5555FF, 0x1111BB),
    JADE(0x55FF55, 0x2A7F2A),
    TOPAZ(0xFFFF55, 0x8A6D00),
    SAPPHIRE(0x55AAFF, 0x1F4F8A),
    ;

    public final Color light;
    public final Color dark;

    ToolEnergyColorConfig(Color light, Color dark) {
        this.light = light;
        this.dark = dark;
    }

    ToolEnergyColorConfig(int light, int dark) {
        this.light = new Color(light);
        this.dark = new Color(dark);
    }
}
