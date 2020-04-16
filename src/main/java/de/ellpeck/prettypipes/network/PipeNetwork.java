package de.ellpeck.prettypipes.network;

import com.google.common.collect.Streams;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
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
import org.jgrapht.ListenableGraph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PipeNetwork implements ICapabilitySerializable<CompoundNBT>, GraphListener<BlockPos, NetworkEdge> {

    public final ListenableGraph<BlockPos, NetworkEdge> graph;
    private final DijkstraShortestPath<BlockPos, NetworkEdge> dijkstra;
    private final Map<BlockPos, List<BlockPos>> nodeToConnectedNodes = new HashMap<>();
    private final Map<BlockPos, PipeTileEntity> tileCache = new HashMap<>();
    private final World world;

    public PipeNetwork(World world) {
        this.world = world;
        this.graph = new DefaultListenableGraph<>(new SimpleWeightedGraph<>(NetworkEdge.class));
        this.graph.addGraphListener(this);
        this.dijkstra = new DijkstraShortestPath<>(this.graph);
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

    public boolean tryInsertItem(BlockPos startPipePos, BlockPos startInventory, ItemStack stack) {
        return this.routeItem(startPipePos, startInventory, stack, speed -> new PipeItem(stack, speed));
    }

    public boolean routeItem(BlockPos startPipePos, BlockPos startInventory, ItemStack stack, Function<Float, PipeItem> itemSupplier) {
        if (!this.isNode(startPipePos))
            return false;
        if (!this.world.isBlockLoaded(startPipePos))
            return false;
        PipeTileEntity startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        this.startProfile("find_destination");
        for (BlockPos pipePos : this.getOrderedDestinations(startPipePos)) {
            PipeTileEntity pipe = this.getPipe(pipePos);
            BlockPos dest = pipe.getAvailableDestination(stack);
            if (dest != null) {
                this.endProfile();
                return this.routeItemToLocation(startPipePos, startInventory, pipe.getPos(), dest, itemSupplier);
            }
        }
        this.endProfile();
        return false;
    }

    public boolean routeItemToLocation(BlockPos startPipePos, BlockPos startInventory, BlockPos destPipe, BlockPos destInventory, Function<Float, PipeItem> itemSupplier) {
        if (!this.isNode(startPipePos))
            return false;
        if (!this.world.isBlockLoaded(startPipePos))
            return false;
        PipeTileEntity startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        this.startProfile("get_path");
        GraphPath<BlockPos, NetworkEdge> path = this.dijkstra.getPath(startPipePos, destPipe);
        this.endProfile();
        if (path == null)
            return false;
        PipeItem item = itemSupplier.apply(startPipe.getItemSpeed());
        item.setDestination(startPipePos, startInventory, destPipe, destInventory, path);
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
        this.startProfile("refresh_node");
        this.graph.removeAllEdges(new ArrayList<>(this.graph.edgesOf(pos)));
        for (NetworkEdge edge : this.createAllEdges(pos, state, false))
            this.addEdge(edge);
        this.endProfile();
    }

    private void addEdge(NetworkEdge edge) {
        this.graph.addEdge(edge.startPipe, edge.endPipe, edge);
        // only use size - 1 so that nodes aren't counted twice for multi-edge paths
        this.graph.setEdgeWeight(edge, edge.pipes.size() - 1);
    }

    private List<NetworkEdge> createAllEdges(BlockPos pos, BlockState state, boolean allAround) {
        this.startProfile("create_all_edges");
        List<NetworkEdge> edges = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            NetworkEdge edge = this.createEdge(pos, state, dir, allAround);
            if (edge != null)
                edges.add(edge);
        }
        this.endProfile();
        return edges;
    }

    private NetworkEdge createEdge(BlockPos pos, BlockState state, Direction dir, boolean allAround) {
        if (!allAround && !state.get(PipeBlock.DIRECTIONS.get(dir)).isConnected())
            return null;
        BlockPos currPos = pos.offset(dir);
        BlockState currState = this.world.getBlockState(currPos);
        if (!(currState.getBlock() instanceof PipeBlock))
            return null;
        this.startProfile("create_edge");
        NetworkEdge edge = new NetworkEdge();
        edge.startPipe = pos;
        edge.pipes.add(pos);
        edge.pipes.add(currPos);

        while (true) {
            // if we found a vertex, we can stop since that's the next node
            // we do this here since the first offset pipe also needs to check this
            if (this.isNode(currPos)) {
                edge.endPipe = edge.pipes.get(edge.pipes.size() - 1);
                this.endProfile();
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
        this.endProfile();
        return null;
    }

    public static PipeNetwork get(World world) {
        return world.getCapability(Registry.pipeNetworkCapability).orElse(null);
    }

    private List<BlockPos> getOrderedDestinations(BlockPos node) {
        List<BlockPos> ret = this.nodeToConnectedNodes.get(node);
        if (ret == null) {
            this.startProfile("compile_connected_nodes");
            ShortestPathAlgorithm.SingleSourcePaths<BlockPos, NetworkEdge> paths = this.dijkstra.getPaths(node);
            // sort destinations first by their priority (eg trash pipes should be last)
            // and then by their distance from the specified node
            ret = Streams.stream(new BreadthFirstIterator<>(this.graph, node))
                    .sorted(Comparator.<BlockPos>comparingInt(p -> this.getPipe(p).getPriority()).reversed().thenComparing(paths::getWeight))
                    .collect(Collectors.toList());
            this.nodeToConnectedNodes.put(node, ret);
            this.endProfile();
        }
        return ret;
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        this.edgeModified(e);
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        this.edgeModified(e);
    }

    private void edgeModified(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        // uncache all connection infos that contain the removed edge's vertices
        this.startProfile("clear_node_cache");
        this.nodeToConnectedNodes.values().removeIf(
                nodes -> nodes.stream().anyMatch(n -> n.equals(e.getEdgeSource()) || n.equals(e.getEdgeTarget())));
        this.endProfile();
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<BlockPos> e) {
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<BlockPos> e) {
    }

    private void startProfile(String name) {
        this.world.getProfiler().startSection(() -> PrettyPipes.ID + ":pipe_network_" + name);
    }

    private void endProfile() {
        this.world.getProfiler().endSection();
    }
}
