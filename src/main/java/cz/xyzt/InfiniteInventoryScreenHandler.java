package cz.xyzt;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class InfiniteInventoryScreenHandler extends ScreenHandler {
    private final InfiniteInventory inventory;
    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    private final PlayerEntity player;
    private final InfiniteInventoryData data;

    public InfiniteInventoryScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, InfiniteInventoryData.PACKET_CODEC.decode(buf));
    }

    public InfiniteInventoryScreenHandler(int syncId, PlayerInventory playerInventory, InfiniteInventoryData data) {
        super(InfiniteInventoryMod.INFINITE_INVENTORY_SCREEN_HANDLER, syncId);
        this.player = playerInventory.player;
        this.data = data;

        World world = player.getWorld();
        if (!world.isClient) {
            InventoryManager inventoryManager = getInventoryManager((ServerWorld) world);
            this.inventory = inventoryManager.getInventory();
        } else {
            // Create a dummy inventory for the client
            this.inventory = new InfiniteInventory(null);
        }

        // Add slots for the infinite inventory
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add slots for the player inventory
        int playerInventoryStartY = 18 + ROWS * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryStartY + row * 18));
            }
        }

        // Add slots for the player hotbar
        int hotbarY = playerInventoryStartY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            itemStack = slotStack.copy();
            if (index < this.inventory.size()) {
                if (!this.insertItem(slotStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(slotStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return itemStack;
    }

    public InfiniteInventory getInventory() {
        return inventory;
    }

    public void updateSlots() {
        for (int i = 0; i < inventory.size(); i++) {
            this.slots.get(i).setStack(inventory.getStack(i));
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        World world = player.getWorld();
        if (!world.isClient) {
            saveInventory((ServerWorld) world);
        }
    }

    private InventoryManager getInventoryManager(World world) {
        if (world.isClient) {
            throw new IllegalStateException("Attempted to get InventoryManager on client side");
        }
        
        if (world instanceof ServerWorld serverWorld) {
            MinecraftServer server = serverWorld.getServer();
            return InventoryManager.getServerState(server);
        } else {
            throw new IllegalStateException("World is neither client nor server");
        }
    }

    private void saveInventory(ServerWorld world) {
        MinecraftServer server = world.getServer();
        InventoryManager.saveInventory(server);
    }
}