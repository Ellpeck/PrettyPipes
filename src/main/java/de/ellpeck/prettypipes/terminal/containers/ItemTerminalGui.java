package de.ellpeck.prettypipes.terminal.containers;

import com.mojang.blaze3d.platform.InputConstants;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.misc.ItemTerminalWidget;
import de.ellpeck.prettypipes.misc.PlayerPrefs;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketRequest;
import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ALL")
public class ItemTerminalGui extends AbstractContainerScreen<ItemTerminalContainer> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PrettyPipes.ID, "textures/gui/item_terminal.png");

    public List<ItemStack> currentlyCrafting;
    public EditBox search;

    // craftables have the second parameter set to true
    private final List<Pair<ItemStack, Boolean>> sortedItems = new ArrayList<>();
    private List<ItemStack> items;
    private List<ItemStack> craftables;
    private Button minusButton;
    private Button plusButton;
    private Button requestButton;
    private Button orderButton;
    private Button ascendingButton;
    private Button cancelCraftingButton;
    private String lastSearchText;
    private int requestAmount = 1;
    private int scrollOffset;
    private ItemStack hoveredCrafting;
    private boolean isScrolling;

    public ItemTerminalGui(ItemTerminalContainer screenContainer, Inventory inv, Component titleIn, int width, int height) {
        super(screenContainer, inv, titleIn, width, height);
    }

    public ItemTerminalGui(ItemTerminalContainer screenContainer, Inventory inv, Component titleIn) {
        this(screenContainer, inv, titleIn, 176 + 15, 236);
    }

    @Override
    protected void init() {
        super.init();

        this.search = this.addRenderableWidget(new EditBox(this.font, this.leftPos + this.getXOffset() + 97, this.topPos + 6, 86, 8, Component.literal("")));
        this.search.setBordered(false);
        this.lastSearchText = "";
        if (this.items != null)
            this.updateWidgets();

        this.plusButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            var modifier = ItemTerminalGui.requestModifier(this.minecraft);
            if (modifier > 1 && this.requestAmount == 1) {
                this.requestAmount = modifier;
            } else {
                this.requestAmount += modifier;
            }
            // 384 items is 6 stacks, which is what fits into the terminal slots
            if (this.requestAmount > 384)
                this.requestAmount = 384;
        }).bounds(this.leftPos + this.getXOffset() + 95 - 7 + 12, this.topPos + 103, 12, 12).build());
        this.minusButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            this.requestAmount -= ItemTerminalGui.requestModifier(this.minecraft);
            if (this.requestAmount < 1)
                this.requestAmount = 1;
        }).bounds(this.leftPos + this.getXOffset() + 95 - 7 - 24, this.topPos + 103, 12, 12).build());
        this.minusButton.active = false;
        this.requestButton = this.addRenderableWidget(Button.builder(Component.translatable("info." + PrettyPipes.ID + ".request"), button -> {
            var widget = this.streamWidgets().filter(w -> w.selected).findFirst();
            if (!widget.isPresent())
                return;
            var stack = widget.get().stack.copy();
            if (stack.isEmpty())
                return;
            stack.setCount(1);
            ClientPacketDistributor.sendToServer(new PacketRequest(this.menu.tile.getBlockPos(), stack, this.requestAmount));
            this.requestAmount = 1;
        }).bounds(this.leftPos + this.getXOffset() + 95 - 7 - 25, this.topPos + 115, 50, 20).build());
        this.requestButton.active = false;
        this.orderButton = this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            if (this.sortedItems == null)
                return;
            var prefs = PlayerPrefs.get();
            prefs.terminalItemOrder = prefs.terminalItemOrder.next();
            prefs.save();
            this.updateWidgets();
        }).bounds(this.leftPos - 22, this.topPos, 20, 20).build());
        this.ascendingButton = this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            if (this.sortedItems == null)
                return;
            var prefs = PlayerPrefs.get();
            prefs.terminalAscending = !prefs.terminalAscending;
            prefs.save();
            this.updateWidgets();
        }).bounds(this.leftPos - 22, this.topPos + 22, 20, 20).build());
        this.cancelCraftingButton = this.addRenderableWidget(Button.builder(Component.translatable("info." + PrettyPipes.ID + ".cancel_all"), b -> {}).bounds(this.leftPos + this.imageWidth + 4, this.topPos + 4 + 64, 54, 20).build());
        this.cancelCraftingButton.visible = false;
        for (var y = 0; y < 4; y++) {
            for (var x = 0; x < 9; x++)
                this.addRenderableWidget(new ItemTerminalWidget(this.leftPos + this.getXOffset() + 8 + x * 18, this.topPos + 18 + y * 18, x, y, this));
        }
    }

    protected int getXOffset() {
        return 0;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.requestButton.active = this.streamWidgets().anyMatch(w -> w.selected);
        this.plusButton.active = this.requestAmount < 384;
        this.minusButton.active = this.requestAmount > 1;

        if (this.items != null) {
            var text = this.search.getValue();
            if (!this.lastSearchText.equals(text)) {
                this.lastSearchText = text;
                this.updateWidgets();
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && event.x() >= this.leftPos + this.getXOffset() + 172 && this.topPos + event.y() >= 18 && event.x() < this.leftPos + this.getXOffset() + 172 + 12 && event.y() < this.topPos + 18 + 70) {
            this.isScrolling = true;
            return true;
        } else if (event.button() == 1 && event.x() >= this.search.getX() && event.x() <= this.search.getX() + this.search.getWidth() && event.y() >= this.search.getY() && event.y() <= this.search.getY() + 8) {
            this.search.setValue("");
            this.search.setFocused(true);
            this.setFocused(this.search);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // we have to do the click logic here because JEI is activated when letting go of the mouse button
        // and vanilla buttons are activated when the click starts, so we'll always invoke jei accidentally by default
        if (event.button() == 0 && this.cancelCraftingButton.visible && this.cancelCraftingButton.isHovered()) {
            if (this.currentlyCrafting != null && !this.currentlyCrafting.isEmpty()) {
                ClientPacketDistributor.sendToServer(new PacketButton(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.CANCEL_CRAFTING, List.of(this.minecraft.hasShiftDown() ? 1 : 0)));
                return true;
            }
        }
        if (event.button() == 0)
            this.isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isScrolling) {
            var percentage = Mth.clamp(((float) event.y() - (this.topPos + 18) - 7.5F) / (70 - 15), 0, 1);
            var offset = (int) (percentage * (float) (this.sortedItems.size() / 9 - 3));
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // for some reason we have to do this to make the text field allow the inventory key to be typed
        if (this.search.isFocused()) {
            var mouseKey = InputConstants.getKey(event);
            if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey))
                return false;
        }
        return super.keyPressed(event);
    }

    public void updateItemList(List<ItemStack> items, List<ItemStack> craftables, List<ItemStack> currentlyCrafting) {
        this.items = items;
        this.craftables = craftables;
        this.currentlyCrafting = currentlyCrafting;
        this.updateWidgets();
    }

    public void updateWidgets() {
        var prefs = PlayerPrefs.get();
        this.ascendingButton.setMessage(Component.literal(prefs.terminalAscending ? "^" : "v"));
        this.orderButton.setMessage(Component.literal(prefs.terminalItemOrder.name().substring(0, 1)));
        this.cancelCraftingButton.visible = this.currentlyCrafting != null && !this.currentlyCrafting.isEmpty();

        var comparator = prefs.terminalItemOrder.makeComparator.apply(this.minecraft.level.registryAccess());
        if (!prefs.terminalAscending)
            comparator = comparator.reversed();

        // add all items to the sorted items list
        this.sortedItems.clear();
        for (var stack : this.items)
            this.sortedItems.add(Pair.of(stack, false));
        for (var stack : this.craftables)
            this.sortedItems.add(Pair.of(stack, true));

        // compare by craftability first, and then by the player's chosen order
        Comparator<Pair<ItemStack, Boolean>> fullComparator = Comparator.comparing(Pair::getRight);
        this.sortedItems.sort(fullComparator.thenComparing(Pair::getLeft, comparator));

        var searchText = this.search.getValue();
        if (!Strings.isNullOrEmpty(searchText)) {
            this.sortedItems.removeIf(s -> {
                var search = searchText;
                String toCompare;
                if (search.startsWith("@")) {
                    // search mod id
                    toCompare = s.getLeft().getItem().getCreatorModId(this.minecraft.level.registryAccess(), s.getLeft());
                    search = search.substring(1);
                } else if (search.startsWith("#")) {
                    // search item description
                    var hoverText = s.getLeft().getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player,
                        this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
                    toCompare = hoverText.stream().map(Component::getString).collect(Collectors.joining("\n"));
                    search = search.substring(1);
                } else {
                    // don't use formatted text here since we want to search for name
                    toCompare = s.getLeft().getHoverName().getString();
                }
                return !toCompare.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
            });
        }

        if (this.sortedItems.size() < 9 * 4)
            this.scrollOffset = 0;

        var widgets = this.streamWidgets().collect(Collectors.toList());
        for (var i = 0; i < widgets.size(); i++) {
            var widget = widgets.get(i);
            var index = i + this.scrollOffset * 9;
            if (index >= this.sortedItems.size()) {
                widget.stack = ItemStack.EMPTY;
                widget.craftable = false;
                widget.visible = false;
            } else {
                var stack = this.sortedItems.get(index);
                widget.stack = stack.getLeft();
                widget.craftable = stack.getRight();
                widget.visible = true;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        for (var widget : this.renderables) {
            if (widget instanceof ItemTerminalWidget terminal)
                terminal.renderToolTip(graphics, mouseX, mouseY);
        }
        if (this.sortedItems != null) {
            var prefs = PlayerPrefs.get();
            if (this.orderButton.isHovered())
                graphics.setTooltipForNextFrame(this.font, Component.translatable("info." + PrettyPipes.ID + ".order", I18n.get("info." + PrettyPipes.ID + ".order." + prefs.terminalItemOrder.name().toLowerCase(Locale.ROOT))), mouseX, mouseY);
            if (this.ascendingButton.isHovered())
                graphics.setTooltipForNextFrame(this.font, Component.translatable("info." + PrettyPipes.ID + "." + (prefs.terminalAscending ? "ascending" : "descending")), mouseX, mouseY);
        }
        if (this.cancelCraftingButton.visible && this.cancelCraftingButton.isHovered()) {
            var tooltip = I18n.get("info." + PrettyPipes.ID + ".cancel_all.desc").split("\n");
            graphics.setTooltipForNextFrame(this.font, Arrays.stream(tooltip).map(Component::literal).collect(Collectors.toList()), Optional.empty(), mouseX, mouseY);
        }
        if (!this.hoveredCrafting.isEmpty())
            graphics.setTooltipForNextFrame(this.font, this.hoveredCrafting, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        graphics.text(this.font, this.playerInventoryTitle.getString(), 8 + this.getXOffset(), this.imageHeight - 96 + 2, 4210752, false);
        graphics.text(this.font, this.title.getString(), 8, 6, 4210752, false);

        var amount = String.valueOf(this.requestAmount);
        graphics.text(this.font, amount, (176 + 15 - this.font.width(amount)) / 2 - 7 + this.getXOffset(), 106, 4210752, false);

        if (this.currentlyCrafting != null && !this.currentlyCrafting.isEmpty()) {
            graphics.text(this.font, I18n.get("info." + PrettyPipes.ID + ".crafting"), this.imageWidth + 4, 4 + 6, 4210752, false);
            if (this.currentlyCrafting.size() > 6)
                graphics.text(this.font, ". . .", this.imageWidth + 24, 4 + 51, 4210752, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blitInscribed(this.getTexture(), this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.sortedItems != null && this.sortedItems.size() >= 9 * 4) {
            var percentage = this.scrollOffset / (float) (this.sortedItems.size() / 9 - 3);
            graphics.blitInscribed(this.getTexture(), this.leftPos + this.getXOffset() + 172, this.topPos + 18 + (int) (percentage * (70 - 15)), 232, 241, 12, 15);
        } else {
            graphics.blitInscribed(this.getTexture(), this.leftPos + this.getXOffset() + 172, this.topPos + 18, 244, 241, 12, 15);
        }

        // draw the items that are currently crafting
        this.hoveredCrafting = ItemStack.EMPTY;
        if (this.currentlyCrafting != null && !this.currentlyCrafting.isEmpty()) {
            graphics.blitInscribed(ItemTerminalGui.TEXTURE, this.leftPos + this.imageWidth, this.topPos + 4, 191, 0, 65, 89);

            var x = 0;
            var y = 0;
            for (var stack : this.currentlyCrafting) {
                var itemX = this.leftPos + this.imageWidth + 4 + x * 18;
                var itemY = this.topPos + 4 + 16 + y * 18;
                graphics.item(stack, itemX, itemY);
                graphics.itemDecorations(this.font, stack, itemX, itemY, String.valueOf(stack.getCount()));
                if (mouseX >= itemX && mouseY >= itemY && mouseX < itemX + 16 && mouseY < itemY + 18)
                    this.hoveredCrafting = stack;
                x++;
                if (x >= 3) {
                    x = 0;
                    y++;
                    if (y >= 2)
                        break;
                }
            }
        }
    }

    protected Identifier getTexture() {
        return ItemTerminalGui.TEXTURE;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.sortedItems != null && this.sortedItems.size() >= 9 * 4) {
            var offset = Mth.clamp(this.scrollOffset - (int) Math.signum(scrollY), 0, this.sortedItems.size() / 9 - 3);
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
        }
        return true;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T p_169406_) {
        // overriding to public for JEIPrettyPipesPlugin
        return super.addRenderableWidget(p_169406_);
    }

    public Stream<ItemTerminalWidget> streamWidgets() {
        return this.renderables.stream()
            .filter(w -> w instanceof ItemTerminalWidget)
            .map(w -> (ItemTerminalWidget) w);
    }

    public static int requestModifier(Minecraft mc) {
        if (mc.hasControlDown()) {
            return 10;
        } else if (mc.hasShiftDown()) {
            return 64;
        } else {
            return 1;
        }
    }
}
