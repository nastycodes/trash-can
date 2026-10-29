package codes.nasty.trashcan.test;

import codes.nasty.trashcan.TrashCan;
import codes.nasty.trashcan.TrashCanConfig;
import codes.nasty.trashcan.block.TrashCanBlock;
import codes.nasty.trashcan.block.TrashCanBlockEntity;
import codes.nasty.trashcan.client.TrashCanScreen;
import codes.nasty.trashcan.client.config.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.ModMenu;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * End-to-end check in a real client (English, survival, 1920x1080): opens the can with
 * a right click, moves items in, deletes them with the two-step button and verifies everything on the server. Also
 * checks that a stale confirmation does not delete, that contents survive saving and drop when the can is broken,
 * the options "ask before deleting" off and "delete when closed" on, and the config screen behind Mod Menu's button.
 * Run with {@code gradlew runClientGameTest}; screenshots land in build/run/clientGameTest/screenshots.
 */
public class TrashCanClientGameTest implements FabricClientGameTest {
	private static final int IMAGE_WIDTH = 176;
	private static final int IMAGE_HEIGHT = 114 + TrashCanBlockEntity.ROWS * 18;
	/** Menu slots: the trash can slots come first, then the 27 inventory slots, then the hotbar. */
	private static final int HOTBAR_START = TrashCanBlockEntity.SIZE + 27;

