package codes.nasty.trashcan;

import codes.nasty.trashcan.block.TrashCanBlock;
import codes.nasty.trashcan.block.TrashCanBlockEntity;
import codes.nasty.trashcan.menu.TrashCanMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrashCan implements ModInitializer {
	public static final String MOD_ID = "trashcan";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier TRASH_CAN_ID = id("trash_can");
	private static final ResourceKey<Block> TRASH_CAN_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, TRASH_CAN_ID);
	private static final ResourceKey<Item> TRASH_CAN_ITEM_KEY = ResourceKey.create(Registries.ITEM, TRASH_CAN_ID);

	/** Metal like the hopper: needs a pickaxe, and the open lid lets light through. */
	public static final Block TRASH_CAN = Registry.register(
		BuiltInRegistries.BLOCK,
		TRASH_CAN_BLOCK_KEY,
		new TrashCanBlock(BlockBehaviour.Properties.of()
			.setId(TRASH_CAN_BLOCK_KEY)
			.mapColor(MapColor.METAL)
			.requiresCorrectToolForDrops()
			.strength(3.0F, 4.8F)
			.sound(SoundType.METAL)
			.noOcclusion())
	);

	public static final Item TRASH_CAN_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		TRASH_CAN_ITEM_KEY,
		new BlockItem(TRASH_CAN, new Item.Properties().setId(TRASH_CAN_ITEM_KEY).useBlockDescriptionPrefix())
	);

	public static final BlockEntityType<TrashCanBlockEntity> TRASH_CAN_BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		TRASH_CAN_ID,
		FabricBlockEntityTypeBuilder.create(TrashCanBlockEntity::new, TRASH_CAN).build()
	);

	public static final MenuType<TrashCanMenu> TRASH_CAN_MENU = Registry.register(
		BuiltInRegistries.MENU,
		TRASH_CAN_ID,
		new MenuType<>(TrashCanMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		TrashCanConfig.load();

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> entries.addAfter(Items.BARREL, TRASH_CAN_ITEM));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("reload").executes(context -> {
					try {
						TrashCanConfig.reload();
					} catch (Exception e) {
						context.getSource().sendFailure(Component.translatable("trashcan.command.reload_failed", e.getMessage()));
						return 0;
					}
					context.getSource().sendSuccess(() -> Component.translatable("trashcan.command.reloaded"), true);
					return 1;
				}))
		));
	}
}
