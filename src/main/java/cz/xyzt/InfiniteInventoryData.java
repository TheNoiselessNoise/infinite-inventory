package cz.xyzt;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record InfiniteInventoryData(String title) {
    public static final PacketCodec<PacketByteBuf, InfiniteInventoryData> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.STRING,
        InfiniteInventoryData::title,
        InfiniteInventoryData::new
    );
}