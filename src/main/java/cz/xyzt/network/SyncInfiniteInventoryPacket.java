package cz.xyzt.network;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import cz.xyzt.InfiniteInventoryMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SyncInfiniteInventoryPacket(List<ItemStack> items) implements CustomPayload {
    public static final CustomPayload.Id<SyncInfiniteInventoryPacket> ID = 
        new CustomPayload.Id<>(Identifier.of(InfiniteInventoryMod.MOD_ID, "infinite_inventory_sync"));

    public static final PacketCodec<PacketByteBuf, SyncInfiniteInventoryPacket> CODEC = 
        new PacketCodec<>() {
            @Override
            public void encode(PacketByteBuf buf, SyncInfiniteInventoryPacket value) {
                NbtList nbtTagList = new NbtList();
                for (int i = 0; i < value.items.size(); i++) {
                    ItemStack stack = value.items.get(i);
                    if (!stack.isEmpty()) {
                        NbtCompound itemTag = new NbtCompound();
                        itemTag.putInt("Slot", i);
                        Identifier id = Registries.ITEM.getId(stack.getItem());
                        itemTag.put("Item", NbtOps.INSTANCE.createString(id.toString()));
                        itemTag.put("Count", NbtOps.INSTANCE.createInt(stack.getCount()));
                        nbtTagList.add(itemTag);
                    }
                }
                NbtCompound nbt = new NbtCompound();
                nbt.put("Items", nbtTagList);
                buf.writeNbt(nbt);
            }

            @Override
            public SyncInfiniteInventoryPacket decode(PacketByteBuf buf) {
                NbtCompound nbt = buf.readNbt();
                NbtList listNbt = nbt.getList("Items", 10); // 10 is the NBT type for Compound
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < listNbt.size(); i++) {
                    NbtCompound itemNbt = listNbt.getCompound(i);
                    int slot = itemNbt.getInt("Slot");
                    String itemId = itemNbt.getString("Item");
                    int count = itemNbt.getInt("Count");
                    
                    Optional<Item> optionalItem = Registries.ITEM.getOrEmpty(Identifier.of(itemId));
                    if (optionalItem.isPresent()) {
                        ItemStack stack = new ItemStack(optionalItem.get(), count);
                        while (items.size() <= slot) {
                            items.add(ItemStack.EMPTY);
                        }
                        items.set(slot, stack);
                    }
                }
                return new SyncInfiniteInventoryPacket(items);
            }
        };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}