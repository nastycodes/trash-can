package codes.nasty.trashcan.client;

import codes.nasty.trashcan.TrashCanConfig;
import codes.nasty.trashcan.menu.TrashCanMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The vanilla single chest screen with a delete button in the title row. The first click only arms the
 * button ("Sure?"); a second click within {@link TrashCanConfig#confirmSeconds} sends the delete request to the
 * server. With {@link TrashCanConfig#requireConfirmation} off, the first click deletes right away. Both settings
 * come from this client's own config.
 */
public class TrashCanScreen extends ContainerScreen {
	/** Button bounds in GUI coordinates: right-aligned with the slot grid, centered on the title. */
	public static final int BUTTON_WIDTH = 52;
	public static final int BUTTON_HEIGHT = 12;
	public static final int BUTTON_X = 176 - 7 - BUTTON_WIDTH;
	public static final int BUTTON_Y = 4;

	private static final Component DELETE = Component.translatable("gui.trashcan.delete");
	private static final Component CONFIRM = Component.translatable("gui.trashcan.delete.confirm").withStyle(ChatFormatting.RED);
	private static final Component EMPTY_TOOLTIP = Component.translatable("gui.trashcan.delete.empty");
	private static final Component CONFIRM_TOOLTIP = Component.translatable("gui.trashcan.delete.confirm.tooltip");

	private Button deleteButton;
	private int confirmTicks;
	/** What the button shows right now, so the tooltip is only rebuilt when it changes. */
	private int shownCount = -1;
	private boolean shownConfirm;

	public TrashCanScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		this.deleteButton = this.addRenderableWidget(Button.builder(DELETE, button -> this.onDeletePressed())
			.bounds(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());
		this.shownCount = -1;
		this.updateButton();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (this.confirmTicks > 0) {
			this.confirmTicks--;
		}
		this.updateButton();
	}

	private void onDeletePressed() {
		TrashCanConfig config = TrashCanConfig.get();
		if (this.confirmTicks > 0 || !config.requireConfirmation) {
			this.confirmTicks = 0;
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, TrashCanMenu.DELETE_BUTTON);
		} else {
			this.confirmTicks = config.confirmTicks();
		}
		this.updateButton();
	}

	private void updateButton() {
		int count = this.itemCount();
		if (count == 0) {
			this.confirmTicks = 0;
		}
		boolean confirm = this.confirmTicks > 0;
		if (count == this.shownCount && confirm == this.shownConfirm) {
			return;
		}
		this.shownCount = count;
		this.shownConfirm = confirm;

		this.deleteButton.active = count > 0;
		this.deleteButton.setMessage(confirm ? CONFIRM : DELETE);
		Component tooltip = count == 0 ? EMPTY_TOOLTIP
			: confirm ? CONFIRM_TOOLTIP
			: Component.translatable("gui.trashcan.delete.tooltip", count);
		this.deleteButton.setTooltip(Tooltip.create(tooltip));
	}

	/** Items in the trash can slots as the client sees them; the player's own inventory does not count. */
	private int itemCount() {
		int count = 0;
		for (ItemStack stack : this.menu.getContainer()) {
			count += stack.getCount();
		}
		return count;
	}

	public boolean isConfirming() {
		return this.confirmTicks > 0;
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// A long custom name (renamed in an anvil) is cut off in front of the button instead of running under it.
		int maxWidth = BUTTON_X - 3 - this.titleLabelX;
		FormattedCharSequence title = this.font.width(this.title) <= maxWidth
			? this.title.getVisualOrderText()
			: Language.getInstance().getVisualOrder(FormattedText.composite(
				this.font.substrByWidth(this.title, maxWidth - this.font.width(CommonComponents.ELLIPSIS)),
				CommonComponents.ELLIPSIS));
		graphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
	}
}
