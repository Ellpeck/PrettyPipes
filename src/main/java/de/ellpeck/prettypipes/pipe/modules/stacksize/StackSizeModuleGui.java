package de.ellpeck.prettypipes.pipe.modules.stacksize;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.packets.PacketButton.ButtonResult;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.Supplier;

public class StackSizeModuleGui extends AbstractPipeGui<StackSizeModuleContainer> {
    public StackSizeModuleGui(StackSizeModuleContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();
        TextFieldWidget textField = this.addButton(new TextFieldWidget(this.font, this.guiLeft + 7, this.guiTop + 17 + 32 + 10, 40, 20, new TranslationTextComponent("info." + PrettyPipes.ID + ".max_stack_size")) {
            @Override
            public void writeText(String textToWrite) {
                StringBuilder ret = new StringBuilder();
                for (char c : textToWrite.toCharArray()) {
                    if (Character.isDigit(c))
                        ret.append(c);
                }
                super.writeText(ret.toString());
            }
        });
        textField.setText(String.valueOf(StackSizeModuleItem.getMaxStackSize(this.container.moduleStack)));
        textField.setMaxStringLength(4);
        textField.setResponder(s -> {
            if (s.isEmpty())
                return;
            int amount = Integer.parseInt(s);
            PacketButton.sendAndExecute(this.container.tile.getPos(), ButtonResult.STACK_SIZE_AMOUNT, amount);
        });
        Supplier<TranslationTextComponent> buttonText = () -> new TranslationTextComponent("info." + PrettyPipes.ID + ".limit_to_max_" + (StackSizeModuleItem.getLimitToMaxStackSize(this.container.moduleStack) ? "on" : "off"));
        this.addButton(new Button(this.guiLeft + 7, this.guiTop + 17 + 32 + 10 + 22, 120, 20, buttonText.get(), b -> {
            PacketButton.sendAndExecute(this.container.tile.getPos(), ButtonResult.STACK_SIZE_MODULE_BUTTON);
            b.setMessage(buttonText.get());
        }));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(matrix, mouseX, mouseY);
        this.font.drawString(matrix, I18n.format("info." + PrettyPipes.ID + ".max_stack_size") + ":", 7, 17 + 32, 4210752);

    }
}
