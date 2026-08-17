package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
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
    /** 第二技能释放键（剑类双技能：键二释放第 2 个技能），默认 R，玩家可自定义 */
    SKILL_RELEASE_2("skill_release_2", GLFW.GLFW_KEY_R, "Skill Release 2"),
    /** 第三技能释放键（键三释放第 3 个技能），默认 G，玩家可自定义 */
    SKILL_RELEASE_3("skill_release_3", GLFW.GLFW_KEY_G, "Skill Release 3"),
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

    public KeyMapping getKeybind() {
        return keybind;
    }

    public boolean isPressed() {
        if (!modifiable)
            return isKeyDown(key);
        // 键位只在客户端注册；服务端（keybind 为 null）视为未按下，避免 NPE
        if (keybind == null)
            return false;
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