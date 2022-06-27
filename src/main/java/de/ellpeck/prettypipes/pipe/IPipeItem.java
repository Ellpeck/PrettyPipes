package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.network.NetworkEdge;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.INBTSerializable;
import org.jgrapht.GraphPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

public interface IPipeItem extends INBTSerializable<CompoundTag> {

    Map<ResourceLocation, BiFunction<ResourceLocation, CompoundTag, IPipeItem>> TYPES = new HashMap<>(
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
    void render(PipeBlockEntity tile, PoseStack matrixStack, Random random, float partialTicks, int light, int overlay, MultiBufferSource source);

    static IPipeItem load(CompoundTag nbt) {
        var type = new ResourceLocation(nbt.getString("type"));
        var func = IPipeItem.TYPES.get(type);
        return func != null ? func.apply(type, nbt) : null;
    }
}
