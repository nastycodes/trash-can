package codes.nasty.trashcan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings, stored in {@code config/trashcan.json}.
 * Unknown keys are ignored and missing keys fall back to the defaults below,
 * so the file is rewritten on every successful load to pick up new options.
 * <p>
 * {@link #requireConfirmation} and {@link #confirmSeconds} are read by the client's delete button, so every player
 * decides them in their own config. The other options are read by the server (or the singleplayer world).
 */
public final class TrashCanConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(TrashCan.MOD_ID + ".json");
	private static TrashCanConfig instance = new TrashCanConfig();

	/** Client: the first click on "Delete" only arms the button ("Sure?"), a second click deletes. */
	public boolean requireConfirmation = true;
	/** Client: how long the armed button waits for the second click. */
	public int confirmSeconds = 3;
	/** Server: smoke and a hiss when a trash can is emptied. */
	public boolean deleteEffects = true;
	/** Server: writes every deletion to the log, with player, item count and position. */
	public boolean logDeletions = true;
	/**
	 * Server: the contents are deleted automatically when the last player closes the trash can.
	 * Hoppers can still fill it; nothing is deleted while somebody has it open.
	 */
	public boolean deleteOnClose = false;

	public static TrashCanConfig get() {
		return instance;
	}

	/** Replaces the active settings, e.g. from game tests. The values are checked like a loaded file. */
	public static void apply(TrashCanConfig config) {
		config.sanitize();
		instance = config;
	}

	/** Loads the config at startup; a broken file is reported and the defaults are used instead. */
	public static void load() {
		try {
			reload();
		} catch (IOException | JsonParseException e) {
			TrashCan.LOGGER.error("Could not read {}, using default settings", PATH, e);
			apply(new TrashCanConfig());
		}
	}

	public static void reload() throws IOException {
		TrashCanConfig config = new TrashCanConfig();
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				TrashCanConfig parsed = GSON.fromJson(reader, TrashCanConfig.class);
				if (parsed != null) {
					config = parsed;
				}
			}
		}
		apply(config);
		save(config);
	}

	/** Activates settings changed in the config screen and writes them to the file. */
	public static void update(TrashCanConfig config) {
		apply(config);
		save(config);
	}

	/** A deep copy, for the config screen to change. */
	public TrashCanConfig copy() {
		return GSON.fromJson(GSON.toJsonTree(this), TrashCanConfig.class);
	}

	private static void save(TrashCanConfig config) {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			TrashCan.LOGGER.error("Could not write {}", PATH, e);
		}
	}

	/** Ticks the armed delete button waits for the second click. */
	public int confirmTicks() {
		return this.confirmSeconds * 20;
	}

	private void sanitize() {
		confirmSeconds = Math.max(1, Math.min(30, confirmSeconds));
	}
}
