package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class PipeRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeRenderer.State> {

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final Random random = new Random();
    private final BlockEntityRendererProvider.Context ctx;

    public PipeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PipeBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.items.clear();
        for (var item : blockEntity.getItems())
            state.items.put(item, item.extractRenderState(this.ctx, blockEntity, partialTicks, cameraPosition, breakProgress));
        if (blockEntity.cover != null) {
            this.ctx.blockModelResolver().update(state.cover, blockEntity.cover, PipeRenderer.BLOCK_DISPLAY_CONTEXT);
        } else {
            state.cover.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.items.isEmpty()) {
            poseStack.pushPose();
            var tilePos = state.blockPos;
            poseStack.translate(-tilePos.getX(), -tilePos.getY(), -tilePos.getZ());
            for (var item : state.items.entrySet()) {
                poseStack.pushPose();
                item.getKey().render(state, item.getValue(), this.random, poseStack, submitNodeCollector, camera);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        if (!state.cover.isEmpty()) {
            poseStack.pushPose();
            state.cover.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(PipeBlockEntity blockEntity) {
        // our render bounding box should always be the full block in case we're covered
        return new AABB(blockEntity.getBlockPos());
    }

    public static class State extends BlockEntityRenderState {
        public final BlockModelRenderState cover = new BlockModelRenderState();
        public final Map<IPipeItem, IPipeItem.RenderState> items = new HashMap<>();
    }
}
