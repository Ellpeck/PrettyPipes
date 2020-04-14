package de.ellpeck.prettypipes.network;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.blocks.pipe.PipeBlock;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.packets.PacketItemEnterPipe;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.ClosestFirstIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PipeNetwork implements ICapabilitySerializable<CompoundNBT> {

    public final SimpleWeightedGraph<BlockPos, NetworkEdge> graph = new SimpleWeightedGraph<>(NetworkEdge.class);
    private final DijkstraShortestPath<BlockPos, NetworkEdge> dijkstra = new DijkstraShortestPath<>(this.graph);
    private final Map<BlockPos, PipeTileEntity> tileCache = new HashMap<>();
    private final World world;

    public PipeNetwork(World world) {
        this.world = world;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == Registry.pipeNetworkCapability ? LazyOptional.of(() -> (T) this) : null;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        ListNBT nodes = new ListNBT();
        for (BlockPos node : this.graph.vertexSet())
            nodes.add(NBTUtil.writeBlockPos(node));
        nbt.put("nodes", nodes);
        ListNBT edges = new ListNBT();
        for (NetworkEdge edge : this.graph.edgeSet())
            edges.add(edge.serializeNBT());
        nbt.put("edges", edges);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.graph.removeAllVertices(new ArrayList<>(this.graph.vertexSet()));
        ListNBT nodes = nbt.getList("nodes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++)
            this.graph.addVertex(NBTUtil.readBlockPos(nodes.getCompound(i)));
        ListNBT edges = nbt.getList("edges", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++)
            this.addEdge(new NetworkEdge(edges.getCompound(i)));
    }

    public void addNode(BlockPos pos, BlockState state) {
        if (!this.isNode(pos)) {
            this.graph.addVertex(pos);
            this.refreshNode(pos, state);
        }
    }

    public void removeNode(BlockPos pos) {
        if (this.isNode(pos))
            this.graph.removeVertex(pos);
    }

    public boolean isNode(BlockPos pos) {
        return this.graph.containsVertex(pos);
    }

    public void onPipeChanged(BlockPos pos, BlockState state) {
        List<NetworkEdge> neighbors = this.createAllEdges(pos, state, true);
        // if we only have one neighbor, then there can't be any new connections
        if (neighbors.size() <= 1 && !this.isNode(pos))
            return;
        for (NetworkEdge edge : neighbors)
            this.refreshNode(edge.endPipe, this.world.getBlockState(edge.endPipe));
    }

    public boolean tryInsertItem(BlockPos startPipePos, BlockPos originInv, ItemStack stack) {
        return this.routeItem(startPipePos, stack, () -> new PipeItem(stack, startPipePos, originInv));
    }

    public boolean routeItem(BlockPos startPipePos, ItemStack stack, Supplier<PipeItem> itemSupplier) {
        if (!this.isNode(startPipePos))
            return false;
        if (!this.world.isBlockLoaded(startPipePos))
            return false;
        PipeTileEntity startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        ClosestFirstIterator<BlockPos, NetworkEdge> it = new ClosestFirstIterator<>(this.graph, startPipePos);
        while (it.hasNext()) {
            PipeTileEntity pipe = this.getPipe(it.next());
            // don't try to insert into yourself, duh
            if (pipe == startPipe)
                continue;
            BlockPos dest = pipe.getAvailableDestination(stack);
            if (dest != null)
                return this.routeItemToLocation(startPipePos, pipe.getPos(), dest, itemSupplier);
        }
        return false;
    }

    public boolean routeItemToLocation(BlockPos startPipePos, BlockPos destPipe, BlockPos destInventory, Supplier<PipeItem> itemSupplier) {
        if (!this.isNode(startPipePos))
            return false;
        if (!this.world.isBlockLoaded(startPipePos))
            return false;
        PipeTileEntity startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        GraphPath<BlockPos, NetworkEdge> path = this.dijkstra.getPath(startPipePos, destPipe);
        PipeItem item = itemSupplier.get();
        item.setDestination(startPipePos, destPipe, destInventory, path);
        if (!startPipe.items.contains(item))
            startPipe.items.add(item);
        PacketHandler.sendToAllLoaded(this.world, startPipePos, new PacketItemEnterPipe(startPipePos, item));
        return true;
    }

    public PipeTileEntity getPipe(BlockPos pos) {
        PipeTileEntity tile = this.tileCache.get(pos);
        if (tile == null || tile.isRemoved()) {
            tile = Utility.getTileEntity(PipeTileEntity.class, this.world, pos);
            this.tileCache.put(pos, tile);
        }
        return tile;
    }

    private void refreshNode(BlockPos pos, BlockState state) {
        this.graph.removeAllEdges(new ArrayList<>(this.graph.edgesOf(pos)));
        for (NetworkEdge edge : this.createAllEdges(pos, state, false))
            this.addEdge(edge);
    }

    private void addEdge(NetworkEdge edge) {
        this.graph.addEdge(edge.startPipe, edge.endPipe, edge);
        this.graph.setEdgeWeight(edge, edge.pipes.size());
    }

    private List<NetworkEdge> createAllEdges(BlockPos pos, BlockState state, boolean allAround) {
        List<NetworkEdge> edges = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            NetworkEdge edge = this.createEdge(pos, state, dir, allAround);
            if (edge != null)
                edges.add(edge);
        }
        return edges;
    }

    private NetworkEdge createEdge(BlockPos pos, BlockState state, Direction dir, boolean allAround) {
        if (!allAround && !state.get(PipeBlock.DIRECTIONS.get(dir)).isConnected())
            return null;
        BlockPos currPos = pos.offset(dir);
        BlockState currState = this.world.getBlockState(currPos);
        if (!(currState.getBlock() instanceof PipeBlock))
            return null;
        NetworkEdge edge = new NetworkEdge();
        edge.startPipe = pos;
        edge.pipes.add(pos);
        edge.pipes.add(currPos);

        while (true) {
            // if we found a vertex, we can stop since that's the next node
            // we do this here since the first offset pipe also needs to check this
            if (this.isNode(currPos)) {
                edge.endPipe = edge.pipes.get(edge.pipes.size() - 1);
                return edge;
            }

            boolean found = false;
            for (Direction nextDir : Direction.values()) {
                if (!currState.get(PipeBlock.DIRECTIONS.get(nextDir)).isConnected())
                    continue;
                BlockPos offset = currPos.offset(nextDir);
                BlockState offState = this.world.getBlockState(offset);
                if (!(offState.getBlock() instanceof PipeBlock))
                    continue;
                if (edge.pipes.contains(offset))
                    continue;
                edge.pipes.add(offset);
                currPos = offset;
                currState = offState;
                found = true;
                break;
            }
            if (!found)
                break;
        }
        return null;
    }

    public static PipeNetwork get(World world) {
        return world.getCapability(Registry.pipeNetworkCapability).orElse(null);
    }
}
