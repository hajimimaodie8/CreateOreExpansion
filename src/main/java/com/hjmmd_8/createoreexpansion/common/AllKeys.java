package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.data.lang.COELangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.Translatable;
import com.mojang.blaze3d.platform.InputConstants;
import net.createmod.catnip.client.ConflictSafeKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;

@EventBusSubscriber(Dist.CLIENT)
public enum AllKeys implements Translatable {

    SKILL_RELEASE("skill_release", GLFW.GLFW_KEY_LEFT_SHIFT, "Skill Release"),
    ;

    public static final Translatable MOD_NAME_TRANSLATABLE = () -> "createoreexpansion.mod_name";

    private KeyMapping keybind;
    private final String description;
    private final String translation;
    private final int key;
    private final boolean modifiable;
    private final boolean conflictSafe;

    AllKeys(int defaultKey) {
        this("", defaultKey, "");
    }

    AllKeys(String description, int defaultKey, String translation) {
        this(description, defaultKey, translation, false);
    }

    AllKeys(String description, int defaultKey, String translation, boolean conflictSafe) {
        this.description = CreateOreExpansion.MOD_ID + ".keyinfo." + description;
        this.key = defaultKey;
        this.modifiable = !description.isEmpty();
        this.translation = translation;
        this.conflictSafe = conflictSafe;
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for (AllKeys key : values())
            if (key.modifiable)
                consumer.accept(key.description, key.translation);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        for (AllKeys key : values()) {
            if (key.conflictSafe) {
                key.keybind = new ConflictSafeKeyMapping(key.description, key.key, MOD_NAME_TRANSLATABLE.getTranslateKey());
            } else {
                key.keybind = new KeyMapping(key.description, key.key, MOD_NAME_TRANSLATABLE.getTranslateKey());
            }
            if (!key.modifiable)
                continue;

            event.register(key.keybind);
        }
    }

    public static COELangProvider.Builder translate(COELangProvider.Builder builder) {
        return builder
                .add(AllKeys.MOD_NAME_TRANSLATABLE,
                        "机械动力：矿物拓展", "Create: Ore Expansion")
                .add(AllKeys.SKILL_RELEASE,
                        "技能释放", "Release skill")
                ;
    }

    public KeyMapping getKeybind() {
        return keybind;
    }

    public boolean isPressed() {
        if (!modifiable)
            return isKeyDown(key);
        return keybind.isDown();
    }

    public String getBoundKey() {
        return keybind.getTranslatedKeyMessage()
                .getString()
                .toUpperCase();
    }

    public boolean doesModifierAndCodeMatch(int code) {
        boolean codeMatches = code == keybind.getKey().getValue();

        boolean modifierMatches;
        KeyModifier modifier = keybind.getKeyModifier();
        if (modifier == KeyModifier.NONE) {
            modifierMatches = true;
        } else {
            modifierMatches = KeyModifier.getActiveModifiers().contains(modifier);
        }

        return codeMatches && modifierMatches;
    }

    public static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance()
                .getWindow()
                .getWindow(), key);
    }

    public static boolean isMouseButtonDown(int button) {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance()
                .getWindow()
                .getWindow(), button) == 1;
    }

    @Override
    public String getTranslateKey() {
        return description;
    }
}