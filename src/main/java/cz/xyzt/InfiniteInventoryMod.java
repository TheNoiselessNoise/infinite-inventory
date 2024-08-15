package cz.xyzt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.xyzt.network.OpenInfiniteInventoryPacket;
import cz.xyzt.network.SyncInfiniteInventoryPacket;

public class InfiniteInventoryMod implements ModInitializer {
	public static final String MOD_ID = "infinite_inventory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier INFINITE_INVENTORY_ID = Identifier.of(MOD_ID, "infinite_inventory");
    public static ExtendedScreenHandlerType<InfiniteInventoryScreenHandler, InfiniteInventoryData> INFINITE_INVENTORY_SCREEN_HANDLER;
	
	@Override
	public void onInitialize() {
		LOGGER.info("#############################################");
		LOGGER.info("#############################################");
		LOGGER.info("#### Initializing Infinite Inventory Mod ####");
		LOGGER.info("#############################################");
		LOGGER.info("#############################################");

		
		INFINITE_INVENTORY_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(
			InfiniteInventoryScreenHandler::new,
			InfiniteInventoryData.PACKET_CODEC
		);
        Registry.register(Registries.SCREEN_HANDLER, INFINITE_INVENTORY_ID, INFINITE_INVENTORY_SCREEN_HANDLER);

		PayloadTypeRegistry.playC2S().register(OpenInfiniteInventoryPacket.ID, OpenInfiniteInventoryPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
            OpenInfiniteInventoryPacket.ID,
            (packet, context) -> {
				InfiniteInventoryMod.openInventory(context.player());
            }
        );

        PayloadTypeRegistry.playC2S().register(SyncInfiniteInventoryPacket.ID, SyncInfiniteInventoryPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
            SyncInfiniteInventoryPacket.ID, 
            (packet, context) -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = player.getServer();

                InventoryManager inventoryManager = InventoryManager.getServerState(server);
                inventoryManager.openInventoryFor(player);
            }
        );

		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
	}

    public static void openInventory(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            serverPlayer.openHandledScreen(new InfiniteInventoryFactory("Infinite Inventory"));
        }
    }

	private void onServerStarted(MinecraftServer server) {
        InventoryManager.getServerState(server);
        LOGGER.info("Infinite Inventory initialized successfully");
    }

    private void onServerStopping(MinecraftServer server) {
        InventoryManager.saveInventory(server);
        LOGGER.info("Infinite Inventory saved successfully");
    }

    public static InfiniteInventory getInfiniteInventory() {
        return InventoryManager.getInstance().getInventory();
    }
}