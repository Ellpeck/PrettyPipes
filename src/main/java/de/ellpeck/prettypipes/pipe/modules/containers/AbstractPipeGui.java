package de.ellpeck.prettypipes.pipe.modules.containers;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketHandler;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPipeGui<T extends AbstractPipeContainer<?>> extends ContainerScreen<T> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/pipe.png");
    private final List<Tab> tabs = new ArrayList<>();
    private final ItemStack[] lastItems = new ItemStack[this.container.tile.modules.getSlots()];

    public AbstractPipeGui(T screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176;
        this.ySize = 171 + 32;
    }

    @Override
    protected void init() {
        super.init();
        this.initTabs();
    }

    @Override
    public void tick() {
        super.tick();

        boolean changed = false;
        for (int i = 0; i < this.container.tile.modules.getSlots(); i++) {
            ItemStack stack = this.container.tile.modules.getStackInSlot(i);
            if (stack != this.lastItems[i]) {
                this.lastItems[i] = stack;
                changed = true;
            }
        }
        if (changed)
            this.initTabs();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        for (Widget widget : this.buttons) {
            if (widget.isHovered())
                widget.renderToolTip(mouseX, mouseY);
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
        this.font.drawString(this.title.getFormattedText(), 8, 6 + 32, 4210752);
        for (Tab tab : this.tabs)
            tab.drawForeground(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(this.guiLeft, this.guiTop + 32, 0, 0, 176, 171);

        for (Tab tab : this.tabs)
            tab.draw();

        // draw the slots since we're using a blank ui
        for (Slot slot : this.container.inventorySlots) {
            if (slot.inventory == this.playerInventory)
                continue;
            this.blit(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 176, 62, 18, 18);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        for (Tab tab : this.tabs) {
            if (tab.onClicked(x, y, button))
                return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private void initTabs() {
        this.tabs.clear();
        this.tabs.add(new Tab(new ItemStack(Registry.pipeBlock), null, 0, -1));
        for (int i = 0; i < this.container.tile.modules.getSlots(); i++) {
            ItemStack stack = this.container.tile.modules.getStackInSlot(i);
            if (stack.isEmpty())
                continue;
            IModule module = (IModule) stack.getItem();
            if (module.hasContainer(stack, this.container.tile))
                this.tabs.add(new Tab(stack, module, this.tabs.size(), i));
        }
    }

    private class Tab {
        private final ItemStack moduleStack;
        private final IModule module;
        private final int index;
        private final int x;
        private final int y;

        public Tab(ItemStack moduleStack, IModule module, int tabIndex, int index) {
            this.moduleStack = moduleStack;
            this.module = module;
            this.index = index;
            this.x = AbstractPipeGui.this.guiLeft + 5 + tabIndex * 28;
            this.y = AbstractPipeGui.this.guiTop;
        }

        private void draw() {
            int y = 2;
            int v = 0;
            int height = 30;
            int itemOffset = 9;
            if (this.module == AbstractPipeGui.this.container.module) {
                y = 0;
                v = 30;
                height = 32;
                itemOffset = 7;
            }
            AbstractPipeGui.this.blit(this.x, this.y + y, 176, v, 28, height);

            AbstractPipeGui.this.itemRenderer.renderItemIntoGUI(this.moduleStack, this.x + 6, this.y + itemOffset);
            AbstractPipeGui.this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        }

        private void drawForeground(int mouseX, int mouseY) {
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 28 || mouseY >= this.y + 32)
                return;
            AbstractPipeGui.this.renderTooltip(this.moduleStack.getDisplayName().getFormattedText(), mouseX - AbstractPipeGui.this.guiLeft, mouseY - AbstractPipeGui.this.guiTop);
        }

        private boolean onClicked(double mouseX, double mouseY, int button) {
            if (this.module == AbstractPipeGui.this.container.module)
                return false;
            if (button != 0)
                return false;
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 28 || mouseY >= this.y + 32)
                return false;
            PacketHandler.sendToServer(new PacketButton(AbstractPipeGui.this.container.tile.getPos(), PacketButton.ButtonResult.PIPE_TAB, this.index));
            AbstractPipeGui.this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1));
            return true;
        }
    }
}
