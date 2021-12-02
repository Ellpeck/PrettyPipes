package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;

import java.util.Random;

public class PipeRenderer implements BlockEntityRenderer<PipeBlockEntity> {

    private final Random random = new Random();

    public PipeRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PipeBlockEntity tile, float partialTicks, PoseStack matrixStack, MultiBufferSource source, int light, int overlay) {
        if (!tile.getItems().isEmpty()) {
            matrixStack.pushPose();
            BlockPos tilePos = tile.getBlockPos();
            matrixStack.translate(-tilePos.getX(), -tilePos.getY(), -tilePos.getZ());
            for (IPipeItem item : tile.getItems()) {
                matrixStack.pushPose();
                item.render(tile, matrixStack, this.random, partialTicks, light, overlay, source);
                matrixStack.popPose();
            }
            matrixStack.popPose();
        }
        if (tile.cover != null) {
            matrixStack.pushPose();
            ForgeBlockModelRenderer.enableCaching();
            // TODO figure out how to render covers, maybe finally use baked models bleh
            /*for (RenderType layer : RenderType.chunkBufferLayers()) {
                if (!RenderTypeLookup.canRenderInLayer(tile.cover, layer))
                    continue;
                ForgeHooksClient.setRenderType(layer);
                Minecraft.getInstance().getBlockRenderer().renderBatched(tile.cover,tile.getBlockPos(),null, matrixStack,null, light, overlay, EmptyModelData.INSTANCE);
            }*/
            ForgeHooksClient.setRenderType(null);
            ForgeBlockModelRenderer.clearCache();
            matrixStack.popPose();
        }
    }

}
