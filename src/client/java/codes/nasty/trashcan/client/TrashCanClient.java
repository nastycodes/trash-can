package codes.nasty.trashcan.client;

import codes.nasty.trashcan.TrashCan;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class TrashCanClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(TrashCan.TRASH_CAN_MENU, TrashCanScreen::new);
	}
}
