package de.ellpeck.prettypipes.terminal.containers;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketRequest;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemTerminalGui extends ContainerScreen<ItemTerminalContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/item_terminal.png");
    private List<ItemStack> items;
    private Button minusButton;
    private Button plusButton;
    private Button requestButton;
    private int requestAmount = 1;
    private int scrollOffset;

    public ItemTerminalGui(ItemTerminalContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176 + 15;
        this.ySize = 236;
    }

    @Override
    protected void init() {
        super.init();
        this.plusButton = this.addButton(new Button(this.guiLeft + this.xSize / 2 - 7 + 12, this.guiTop + 103, 12, 12, "+", button -> {
            int modifier = requestModifier();
            if (modifier > 1 && this.requestAmount == 1) {
                this.requestAmount = modifier;
            } else {
                this.requestAmount += modifier;
            }
            if (this.requestAmount > 384)
                this.requestAmount = 384;
        }));
        this.minusButton = this.addButton(new Button(this.guiLeft + this.xSize / 2 - 7 - 24, this.guiTop + 103, 12, 12, "-", button -> {
            this.requestAmount -= requestModifier();
            if (this.requestAmount < 1)
                this.requestAmount = 1;
        }));
        this.minusButton.active = false;
        this.requestButton = this.addButton(new Button(this.guiLeft + this.xSize / 2 - 7 - 25, this.guiTop + 115, 50, 20, I18n.format("info." + PrettyPipes.ID + ".request"), button -> {
            Optional<ItemTerminalWidget> widget = this.streamWidgets().filter(w -> w.selected).findFirst();
            if (!widget.isPresent())
                return;
            ItemStack stack = widget.get().stack.copy();
            stack.setCount(1);
            PacketHandler.sendToServer(new PacketRequest(this.container.tile.getPos(), stack, this.requestAmount));
            this.requestAmount = 1;
        }));
        this.requestButton.active = false;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++)
                this.addButton(new ItemTerminalWidget(this.guiLeft + 8 + x * 18, this.guiTop + 18 + y * 18, x, y, this));
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.requestButton.active = this.streamWidgets().anyMatch(w -> w.selected);
        this.plusButton.active = this.requestAmount < 384;
        this.minusButton.active = this.requestAmount > 1;
    }

    public void updateItemList(List<ItemStack> items) {
        this.items = items;
        this.updateWidgets();
    }

    private void updateWidgets() {
        List<ItemTerminalWidget> widgets = this.streamWidgets().collect(Collectors.toList());
        for (int i = 0; i < widgets.size(); i++) {
            ItemTerminalWidget widget = widgets.get(i);
            int index = i + this.scrollOffset * 9;
            if (index >= this.items.size()) {
                widget.stack = ItemStack.EMPTY;
                widget.visible = false;
            } else {
                widget.stack = this.items.get(index);
                widget.visible = true;
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        for (Widget widget : this.buttons) {
            if (widget instanceof ItemTerminalWidget)
                widget.renderToolTip(mouseX, mouseY);
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
        this.font.drawString(this.title.getFormattedText(), 8, 6, 4210752);

        String amount = String.valueOf(this.requestAmount);
        this.font.drawString(amount, (this.xSize - this.font.getStringWidth(amount)) / 2F - 7, 106, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        if (this.items != null && this.items.size() >= 9 * 4) {
            float percentage = this.scrollOffset / (float) (this.items.size() / 9 - 3);
            this.blit(this.guiLeft + 172, this.guiTop + 18 + (int) (percentage * (70 - 15)), 244, 0, 12, 15);
        } else {
            this.blit(this.guiLeft + 172, this.guiTop + 18, 244, 15, 12, 15);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scroll) {
        if (this.items != null && this.items.size() >= 9 * 4) {
            int offset = MathHelper.clamp(this.scrollOffset - (int) Math.signum(scroll), 0, this.items.size() / 9 - 3);
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
        }
        return true;
    }

    public Stream<ItemTerminalWidget> streamWidgets() {
        return this.buttons.stream()
                .filter(w -> w instanceof ItemTerminalWidget)
                .map(w -> (ItemTerminalWidget) w);
    }

    private static int requestModifier() {
        if (hasControlDown()) {
            return 10;
        } else if (hasShiftDown()) {
            return 64;
        } else {
            return 1;
        }
    }
}
