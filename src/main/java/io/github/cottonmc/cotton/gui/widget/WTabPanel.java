package io.github.cottonmc.cotton.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LibGui;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.Tab;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// TODO: Different tab positions

/**
 * A panel that contains creative inventory-style tabs on the top.
 *
 * @since 3.0.0
 */
public class WTabPanel extends WPanel {
	private static final int TAB_PADDING = 4;
	private static final int TAB_WIDTH = 28;
	private static final int TAB_HEIGHT = 30;
	private static final int PANEL_PADDING = 8; // The padding of BackgroundPainter.VANILLA
	private static final int ICON_SIZE = 16;
	private final WBox tabRibbon = new WBox(Axis.HORIZONTAL).setSpacing(1);
	private final List<WTab> tabWidgets = new ArrayList<>();
	private final WCardPanel mainPanel = new WCardPanel();

	/**
	 * Constructs a new tab panel.
	 */
	public WTabPanel() {
		add(tabRibbon, 0, 0);
		add(mainPanel, PANEL_PADDING, TAB_HEIGHT + PANEL_PADDING);
	}

	private void add(WWidget widget, int x, int y) {
		children.add(widget);
		widget.setParent(this);
		widget.setLocation(x, y);
		expandToFit(widget);
	}

	/**
	 * Adds a tab to this panel.
	 *
	 * @param tab the added tab
	 */
	public void add(Tab tab) {
		WTab tabWidget = new WTab(tab);

		if (tabWidgets.isEmpty()) {
			tabWidget.selected = true;
		}

		tabWidgets.add(tabWidget);
		tabRibbon.add(tabWidget, TAB_WIDTH, TAB_HEIGHT + TAB_PADDING);
		mainPanel.add(tab.getWidget());
	}

	/**
	 * Configures and adds a tab to this panel.
	 *
	 * @param widget       the contained widget
	 * @param configurator the tab configurator
	 */
	public void add(WWidget widget, Consumer<Tab.Builder> configurator) {
		Tab.Builder builder = new Tab.Builder(widget);
		configurator.accept(builder);
		add(builder.build());
	}

	@Override
	public void setSize(int x, int y) {
		super.setSize(x, y);
		tabRibbon.setSize(x, TAB_HEIGHT);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void addPainters() {
		super.addPainters();
		mainPanel.setBackgroundPainter(BackgroundPainter.VANILLA);
	}

	private final class WTab extends WWidget {
		private final Tab data;
		boolean selected = false;

		WTab(Tab data) {
			this.data = data;
		}

		@Override
		public boolean canFocus() {
			return true;
		}

		@Environment(EnvType.CLIENT)
		@Override
		public InputResult onClick(int x, int y, int button) {
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

			for (WTab tab : tabWidgets) {
				tab.selected = (tab == this);
			}

			mainPanel.setSelectedCard(data.getWidget());
			WTabPanel.this.layout();
			return InputResult.PROCESSED;
		}

		@Environment(EnvType.CLIENT)
		@Override
		public void onKeyPressed(int ch, int key, int modifiers) {
			if (isActivationKey(ch)) {
				onClick(0, 0, 0);
			}
		}

		@Environment(EnvType.CLIENT)
		@Override
		public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
			TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
			Text title = data.getTitle();
			Icon icon = data.getIcon();

			if (title != null) {
				int width = TAB_WIDTH + renderer.getWidth(title);
				if (icon == null) width = Math.max(TAB_WIDTH, width - ICON_SIZE);

				if (this.width != width) {
					setSize(width, this.height);
					getParent().layout();
				}
			}

			(selected ? Painters.SELECTED_TAB : Painters.UNSELECTED_TAB).paintBackground(matrices, x, y, this);
			if (isFocused()) {
				(selected ? Painters.SELECTED_TAB_FOCUS_BORDER : Painters.UNSELECTED_TAB_FOCUS_BORDER).paintBackground(matrices, x, y, this);
			}

			int iconX = 6;

			if (title != null) {
				int titleX = (icon != null) ? iconX + ICON_SIZE + 1 : 0;
				int titleY = (height - TAB_PADDING - renderer.fontHeight) / 2 + 1;
				int width = (icon != null) ? this.width - iconX - ICON_SIZE : this.width;
				HorizontalAlignment align = (icon != null) ? HorizontalAlignment.LEFT : HorizontalAlignment.CENTER;

				int color;
				if (LibGui.isDarkMode()) {
					color = WLabel.DEFAULT_DARKMODE_TEXT_COLOR;
				} else {
					color = selected ? WLabel.DEFAULT_TEXT_COLOR : 0xEEEEEE;
				}

				ScreenDrawing.drawString(matrices, title.asOrderedText(), align, x + titleX, y + titleY, width, color);
			}

			if (icon != null) {
				icon.paint(matrices, x + iconX, y + (height - TAB_PADDING - ICON_SIZE) / 2, ICON_SIZE);
			}
		}

		@Override
		public void addTooltip(TooltipBuilder tooltip) {
			data.addTooltip(tooltip);
		}
	}

	/**
	 * Internal background painter instances for tabs.
	 */
	@Environment(EnvType.CLIENT)
	final static class Painters {
		static final BackgroundPainter SELECTED_TAB = BackgroundPainter.createLightDarkVariants(
				BackgroundPainter.createNinePatch(new Identifier("libgui", "textures/widget/tab/selected_light.png")).setTopPadding(2),
				BackgroundPainter.createNinePatch(new Identifier("libgui", "textures/widget/tab/selected_dark.png")).setTopPadding(2)
		);

		static final BackgroundPainter UNSELECTED_TAB = BackgroundPainter.createLightDarkVariants(
				BackgroundPainter.createNinePatch(new Identifier("libgui", "textures/widget/tab/unselected_light.png")),
				BackgroundPainter.createNinePatch(new Identifier("libgui", "textures/widget/tab/unselected_dark.png"))
		);

		static final BackgroundPainter SELECTED_TAB_FOCUS_BORDER = BackgroundPainter.createNinePatch(new Identifier("libgui", "textures/widget/tab/focus.png")).setTopPadding(2);
		static final BackgroundPainter UNSELECTED_TAB_FOCUS_BORDER = BackgroundPainter.createNinePatch(new Identifier("libgui", "textures/widget/tab/focus.png"));
	}
}
