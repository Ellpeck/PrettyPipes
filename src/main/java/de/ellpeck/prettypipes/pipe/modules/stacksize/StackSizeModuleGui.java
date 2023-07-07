package de.ellpeck.prettypipes.pipe.modules.stacksize;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketButton.ButtonResult;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Supplier;

public class StackSizeModuleGui extends AbstractPipeGui<StackSizeModuleContainer> {

    public StackSizeModuleGui(StackSizeModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        var textField = this.addRenderableWidget(new EditBox(this.font, this.leftPos + 7, this.topPos + 17 + 32 + 10, 40, 20, Component.translatable("info." + PrettyPipes.ID + ".max_stack_size")) {
            @Override
            public void insertText(String textToWrite) {
                var ret = new StringBuilder();
                for (var c : textToWrite.toCharArray()) {
                    if (Character.isDigit(c))
                        ret.append(c);
                }
                super.insertText(ret.toString());
            }

        });
        textField.setValue(String.valueOf(StackSizeModuleItem.getMaxStackSizeForModule(this.menu.moduleStack)));
        textField.setMaxLength(4);
        textField.setResponder(s -> {
            if (s.isEmpty())
                return;
            var amount = Integer.parseInt(s);
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), ButtonResult.STACK_SIZE_AMOUNT, amount);
        });
        Supplier<Component> buttonText = () -> Component.translatable("info." + PrettyPipes.ID + ".limit_to_max_" + (StackSizeModuleItem.getLimitToMaxStackSize(this.menu.moduleStack) ? "on" : "off"));
        this.addRenderableWidget(Button.builder(buttonText.get(), b -> {
            PacketButton.sendAndExecute(this.menu.tile.getBlockPos(), ButtonResult.STACK_SIZE_MODULE_BUTTON);
            b.setMessage(buttonText.get());
        }).bounds(this.leftPos + 7, this.topPos + 17 + 32 + 10 + 22, 120, 20).build());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, I18n.get("info." + PrettyPipes.ID + ".max_stack_size") + ":", 7, 17 + 32, 4210752);

    }
}
