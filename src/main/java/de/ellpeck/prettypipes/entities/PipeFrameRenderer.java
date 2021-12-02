package de.ellpeck.prettypipes.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;

public class PipeFrameRenderer extends ItemFrameRenderer<PipeFrameEntity> {

    public PipeFrameRenderer(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void render(PipeFrameEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pushPose();
        var direction = entityIn.getDirection();
        var vec3d = this.getRenderOffset(entityIn, partialTicks);
        matrixStackIn.translate(-vec3d.x, -vec3d.y, -vec3d.z);
        matrixStackIn.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
        matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(entityIn.getXRot()));
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityIn.getYRot()));

        var font = this.getFont();
        var amount = entityIn.getAmount();
        var ammountStrg = amount < 0 ? "?" : String.valueOf(amount);
        var x = 0.5F - font.width(ammountStrg) / 2F;
        var matrix4f = matrixStackIn.last().pose();
        matrixStackIn.translate(0, 0.285F, 0.415F);
        matrixStackIn.scale(-0.02F, -0.02F, 0.02F);
        font.drawInBatch(ammountStrg, x, 0, 0xFFFFFF, true, matrix4f, bufferIn, false, 0, packedLightIn);

        matrixStackIn.popPose();
    }
}
