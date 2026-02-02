package de.ellpeck.prettypipes.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        super(xIn, yIn, 16, 16, Component.literal(""));
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
    protected void renderWidget(GuiGraphics graphics, int p_268034_, int p_268009_, float p_268085_) {
        var mc = this.screen.getMinecraft();
        graphics.pose().translate(0, 0, 100);
        if (this.selected)
            graphics.fill(this.getX(), this.getY(), this.getX() + 16, this.getY() + 16, -2130706433);
        RenderSystem.enableDepthTest();
        graphics.renderItem(this.stack, this.getX(), this.getY());
        var amount = ItemTerminalWidget.displayAmount(!this.craftable ? this.stack.getCount() : 0);
        graphics.renderItemDecorations(mc.font, this.stack, this.getX(), this.getY(), amount);
        graphics.pose().translate(0, 0, -100);

        if (this.isHoveredOrFocused()) {
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, false);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + 16, this.getY() + 16, -2130706433, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.visible && this.isHovered()) {
            var tooltip = Screen.getTooltipFromItem(this.screen.getMinecraft(), this.stack);
            if (this.stack.getCount() >= 1000) {
                var comp = tooltip.get(0);
                if (comp instanceof MutableComponent m)
                    tooltip.set(0, m.append(Component.literal(" (" + this.stack.getCount() + ')').withStyle(ChatFormatting.BOLD)));
            }
            graphics.renderTooltip(this.screen.getMinecraft().font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        // we don't want a sound :)
    }

    private static String displayAmount(int amount) {
        if (amount >= 1_000_000)
            return I18n.get("info." + PrettyPipes.ID + ".million", amount / 1_000_000);
        if (amount >= 1_000)
            return I18n.get("info." + PrettyPipes.ID + ".thousand", amount / 1_000);
        return String.valueOf(amount);
    }

}
