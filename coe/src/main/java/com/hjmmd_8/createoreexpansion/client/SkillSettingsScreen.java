package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.integration.skiller.SkillSettingsPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 本模组的小设置界面（由键位界面里的「技能设置」键打开）。
 *
 * <p>目前只有一个开关：<b>创造模式释放技能是否消耗能量</b>。用户明确要求"开关放键位绑定
 * 界面里的一项，按它打开一个小设置界面"，而不是去改配置文件——所以这里只有一屏一按钮，
 * 不引入任何配置项。</p>
 *
 * <h2>值的来源与去向</h2>
 * <ul>
 *     <li><b>显示</b>：{@link SkillSettingsPayload#clientValue}——服务端广播/登录补发的
 *         权威值的客户端镜像（打开界面时读一次当作初值）。</li>
 *     <li><b>点击</b>：先乐观地翻转本地按钮文案（手感），再
 *         {@code sendToServer(new SkillSettingsPayload(新值))}；服务端校验后写存档并广播回来，
 *         回包会覆盖 {@link SkillSettingsPayload#clientValue}。所以"值以服务端为准"。</li>
 * </ul>
 *
 * <p>{@code isPauseScreen() = false}：与 Create 的设置界面一致——不暂停世界，
 * 免得单人游戏里开着界面时联机侧看到你站着不动却被怪物打（也更符合"这是个即时开关"）。</p>
 *
 * @since 1.0.0
 */
@OnlyIn(Dist.CLIENT)
public class SkillSettingsScreen extends Screen {

    /** 开关按钮宽度（够放下中英文案） */
    private static final int TOGGLE_WIDTH = 280;
    private static final int BUTTON_HEIGHT = 20;

    private static final Component HINT =
            Component.translatable("createoreexpansion.skill_settings.hint");
    private static final Component TOGGLE_ON =
            Component.translatable("createoreexpansion.skill_settings.consume_creative.on");
    private static final Component TOGGLE_OFF =
            Component.translatable("createoreexpansion.skill_settings.consume_creative.off");
    private static final Component DONE = Component.translatable("gui.done");

    /** 当前显示的值（打开界面时取服务端镜像，点击后本地乐观翻转） */
    private boolean consumeInCreative;

    private Button toggleButton;

    public SkillSettingsScreen() {
        super(Component.translatable("createoreexpansion.skill_settings.title"));
        this.consumeInCreative = SkillSettingsPayload.clientValue;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        this.toggleButton = Button.builder(toggleLabel(), button -> {
                    consumeInCreative = !consumeInCreative;
                    button.setMessage(toggleLabel());
                    // 只发"意愿"：服务端校验发送者后写存档，再把权威值广播回来
                    PacketDistributor.sendToServer(new SkillSettingsPayload(consumeInCreative));
                })
                .bounds(centerX - TOGGLE_WIDTH / 2, this.height / 2 - BUTTON_HEIGHT / 2,
                        TOGGLE_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(this.toggleButton);

        addRenderableWidget(Button.builder(DONE, button -> onClose())
                .bounds(centerX - 60, this.height / 2 + 30, 120, BUTTON_HEIGHT)
                .build());
    }

    private Component toggleLabel() {
        return consumeInCreative ? TOGGLE_ON : TOGGLE_OFF;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, HINT, this.width / 2, this.height / 2 - 34, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
