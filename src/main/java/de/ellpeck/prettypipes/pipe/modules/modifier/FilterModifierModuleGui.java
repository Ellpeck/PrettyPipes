package de.ellpeck.prettypipes.pipe.modules.modifier;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public class FilterModifierModuleGui extends AbstractPipeGui<FilterModifierModuleContainer> {
    private int scrollOffset;
    private boolean isScrolling;
    private List<ResourceLocation> tags;
    private final List<Tag> tagButtons = new ArrayList<>();

    public FilterModifierModuleGui(FilterModifierModuleContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(matrix, partialTicks, mouseX, mouseY);
        this.blit(matrix, this.guiLeft + 7, this.guiTop + 32 + 15, 0, 196, 162, 60);

        for (Tag tag : this.tagButtons)
            tag.draw(matrix, mouseX, mouseY);

        if (this.tags.size() >= 6) {
            float percentage = this.scrollOffset / (float) (this.tags.size() - 5);
            this.blit(matrix, this.guiLeft + 156, this.guiTop + 32 + 16 + (int) (percentage * (58 - 15)), 232, 241, 12, 15);
        } else {
            this.blit(matrix, this.guiLeft + 156, this.guiTop + 32 + 16, 244, 241, 12, 15);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.tags = this.container.getTags();
        this.updateWidgets();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Tag tag : this.tagButtons) {
            if (tag.onClicked(mouseX, mouseY, button))
                return true;
        }
        if (button == 0 && mouseX >= this.guiLeft + 156 && this.guiTop + mouseY >= 32 + 16 && mouseX < this.guiLeft + 156 + 12 && mouseY < this.guiTop + 32 + 16 + 58) {
            this.isScrolling = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0)
            this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int i, double j, double k) {
        if (this.isScrolling) {
            float percentage = MathHelper.clamp(((float) mouseY - (this.guiTop + 32 + 18)) / (58 - 15), 0, 1);
            int offset = (int) (percentage * (float) (this.tags.size() - 5));
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, i, j, k);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scroll) {
        if (this.tags.size() >= 6) {
            int offset = MathHelper.clamp(this.scrollOffset - (int) Math.signum(scroll), 0, this.tags.size() - 5);
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
        }
        return true;
    }

    private void updateWidgets() {
        this.tagButtons.clear();
        for (int i = 0; i < 5; i++) {
            if (i >= this.tags.size())
                break;
            this.tagButtons.add(new Tag(this.tags.get(this.scrollOffset + i), this.guiLeft + 10, this.guiTop + 32 + 17 + i * 12));
        }
    }

    private class Tag {

        private final ResourceLocation tag;
        private final int x;
        private final int y;

        public Tag(ResourceLocation tag, int x, int y) {
            this.tag = tag;
            this.x = x;
            this.y = y;
        }

        private void draw(MatrixStack matrix, double mouseX, double mouseY) {
            int color = 4210752;
            String text = this.tag.toString();
            if (mouseX >= this.x && mouseY >= this.y && mouseX < this.x + 140 && mouseY < this.y + 12)
                color = 0xFFFFFF;
            if (this.tag.equals(FilterModifierModuleItem.getFilterTag(FilterModifierModuleGui.this.container.moduleStack)))
                text = TextFormatting.BOLD + text;
            FilterModifierModuleGui.this.font.drawString(matrix, text, this.x, this.y, color);
            FilterModifierModuleGui.this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        }

        private boolean onClicked(double mouseX, double mouseY, int button) {
            if (button != 0)
                return false;
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 140 || mouseY >= this.y + 12)
                return false;
            PacketButton.sendAndExecute(FilterModifierModuleGui.this.container.tile.getPos(), PacketButton.ButtonResult.TAG_FILTER, FilterModifierModuleGui.this.tags.indexOf(this.tag));
            FilterModifierModuleGui.this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1));
            return true;
        }
    }
}
