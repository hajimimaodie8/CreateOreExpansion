package com.hjmmd_8.createoreexpansion.content.skill.input;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.i18n.Translatable;
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

/**
 * 本模组的技能按键（P3l：从 {@code common/} 顶层搬进 COE 的 {@code content/skill/input/}）。
 *
 * <p><b>为什么它属于 COE 而不是 core</b>：四个键全部服务于技能释放/技能设置
 * （{@code SKILL_RELEASE*} 是技能释放键，{@code SKILL_SETTINGS} 开关本模组的技能设置界面）。
 * 留在 {@code common} 顶层会因 implements {@link Translatable}（P4e 起住在 core 的
 * {@code common/i18n}，core 编译期看不见）把整个包卡住；探针实测该项就是 1 个「找不到符号」。</p>
 *
 * <p>唯一的非 COE 消费者是 {@code mixin/ServerPlayerGameModeMixin}（SHARED 路径，
 * 允许 import COE）。注解里的 modid 恒为 {@code createoreexpansion}（{@code CoeCore.MOD_ID}），
 * 键位注册时机、{@code keyinfo.*} 语言键、默认按键全部一字未变。</p>
 */
@EventBusSubscriber(Dist.CLIENT)
public enum AllKeys implements Translatable {

    SKILL_RELEASE("skill_release", GLFW.GLFW_KEY_LEFT_SHIFT, "Skill Release"),
    /** 第二技能释放键（剑类双技能：键二释放第 2 个技能），默认 R，玩家可自定义 */
    SKILL_RELEASE_2("skill_release_2", GLFW.GLFW_KEY_R, "Skill Release 2"),
    /** 第三技能释放键（键三释放第 3 个技能），默认 G，玩家可自定义 */
    SKILL_RELEASE_3("skill_release_3", GLFW.GLFW_KEY_G, "Skill Release 3"),
    /** 技能设置：打开本模组的小设置界面（"创造模式释放技能是否消耗能量"开关），默认 J
     *  ——J 与最常用的 WASD/E/R/G/C/空格/Shift/Ctrl 都不冲突，也在 Create 自己的键位之外 */
    SKILL_SETTINGS("skill_settings", GLFW.GLFW_KEY_J, "Skill Settings"),
    /** 机器旋转的修饰键：与"扳手右键"组合 = 旋转本模组无模式机器（交互规则第 4 条）。
     * 默认左 Ctrl，<b>玩家可在"控制"里自定义</b>（原先写死在 core 里，改不了）。 */
    ROTATE_MODIFIER("rotate_modifier", GLFW.GLFW_KEY_LEFT_CONTROL, "Rotate Machine (Modifier)"),
    /**
     * <b>装备模式开关</b>（用户 2026-10-01 定稿，<b>2026-10-02 由"按住"改为"开关"</b>）：
     * 按一下打开（松手仍然打开），再按一下关闭；打开期间按任一技能键 = 释放<b>装备</b>的技能。
     *
     * <p>为什么要这个键：工具与装备的<b>技能槽位号必须分开</b>（工具 0/1/2、装备 3/4/5）。
     * 若不分开，按下键一时 {@code CoeSkillRelease.release} 会遍历到<b>两个</b>槽位 0 的绑定
     * （工具的和装备的）并同时释放 —— 能量双扣、两个效果同一 tick 生效、预览也不知显示哪个。
     * 这个键把客户端键源切成"模式"：<b>打开期间槽位 0/1/2 一律报未按下</b>，于是工具技能被抑制、
     * 装备技能独占按键（服务端与内核都不需要认识这个键，见 {@code CoeSkillClient}）。</p>
     *
     * <p><b>为什么是开关而不是按住</b>（用户 2026-10-02 原话）：按住时若还要按技能键，
     * 左手就必须一直压着这个键，非常费事。因此它<b>不</b>参与 {@code isPressed()} 判定，
     * 只由 {@code CoeSkillClient} 用 {@code consumeClick()} 边沿触发翻转一个锁存。</p>
     *
     * <p><b>代价</b>：开关打开期间工具技能（键一/二/三）整体让位给装备技能，要按一下本键切回。</p>
     *
     * <p>默认左 Alt：左手拇指自然位置，与 Shift/R/G（技能键）和左 Ctrl（旋转修饰键）都不冲突；
     * 玩家可在"控制"里自定义。</p>
     */
    EQUIPMENT_MODIFIER("equipment_modifier", GLFW.GLFW_KEY_LEFT_ALT, "Equipment Skill Toggle"),
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
        this.description = CoeCore.REGISTRY_NAMESPACE + ".keyinfo." + description;
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
        // 把 Ctrl+扳手 的修饰键指向上面这个玩家可自定义的键位（core 只留钩子，见 MachineRotateClient）。
        com.hjmmd_8.createoreexpansion.common.machine.MachineRotateClient
                .installModifierSource(ROTATE_MODIFIER::isPressed);
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