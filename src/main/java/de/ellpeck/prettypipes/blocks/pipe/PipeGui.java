package de.ellpeck.prettypipes.blocks.pipe;

import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketHandler;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PipeGui extends ContainerScreen<PipeContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(PrettyPipes.ID, "textures/gui/pipe.png");
    private final List<Tab> tabs = new ArrayList<>();
    private int lastTabAmount;

    public PipeGui(PipeContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
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
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
        this.font.drawString(this.title.getFormattedText(), 8, 6 + 32, 4210752);
        if (this.container.openModule != null)
            this.container.openModule.drawContainerGuiForeground(this.container.tile, this.container, this, mouseX, mouseY);

        for (Tab tab : this.tabs)
            tab.drawForeground(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        this.blit(this.guiLeft, this.guiTop + 32, 0, 0, 176, 171);

        for (Tab tab : this.tabs)
            tab.draw();

        if (this.container.openModule == null) {
            for (int i = 0; i < 3; i++)
                this.blit(this.guiLeft + 61 + i * 18, this.guiTop + 32 + 16, 176, 62, 18, 18);
        } else {
            this.container.openModule.drawContainerGuiBackground(this.container.tile, this.container, this, mouseX, mouseY);
        }
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
        // this might cause unnecessary tab initializations, but
        // it's a pretty cheap operation so it should be fine
        if (slotIn != null)
            this.initTabs();
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
            if (module.hasContainerTab(this.container.tile, this.container))
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
            this.x = PipeGui.this.guiLeft + 5 + tabIndex * 28;
            this.y = PipeGui.this.guiTop;
        }

        private void draw() {
            int y = 2;
            int v = 0;
            int height = 30;
            int itemOffset = 9;
            if (this.module == PipeGui.this.container.openModule) {
                y = 0;
                v = 30;
                height = 32;
                itemOffset = 7;
            }
            PipeGui.this.blit(this.x, this.y + y, 176, v, 28, height);

            PipeGui.this.itemRenderer.renderItemIntoGUI(this.moduleStack, this.x + 6, this.y + itemOffset);
            PipeGui.this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        }

        private void drawForeground(int mouseX, int mouseY) {
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 28 || mouseY >= this.y + 32)
                return;
            PipeGui.this.renderTooltip(this.moduleStack.getDisplayName().getFormattedText(), mouseX - PipeGui.this.guiLeft, mouseY - PipeGui.this.guiTop);
        }

        private boolean onClicked(double mouseX, double mouseY, int button) {
            if (this.module == PipeGui.this.container.openModule)
                return false;
            if (button != 0)
                return false;
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 28 || mouseY >= this.y + 32)
                return false;
            PacketHandler.sendToServer(new PacketButton(PipeGui.this.container.tile.getPos(), PacketButton.ButtonResult.PIPE_TAB, this.index));
            PipeGui.this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1));
            return true;
        }
    }
}
