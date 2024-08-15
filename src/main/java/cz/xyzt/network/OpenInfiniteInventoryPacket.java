package cz.xyzt.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import cz.xyzt.InfiniteInventoryMod;

public record OpenInfiniteInventoryPacket() implements CustomPayload {
    public static final CustomPayload.Id<OpenInfiniteInventoryPacket> ID = 
        new CustomPayload.Id<>(Identifier.of(InfiniteInventoryMod.MOD_ID, "open_infinite_inventory"));

    public static final PacketCodec<RegistryByteBuf, OpenInfiniteInventoryPacket> CODEC = 
        new PacketCodec<>() {
            @Override
            public void encode(RegistryByteBuf buf, OpenInfiniteInventoryPacket value) {
                // No data to encode
            }

            @Override
            public OpenInfiniteInventoryPacket decode(RegistryByteBuf buf) {
                return new OpenInfiniteInventoryPacket();
            }
        };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}