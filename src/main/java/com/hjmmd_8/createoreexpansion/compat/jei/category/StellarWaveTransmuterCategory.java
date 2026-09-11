package com.hjmmd_8.createoreexpansion.compat.jei.category;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 星辉波变器"工作原理"JEI 分类（无真实配方，单张流程说明卡）。
 *
 * <p>展示：能量波从机器侧面穿入 → 转成携带载荷的变体波从对侧穿出；说明载荷内容与
 * 链式加工语义。不逐机器动画（按要求从简）：中间只静态渲染变器方块物品图标，
 * 两侧以波色块示意"原波 → 变体波"。</p>
 *
 * <p>流程卡注册进 {@code CreateOreExpansionJEI}（compat/jei/CreateOreExpansionJEI），
 * 并把变器方块登记为 catalyst——玩家在 JEI 里点开方块即可看到说明。</p>
 */
@ParametersAreNonnullByDefault
public class StellarWaveTransmuterCategory implements IRecipeCategory<StellarWaveTransmuterCategory.FlowRecipe> {

	/** 展示卡 JEI 类型（内容为无字段占位对象，仅用于驱动分类展示）。 */
	public static final RecipeType<FlowRecipe> TYPE =
		RecipeType.create(CreateOreExpansion.MOD_ID, "stellar_wave_transmuter", FlowRecipe.class);

	/** 无字段占位：展示卡本身即说明，不依赖任何真实配方数据。 */
	public static final class FlowRecipe {
	}

	private static final IDrawable BACKGROUND = new EmptyBackground(177, 118);
	private static final IDrawable ICON = new ItemIcon(() -> new ItemStack(AllBlocks.STELLAR_WAVE_TRANSMUTER.get()));

	public StellarWaveTransmuterCategory() {
	}

	@NotNull
	@Override
	public RecipeType<FlowRecipe> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("createoreexpansion.recipe.stellar_wave_transmuter");
	}

	@SuppressWarnings("removal")
	@Override
	public IDrawable getBackground() {
		return BACKGROUND;
	}

	@Override
	public IDrawable getIcon() {
		return ICON;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, FlowRecipe recipe, IFocusGroup focuses) {
		// 纯说明卡：无配方槽位
	}

	@Override
	public void draw(FlowRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Font font = Minecraft.getInstance().font;

		// ===== 上排：原波 → 变器 → 变体波（波色块 + 方块图标 + 箭头） =====
		// 原波色块（黄，等级 1 波主色）
		graphics.fill(22, 14, 40, 32, 0xFFF0C800);
		// 变器方块图标（JEI 槽内静态渲染物品图标，无需逐机器动画）
		AllGuiTextures.JEI_SLOT.render(graphics, 58, 10);
		graphics.renderItem(new ItemStack(AllBlocks.STELLAR_WAVE_TRANSMUTER.get()), 60, 12);
		// 变体波色块（玫红粉，象征变体波——实际颜色随原波等级，此处仅示意）
		graphics.fill(104, 14, 122, 32, 0xFFFF4073);
		// 箭头（→）
		graphics.drawString(font, "\u2192", 46, 17, 0xFFCFCFCF, false);
		graphics.drawString(font, "\u2192", 90, 17, 0xFFCFCFCF, false);

		// ===== 下排：分步说明（按宽度自动换行，兼容中/英文） =====
		int y = 44;
		int x = 8;
		int maxWidth = 161;
		int[] colors = { 0xFFE0B000, 0xFFCFCFCF, 0xFF9AD5FF, 0xFFCFCFCF };
		String[] keys = {
			"createoreexpansion.jei.transmuter.line1",
			"createoreexpansion.jei.transmuter.line2",
			"createoreexpansion.jei.transmuter.line3",
			"createoreexpansion.jei.transmuter.line4"
		};
		for (int i = 0; i < keys.length; i++) {
			String text = Component.translatable(keys[i]).getString();
			for (String line : wrap(font, text, maxWidth)) {
				graphics.drawString(font, line, x, y, colors[i], false);
				y += 10;
			}
			y += 3; // 段落间距
		}
	}

	/** 按字体宽度硬换行（用于翻译文案，避免溢出卡片）。 */
	private static List<String> wrap(Font font, String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int width = 0;
		for (int i = 0; i < text.length();) {
			int cp = text.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			int w = font.width(ch);
			if (width + w > maxWidth && current.length() > 0) {
				lines.add(current.toString());
				current.setLength(0);
				width = 0;
			}
			current.append(ch);
			width += w;
			i += Character.charCount(cp);
		}
		if (current.length() > 0)
			lines.add(current.toString());
		return lines;
	}

	/** 注册单张展示卡 + 变器方块 catalyst（CreateOreExpansionJEI 调用）。 */
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(TYPE, List.of(new FlowRecipe()));
	}

	public void registerCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(AllBlocks.STELLAR_WAVE_TRANSMUTER.get()), TYPE);
	}

	/** 供其它代码静态引用（如需）。 */
	public static ResourceLocation categoryId() {
		return TYPE.getUid();
	}
}
