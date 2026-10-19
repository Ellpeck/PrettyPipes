package de.ellpeck.prettypipes.pipe.modules.modifier;

import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class FilterModifierModuleGui extends AbstractPipeGui<FilterModifierModuleContainer> {

    private int scrollOffset;
    private boolean isScrolling;
    private List<Identifier> tags;
    private final List<Tag> tagButtons = new ArrayList<>();

    public FilterModifierModuleGui(FilterModifierModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blitInscribed(AbstractPipeGui.TEXTURE, this.leftPos + 7, this.topPos + 32 + 15, 0, 196, 162, 60);

        for (var tag : this.tagButtons)
            tag.draw(graphics, mouseX, mouseY);

        if (this.tags.size() >= 6) {
            var percentage = this.scrollOffset / (float) (this.tags.size() - 5);
            graphics.blitInscribed(AbstractPipeGui.TEXTURE, this.leftPos + 156, this.topPos + 32 + 16 + (int) (percentage * (58 - 15)), 232, 241, 12, 15);
        } else {
            graphics.blitInscribed(AbstractPipeGui.TEXTURE, this.leftPos + 156, this.topPos + 32 + 16, 244, 241, 12, 15);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.tags = this.menu.getTags();
        this.updateWidgets();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (var tag : this.tagButtons) {
            if (tag.onClicked(event.x(), event.y(), event.button()))
                return true;
        }
        if (event.button() == 0 && event.x() >= this.leftPos + 156 && this.topPos + event.y() >= 32 + 16 && event.x() < this.leftPos + 156 + 12 && event.y() < this.topPos + 32 + 16 + 58) {
            this.isScrolling = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0)
            this.isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isScrolling) {
            var percentage = Mth.clamp(((float) event.y() - (this.topPos + 32 + 18)) / (58 - 15), 0, 1);
            var offset = Math.max(0, (int) (percentage * (float) (this.tags.size() - 5)));
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.tags.size() >= 6) {
            var offset = Mth.clamp(this.scrollOffset - (int) Math.signum(scrollY), 0, this.tags.size() - 5);
            if (offset != this.scrollOffset) {
                this.scrollOffset = offset;
                this.updateWidgets();
            }
        }
        return true;
    }

    private void updateWidgets() {
        this.tagButtons.clear();
        for (var i = 0; i < 5; i++) {
            if (i >= this.tags.size())
                break;
            this.tagButtons.add(new Tag(this.tags.get(this.scrollOffset + i), this.leftPos + 10, this.topPos + 32 + 17 + i * 12));
        }
    }

    private class Tag {

        private final Identifier tag;
        private final int x;
        private final int y;

        public Tag(Identifier tag, int x, int y) {
            this.tag = tag;
            this.x = x;
            this.y = y;
        }

        private void draw(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            var color = 4210752;
            var text = this.tag.toString();
            if (mouseX >= this.x && mouseY >= this.y && mouseX < this.x + 140 && mouseY < this.y + 12)
                color = 0xFFFFFF;
            if (this.tag.equals(FilterModifierModuleItem.getFilterTag(FilterModifierModuleGui.this.menu.moduleStack)))
                text = ChatFormatting.BOLD + text;
            graphics.text(FilterModifierModuleGui.this.font, text, this.x, this.y, color, false);
        }

        private boolean onClicked(double mouseX, double mouseY, int button) {
            if (button != 0)
                return false;
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 140 || mouseY >= this.y + 12)
                return false;
            PacketButton.sendAndExecute(FilterModifierModuleGui.this.menu.tile.getBlockPos(), PacketButton.ButtonResult.TAG_FILTER, List.of(FilterModifierModuleGui.this.tags.indexOf(this.tag)));
            FilterModifierModuleGui.this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            return true;
        }
    }
}
