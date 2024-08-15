package cz.xyzt;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class InfiniteInventoryScreen extends HandledScreen<InfiniteInventoryScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/generic_54.png");
    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 15;
    
    private TextFieldWidget searchBar;
    private ButtonWidget sortButton;
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private InfiniteInventory inventory;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    public InfiniteInventoryScreen(InfiniteInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
        this.backgroundHeight = 114 + ROWS * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        // Add search bar
        int sebX = this.x + 180;
        int sebY = this.y + 4;
        int sebW = 80;
        int sebH = 12;
        this.searchBar = new TextFieldWidget(this.textRenderer, sebX, sebY, sebW, sebH, Text.of("Search..."));
        this.searchBar.setMaxLength(50);
        this.searchBar.setVisible(true);
        this.searchBar.setEditableColor(16777215);
        this.searchBar.setDrawsBackground(true);  // Set to true to make it visible
        this.addDrawableChild(this.searchBar);

        // Add sort button
        int sobX = this.x + 180;
        int sobY = this.y + this.searchBar.getHeight() + 8;
        int sobW = 60;
        int sobH = 16;
        this.sortButton = ButtonWidget.builder(Text.of("Sort: A-Z"), button -> {
            inventory.toggleSortMode();
            button.setMessage(Text.of("Sort: " + (inventory.isSortAlphabetically() ? "A-Z" : "Count")));
            updateInventoryContents();
        }).dimensions(sobX, sobY, sobW, sobH).build();
        this.addDrawableChild(this.sortButton);

        // Modify up button
        int ubX = this.x + 180;
        int ubY = this.y + this.searchBar.getHeight() + this.sortButton.getHeight() + 12;
        int ubW = 60;
        int ubH = 16;
        this.upButton = ButtonWidget.builder(Text.of("Up"), button -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                updateInventoryContents();
            }
        }).dimensions(ubX, ubY, ubW, ubH).build();
        this.addDrawableChild(this.upButton);

        // Modify down button
        int dbX = this.x + 180;
        int dbY = this.y + this.searchBar.getHeight() + this.sortButton.getHeight() + this.upButton.getHeight() + 16;
        int dbW = 60;
        int dbH = 16;
        this.downButton = ButtonWidget.builder(Text.of("Down"), button -> {
            if (scrollOffset < maxScrollOffset) {
                scrollOffset++;
                updateInventoryContents();
            }
        }).dimensions(dbX, dbY, dbW, dbH).build();
        this.addDrawableChild(this.downButton);

        updateInventoryContents();
        updateScrollButtons();
    }

    public void onUpdate() {
        String searchTerm = searchBar.getText().toLowerCase();
        inventory.updateShownItems(searchTerm);
        updateInventoryContents();
        updateScrollButtons();
    }

    private void updateScrollButtons() {
        List<ItemStack> filteredItems = inventory.getFilteredItems();
        upButton.active = scrollOffset > 0;
        downButton.active = filteredItems.size() > COLUMNS * ROWS;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            updateInventoryContents();
            return true;
        } else if (verticalAmount < 0 && scrollOffset < maxScrollOffset) {
            scrollOffset++;
            updateInventoryContents();
            return true;
        }
        return false;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, ROWS * 18 + 17);
        context.drawTexture(TEXTURE, x, y + ROWS * 18 + 17, 0, 126, backgroundWidth, 96);

        // scrollbar
        int scrollbarX = x + 176;
        int scrollbarY = y + 18;
        int scrollbarHeight = ROWS * 18 - SCROLLBAR_HEIGHT;
        context.drawTexture(TEXTURE, scrollbarX, scrollbarY, 232, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
        context.drawTexture(TEXTURE, scrollbarX, scrollbarY + SCROLLBAR_HEIGHT, 232, 15, SCROLLBAR_WIDTH, scrollbarHeight);
        
        if (maxScrollOffset > 0) {
            float scrollPercentage = (float) scrollOffset / maxScrollOffset;
            int scrollbarYOffset = (int) (scrollPercentage * (scrollbarHeight - SCROLLBAR_HEIGHT));
            context.drawTexture(TEXTURE, scrollbarX, scrollbarY + scrollbarYOffset, 244, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        searchBar.render(context, mouseX, mouseY, delta);
        sortButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, 8, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBar.keyPressed(keyCode, scanCode, modifiers)) {
            updateInventoryContents();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchBar.charTyped(chr, modifiers)) {
            updateInventoryContents();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void updateInventoryContents() {
        String searchTerm = searchBar.getText().toLowerCase();
        inventory.updateShownItems(searchTerm);
        List<ItemStack> filteredItems = inventory.getFilteredItems();
        
        // Update the inventory slots based on the current scroll offset
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = row * COLUMNS + col;
                int itemIndex = (scrollOffset + row) * COLUMNS + col;
                
                if (itemIndex < filteredItems.size()) {
                    handler.setSlot(slotIndex, filteredItems.get(itemIndex));
                } else {
                    handler.setSlot(slotIndex, ItemStack.EMPTY);
                }
            }
        }
        
        updateScrollButtons();
    }
}