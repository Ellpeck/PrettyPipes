package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.function.Supplier;

public class CraftingModuleGui extends AbstractPipeGui<CraftingModuleContainer> {

    public CraftingModuleGui(CraftingModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        graphics.blit(AbstractPipeGui.TEXTURE, this.leftPos + 176 / 2 - 16 / 2, this.topPos + 32 + 18 * 2, 176, 80, 16, 16);
    }

    @Override
    protected void init() {
        super.init();
        var cacheText = (Supplier<String>) () -> "info." + PrettyPipes.ID + ".ensure_item_order_" + (this.menu.ensureItemOrder ? "on" : "off");
        this.addRenderableWidget(Button.builder(Component.translatable(cacheText.get()), button -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.ENSURE_ITEM_ORDER_BUTTON, List.of());
            button.setMessage(Component.translatable(cacheText.get()));
        }).bounds(this.leftPos + this.imageWidth - 7 - 20, this.topPos + 17 + 32 + 18 * 2 + 2, 20, 20).tooltip(
            Tooltip.create(Component.translatable("info." + PrettyPipes.ID + ".ensure_item_order.description").withStyle(ChatFormatting.GRAY))).build());

        var singleText = (Supplier<String>) () -> "info." + PrettyPipes.ID + ".insert_singles_" + (this.menu.insertSingles ? "on" : "off");
        this.addRenderableWidget(Button.builder(Component.translatable(singleText.get()), button -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.INSERT_SINGLES_BUTTON, List.of());
            button.setMessage(Component.translatable(singleText.get()));
        }).bounds(this.leftPos + this.imageWidth - 7 - 20 - 22, this.topPos + 17 + 32 + 18 * 2 + 2, 20, 20).tooltip(
            Tooltip.create(Component.translatable("info." + PrettyPipes.ID + ".insert_singles.description").withStyle(ChatFormatting.GRAY))).build());

        var redstoneText = (Supplier<String>) () -> "info." + PrettyPipes.ID + ".emit_redstone_" + (this.menu.emitRedstone ? "on" : "off");
        this.addRenderableWidget(Button.builder(Component.translatable(redstoneText.get()), button -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.EMIT_REDSTONE_BUTTON, List.of());
            button.setMessage(Component.translatable(redstoneText.get()));
        }).bounds(this.leftPos + 7, this.topPos + 17 + 32 + 18 * 2 + 2, 20, 20).tooltip(
            Tooltip.create(Component.translatable("info." + PrettyPipes.ID + ".emit_redstone.description").withStyle(ChatFormatting.GRAY))).build());
    }

}
