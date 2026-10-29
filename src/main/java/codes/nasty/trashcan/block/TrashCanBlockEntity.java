package codes.nasty.trashcan.block;

import codes.nasty.trashcan.TrashCan;
import codes.nasty.trashcan.TrashCanConfig;
import codes.nasty.trashcan.menu.TrashCanMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds 27 stacks like a single chest, so hoppers and comparators work with it as usual.
 * Nothing is lost until a player presses the delete button, which ends up in {@link #deleteContents},
 * or, with {@link TrashCanConfig#deleteOnClose}, until the last player closes the can.
 */
public class TrashCanBlockEntity extends BaseContainerBlockEntity {
	public static final int ROWS = 3;
	public static final int SIZE = ROWS * 9;
	private static final Component DEFAULT_NAME = Component.translatable("container.trashcan.trash_can");

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	/** Name of the player who closed the can last, for the log of {@link TrashCanConfig#deleteOnClose}. */
	private String lastUser = "?";
	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			TrashCanBlockEntity.this.playSound(SoundEvents.COPPER_CHEST_OPEN);
			TrashCanBlockEntity.this.updateBlockState(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			TrashCanBlockEntity.this.playSound(SoundEvents.COPPER_CHEST_CLOSE);
			TrashCanBlockEntity.this.updateBlockState(state, false);
			// Only called once the last player closed the can, so nobody is looking while it empties.
			if (TrashCanConfig.get().deleteOnClose) {
				TrashCanBlockEntity.this.delete(TrashCanBlockEntity.this.lastUser, true);
			}
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof TrashCanMenu menu && menu.getContainer() == TrashCanBlockEntity.this;
		}
	};

	public TrashCanBlockEntity(BlockPos pos, BlockState state) {
		super(TrashCan.TRASH_CAN_BLOCK_ENTITY, pos, state);
	}

	/** Throws everything inside away for good and returns how many items that were. */
	public int deleteContents(Player player) {
		return this.delete(player.getName().getString(), false);
	}

	private int delete(String player, boolean onClose) {
		int count = 0;
		for (ItemStack stack : this.items) {
			count += stack.getCount();
		}
		if (count == 0) {
			return 0;
		}

		this.clearContent();
		this.setChanged();
		TrashCanConfig config = TrashCanConfig.get();
		if (config.deleteEffects && this.level instanceof ServerLevel level) {
			BlockPos pos = this.getBlockPos();
			level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
		}
		if (config.logDeletions) {
			String where = this.getBlockPos().toShortString();
			if (onClose) {
				TrashCan.LOGGER.info("The trash can at {} deleted {} items when {} closed it", where, count, player);
			} else {
				TrashCan.LOGGER.info("{} deleted {} items in the trash can at {}", player, count, where);
			}
		}
		return count;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new TrashCanMenu(containerId, inventory, this);
	}

	@Override
	public void startOpen(ContainerUser user) {
		if (!this.remove && !user.getLivingEntity().isSpectator()) {
			this.openersCounter.incrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!this.remove && !user.getLivingEntity().isSpectator()) {
			this.lastUser = user.getLivingEntity().getName().getString();
			this.openersCounter.decrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
		}
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
	}

	/** Closes the lid once nobody is looking inside anymore, e.g. after a player walked away. */
	public void recheckOpen() {
		if (!this.remove) {
			this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
		}
	}

	private void updateBlockState(BlockState state, boolean open) {
		this.level.setBlock(this.getBlockPos(), state.setValue(TrashCanBlock.OPEN, open), Block.UPDATE_ALL);
	}

	private void playSound(SoundEvent sound) {
		BlockPos pos = this.getBlockPos();
		this.level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, sound, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
	}
}
