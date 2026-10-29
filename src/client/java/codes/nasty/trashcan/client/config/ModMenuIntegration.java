package codes.nasty.trashcan.client.config;

import codes.nasty.trashcan.TrashCan;
import codes.nasty.trashcan.TrashCanConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/** The config button in Mod Menu's mod list. Mod Menu is optional; without it this class is never loaded. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ModMenuIntegration::createScreen;
	}

	public static Screen createScreen(Screen parent) {
		return new ConfigScreen<>(TrashCan.MOD_ID, parent, TrashCanConfig.get().copy(), new TrashCanConfig(), TrashCanConfig::update)
			.section("button")
			.toggle("requireConfirmation", c -> c.requireConfirmation, (c, v) -> c.requireConfirmation = v)
			.integer("confirmSeconds", 1, 30, c -> c.confirmSeconds, (c, v) -> c.confirmSeconds = v)
			.section("deleting")
			.toggle("deleteOnClose", c -> c.deleteOnClose, (c, v) -> c.deleteOnClose = v)
			.toggle("deleteEffects", c -> c.deleteEffects, (c, v) -> c.deleteEffects = v)
			.toggle("logDeletions", c -> c.logDeletions, (c, v) -> c.logDeletions = v);
	}
}
