package de.ellpeck.prettypipes.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ItemTerminalWidget extends AbstractWidget {

    private final ItemTerminalGui screen;
    public final int gridX;
    public final int gridY;
    public boolean selected;
    public ItemStack stack = ItemStack.EMPTY;
    public boolean craftable;

    public ItemTerminalWidget(int xIn, int yIn, int gridX, int gridY, ItemTerminalGui screen) {
        super(xIn, yIn, 16, 16, new TextComponent(""));
        this.gridX = gridX;
        this.gridY = gridY;
        this.screen = screen;
        this.visible = false;
    }

    @Override
    public void onClick(double x, double y) {
        this.screen.streamWidgets().forEach(w -> w.selected = false);
        this.selected = true;
    }

    @Override
    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        var mc = this.screen.getMinecraft();
        var renderer = mc.getItemRenderer();
        this.setBlitOffset(100);
        renderer.blitOffset = 100;
        if (this.selected)
            GuiComponent.fill(matrix, this.x, this.y, this.x + 16, this.y + 16, -2130706433);
        RenderSystem.enableDepthTest();
        renderer.renderGuiItem(this.stack, this.x, this.y);
        var amount = !this.craftable ? this.stack.getCount() : 0;
        var amountStrg = this.stack.getCount() >= 1000 ? amount / 1000 + "k" : String.valueOf(amount);
        renderer.renderGuiItemDecorations(mc.font, this.stack, this.x, this.y, amountStrg);
        renderer.blitOffset = 0;
        this.setBlitOffset(0);

        if (this.isHoveredOrFocused()) {
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, false);
            this.fillGradient(matrix, this.x, this.y, this.x + 16, this.y + 16, -2130706433, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
        if (this.visible && this.isHoveredOrFocused()) {
            var tooltip = this.screen.getTooltipFromItem(this.stack);
            if (this.stack.getCount() >= 1000) {
                var comp = tooltip.get(0);
                if (comp instanceof TextComponent text) {
                    tooltip.set(0, text.append(new TextComponent(" (" + this.stack.getCount() + ')').withStyle(ChatFormatting.BOLD)));
                }
            }
            this.screen.renderTooltip(matrix, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
