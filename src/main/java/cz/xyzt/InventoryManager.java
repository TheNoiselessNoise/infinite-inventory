package cz.xyzt;

import cz.xyzt.network.SyncInfiniteInventoryPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class InventoryManager extends PersistentState {
    private final InfiniteInventory inventory;
    private static final String DATA_NAME = "infinite_inventory";
    private static InventoryManager instance;

    public InventoryManager() {
        this.inventory = new InfiniteInventory(this::markDirty);
    }

    public InventoryManager(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        this();
        InfiniteInventoryMod.LOGGER.warn("Reading inventory from NBT");
        this.inventory.readNbt(nbt.getCompound("Inventory"), registryLookup);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        InfiniteInventoryMod.LOGGER.warn("Writing inventory to NBT");
        nbt.put("Inventory", this.inventory.writeNbt(new NbtCompound(), registryLookup));
        return nbt;
    }

    public static final PersistentState.Type<InventoryManager> TYPE = new PersistentState.Type<>(
        InventoryManager::new,
        (nbt, registryLookup) -> new InventoryManager(nbt, registryLookup),
        null // or provide a data fixer if needed
    );

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public static InventoryManager getServerState(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("Server is null when trying to get InventoryManager state");
        }
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        instance = persistentStateManager.getOrCreate(TYPE, DATA_NAME);
        return instance;
    }

    public InfiniteInventory getInventory() {
        return inventory;
    }

    public void openInventoryFor(ServerPlayerEntity player) {
        // Open the inventory screen for the player
        // This might involve sending a packet to the client to open the screen
        // and then syncing the initial inventory state
        syncWithClient(player);
    }

    public void syncWithClient(ServerPlayerEntity player) {
        SyncInfiniteInventoryPacket packet = new SyncInfiniteInventoryPacket(inventory.getAllItems());
        ServerPlayNetworking.send(player, packet);
    }

    public static void saveInventory(MinecraftServer server) {
        if (server == null) {
            InfiniteInventoryMod.LOGGER.warn("Attempted to save inventory with null server");
            return;
        }
        InventoryManager manager = getServerState(server);
        manager.markDirty();
        InfiniteInventoryMod.LOGGER.warn("Inventory marked for saving");
    }
}