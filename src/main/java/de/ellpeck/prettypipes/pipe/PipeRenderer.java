package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;

import java.util.Random;

public class PipeRenderer implements BlockEntityRenderer<PipeBlockEntity> {

    private final Random random = new Random();

    public PipeRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PipeBlockEntity tile, float partialTicks, PoseStack matrixStack, MultiBufferSource source, int light, int overlay) {
        if (!tile.getItems().isEmpty() && Config.CLIENT.pipeRender.get()) {
            matrixStack.pushPose();
            var tilePos = tile.getBlockPos();
            matrixStack.translate(-tilePos.getX(), -tilePos.getY(), -tilePos.getZ());
            for (var item : tile.getItems()) {
                matrixStack.pushPose();
                item.render(tile, matrixStack, this.random, partialTicks, light, overlay, source);
                matrixStack.popPose();
            }
            matrixStack.popPose();
        }
        if (tile.cover != null) {
            matrixStack.pushPose();
            ForgeBlockModelRenderer.enableCaching();
            var renderer = Minecraft.getInstance().getBlockRenderer();
            for (var layer : RenderType.chunkBufferLayers()) {
                if (!ItemBlockRenderTypes.canRenderInLayer(tile.cover, layer))
                    continue;
                ForgeHooksClient.setRenderType(layer);
                renderer.getModelRenderer().tesselateBlock(tile.getLevel(), renderer.getBlockModel(tile.cover), tile.cover, tile.getBlockPos(), matrixStack, source.getBuffer(layer), true, new Random(), tile.cover.getSeed(tile.getBlockPos()), overlay);
            }
            ForgeHooksClient.setRenderType(null);
            ForgeBlockModelRenderer.clearCache();
            matrixStack.popPose();
        }
    }

}
