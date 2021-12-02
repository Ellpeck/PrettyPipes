package de.ellpeck.prettypipes.pipe.modules.craft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeGui;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CraftingModuleGui extends AbstractPipeGui<CraftingModuleContainer> {

    public CraftingModuleGui(CraftingModuleContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(PoseStack matrix, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrix, partialTicks, mouseX, mouseY);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.blit(matrix, this.leftPos + 176 / 2 - 16 / 2, this.topPos + 32 + 18 * 2, 176, 80, 16, 16);
    }
}
