package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.network.NetworkEdge;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jgrapht.GraphPath;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

public interface IPipeItem extends ValueIOSerializable {

    Map<Identifier, BiFunction<Identifier, ValueInput, IPipeItem>> TYPES = new HashMap<>(
        Collections.singletonMap(PipeItem.TYPE, PipeItem::new));

    ItemStack getContent();

    void setDestination(BlockPos startInventory, BlockPos destInventory, GraphPath<BlockPos, NetworkEdge> path);

    void updateInPipe(PipeBlockEntity currPipe);

    void drop(Level world, ItemStack stack);

    BlockPos getDestPipe();

    BlockPos getCurrentPipe();

    BlockPos getDestInventory();

    int getItemsOnTheWay(BlockPos goalInv);

    @OnlyIn(Dist.CLIENT)
    RenderState extractRenderState(BlockEntityRendererProvider.Context ctx, PipeBlockEntity blockEntity, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress);

    @OnlyIn(Dist.CLIENT)
    void render( PipeRenderer.State pipeState, RenderState state, Random random, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);

    static IPipeItem load(ValueInput input) {
        var type = Identifier.parse(input.getStringOr("type", null));
        var func = IPipeItem.TYPES.get(type);
        return func != null ? func.apply(type, input) : null;
    }

    @OnlyIn(Dist.CLIENT)
    class RenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float x;
        public float y;
        public float z;
    }
}
