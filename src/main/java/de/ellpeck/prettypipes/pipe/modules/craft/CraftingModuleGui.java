package de.ellpeck.prettypipes.pipe.modules.craft;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
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

        var redstoneText = (Supplier<String>) () -> "info." + PrettyPipes.ID + ".emit_redstone_" + (this.menu.emitRedstone ? "on" : "off");
        this.addRenderableWidget(Button.builder(Component.translatable(redstoneText.get()), button -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.EMIT_REDSTONE_BUTTON, List.of());
            button.setMessage(Component.translatable(redstoneText.get()));
        }).bounds(this.leftPos + this.imageWidth - 7 - 20, this.topPos + 17 + 32 + 18 * 2 + 2, 20, 20).tooltip(
            Tooltip.create(Component.translatable("info." + PrettyPipes.ID + ".emit_redstone.description").withStyle(ChatFormatting.GRAY))).build());
        var unstackedText = (Supplier<String>) () -> "info." + PrettyPipes.ID + ".insert_unstacked_" + (this.menu.insertUnstacked ? "on" : "off");
        this.addRenderableWidget(Button.builder(Component.translatable(unstackedText.get()), button -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.INSERT_UNSTACKED_BUTTON, List.of());
            button.setMessage(Component.translatable(unstackedText.get()));
        }).bounds(this.leftPos + this.imageWidth - 7 - 20 - 22, this.topPos + 17 + 32 + 18 * 2 + 2, 20, 20).tooltip(
            Tooltip.create(Component.translatable("info." + PrettyPipes.ID + ".insert_unstacked.description").withStyle(ChatFormatting.GRAY))).build());

        var insertionTypeText = (Supplier<MutableComponent>) () -> Component.translatable(this.menu.insertionType.translationKey());
        var insertionTypeTooltip = (Supplier<Tooltip>) () -> Tooltip.create(Component.translatable(this.menu.insertionType.translationKey() + ".description").withStyle(ChatFormatting.GRAY));
        this.addRenderableWidget(Button.builder(insertionTypeText.get(), button -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), PacketButton.ButtonResult.INSERTION_TYPE_BUTTON, List.of());
            button.setMessage(insertionTypeText.get());
            button.setTooltip(insertionTypeTooltip.get());
        }).bounds(this.leftPos + 7, this.topPos + 17 + 32 + 18 * 2 + 2, 42, 20).tooltip(insertionTypeTooltip.get()).build());
    }

}
