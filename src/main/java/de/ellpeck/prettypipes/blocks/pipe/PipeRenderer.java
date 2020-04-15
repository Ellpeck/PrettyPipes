package de.ellpeck.prettypipes.blocks.pipe;

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
    public void render(PipeTileEntity tile, float v, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, int k, int i1) {
        if (tile.items.isEmpty())
            return;
        matrixStack.push();
        BlockPos tilePos = tile.getPos();
        matrixStack.translate(-tilePos.getX(), -tilePos.getY(), -tilePos.getZ());
        for (PipeItem item : tile.items) {
            matrixStack.push();
            matrixStack.translate(
                    MathHelper.lerp(v, item.lastX, item.x),
                    MathHelper.lerp(v, item.lastY, item.y),
                    MathHelper.lerp(v, item.lastZ, item.z));

            if (item.stack.getItem() instanceof BlockItem) {
                float scale = 0.7F;
                matrixStack.scale(scale, scale, scale);
                matrixStack.translate(0, -0.2F, 0);
            } else {
                float scale = 0.45F;
                matrixStack.scale(scale, scale, scale);
                matrixStack.translate(0, -0.1F, 0);
            }

            this.random.setSeed(Item.getIdFromItem(item.stack.getItem()) + item.stack.getDamage());
            int amount = this.getModelCount(item.stack);

            for (int i = 0; i < amount; i++) {
                matrixStack.push();
                if (amount > 1) {
                    matrixStack.translate(
                            (this.random.nextFloat() * 2.0F - 1.0F) * 0.25F * 0.5F,
                            (this.random.nextFloat() * 2.0F - 1.0F) * 0.25F * 0.5F,
                            (this.random.nextFloat() * 2.0F - 1.0F) * 0.25F * 0.5F);
                }
                Minecraft.getInstance().getItemRenderer().renderItem(item.stack, ItemCameraTransforms.TransformType.GROUND, k, i1, matrixStack, iRenderTypeBuffer);
                matrixStack.pop();
            }
            matrixStack.pop();
        }
        matrixStack.pop();
    }

    protected int getModelCount(ItemStack stack) {
        int i = 1;
        if (stack.getCount() > 48) {
            i = 5;
        } else if (stack.getCount() > 32) {
            i = 4;
        } else if (stack.getCount() > 16) {
            i = 3;
        } else if (stack.getCount() > 1) {
            i = 2;
        }
        return i;
    }
}
