package cz.xyzt.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cz.xyzt.InfiniteInventoryScreen;
import cz.xyzt.network.OpenInfiniteInventoryPacket;

@Mixin(Keyboard.class)
public class InventoryKeyMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && key == 73 && action == 1) { // 73 is the key code for 'I'
            Screen currentScreen = client.currentScreen;
            if (currentScreen == null && !(currentScreen instanceof InfiniteInventoryScreen) && client.player.getInventory().size() > 0) {
                ClientPlayNetworking.send(new OpenInfiniteInventoryPacket());
                ci.cancel();
            }
        }
    }
}