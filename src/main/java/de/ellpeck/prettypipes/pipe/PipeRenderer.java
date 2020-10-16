package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class PipeRenderer extends TileEntityRenderer<PipeTileEntity> {

    private final Random random = new Random();

    public PipeRenderer(TileEntityRendererDispatcher disp) {
        super(disp);
    }

    @Override
    public void render(PipeTileEntity tile, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, int k, int i1) {
        if (tile.getItems().isEmpty())
            return;
        matrixStack.push();
        BlockPos tilePos = tile.getPos();
        matrixStack.translate(-tilePos.getX(), -tilePos.getY(), -tilePos.getZ());
        for (PipeItem item : tile.getItems()) {
            matrixStack.push();
            item.render(tile, matrixStack, this.random, partialTicks, k, i1, iRenderTypeBuffer);
            matrixStack.pop();
        }
        matrixStack.pop();
    }
}
