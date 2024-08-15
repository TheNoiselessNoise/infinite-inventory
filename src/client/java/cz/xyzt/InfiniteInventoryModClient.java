package cz.xyzt;

import net.minecraft.client.MinecraftClient;

import cz.xyzt.network.OpenInfiniteInventoryPacket;
import cz.xyzt.network.SyncInfiniteInventoryPacket;
import cz.xyzt.network.UpdateInfiniteInventoryPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class InfiniteInventoryModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(InfiniteInventoryMod.INFINITE_INVENTORY_SCREEN_HANDLER, InfiniteInventoryScreen::new);

		// open inventory

		PayloadTypeRegistry.playS2C().register(OpenInfiniteInventoryPacket.ID, OpenInfiniteInventoryPacket.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(OpenInfiniteInventoryPacket.ID, (payload, context) -> {
			context.client().execute(() -> {
				MinecraftClient client = context.client();
				InfiniteInventoryMod.openInventory(client.player);
			});
		});

		// sync inventory

		PayloadTypeRegistry.playS2C().register(SyncInfiniteInventoryPacket.ID, SyncInfiniteInventoryPacket.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(SyncInfiniteInventoryPacket.ID, (payload, context) -> {
			context.client().execute(() -> {
				MinecraftClient client = context.client();

				if (client.player.currentScreenHandler instanceof InfiniteInventoryScreenHandler) {
					InfiniteInventoryScreenHandler handler = (InfiniteInventoryScreenHandler) client.player.currentScreenHandler;
                    handler.getInventory().syncWithClient(payload.items());
				}
			});
		});

		// update inventory

		PayloadTypeRegistry.playS2C().register(UpdateInfiniteInventoryPacket.ID, UpdateInfiniteInventoryPacket.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(UpdateInfiniteInventoryPacket.ID, (payload, context) -> {
			context.client().execute(() -> {
				MinecraftClient client = context.client();

				if (client.currentScreen instanceof InfiniteInventoryScreen) {
					InfiniteInventoryScreen screen = (InfiniteInventoryScreen) client.currentScreen;
					screen.onUpdate();
				}
			});
		});
	}
}