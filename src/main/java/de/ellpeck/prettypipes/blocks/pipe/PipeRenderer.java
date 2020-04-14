package de.ellpeck.prettypipes.blocks.pipe;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;

public class PipeRenderer extends TileEntityRenderer<PipeTileEntity> {

    public PipeRenderer(TileEntityRendererDispatcher disp) {
        super(disp);
    }

    @Override
    public void render(PipeTileEntity tile, float v, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, int i, int i1) {
        BlockPos pos = tile.getPos();
        for (PipeItem item : tile.items) {
            matrixStack.push();
            matrixStack.translate(item.x - pos.getX(), item.y - pos.getY(), item.z - pos.getZ());
            if (item.stack.getItem() instanceof BlockItem) {
                float scale = 0.65F;
                matrixStack.scale(scale, scale, scale);
                matrixStack.translate(0, -0.2F, 0);
            } else {
                float scale = 0.4F;
                matrixStack.scale(scale, scale, scale);
            }
            Minecraft.getInstance().getItemRenderer().renderItem(item.stack, ItemCameraTransforms.TransformType.GROUND, i, i1, matrixStack, iRenderTypeBuffer);
            matrixStack.pop();
        }
    }
}
