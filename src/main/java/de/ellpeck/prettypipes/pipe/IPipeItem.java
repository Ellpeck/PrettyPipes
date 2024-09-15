package de.ellpeck.prettypipes.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.prettypipes.network.NetworkEdge;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.apache.commons.lang3.function.TriFunction;
import org.jgrapht.GraphPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public interface IPipeItem extends INBTSerializable<CompoundTag> {

    Map<ResourceLocation, TriFunction<HolderLookup.Provider, ResourceLocation, CompoundTag, IPipeItem>> TYPES = new HashMap<>(
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

    static IPipeItem load(HolderLookup.Provider provider, CompoundTag nbt) {
        var type = ResourceLocation.parse(nbt.getString("type"));
        var func = IPipeItem.TYPES.get(type);
        return func != null ? func.apply(provider, type, nbt) : null;
    }

}
