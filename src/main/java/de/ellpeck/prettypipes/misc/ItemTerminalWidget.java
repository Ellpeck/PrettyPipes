package de.ellpeck.prettypipes.misc;

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
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.List;

public class ItemTerminalWidget extends Widget {

    private static final ResourceLocation FONT = new ResourceLocation(PrettyPipes.ID, "unicode");
    private final ItemStack stack;
    private final ItemTerminalGui screen;
    public final int gridX;
    public final int gridY;
    public boolean hidden;

    public ItemTerminalWidget(int xIn, int yIn, int gridX, int gridY, ItemStack stack, ItemTerminalGui screen) {
        super(xIn, yIn, 16, 16, stack.getDisplayName().getFormattedText());
        this.gridX = gridX;
        this.gridY = gridY;
        this.stack = stack;
        this.screen = screen;
    }

    @Override
    protected boolean clicked(double x, double y) {
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!this.hidden)
            super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = this.screen.getMinecraft();
        ItemRenderer renderer = mc.getItemRenderer();
        this.setBlitOffset(100);
        renderer.zLevel = 100;
        RenderSystem.enableDepthTest();
        renderer.renderItemAndEffectIntoGUI(mc.player, this.stack, this.x, this.y);
        int amount = this.stack.getCount();
        String amountStrg = this.stack.getCount() >= 1000 ? amount / 1000 + "k" : String.valueOf(amount);
        FontRenderer font = mc.getFontResourceManager().getFontRenderer(FONT);
        renderer.renderItemOverlayIntoGUI(font, this.stack, this.x, this.y, amountStrg);
        renderer.zLevel = 0;
        this.setBlitOffset(0);

        if (this.isHovered()) {
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, false);
            this.fillGradient(this.x, this.y, this.x + 16, this.y + 16, -2130706433, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    public void renderToolTip(int mouseX, int mouseY) {
        if (this.isHovered()) {
            FontRenderer font = this.stack.getItem().getFontRenderer(this.stack);
            if (font == null)
                font = this.screen.getMinecraft().fontRenderer;
            GuiUtils.preItemToolTip(this.stack);
            List<String> tooltip = this.screen.getTooltipFromItem(this.stack);
            if (this.stack.getCount() >= 1000)
                tooltip.set(0, tooltip.get(0) + TextFormatting.BOLD + " (" + this.stack.getCount() + ')');
            this.screen.renderTooltip(tooltip, mouseX, mouseY, font);
            GuiUtils.postItemToolTip();
        }
    }
}
