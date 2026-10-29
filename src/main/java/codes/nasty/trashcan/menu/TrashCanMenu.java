package codes.nasty.trashcan.menu;

import codes.nasty.trashcan.TrashCan;
import codes.nasty.trashcan.block.TrashCanBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;

/**
 * The single chest menu under its own menu type, so the client opens the screen with the delete button.
 * The button arrives through the vanilla container button packet, which only reaches {@link #clickMenuButton}
 * after the server checked that the player still has this menu open and is not a spectator.
 */
public class TrashCanMenu extends ChestMenu {
	public static final int DELETE_BUTTON = 0;

	/** Client side: the slots are filled by the server. */
	public TrashCanMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(TrashCanBlockEntity.SIZE));
	}

	public TrashCanMenu(int containerId, Inventory inventory, Container container) {
		super(TrashCan.TRASH_CAN_MENU, containerId, inventory, container, TrashCanBlockEntity.ROWS);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		return id == DELETE_BUTTON
			&& this.getContainer() instanceof TrashCanBlockEntity trashCan
			&& trashCan.deleteContents(player) > 0;
	}
}
