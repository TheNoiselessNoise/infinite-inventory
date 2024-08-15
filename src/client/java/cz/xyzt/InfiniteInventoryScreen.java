package cz.xyzt;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class InfiniteInventoryScreen extends HandledScreen<InfiniteInventoryScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/generic_54.png");
    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    
    private TextFieldWidget searchBar;
    private ButtonWidget sortButton;
    private InfiniteInventory inventory;

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
        this.searchBar = new TextFieldWidget(this.textRenderer, this.x + 82, this.y + 6, 80, 12, Text.of("Search..."));
        this.searchBar.setMaxLength(50);
        this.searchBar.setVisible(true);
        this.searchBar.setEditableColor(16777215);
        this.searchBar.setDrawsBackground(true);  // Set to true to make it visible
        this.addDrawableChild(this.searchBar);

        // Add sort button
        this.sortButton = ButtonWidget.builder(Text.of("Sort: A-Z"), button -> {
            inventory.toggleSortMode();
            button.setMessage(Text.of("Sort: " + (inventory.isSortAlphabetically() ? "A-Z" : "Count")));
            updateInventoryContents();
        }).dimensions(this.x + 180, this.y + 4, 60, 16).build();
        this.addDrawableChild(this.sortButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, ROWS * 18 + 17);
        context.drawTexture(TEXTURE, x, y + ROWS * 18 + 17, 0, 126, backgroundWidth, 96);
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
        inventory.filterAndSort(searchTerm);
        // Update the slots in the ScreenHandler
        handler.updateSlots();
    }
}