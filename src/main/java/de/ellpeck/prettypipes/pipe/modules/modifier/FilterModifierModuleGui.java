package de.ellpeck.prettypipes.pipe.modules.modifier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class FilterModifierModuleGui extends AbstractPipeGui<FilterModifierModuleContainer> {

    private int scrollOffset;
    private boolean isScrolling;
    private List<ResourceLocation> tags;
    private final List<Tag> tagButtons = new ArrayList<>();

    public FilterModifierModuleGui(FilterModifierModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(PoseStack matrix, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrix, partialTicks, mouseX, mouseY);
        this.blit(matrix, this.leftPos + 7, this.topPos + 32 + 15, 0, 196, 162, 60);

        for (var tag : this.tagButtons)
            tag.draw(matrix, mouseX, mouseY);

        if (this.tags.size() >= 6) {
            var percentage = this.scrollOffset / (float) (this.tags.size() - 5);
            this.blit(matrix, this.leftPos + 156, this.topPos + 32 + 16 + (int) (percentage * (58 - 15)), 232, 241, 12, 15);
        } else {
            this.blit(matrix, this.leftPos + 156, this.topPos + 32 + 16, 244, 241, 12, 15);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.tags = this.menu.getTags();
        this.updateWidgets();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (var tag : this.tagButtons) {
            if (tag.onClicked(mouseX, mouseY, button))
                return true;
        }
        if (button == 0 && mouseX >= this.leftPos + 156 && this.topPos + mouseY >= 32 + 16 && mouseX < this.leftPos + 156 + 12 && mouseY < this.topPos + 32 + 16 + 58) {
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
            var percentage = Mth.clamp(((float) mouseY - (this.topPos + 32 + 18)) / (58 - 15), 0, 1);
            var offset = Math.max(0, (int) (percentage * (float) (this.tags.size() - 5)));
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
            var offset = Mth.clamp(this.scrollOffset - (int) Math.signum(scroll), 0, this.tags.size() - 5);
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

        private final ResourceLocation tag;
        private final int x;
        private final int y;

        public Tag(ResourceLocation tag, int x, int y) {
            this.tag = tag;
            this.x = x;
            this.y = y;
        }

        private void draw(PoseStack matrix, double mouseX, double mouseY) {
            var color = 4210752;
            var text = this.tag.toString();
            if (mouseX >= this.x && mouseY >= this.y && mouseX < this.x + 140 && mouseY < this.y + 12)
                color = 0xFFFFFF;
            if (this.tag.equals(FilterModifierModuleItem.getFilterTag(FilterModifierModuleGui.this.menu.moduleStack)))
                text = ChatFormatting.BOLD + text;
            FilterModifierModuleGui.this.font.draw(matrix, text, this.x, this.y, color);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, AbstractPipeGui.TEXTURE);
        }

        private boolean onClicked(double mouseX, double mouseY, int button) {
            if (button != 0)
                return false;
            if (mouseX < this.x || mouseY < this.y || mouseX >= this.x + 140 || mouseY >= this.y + 12)
                return false;
            PacketButton.sendAndExecute(FilterModifierModuleGui.this.menu.tile.getBlockPos(), PacketButton.ButtonResult.TAG_FILTER, FilterModifierModuleGui.this.tags.indexOf(this.tag));
            FilterModifierModuleGui.this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            return true;
        }
    }
}
