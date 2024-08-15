package cz.xyzt;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class InfiniteInventoryFactory implements ExtendedScreenHandlerFactory<InfiniteInventoryData> {
    private final InfiniteInventoryData data;

    public InfiniteInventoryFactory(String title) {
        this.data = new InfiniteInventoryData(title);
    }

    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        InfiniteInventoryData.PACKET_CODEC.encode(buf, data);
    }

    @Override
    public InfiniteInventoryData getScreenOpeningData(ServerPlayerEntity player) {
        return data;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal(data.title());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new InfiniteInventoryScreenHandler(syncId, inv, data);
    }
}