package de.ellpeck.prettypipes.misc;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.gui.GuiUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ItemTerminalWidget extends Widget {

    private final ItemTerminalGui screen;
    public final int gridX;
    public final int gridY;
    public boolean selected;
    public ItemStack stack = ItemStack.EMPTY;
    public boolean craftable;

    public ItemTerminalWidget(int xIn, int yIn, int gridX, int gridY, ItemTerminalGui screen) {
        super(xIn, yIn, 16, 16, new StringTextComponent(""));
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
    public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = this.screen.getMinecraft();
        ItemRenderer renderer = mc.getItemRenderer();
        this.setBlitOffset(100);
        renderer.zLevel = 100;
        if (this.selected)
            fill(matrix, this.x, this.y, this.x + 16, this.y + 16, -2130706433);
        RenderSystem.enableDepthTest();
        renderer.renderItemAndEffectIntoGUI(mc.player, this.stack, this.x, this.y);
        int amount = !this.craftable ? this.stack.getCount() : 0;
        String amountStrg = this.stack.getCount() >= 1000 ? amount / 1000 + "k" : String.valueOf(amount);
        RenderSystem.pushMatrix();
        RenderSystem.scalef(0.8F, 0.8F, 1);
        renderer.renderItemOverlayIntoGUI(mc.fontRenderer, this.stack, (int) (this.x / 0.8F) + 4, (int) (this.y / 0.8F) + 4, amountStrg);
        RenderSystem.popMatrix();
        renderer.zLevel = 0;
        this.setBlitOffset(0);

        if (this.isHovered()) {
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, false);
            this.fillGradient(matrix, this.x, this.y, this.x + 16, this.y + 16, -2130706433, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    public void renderToolTip(MatrixStack matrix, int mouseX, int mouseY) {
        if (this.visible && this.isHovered()) {
            GuiUtils.preItemToolTip(this.stack);
            List<ITextComponent> tooltip = this.screen.getTooltipFromItem(this.stack);
            if (this.stack.getCount() >= 1000) {
                ITextComponent comp = tooltip.get(0);
                if (comp instanceof TextComponent) {
                    tooltip.set(0, ((TextComponent) comp).append(new StringTextComponent(" (" + this.stack.getCount() + ')').mergeStyle(TextFormatting.BOLD)));
                }
            }
            this.screen.func_243308_b(matrix, tooltip, mouseX, mouseY);
            GuiUtils.postItemToolTip();
        }
    }
}
