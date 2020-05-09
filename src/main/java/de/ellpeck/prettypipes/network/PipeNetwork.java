package de.ellpeck.prettypipes.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Streams;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemEqualityType;
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
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipeNetwork implements ICapabilitySerializable<CompoundNBT>, GraphListener<BlockPos, NetworkEdge> {

    public final ListenableGraph<BlockPos, NetworkEdge> graph;
    private final DijkstraShortestPath<BlockPos, NetworkEdge> dijkstra;
    private final Map<BlockPos, List<BlockPos>> nodeToConnectedNodes = new HashMap<>();
    private final Map<BlockPos, PipeTileEntity> tileCache = new HashMap<>();
    private final ListMultimap<BlockPos, PipeItem> pipeItems = ArrayListMultimap.create();
    private final ListMultimap<BlockPos, NetworkLock> networkLocks = ArrayListMultimap.create();
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
        return cap == Registry.pipeNetworkCapability ? LazyOptional.of(() -> (T) this) : LazyOptional.empty();
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
        nbt.put("items", Utility.serializeAll(this.pipeItems.values()));
        nbt.put("locks", Utility.serializeAll(this.networkLocks.values()));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.graph.removeAllVertices(new ArrayList<>(this.graph.vertexSet()));
        this.pipeItems.clear();
        this.networkLocks.clear();

        ListNBT nodes = nbt.getList("nodes", NBT.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++)
            this.graph.addVertex(NBTUtil.readBlockPos(nodes.getCompound(i)));
        ListNBT edges = nbt.getList("edges", NBT.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++)
            this.addEdge(new NetworkEdge(edges.getCompound(i)));
        for (PipeItem item : Utility.deserializeAll(nbt.getList("items", NBT.TAG_COMPOUND), PipeItem::new))
            this.pipeItems.put(item.getCurrentPipe(), item);
        for (NetworkLock lock : Utility.deserializeAll(nbt.getList("locks", NBT.TAG_COMPOUND), NetworkLock::new))
            this.createNetworkLock(lock);
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
        for (NetworkEdge edge : neighbors) {
            BlockPos end = edge.getEndPipe();
            this.refreshNode(end, this.world.getBlockState(end));
        }
    }

    public boolean tryInsertItem(BlockPos startPipePos, BlockPos startInventory, ItemStack stack, boolean preventOversending) {
        return this.routeItem(startPipePos, startInventory, stack, speed -> new PipeItem(stack, speed), preventOversending);
    }

    public boolean routeItem(BlockPos startPipePos, BlockPos startInventory, ItemStack stack, Function<Float, PipeItem> itemSupplier, boolean preventOversending) {
        if (!this.isNode(startPipePos))
            return false;
        if (!this.world.isBlockLoaded(startPipePos))
            return false;
        PipeTileEntity startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        this.startProfile("find_destination");
        for (BlockPos pipePos : this.getOrderedNetworkNodes(startPipePos)) {
            if (pipePos.equals(startPipePos))
                continue;
            PipeTileEntity pipe = this.getPipe(pipePos);
            BlockPos dest = pipe.getAvailableDestination(stack, false, preventOversending);
            if (dest != null) {
                this.endProfile();
                return this.routeItemToLocation(startPipePos, startInventory, pipe.getPos(), dest, itemSupplier);
            }
        }
        this.endProfile();
        return false;
    }

    public boolean routeItemToLocation(BlockPos startPipePos, BlockPos startInventory, BlockPos destPipePos, BlockPos destInventory, Function<Float, PipeItem> itemSupplier) {
        if (!this.isNode(startPipePos) || !this.isNode(destPipePos))
            return false;
        if (!this.world.isBlockLoaded(startPipePos) || !this.world.isBlockLoaded(destPipePos))
            return false;
        PipeTileEntity startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        this.startProfile("get_path");
        GraphPath<BlockPos, NetworkEdge> path = this.dijkstra.getPath(startPipePos, destPipePos);
        this.endProfile();
        if (path == null)
            return false;
        PipeItem item = itemSupplier.apply(startPipe.getItemSpeed());
        item.setDestination(startInventory, destInventory, path);
        if (!startPipe.getItems().contains(item))
            startPipe.getItems().add(item);
        PacketHandler.sendToAllLoaded(this.world, startPipePos, new PacketItemEnterPipe(startPipePos, item));
        return true;
    }

    public boolean requestItem(BlockPos destPipe, BlockPos destInventory, ItemStack stack, int amount, ItemEqualityType... equalityTypes) {
        List<NetworkLocation> locations = this.getOrderedNetworkItems(destPipe);
        for (NetworkLocation location : locations) {
            if (this.requestItem(location, destPipe, destInventory, stack, amount, equalityTypes))
                return true;
        }
        return false;
    }

    public boolean requestItem(NetworkLocation location, BlockPos destPipe, BlockPos destInventory, ItemStack stack, int amount, ItemEqualityType... equalityTypes) {
        if (location.pipePos.equals(destPipe))
            return false;
        for (int slot : location.getStackSlots(this.world, stack, equalityTypes)) {
            // try to extract from that location's inventory and send the item
            IItemHandler handler = location.getItemHandler(this.world);
            ItemStack extracted = handler.extractItem(slot, amount, true);
            if (this.routeItemToLocation(location.pipePos, location.getPos(), destPipe, destInventory, speed -> new PipeItem(extracted, speed))) {
                handler.extractItem(slot, extracted.getCount(), false);
                return true;
            }
        }
        return false;
    }

    public PipeTileEntity getPipe(BlockPos pos) {
        PipeTileEntity tile = this.tileCache.get(pos);
        if (tile == null || tile.isRemoved()) {
            tile = Utility.getTileEntity(PipeTileEntity.class, this.world, pos);
            this.tileCache.put(pos, tile);
        }
        return tile;
    }

    public List<NetworkLocation> getOrderedNetworkItems(BlockPos node) {
        if (!this.isNode(node))
            return Collections.emptyList();
        this.startProfile("get_network_items");
        List<NetworkLocation> info = new ArrayList<>();
        for (BlockPos dest : this.getOrderedNetworkNodes(node)) {
            PipeTileEntity pipe = this.getPipe(dest);
            if (!pipe.canNetworkSee())
                continue;
            for (Direction dir : Direction.values()) {
                IItemHandler handler = pipe.getItemHandler(dir, null);
                if (handler == null)
                    continue;
                // check if this handler already exists (double-connected pipes, double chests etc.)
                if (info.stream().anyMatch(l -> l.getItemHandler(this.world) == handler))
                    continue;
                NetworkLocation location = new NetworkLocation(dest, dir);
                if (!location.isEmpty(this.world))
                    info.add(location);
            }
        }
        this.endProfile();
        return info;
    }

    public void createNetworkLock(NetworkLock lock) {
        this.networkLocks.put(lock.location.getPos(), lock);
    }

    public void resolveNetworkLock(NetworkLock lock) {
        this.networkLocks.remove(lock.location.getPos(), lock);
    }

    public List<NetworkLock> getNetworkLocks(BlockPos pos) {
        return this.networkLocks.get(pos);
    }

    public int getLockedAmount(BlockPos pos, ItemStack stack, ItemEqualityType... equalityTypes) {
        return this.getNetworkLocks(pos).stream()
                .filter(l -> ItemEqualityType.compareItems(l.stack, stack, equalityTypes))
                .mapToInt(l -> l.stack.getCount()).sum();
    }

    private void refreshNode(BlockPos pos, BlockState state) {
        this.startProfile("refresh_node");
        this.graph.removeAllEdges(new ArrayList<>(this.graph.edgesOf(pos)));
        for (NetworkEdge edge : this.createAllEdges(pos, state, false))
            this.addEdge(edge);
        this.endProfile();
    }

    private void addEdge(NetworkEdge edge) {
        this.graph.addEdge(edge.getStartPipe(), edge.getEndPipe(), edge);
        // only use size - 1 so that nodes aren't counted twice for multi-edge paths
        this.graph.setEdgeWeight(edge, edge.pipes.size() - 1);
    }

    public BlockPos getNodeFromPipe(BlockPos pos) {
        if (this.isNode(pos))
            return pos;
        BlockState state = this.world.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeBlock))
            return null;
        for (Direction dir : Direction.values()) {
            NetworkEdge edge = this.createEdge(pos, state, dir, false);
            if (edge != null)
                return edge.getEndPipe();
        }
        return null;
    }

    private List<NetworkEdge> createAllEdges(BlockPos pos, BlockState state, boolean ignoreCurrBlocked) {
        this.startProfile("create_all_edges");
        List<NetworkEdge> edges = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            NetworkEdge edge = this.createEdge(pos, state, dir, ignoreCurrBlocked);
            if (edge != null)
                edges.add(edge);
        }
        this.endProfile();
        return edges;
    }

    private NetworkEdge createEdge(BlockPos pos, BlockState state, Direction dir, boolean ignoreCurrBlocked) {
        if (!ignoreCurrBlocked && !state.get(PipeBlock.DIRECTIONS.get(dir)).isConnected())
            return null;
        BlockPos currPos = pos.offset(dir);
        BlockState currState = this.world.getBlockState(currPos);
        if (!(currState.getBlock() instanceof PipeBlock))
            return null;
        this.startProfile("create_edge");
        NetworkEdge edge = new NetworkEdge();
        edge.pipes.add(pos);
        edge.pipes.add(currPos);

        while (true) {
            // if we found a vertex, we can stop since that's the next node
            // we do this here since the first offset pipe also needs to check this
            if (this.isNode(currPos)) {
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

    private List<BlockPos> getOrderedNetworkNodes(BlockPos node) {
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

    public void clearDestinationCache(BlockPos... nodes) {
        this.startProfile("clear_node_cache");
        // remove caches for the nodes
        for (BlockPos node : nodes)
            this.nodeToConnectedNodes.keySet().remove(node);
        // remove caches that contain the nodes as a destination
        this.nodeToConnectedNodes.values().removeIf(cached -> Arrays.stream(nodes).anyMatch(cached::contains));
        this.endProfile();
    }

    public List<PipeItem> getItemsInPipe(BlockPos pos) {
        return this.pipeItems.get(pos);
    }

    public Stream<PipeItem> getPipeItemsOnTheWay(BlockPos goalPipe) {
        this.startProfile("get_pipe_items_on_the_way");
        Stream<PipeItem> ret = this.pipeItems.values().stream().filter(i -> i.getDestPipe().equals(goalPipe));
        this.endProfile();
        return ret;
    }

    public int getItemsOnTheWay(BlockPos goalPipe, ItemStack type, ItemEqualityType... equalityTypes) {
        return this.getPipeItemsOnTheWay(goalPipe)
                .filter(i -> type == null || ItemEqualityType.compareItems(i.stack, type, equalityTypes))
                .mapToInt(i -> i.stack.getCount()).sum();
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        this.clearDestinationCache(e.getEdgeSource(), e.getEdgeTarget());
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        this.clearDestinationCache(e.getEdgeSource(), e.getEdgeTarget());
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<BlockPos> e) {
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<BlockPos> e) {
    }

    public void startProfile(String name) {
        this.world.getProfiler().startSection(() -> PrettyPipes.ID + ":pipe_network_" + name);
    }

    public void endProfile() {
        this.world.getProfiler().endSection();
    }

    public static PipeNetwork get(World world) {
        return world.getCapability(Registry.pipeNetworkCapability).orElse(null);
    }

}
