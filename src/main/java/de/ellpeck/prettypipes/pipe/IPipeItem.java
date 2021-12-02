package de.ellpeck.prettypipes.pipe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.prettypipes.network.NetworkEdge;
import de.ellpeck.prettypipes.network.PipeItem;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

    void updateInPipe(PipeTileEntity currPipe);

    void drop(World world, ItemStack stack);

    BlockPos getDestPipe();

    BlockPos getCurrentPipe();

    BlockPos getDestInventory();

    int getItemsOnTheWay(BlockPos goalInv);

    @OnlyIn(Dist.CLIENT)
    void render(PipeTileEntity tile, MatrixStack matrixStack, Random random, float partialTicks, int light, int overlay, IRenderTypeBuffer buffer);

    static IPipeItem load(CompoundTag nbt) {
        // TODO legacy compat, remove eventually
        if (!nbt.contains("type"))
            nbt.putString("type", PipeItem.TYPE.toString());

        ResourceLocation type = new ResourceLocation(nbt.getString("type"));
        BiFunction<ResourceLocation, CompoundTag, IPipeItem> func = TYPES.get(type);
        return func != null ? func.apply(type, nbt) : null;
    }
}
