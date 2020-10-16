package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.util.Random;

public class PipeRenderer extends TileEntityRenderer<PipeTileEntity> {

    private final Random random = new Random();

    public PipeRenderer(TileEntityRendererDispatcher disp) {
        super(disp);
    }

    @Override
    public void render(PipeTileEntity tile, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int light, int overlay) {
        if (!tile.getItems().isEmpty()) {
            matrixStack.push();
            BlockPos tilePos = tile.getPos();
            matrixStack.translate(-tilePos.getX(), -tilePos.getY(), -tilePos.getZ());
            for (PipeItem item : tile.getItems()) {
                matrixStack.push();
                item.render(tile, matrixStack, this.random, partialTicks, light, overlay, buffer);
                matrixStack.pop();
            }
            matrixStack.pop();
        }
        if (tile.cover != null) {
            matrixStack.push();
            BlockModelRenderer.enableCache();
            for (RenderType layer : RenderType.getBlockRenderTypes()) {
                if (!RenderTypeLookup.canRenderInLayer(tile.cover, layer))
                    continue;
                ForgeHooksClient.setRenderLayer(layer);
                Minecraft.getInstance().getBlockRendererDispatcher().renderBlock(tile.cover, matrixStack, buffer, light, overlay, EmptyModelData.INSTANCE);
            }
            ForgeHooksClient.setRenderLayer(null);
            BlockModelRenderer.disableCache();
            matrixStack.pop();
        }
    }
}
