package de.ellpeck.prettypipes.terminal.containers;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.ItemOrder;
import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import de.ellpeck.prettypipes.misc.PlayerPrefs;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketRequest;
import joptsimple.internal.Strings;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemTerminalGui extends ContainerScreen<ItemTerminalContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/item_terminal.png");

    // craftables have the second parameter set to true
    private final List<Pair<ItemStack, Boolean>> sortedItems = new ArrayList<>();
    private List<ItemStack> items;
    private List<ItemStack> craftables;
    private Button minusButton;
    private Button plusButton;
    private Button requestButton;
    private Button orderButton;
    private Button ascendingButton;
    private String lastSearchText;
    private int requestAmount = 1;
    private int scrollOffset;
    public TextFieldWidget search;

    public ItemTerminalGui(ItemTerminalContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176 + 15;
        this.ySize = 236;
    }

    @Override
    protected void init() {
        super.init();
        this.plusButton = this.addButton(new Button(this.guiLeft + this.getXOffset() + 95 - 7 + 12, this.guiTop + 103, 12, 12, new StringTextComponent("+"), button -> {
            int modifier = requestModifier();
            if (modifier > 1 && this.requestAmount == 1) {
                this.requestAmount = modifier;
            } else {
                this.requestAmount += modifier;
            }
            // 384 items is 6 stacks, which is what fits into the terminal slots
            if (this.requestAmount > 384)
                this.requestAmount = 384;
        }));
        this.minusButton = this.addButton(new Button(this.guiLeft + this.getXOffset() + 95 - 7 - 24, this.guiTop + 103, 12, 12, new StringTextComponent("-"), button -> {
            this.requestAmount -= requestModifier();
            if (this.requestAmount < 1)
                this.requestAmount = 1;
        }));
        this.minusButton.active = false;
        this.requestButton = this.addButton(new Button(this.guiLeft + this.getXOffset() + 95 - 7 - 25, this.guiTop + 115, 50, 20, new TranslationTextComponent("info." + PrettyPipes.ID + ".request"), button -> {
            Optional<ItemTerminalWidget> widget = this.streamWidgets().filter(w -> w.selected).findFirst();
            if (!widget.isPresent())
                return;
            ItemStack stack = widget.get().stack.copy();
            stack.setCount(1);
            PacketHandler.sendToServer(new PacketRequest(this.container.tile.getPos(), stack, this.requestAmount));
            this.requestAmount = 1;
        }));
        this.requestButton.active = false;
        this.orderButton = this.addButton(new Button(this.guiLeft - 22, this.guiTop, 20, 20, new StringTextComponent(""), button -> {
            if (this.sortedItems == null)
                return;
            PlayerPrefs prefs = PlayerPrefs.get();
            prefs.terminalItemOrder = prefs.terminalItemOrder.next();
            prefs.save();
            this.updateWidgets();
        }));
        this.ascendingButton = this.addButton(new Button(this.guiLeft - 22, this.guiTop + 22, 20, 20, new StringTextComponent(""), button -> {
            if (this.sortedItems == null)
                return;
            PlayerPrefs prefs = PlayerPrefs.get();
            prefs.terminalAscending = !prefs.terminalAscending;
            prefs.save();
            this.updateWidgets();
        }));
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++)
                this.addButton(new ItemTerminalWidget(this.guiLeft + this.getXOffset() + 8 + x * 18, this.guiTop + 18 + y * 18, x, y, this));
        }
        this.search = this.addButton(new TextFieldWidget(this.font, this.guiLeft + this.getXOffset() + 97, this.guiTop + 6, 86, 8, new StringTextComponent("")));
        this.search.setEnableBackgroundDrawing(false);
        this.lastSearchText = "";
        if (this.items != null)
            this.updateWidgets();
    }

    protected int getXOffset() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.requestButton.active = this.streamWidgets().anyMatch(w -> w.selected);
        this.plusButton.active = this.requestAmount < 384;
        this.minusButton.active = this.requestAmount > 1;

        this.search.tick();
        if (this.items != null) {
            String text = this.search.getText();
            if (!this.lastSearchText.equals(text)) {
                this.lastSearchText = text;
                this.updateWidgets();
            }
        }
    }

    @Override
    public boolean keyPressed(int x, int y, int z) {
        // for some reason we have to do this to make the text field allow the inventory key to be typed
        if (this.search.isFocused()) {
            InputMappings.Input mouseKey = InputMappings.getInputByCode(x, y);
            if (this.minecraft.gameSettings.keyBindInventory.isActiveAndMatches(mouseKey))
                return false;
        }
        return super.keyPressed(x, y, z);
    }

    public void updateItemList(List<ItemStack> items, List<ItemStack> craftables) {
        this.items = items;
        this.craftables = craftables;
        this.updateWidgets();
    }

    public void updateWidgets() {
        PlayerPrefs prefs = PlayerPrefs.get();
        this.ascendingButton.setMessage(new StringTextComponent(prefs.terminalAscending ? "^" : "v"));
        this.orderButton.setMessage(new StringTextComponent(prefs.terminalItemOrder.name().substring(0, 1)));

        Comparator<ItemStack> comparator = prefs.terminalItemOrder.comparator;
        if (!prefs.terminalAscending)
            comparator = comparator.reversed();

        // add all items to the sorted items list
        this.sortedItems.clear();
        for (ItemStack stack : this.items)
            this.sortedItems.add(Pair.of(stack, false));
        for (ItemStack stack : this.craftables)
            this.sortedItems.add(Pair.of(stack, true));

        // compare by craftability first, and then by the player's chosen order
        Comparator<Pair<ItemStack, Boolean>> fullComparator = Comparator.comparing(Pair::getRight);
        this.sortedItems.sort(fullComparator.thenComparing(Pair::getLeft, comparator));

        String searchText = this.search.getText();
        if (!Strings.isNullOrEmpty(searchText)) {
            this.sortedItems.removeIf(s -> {
                String search = searchText;
                String toCompare;
                if (search.startsWith("@")) {
                    toCompare = s.getLeft().getItem().getCreatorModId(s.getLeft());
                    search = search.substring(1);
                } else {
                    // don't use formatted text here since we want to search for name
                    toCompare = s.getLeft().getDisplayName().getString();
                }
                return !toCompare.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
            });
        }

        if (this.sortedItems.size() < 9 * 4)
            this.scrollOffset = 0;

        List<ItemTerminalWidget> widgets = this.streamWidgets().collect(Collectors.toList());
        for (int i = 0; i < widgets.size(); i++) {
            ItemTerminalWidget widget = widgets.get(i);
            int index = i + this.scrollOffset * 9;
            if (index >= this.sortedItems.size()) {
                widget.stack = ItemStack.EMPTY;
                widget.craftable = false;
                widget.visible = false;
            } else {
                Pair<ItemStack, Boolean> stack = this.sortedItems.get(index);
                widget.stack = stack.getLeft();
                widget.craftable = stack.getRight();
                widget.visible = true;
            }
        }
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        for (Widget widget : this.buttons) {
            if (widget instanceof ItemTerminalWidget)
                widget.renderToolTip(matrix, mouseX, mouseY);
        }
        if (this.sortedItems != null) {
            PlayerPrefs prefs = PlayerPrefs.get();
            if (this.orderButton.isHovered())
                this.renderTooltip(matrix, new TranslationTextComponent("info." + PrettyPipes.ID + ".order", I18n.format("info." + PrettyPipes.ID + ".order." + prefs.terminalItemOrder.name().toLowerCase(Locale.ROOT))), mouseX, mouseY);
            if (this.ascendingButton.isHovered())
                this.renderTooltip(matrix, new TranslationTextComponent("info." + PrettyPipes.ID + "." + (prefs.terminalAscending ? "ascending" : "descending")), mouseX, mouseY);
        }
        this.func_230459_a_(matrix, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
        this.font.drawString(matrix, this.playerInventory.getDisplayName().getString(), 8 + this.getXOffset(), this.ySize - 96 + 2, 4210752);
        this.font.drawString(matrix, this.title.getString(), 8, 6, 4210752);

        String amount = String.valueOf(this.requestAmount);
        this.font.drawString(matrix, amount, (176 + 15 - this.font.getStringWidth(amount)) / 2F - 7 + this.getXOffset(), 106, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bindTexture(this.getTexture());
        this.blit(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        if (this.sortedItems != null && this.sortedItems.size() >= 9 * 4) {
            float percentage = this.scrollOffset / (float) (this.sortedItems.size() / 9 - 3);
            this.blit(matrix, this.guiLeft + this.getXOffset() + 172, this.guiTop + 18 + (int) (percentage * (70 - 15)), 232, 241, 12, 15);
        } else {
            this.blit(matrix, this.guiLeft + this.getXOffset() + 172, this.guiTop + 18, 244, 241, 12, 15);
        }
    }

    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scroll) {
        if (this.sortedItems != null && this.sortedItems.size() >= 9 * 4) {
            int offset = MathHelper.clamp(this.scrollOffset - (int) Math.signum(scroll), 0, this.sortedItems.size() / 9 - 3);
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

    public static int requestModifier() {
        if (hasControlDown()) {
            return 10;
        } else if (hasShiftDown()) {
            return 64;
        } else {
            return 1;
        }
    }
}