	/** Where {@link #prepare} put the can; set on the server thread before anything reads it. */
	private static volatile BlockPos pos;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> client.getLanguageManager().setSelected("en_us"));
		context.runOnClient(client -> client.reloadResourcePacks());
		context.waitFor(client -> client.getOverlay() == null);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");

			server.runOnServer(TrashCanClientGameTest::checkData);
			server.runOnServer(TrashCanClientGameTest::prepare);
			context.waitTicks(10);

			// Right-click the can with dirt in the hand: it opens like a chest instead of placing the dirt.
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(TrashCanScreen.class);
			context.waitTicks(5);
			assertTrue(server.computeOnServer(s -> state(s).getValue(TrashCanBlock.OPEN)), "lid is open while the GUI is open");
			assertTrue(!button(context).active, "delete button is inactive while the can is empty");
			context.takeScreenshot("trashcan_01_empty");

			quickMove(context, HOTBAR_START);
			quickMove(context, HOTBAR_START + 1);
			quickMove(context, HOTBAR_START + 2);
			context.waitTicks(5);
			assertEquals(64 + 1 + 16, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "items in the can");
			assertTrue(server.computeOnServer(s -> state(s).getAnalogOutputSignal(s.overworld(), pos, Direction.NORTH)) > 0, "comparator signal of a filled can");
			hoverGui(context, TrashCanScreen.BUTTON_X + TrashCanScreen.BUTTON_WIDTH / 2, TrashCanScreen.BUTTON_Y + TrashCanScreen.BUTTON_HEIGHT / 2);
			context.waitTicks(5);
			assertTrue(button(context).active, "delete button is active once something is inside");
			context.takeScreenshot("trashcan_02_filled");

			// The first click only asks for confirmation.
			context.getInput().pressMouse(0);
			context.waitTicks(3);
			assertTrue(confirming(context), "button asks for confirmation after the first click");
			assertEquals(81, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "items in the can after the first click");
			context.takeScreenshot("trashcan_03_confirm");

			// The second click deletes everything for good.
			context.getInput().pressMouse(0);
			context.waitTicks(5);
			assertEquals(0, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "items in the can after deleting");
			assertEquals(0, server.computeOnServer(s -> count(player(s).getInventory(), Items.DIRT)), "dirt anywhere in the player's inventory");
			assertEquals(0, server.computeOnServer(s -> count(player(s).getInventory(), Items.DIAMOND_SWORD)), "sword anywhere in the player's inventory");
			assertTrue(!confirming(context), "confirmation is reset after deleting");
			assertTrue(!button(context).active, "delete button is inactive again");
			context.takeScreenshot("trashcan_04_deleted");

			// A confirmation that ran out does not delete: the next click only asks again.
			server.runOnServer(s -> player(s).getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 32)));
			context.waitTicks(3);
			quickMove(context, HOTBAR_START + 3);
			context.waitTicks(3);
			assertEquals(32, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "cobblestone in the can");
			context.getInput().pressMouse(0);
			context.waitTicks(TrashCanConfig.get().confirmTicks() + 5);
			assertTrue(!confirming(context), "confirmation runs out");
			context.getInput().pressMouse(0);
			context.waitTicks(5);
			assertTrue(confirming(context), "click after the timeout asks again");
			assertEquals(32, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "cobblestone after a click on a stale confirmation");

			// Closing keeps the contents and closes the lid.
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitForScreen(null);
			context.waitTicks(10);
			assertTrue(!server.computeOnServer(s -> state(s).getValue(TrashCanBlock.OPEN)), "lid closes with the GUI");
			assertEquals(32, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "cobblestone after closing");

			server.runOnServer(TrashCanClientGameTest::checkSaveAndLoad);
			server.runOnServer(TrashCanClientGameTest::checkBreakDropsContents);

			// Closed and open cans next to each other for a look at both models, without the HUD.
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockState facingPlayer = TrashCan.TRASH_CAN.defaultBlockState().setValue(TrashCanBlock.FACING, Direction.WEST);
				level.setBlockAndUpdate(pos.north(), facingPlayer);
				level.setBlockAndUpdate(pos, facingPlayer.setValue(TrashCanBlock.OPEN, true));
				level.setBlockAndUpdate(pos.south(), facingPlayer);
				ServerPlayer player = player(s);
				player.connection.teleport(pos.getX() - 2.6, pos.getY() + 0.4, pos.getZ() + 0.5, -90.0F, 25.0F);
			});
			context.runOnClient(client -> {
				client.options.hideGui = true;
				client.gui.getChat().clearMessages(false);
			});
			context.waitTicks(40);
			context.takeScreenshot("trashcan_05_blocks");
			context.runOnClient(client -> client.options.hideGui = false);
			server.runOnServer(s -> s.overworld().setBlockAndUpdate(pos, TrashCan.TRASH_CAN.defaultBlockState().setValue(TrashCanBlock.FACING, Direction.WEST)));

			checkWithoutConfirmation(context, server);
			checkDeleteOnClose(context, server);
			checkConfigScreen(context, server);
			TrashCan.LOGGER.info("[gametest] all checks passed");
		}
	}

	/** With "ask before deleting" off, the first click deletes. */
	private static void checkWithoutConfirmation(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(s -> {
			changeConfig(config -> config.requireConfirmation = false);
			placeCanInFront(s);
		});
		context.waitTicks(10);
		server.runOnServer(s -> blockEntity(s).setItem(0, new ItemStack(Items.STONE, 10)));
		openCan(context);
		hoverGui(context, TrashCanScreen.BUTTON_X + TrashCanScreen.BUTTON_WIDTH / 2, TrashCanScreen.BUTTON_Y + TrashCanScreen.BUTTON_HEIGHT / 2);
		context.waitTicks(3);
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		assertEquals(0, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "items after one click without confirmation");
		assertTrue(!confirming(context), "no confirmation step without requireConfirmation");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		server.runOnServer(s -> TrashCanConfig.update(new TrashCanConfig()));
	}

	/** With "delete when closed" on, closing empties the can, but hoppers can still fill it while it is closed. */
	private static void checkDeleteOnClose(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(s -> {
			changeConfig(config -> config.deleteOnClose = true);
			blockEntity(s).setItem(0, new ItemStack(Items.GRAVEL, 5));
		});
		context.waitTicks(10);
		assertEquals(5, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "items put in while closed stay");
		openCan(context);
		context.waitTicks(10);
		assertEquals(5, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "nothing is deleted while the can is open");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
		context.waitTicks(5);
		assertEquals(0, server.computeOnServer(TrashCanClientGameTest::itemsInCan), "closing deleted the contents");
		assertTrue(!server.computeOnServer(s -> state(s).getValue(TrashCanBlock.OPEN)), "lid closes after deleting on close");
		server.runOnServer(s -> TrashCanConfig.update(new TrashCanConfig()));
	}

	/** Opens the settings the way Mod Menu's config button does, changes two options and saves. */
	private static void checkConfigScreen(ClientGameTestContext context, TestServerContext server) {
		context.setScreen(() -> ModMenu.getConfigScreen(TrashCan.MOD_ID, null));
		context.waitForScreen(ConfigScreen.class);
		context.waitTicks(5);
		EditBox seconds = widget(context, EditBox.class, "config.trashcan.confirmSeconds");
		hoverWidget(context, seconds);
		context.takeScreenshot("trashcan_06_config");

		// Out of range turns the field red and blocks saving.
		context.runOnClient(client -> seconds.setValue("99"));
		context.waitTicks(2);
		assertTrue(!widget(context, Button.class, "gui.done").active, "Done is blocked while a field is invalid");
		context.runOnClient(client -> seconds.setValue("5"));
		context.waitTicks(2);
		assertTrue(widget(context, Button.class, "gui.done").active, "Done works again with a valid number");
		// The only option that is off by default: "delete when closed".
		clickWidget(context, widget(context, CycleButton.class, "options.off"));
		context.clickScreenButton("gui.done");
		context.waitForScreen(null);
		context.waitTicks(10);

		assertTrue(server.computeOnServer(s -> TrashCanConfig.get().confirmSeconds) == 5, "confirmation time saved through the screen");
		assertTrue(server.computeOnServer(s -> TrashCanConfig.get().deleteOnClose), "delete when closed switched on through the screen");
		String file = readConfigFile();
		assertTrue(file.contains("\"confirmSeconds\": 5") && file.contains("\"deleteOnClose\": true"), "the screen wrote config/trashcan.json");

		// Esc with unsaved changes asks first; discarding keeps the saved values.
		context.setScreen(() -> ModMenu.getConfigScreen(TrashCan.MOD_ID, null));
		context.waitForScreen(ConfigScreen.class);
		EditBox reopened = widget(context, EditBox.class, "config.trashcan.confirmSeconds");
		assertTrue(context.computeOnClient(client -> reopened.getValue()).equals("5"), "the screen shows the saved value");
		context.runOnClient(client -> reopened.setValue("10"));
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(ConfirmScreen.class);
		context.clickScreenButton("config.trashcan.ui.discard.yes");
		context.waitForScreen(null);
		context.waitTicks(5);
		assertTrue(server.computeOnServer(s -> TrashCanConfig.get().confirmSeconds) == 5, "discarded changes are not saved");

		server.runOnServer(s -> TrashCanConfig.update(new TrashCanConfig()));
	}

	/** Changes the active settings like the config screen does, starting from the current ones. */
	private static void changeConfig(Consumer<TrashCanConfig> change) {
		TrashCanConfig config = TrashCanConfig.get().copy();
		change.accept(config);
		TrashCanConfig.update(config);
	}

	/** Puts a fresh can back at {@link #pos} and turns the player towards it, like {@link #prepare}. */
	private static void placeCanInFront(MinecraftServer server) {
		server.overworld().setBlockAndUpdate(pos, TrashCan.TRASH_CAN.defaultBlockState().setValue(TrashCanBlock.FACING, Direction.WEST));
		BlockPos feet = pos.west(2);
		player(server).connection.teleport(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, -90.0F, 30.0F);
	}

	/** Right-clicks the can the player looks at. */
	private static void openCan(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(TrashCanScreen.class);
		context.waitTicks(5);
	}

	private static String readConfigFile() {
		try {
			return Files.readString(FabricLoader.getInstance().getConfigDir().resolve(TrashCan.MOD_ID + ".json"));
		} catch (IOException e) {
			throw new AssertionError("config file not readable", e);
		}
	}

	/** Finds a widget of the current screen, also inside lists, by the translation key of its message. */
	private static <T extends AbstractWidget> T widget(ClientGameTestContext context, Class<T> type, String translationKey) {
		return context.computeOnClient(client -> {
			List<GuiEventListener> todo = new ArrayList<>(client.screen.children());
			while (!todo.isEmpty()) {
				GuiEventListener listener = todo.removeLast();
				if (type.isInstance(listener) && ((AbstractWidget) listener).getMessage().getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals(translationKey)) {
					return type.cast(listener);
				}
				if (listener instanceof ContainerEventHandler container) {
					todo.addAll(container.children());
				}
			}
			throw new AssertionError("no " + type.getSimpleName() + " with message " + translationKey);
		});
	}

	private static void hoverWidget(ClientGameTestContext context, AbstractWidget widget) {
		double[] cursor = context.computeOnClient(client -> {
			double scale = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
			return new double[] {(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(3);
	}

	private static void clickWidget(ClientGameTestContext context, AbstractWidget widget) {
		hoverWidget(context, widget);
		context.getInput().pressMouse(0);
		context.waitTicks(3);
	}

	private static void checkData(MinecraftServer server) {
		ResourceKey<Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, TrashCan.TRASH_CAN_ID);
		assertTrue(server.getRecipeManager().byKey(recipe).isPresent(), "crafting recipe is loaded");
		var lootTable = ResourceKey.create(Registries.LOOT_TABLE, TrashCan.id("blocks/trash_can"));
		assertTrue(server.reloadableRegistries().getLootTable(lootTable) != LootTable.EMPTY, "block loot table is loaded");
	}

	private static void prepare(MinecraftServer server) {
		ServerPlayer player = player(server);
		player.setGameMode(GameType.SURVIVAL);
		Inventory inventory = player.getInventory();
		inventory.clearContent();
		inventory.setItem(0, new ItemStack(Items.DIRT, 64));
		inventory.setItem(1, new ItemStack(Items.DIAMOND_SWORD));
		inventory.setItem(2, new ItemStack(Items.ROTTEN_FLESH, 16));
		inventory.setSelectedSlot(0);

		BlockPos feet = player.blockPosition();
		pos = feet.east(2);
		player.level().setBlockAndUpdate(pos, TrashCan.TRASH_CAN.defaultBlockState().setValue(TrashCanBlock.FACING, Direction.WEST));
		assertTrue(player.level().getBlockEntity(pos) instanceof TrashCanBlockEntity, "trash can block entity");
		// Look east at the side of the can.
		player.connection.teleport(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, -90.0F, 30.0F);
	}

	private static void checkSaveAndLoad(MinecraftServer server) {
		CompoundTag tag = blockEntity(server).saveWithFullMetadata(server.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(pos, state(server), tag, server.registryAccess());
		assertTrue(loaded instanceof TrashCanBlockEntity, "loaded block entity is a trash can");
		assertEquals(32, count((TrashCanBlockEntity) loaded, Items.COBBLESTONE), "cobblestone after saving and loading");
	}

	private static void checkBreakDropsContents(MinecraftServer server) {
		ServerLevel level = server.overworld();
		level.destroyBlock(pos, true);
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0));
		int cobblestone = 0;
		int cans = 0;
		for (ItemEntity drop : drops) {
			if (drop.getItem().is(Items.COBBLESTONE)) {
				cobblestone += drop.getItem().getCount();
			} else if (drop.getItem().is(TrashCan.TRASH_CAN_ITEM)) {
				cans += drop.getItem().getCount();
			}
			drop.discard();
		}
		assertEquals(32, cobblestone, "cobblestone dropped when the can is broken");
		assertEquals(1, cans, "the can drops itself");
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static BlockState state(MinecraftServer server) {
		return server.overworld().getBlockState(pos);
	}

	private static TrashCanBlockEntity blockEntity(MinecraftServer server) {
		return (TrashCanBlockEntity) server.overworld().getBlockEntity(pos);
	}

	private static int itemsInCan(MinecraftServer server) {
		int count = 0;
		for (ItemStack stack : blockEntity(server)) {
			count += stack.getCount();
		}
		return count;
	}

	private static int count(Container container, Item item) {
		int count = 0;
		for (ItemStack stack : container) {
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static Button button(ClientGameTestContext context) {
		return context.computeOnClient(client -> (Button) ((TrashCanScreen) client.screen).children().stream()
			.filter(child -> child instanceof Button)
			.findFirst()
			.orElseThrow());
	}

	private static boolean confirming(ClientGameTestContext context) {
		return context.computeOnClient(client -> client.screen instanceof TrashCanScreen screen && screen.isConfirming());
	}

	/** Shift-clicks a menu slot through the same path a real click takes. */
	private static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleInventoryMouseClick(
			client.player.containerMenu.containerId, slot, 0, ClickType.QUICK_MOVE, client.player));
	}

	/** Puts the cursor on a point given in the trash can GUI's own coordinates. */
	private static void hoverGui(ClientGameTestContext context, int guiX, int guiY) {
		double[] cursor = context.computeOnClient(client -> {
			var window = client.getWindow();
			double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
			int left = (window.getGuiScaledWidth() - IMAGE_WIDTH) / 2;
			int top = (window.getGuiScaledHeight() - IMAGE_HEIGHT) / 2;
			return new double[] {(left + guiX + 0.5) * scale, (top + guiY + 0.5) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static void assertEquals(int expected, int actual, String what) {
		if (expected != actual) {
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
		}
	}
}
