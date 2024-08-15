package cz.xyzt.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import cz.xyzt.InfiniteInventoryMod;

public record UpdateInfiniteInventoryPacket() implements CustomPayload {
    public static final CustomPayload.Id<UpdateInfiniteInventoryPacket> ID = 
        new CustomPayload.Id<>(Identifier.of(InfiniteInventoryMod.MOD_ID, "infinite_inventory_update"));

    public static final PacketCodec<PacketByteBuf, UpdateInfiniteInventoryPacket> CODEC = 
        new PacketCodec<>() {
            @Override
            public void encode(PacketByteBuf buf, UpdateInfiniteInventoryPacket value) {
                
            }

            @Override
            public UpdateInfiniteInventoryPacket decode(PacketByteBuf buf) {
                return new UpdateInfiniteInventoryPacket();
            }
        };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}