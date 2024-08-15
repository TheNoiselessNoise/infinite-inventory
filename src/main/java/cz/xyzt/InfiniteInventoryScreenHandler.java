package cz.xyzt;

import cz.xyzt.network.SyncInfiniteInventoryPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class InfiniteInventoryScreenHandler extends ScreenHandler {
    private final InfiniteInventory inventory;
    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    private final PlayerEntity player;
    private final InfiniteInventoryData data;

    private void debugLog(String message) {
        InfiniteInventoryMod.LOGGER.info("----- [InfiniteInventoryScreenHandler] -----");
        InfiniteInventoryMod.LOGGER.info(message);
        InfiniteInventoryMod.LOGGER.info("--------------------------------------------");
    }

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

    private boolean canCombine(ItemStack stack1, ItemStack stack2) {
        return !stack1.isEmpty() && !stack2.isEmpty() && 
            stack1.isOf(stack2.getItem()) && 
            ItemStack.areItemsEqual(stack1, stack2);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }

        Slot slot = this.slots.get(slotIndex);
        ItemStack cursorStack = getCursorStack();

        switch (actionType) {
            case PICKUP:
                if (slot.hasStack()) {
                    if (cursorStack.isEmpty()) {
                        if (button == 0) {
                            // Left click: Pick up the entire stack
                            setCursorStack(slot.getStack().copy());
                            slot.setStack(ItemStack.EMPTY);
                        } else if (button == 1) {
                            // Right click: Split the stack
                            ItemStack slotStack = slot.getStack();
                            int amount = (slotStack.getCount() + 1) / 2; // Round up division
                            ItemStack splitStack = slotStack.copy();
                            splitStack.setCount(amount);
                            setCursorStack(splitStack);
                            slotStack.decrement(amount);
                            if (slotStack.isEmpty()) {
                                slot.setStack(ItemStack.EMPTY);
                            }
                        }
                    } else if (canCombine(cursorStack, slot.getStack())) {
                        // Combine stacks (left-click logic remains the same)
                        if (button == 0) {
                            // Left click: try to pick up the entire stack
                            int amount = Math.min(slot.getStack().getCount(), cursorStack.getMaxCount() - cursorStack.getCount());
                            cursorStack.increment(amount);
                            slot.getStack().decrement(amount);
                            if (slot.getStack().isEmpty()) {
                                slot.setStack(ItemStack.EMPTY);
                            }
                        } else if (button == 1) {
                            // Right click: place one item from cursor to slot
                            if (slot.getStack().getCount() < slot.getStack().getMaxCount()) {
                                slot.getStack().increment(1);
                                cursorStack.decrement(1);
                                if (cursorStack.isEmpty()) {
                                    setCursorStack(ItemStack.EMPTY);
                                }
                            }
                        }
                    }
                } else if (!cursorStack.isEmpty()) {
                    // Place item in empty slot
                    if (button == 0) {
                        // Left click: place entire stack
                        slot.setStack(cursorStack.copy());
                        setCursorStack(ItemStack.EMPTY);
                    } else if (button == 1) {
                        // Right click: place one item
                        ItemStack oneItem = cursorStack.copy();
                        oneItem.setCount(1);
                        slot.setStack(oneItem);
                        cursorStack.decrement(1);
                        if (cursorStack.isEmpty()) {
                            setCursorStack(ItemStack.EMPTY);
                        }
                    }
                }
                break;
            case QUICK_MOVE:
                if (slot.hasStack()) {
                    ItemStack movedStack = this.quickMove(player, slotIndex);
                    if (!movedStack.isEmpty()) {
                        // The quickMove was successful, update the slot
                        slot.setStack(ItemStack.EMPTY);
                    }
                }
                break;
            default:
                super.onSlotClick(slotIndex, button, actionType, player);
                break;
        }

        // Sync the inventory with the client
        World world = player.getWorld();
        if (!world.isClient) {
            syncWithClients();
        }

        sendInventorySync();
    }

    private void syncWithClients() {
        for (int i = 0; i < this.slots.size(); i++) {
            ItemStack stack = this.slots.get(i).getStack();
            for (ServerPlayerEntity player : ((ServerWorld) this.player.getWorld()).getPlayers()) {
                player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(this.syncId, this.nextRevision(), i, stack));
            }
        }
        // Also sync the cursor stack
        ((ServerPlayerEntity) player).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-1, this.nextRevision(), -1, this.getCursorStack()));
    }

    public void setSlot(int index, ItemStack stack) {
        if (index >= 0 && index < this.slots.size()) {
            this.slots.get(index).setStack(stack);
        }
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
            } else {
                boolean inserted = false;
                for (int i = 0; i < this.inventory.size(); i++) {
                    if (this.insertItem(slotStack, i, i + 1, false)) {
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    // If the item couldn't be inserted into existing slots, add a new slot
                    this.inventory.addItemDirect(slotStack.copy());
                    slotStack.setCount(0);
                }
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        sendInventorySync();

        return itemStack;
    }

    public InfiniteInventory getInventory() {
        return inventory;
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

    private void sendInventorySync() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            SyncInfiniteInventoryPacket syncPacket = new SyncInfiniteInventoryPacket(inventory.getAllItems());
            ServerPlayNetworking.send(serverPlayer, syncPacket);
        }
    }
}