package cz.xyzt.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cz.xyzt.InfiniteInventoryMod;

// @Mixin(MinecraftServer.class)
// public class InventoryMixin {
	// @Inject(at = @At("HEAD"), method = "loadWorld")
	// private void init(CallbackInfo info) {
	// 	// This code is injected into the start of MinecraftServer.loadWorld()V
	// }
// }

@Mixin(PlayerEntity.class)
public class InventoryMixin {
	@Inject(method = "openHandledScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenInventory(NamedScreenHandlerFactory factory, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (player instanceof ServerPlayerEntity && factory == player.playerScreenHandler) {
            InfiniteInventoryMod.openInventory((ServerPlayerEntity) player);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}