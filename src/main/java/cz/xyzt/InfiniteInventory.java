package cz.xyzt;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InfiniteInventory implements Inventory {
    private List<ItemStack> allItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private boolean sortAlphabetically = true;
    private static final int DISPLAY_SIZE = 54; // 6 rows of 9 slots

    private final Runnable markDirtyCallback;

    public InfiniteInventory(Runnable markDirtyCallback) {
        this.markDirtyCallback = markDirtyCallback;
    }

    public boolean isSortAlphabetically() {
        return sortAlphabetically;
    }

    public List<ItemStack> getAllItems() {
        return new ArrayList<>(allItems);
    }

    @Override
    public int size() {
        return DISPLAY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return allItems.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot < filteredItems.size() ? filteredItems.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] removeStack called for slot " + slot + " with amount " + amount);
        if (slot < filteredItems.size()) {
            ItemStack stack = filteredItems.get(slot);
            ItemStack removedStack = stack.split(amount);
            if (stack.isEmpty()) {
                filteredItems.remove(slot);
                allItems.remove(stack);
            }
            markDirty();  // Call markDirty() after modifying the inventory
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] removeStack result: " + removedStack);
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] After removeStack: allItems size = " + allItems.size() + ", filteredItems size = " + filteredItems.size());
            return removedStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] removeStack called for slot " + slot);
        if (slot < filteredItems.size()) {
            ItemStack stack = filteredItems.remove(slot);
            allItems.remove(stack);
            markDirty();  // Call markDirty() after modifying the inventory
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] removeStack result: " + stack);
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] After removeStack: allItems size = " + allItems.size() + ", filteredItems size = " + filteredItems.size());
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] setStack called for slot " + slot + " with stack " + stack);
        if (slot < DISPLAY_SIZE) {
            if (slot >= filteredItems.size()) {
                filteredItems.add(stack);
                allItems.add(stack);
                InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Added new stack to slot " + slot + ": " + stack.toString());
            } else {
                ItemStack existingStack = filteredItems.get(slot);
                allItems.remove(existingStack);
                filteredItems.set(slot, stack);
                if (!stack.isEmpty()) {
                    allItems.add(stack);
                    InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Updated stack in slot " + slot + ": " + stack.toString());
                } else {
                    InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Removed stack from slot " + slot);
                }
            }
            markDirty();
        }
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Current allItems size: " + allItems.size());
    }

    @Override
    public void markDirty() {
        if (this.markDirtyCallback != null) {
            this.markDirtyCallback.run();
        }
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] InfiniteInventory marked dirty");
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        allItems.clear();
        filteredItems.clear();
    }

    public void filterAndSort(String searchTerm) {
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Filtering and sorting. AllItems size before: " + allItems.size());
        if (searchTerm.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            filteredItems = allItems.stream()
                .filter(stack -> stack.getName().getString().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
        }
        sort();
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Filtering and sorting complete. AllItems size after: " + allItems.size() + ", FilteredItems size: " + filteredItems.size());
    }

    public void sort() {
        if (sortAlphabetically) {
            filteredItems.sort(Comparator.comparing(stack -> stack.getName().getString().toLowerCase()));
        } else {
            filteredItems.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        }
    }

    public void toggleSortMode() {
        sortAlphabetically = !sortAlphabetically;
        sort();
    }

    public void syncWithClient(List<ItemStack> serverItems) {
        this.allItems = new ArrayList<>(serverItems);
        filterAndSort("");
        markDirty();
    }

    public void addItem(ItemStack stack) {
        for (ItemStack existingStack : allItems) {
            if (canCombine(existingStack, stack)) {
                int spaceLeft = Integer.MAX_VALUE - existingStack.getCount();
                int amountToAdd = Math.min(spaceLeft, stack.getCount());
                existingStack.increment(amountToAdd);
                stack.decrement(amountToAdd);
                InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Combined stack: " + existingStack.toString());
                if (stack.isEmpty()) {
                    filterAndSort("");
                    markDirty();
                    return;
                }
            }
        }
        if (!stack.isEmpty()) {
            allItems.add(stack.copy());
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Added new stack: " + stack.toString());
        }
        filterAndSort("");
        markDirty();
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Current allItems size: " + allItems.size());
    }

    private boolean canCombine(ItemStack stack1, ItemStack stack2) {
        return !stack1.isEmpty() && !stack2.isEmpty() && 
            stack1.isOf(stack2.getItem()) && 
            ItemStack.areItemsEqual(stack1, stack2);
    }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] allItems.size(): " + allItems.size());

        NbtList nbtTagList = new NbtList();
        for (int i = 0; i < allItems.size(); i++) {
            ItemStack stack = allItems.get(i);

            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Stack at: " + i + " is: " + stack);

            if (!stack.isEmpty()) {
                InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Writing item to NBT: " + stack);
                InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Slot: " + i);
                InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Item: " + stack.getItem());
                InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Count: " + stack.getCount());

                NbtCompound itemTag = new NbtCompound();
                itemTag.putInt("Slot", i);
                Identifier id = Registries.ITEM.getId(stack.getItem());
                itemTag.put("Item", NbtOps.INSTANCE.createString(id.toString()));
                itemTag.put("Count", NbtOps.INSTANCE.createInt(stack.getCount()));
                nbtTagList.add(itemTag);
            }
        }

        NbtCompound newNbt = new NbtCompound();
        newNbt.put("Items", nbtTagList);
        newNbt.putBoolean("SortAlphabetically", sortAlphabetically);
        return newNbt;
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList listNbt = nbt.getList("Items", 10); // 10 is the NBT type for Compound
        allItems.clear();
        InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Reading items from NBT, count: " + listNbt.size());
        for (int i = 0; i < listNbt.size(); i++) {
            NbtCompound itemNbt = listNbt.getCompound(i);
            int slot = itemNbt.getInt("Slot");
            String itemId = itemNbt.getString("Item");
            int count = itemNbt.getInt("Count");

            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Reading item from NBT: " + itemId);
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Slot: " + slot);
            InfiniteInventoryMod.LOGGER.warn("[INVENTORY] Count: " + count);
            
            Optional<Item> optionalItem = Registries.ITEM.getOrEmpty(Identifier.of(itemId));
            if (optionalItem.isPresent()) {
                ItemStack stack = new ItemStack(optionalItem.get(), count);
                while (allItems.size() <= slot) {
                    allItems.add(ItemStack.EMPTY);
                }
                allItems.set(slot, stack);
            }
        }
        sortAlphabetically = nbt.getBoolean("SortAlphabetically");
        filterAndSort("");
    }
}