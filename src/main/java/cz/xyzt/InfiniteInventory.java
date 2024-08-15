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
    private String lastSearchTerm = "";

    public InfiniteInventory(Runnable markDirtyCallback) {
        this.markDirtyCallback = markDirtyCallback;
    }

    private void debugLog(String message) {
        InfiniteInventoryMod.LOGGER.info("----- [InfiniteInventory] -----");
        InfiniteInventoryMod.LOGGER.info(message);
        InfiniteInventoryMod.LOGGER.info("-------------------------------");
    }

    public boolean isSortAlphabetically() {
        return sortAlphabetically;
    }

    public List<ItemStack> getAllItems() {
        return new ArrayList<>(allItems);
    }

    public List<ItemStack> getFilteredItems() {
        return new ArrayList<>(filteredItems);
    }

    @Override
    public int size() {
        return DISPLAY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return allItems.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot < allItems.size() ? allItems.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < allItems.size()) {
            ItemStack stack = allItems.get(slot);
            if (amount >= stack.getCount()) {
                allItems.set(slot, ItemStack.EMPTY);
            } else {
                ItemStack split = stack.split(amount);
                if (stack.isEmpty()) {
                    allItems.set(slot, ItemStack.EMPTY);
                }
                markDirty();
                return split;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot < allItems.size()) {
            ItemStack stack = allItems.get(slot);
            allItems.set(slot, ItemStack.EMPTY);
            markDirty();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        while (allItems.size() <= slot) {
            allItems.add(ItemStack.EMPTY);
        }
        allItems.set(slot, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        if (this.markDirtyCallback != null) {
            this.markDirtyCallback.run();
        }
        updateShownItems(lastSearchTerm);
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
        if (searchTerm.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            filteredItems = allItems.stream()
                .filter(stack -> stack.getName().getString().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (sortAlphabetically) {
            filteredItems.sort(Comparator.comparing(stack -> stack.getName().getString().toLowerCase()));
        } else {
            filteredItems.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        }
    }

    public void toggleSortMode() {
        sortAlphabetically = !sortAlphabetically;
        updateShownItems(lastSearchTerm);
    }

    public void updateShownItems(String searchTerm) {
        lastSearchTerm = searchTerm;

        filterAndSort(searchTerm);

        while (filteredItems.size() < DISPLAY_SIZE) {
            filteredItems.add(ItemStack.EMPTY);
        }
    }

    public void syncWithClient(List<ItemStack> serverItems) {
        this.allItems.clear();
        this.allItems.addAll(serverItems);
        updateShownItems(lastSearchTerm);
        markDirty();
    }

    public void addItemDirect(ItemStack stack) {
        allItems.add(stack);
        updateShownItems(lastSearchTerm);
        markDirty();
    }

    public void addItem(ItemStack stack) {
        for (ItemStack existingStack : allItems) {
            if (canCombine(existingStack, stack)) {
                int spaceLeft = existingStack.getMaxCount() - existingStack.getCount();
                int amountToAdd = Math.min(spaceLeft, stack.getCount());
                existingStack.increment(amountToAdd);
                stack.decrement(amountToAdd);
                if (stack.isEmpty()) {
                    markDirty();
                    return;
                }
            }
        }
        if (!stack.isEmpty()) {
            allItems.add(stack.copy());
        }
        updateShownItems(lastSearchTerm);
        markDirty();
    }

    private boolean canCombine(ItemStack stack1, ItemStack stack2) {
        return !stack1.isEmpty() && !stack2.isEmpty() && 
            stack1.isOf(stack2.getItem()) && 
            ItemStack.areItemsEqual(stack1, stack2);
    }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList nbtTagList = new NbtList();
        for (int i = 0; i < allItems.size(); i++) {
            ItemStack stack = allItems.get(i);

            if (!stack.isEmpty()) {
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
        for (int i = 0; i < listNbt.size(); i++) {
            NbtCompound itemNbt = listNbt.getCompound(i);
            int slot = itemNbt.getInt("Slot");
            String itemId = itemNbt.getString("Item");
            int count = itemNbt.getInt("Count");

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
        updateShownItems(lastSearchTerm);
    }
}