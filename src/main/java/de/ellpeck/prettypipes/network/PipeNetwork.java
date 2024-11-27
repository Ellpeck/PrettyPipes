package de.ellpeck.prettypipes.network;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Streams;
import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.packets.PacketItemEnterPipe;
import de.ellpeck.prettypipes.pipe.IPipeItem;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.ListenableGraph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipeNetwork extends SavedData implements GraphListener<BlockPos, NetworkEdge> {

    private static final Factory<PipeNetwork> FACTORY = new Factory<>(PipeNetwork::new, PipeNetwork::new);
    private static PipeNetwork clientNetwork;

    public final ListenableGraph<BlockPos, NetworkEdge> graph;

    private final DijkstraShortestPath<BlockPos, NetworkEdge> dijkstra;
    private final Map<BlockPos, List<BlockPos>> nodeToConnectedNodes = new HashMap<>();
    private final Map<BlockPos, PipeBlockEntity> tileCache = new HashMap<>();
    private final ListMultimap<BlockPos, IPipeItem> pipeItems = ArrayListMultimap.create();
    private final ListMultimap<BlockPos, NetworkLock> networkLocks = ArrayListMultimap.create();
    private Level level;

    public PipeNetwork() {
        this.graph = new DefaultListenableGraph<>(new SimpleWeightedGraph<>(NetworkEdge.class));
        this.graph.addGraphListener(this);
        this.dijkstra = new DijkstraShortestPath<>(this.graph);
    }

    public PipeNetwork(CompoundTag nbt, HolderLookup.Provider provider) {
        this();
        for (var node : nbt.getList("nodes", Tag.TAG_INT_ARRAY))
            this.graph.addVertex(Utility.readBlockPos(node));
        var edges = nbt.getList("edges", Tag.TAG_COMPOUND);
        for (var i = 0; i < edges.size(); i++)
            this.addEdge(new NetworkEdge(provider, edges.getCompound(i)));
        for (var item : Utility.deserializeAll(nbt.getList("items", Tag.TAG_COMPOUND), i -> IPipeItem.load(provider, i)))
            this.pipeItems.put(item.getCurrentPipe(), item);
        for (var lock : Utility.deserializeAll(nbt.getList("locks", Tag.TAG_COMPOUND), t -> new NetworkLock(provider, t)))
            this.createNetworkLock(lock);
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        this.clearDestinationCache(e.getEdge().pipes);
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<BlockPos, NetworkEdge> e) {
        this.clearDestinationCache(e.getEdge().pipes);
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<BlockPos> e) {
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<BlockPos> e) {
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public String toString() {
        return "PipeNetwork{" +
            "\ngraph=" + this.graph +
            ",\nnodeToConnectedNodes=" + this.nodeToConnectedNodes +
            ",\ntileCache=" + this.tileCache.keySet() +
            ",\npipeItems=" + this.pipeItems +
            ",\nnetworkLocks=" + this.networkLocks + '}';
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        var nodes = new ListTag();
        for (var node : this.graph.vertexSet())
            nodes.add(NbtUtils.writeBlockPos(node));
        nbt.put("nodes", nodes);
        var edges = new ListTag();
        for (var edge : this.graph.edgeSet())
            edges.add(edge.serializeNBT(provider));
        nbt.put("edges", edges);
        nbt.put("items", Utility.serializeAll(this.pipeItems.values(), i -> i.serializeNBT(provider)));
        nbt.put("locks", Utility.serializeAll(this.networkLocks.values(), l -> l.serializeNBT(provider)));
        return nbt;
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
        var neighbors = this.createAllEdges(pos, state, true);
        // if we only have one neighbor, then there can't be any new connections
        if (neighbors.size() <= 1 && !this.isNode(pos))
            return;
        for (var edge : neighbors) {
            var end = edge.getEndPipe();
            this.refreshNode(end, this.level.getBlockState(end));
        }
    }

    public ItemStack routeItem(BlockPos startPipePos, BlockPos startInventory, ItemStack stack, boolean preventOversending) {
        return this.routeItem(startPipePos, startInventory, stack, PipeItem::new, preventOversending);
    }

    public ItemStack routeItem(BlockPos startPipePos, BlockPos startInventory, ItemStack stack, BiFunction<ItemStack, Float, IPipeItem> itemSupplier, boolean preventOversending) {
        if (!this.isNode(startPipePos))
            return stack;
        if (!this.level.isLoaded(startPipePos))
            return stack;
        var startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return stack;
        this.startProfile("find_destination");
        var nodes = this.getOrderedNetworkNodes(startPipePos);
        for (var i = 0; i < nodes.size(); i++) {
            var pipePos = nodes.get(startPipe.getNextNode(nodes, i));
            if (!this.level.isLoaded(pipePos))
                continue;
            var pipe = this.getPipe(pipePos);
            var dest = pipe.getAvailableDestination(Direction.values(), stack, false, preventOversending);
            if (dest == null || dest.getLeft().equals(startInventory))
                continue;
            var sup = (Function<Float, IPipeItem>) speed -> itemSupplier.apply(dest.getRight(), speed);
            if (this.routeItemToLocation(startPipePos, startInventory, pipe.getBlockPos(), dest.getLeft(), dest.getRight(), sup)) {
                var remain = stack.copy();
                remain.shrink(dest.getRight().getCount());
                this.endProfile();
                return remain;
            }
        }
        this.endProfile();
        return stack;
    }

    public boolean routeItemToLocation(BlockPos startPipePos, BlockPos startInventory, BlockPos destPipePos, BlockPos destInventory, ItemStack stack, Function<Float, IPipeItem> itemSupplier) {
        if (!this.isNode(startPipePos) || !this.isNode(destPipePos))
            return false;
        if (!this.level.isLoaded(startPipePos) || !this.level.isLoaded(destPipePos))
            return false;
        var startPipe = this.getPipe(startPipePos);
        if (startPipe == null)
            return false;
        this.startProfile("get_path");
        var path = this.dijkstra.getPath(startPipePos, destPipePos);
        this.endProfile();
        if (path == null)
            return false;
        var item = itemSupplier.apply(startPipe.getItemSpeed(stack));
        item.setDestination(startInventory, destInventory, path);
        startPipe.addNewItem(item);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) this.level, new ChunkPos(startPipePos), new PacketItemEnterPipe(startPipePos, item.serializeNBT(this.level.registryAccess())));
        return true;
    }

    public ItemStack requestItem(BlockPos destPipe, BlockPos destInventory, ItemStack stack, ItemEquality... equalityTypes) {
        var remain = stack.copy();
        // check existing items
        for (var location : this.getOrderedNetworkItems(destPipe)) {
            remain = this.requestExistingItem(location, destPipe, destInventory, null, remain, equalityTypes);
            if (remain.isEmpty())
                return remain;
        }
        // check craftable items
        return this.requestCraftedItem(destPipe, null, remain, new Stack<>(), equalityTypes);
    }

    public ItemStack requestCraftedItem(BlockPos destPipe, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain, ItemEquality... equalityTypes) {
        for (var craftable : this.getAllCraftables(destPipe)) {
            if (!ItemEquality.compareItems(stack, craftable.getRight(), equalityTypes))
                continue;
            var pipe = this.getPipe(craftable.getLeft());
            if (pipe == null)
                continue;
            stack = pipe.craft(destPipe, unavailableConsumer, stack, dependencyChain);
            if (stack.isEmpty())
                break;
        }
        return stack;
    }

    public ItemStack requestExistingItem(NetworkLocation location, BlockPos destPipe, BlockPos destInventory, NetworkLock ignoredLock, ItemStack stack, ItemEquality... equalityTypes) {
        return this.requestExistingItem(location, destPipe, destInventory, ignoredLock, PipeItem::new, stack, equalityTypes);
    }

    public ItemStack requestExistingItem(NetworkLocation location, BlockPos destPipe, BlockPos destInventory, NetworkLock ignoredLock, BiFunction<ItemStack, Float, IPipeItem> itemSupplier, ItemStack stack, ItemEquality... equalityTypes) {
        if (location.getPos().equals(destInventory))
            return stack;
        // make sure we don't pull any locked items
        var amount = location.getItemAmount(this.level, stack, equalityTypes);
        if (amount <= 0)
            return stack;
        amount -= this.getLockedAmount(location.getPos(), stack, ignoredLock, equalityTypes);
        if (amount <= 0)
            return stack;
        var remain = stack.copy();
        // make sure we only extract less than or equal to the requested amount
        if (remain.getCount() < amount)
            amount = remain.getCount();
        remain.shrink(amount);
        for (int slot : location.getStackSlots(this.level, stack, equalityTypes)) {
            // try to extract from that location's inventory and send the item
            var handler = location.getItemHandler(this.level);
            var extracted = handler.extractItem(slot, amount, true);
            if (this.routeItemToLocation(location.pipePos, location.getPos(), destPipe, destInventory, extracted, speed -> itemSupplier.apply(extracted, speed))) {
                handler.extractItem(slot, extracted.getCount(), false);
                amount -= extracted.getCount();
                if (amount <= 0)
                    break;
            }
        }
        return remain;
    }

    public PipeBlockEntity getPipe(BlockPos pos) {
        var tile = this.tileCache.get(pos);
        if (tile == null || tile.isRemoved()) {
            tile = Utility.getBlockEntity(PipeBlockEntity.class, this.level, pos);
            if (tile != null)
                this.tileCache.put(pos, tile);
        }
        return tile;
    }

    public void uncachePipe(BlockPos pos) {
        this.tileCache.remove(pos);
    }

    public List<Pair<BlockPos, ItemStack>> getCurrentlyCrafting(BlockPos node, boolean includeCanceled, ItemEquality... equalityTypes) {
        this.startProfile("get_currently_crafting");
        List<Pair<BlockPos, ItemStack>> items = new ArrayList<>();
        var craftingPipes = this.getAllCraftables(node).stream().map(c -> this.getPipe(c.getLeft())).distinct().iterator();
        while (craftingPipes.hasNext()) {
            var pipe = craftingPipes.next();
            for (var craft : pipe.activeCrafts) {
                var data = craft.getRight();
                if (!includeCanceled && data.canceled)
                    continue;
                // add up all the items that should go to the same location
                var existing = items.stream()
                    .filter(s -> s.getLeft().equals(data.resultDestPipe) && ItemEquality.compareItems(s.getRight(), data.resultStackRemain, equalityTypes))
                    .findFirst();
                if (existing.isPresent()) {
                    existing.get().getRight().grow(data.resultStackRemain.getCount());
                } else {
                    items.add(Pair.of(data.resultDestPipe, data.resultStackRemain.copy()));
                }
            }
        }
        this.endProfile();
        return items;
    }

    public int getCurrentlyCraftingAmount(BlockPos destNode, ItemStack stack, boolean includeCanceled, ItemEquality... equalityTypes) {
        return this.getCurrentlyCrafting(destNode, includeCanceled).stream()
            .filter(p -> p.getLeft().equals(destNode) && ItemEquality.compareItems(p.getRight(), stack, equalityTypes))
            .mapToInt(p -> p.getRight().getCount()).sum();
    }

    public List<Pair<BlockPos, ItemStack>> getAllCraftables(BlockPos node) {
        if (!this.isNode(node))
            return Collections.emptyList();
        this.startProfile("get_all_craftables");
        List<Pair<BlockPos, ItemStack>> craftables = new ArrayList<>();
        for (var dest : this.getOrderedNetworkNodes(node)) {
            if (!this.level.isLoaded(dest))
                continue;
            var pipe = this.getPipe(dest);
            for (var stack : pipe.getAllCraftables())
                craftables.add(Pair.of(pipe.getBlockPos(), stack));
        }
        this.endProfile();
        return craftables;
    }

    public int getCraftableAmount(BlockPos node, Consumer<ItemStack> unavailableConsumer, ItemStack stack, Stack<ItemStack> dependencyChain, ItemEquality... equalityTypes) {
        var total = 0;
        for (var pair : this.getAllCraftables(node)) {
            if (!ItemEquality.compareItems(pair.getRight(), stack, equalityTypes))
                continue;
            if (!this.level.isLoaded(pair.getLeft()))
                continue;
            var pipe = this.getPipe(pair.getLeft());
            if (pipe != null)
                total += pipe.getCraftableAmount(unavailableConsumer, stack, dependencyChain);
        }
        return total;
    }

    public List<NetworkLocation> getOrderedNetworkItems(BlockPos node) {
        if (!this.isNode(node))
            return Collections.emptyList();
        this.startProfile("get_network_items");
        var ret = new LinkedHashMap<IItemHandler, NetworkLocation>();
        for (var dest : this.getOrderedNetworkNodes(node)) {
            if (!this.level.isLoaded(dest))
                continue;
            var pipe = this.getPipe(dest);
            for (var dir : Direction.values()) {
                var handler = pipe.getItemHandler(dir);
                if (handler == null || !pipe.canNetworkSee(dir, handler))
                    continue;
                // check if this handler already exists (double-connected pipes, double chests etc.)
                if (ret.containsKey(handler))
                    continue;
                var location = new NetworkLocation(dest, dir);
                if (!location.isEmpty(this.level))
                    ret.put(handler, location);
            }
        }
        this.endProfile();
        return new ArrayList<>(ret.values());
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

    public int getLockedAmount(BlockPos pos, ItemStack stack, NetworkLock ignoredLock, ItemEquality... equalityTypes) {
        return this.getNetworkLocks(pos).stream()
            .filter(l -> !l.equals(ignoredLock) && ItemEquality.compareItems(l.stack, stack, equalityTypes))
            .mapToInt(l -> l.stack.getCount()).sum();
    }

    private void refreshNode(BlockPos pos, BlockState state) {
        this.startProfile("refresh_node");
        this.graph.removeAllEdges(new ArrayList<>(this.graph.edgesOf(pos)));
        for (var edge : this.createAllEdges(pos, state, false))
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
        var state = this.level.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeBlock))
            return null;
        for (var dir : Direction.values()) {
            var edge = this.createEdge(pos, state, dir, false);
            if (edge != null)
                return edge.getEndPipe();
        }
        return null;
    }

    public void clearCaches() {
        this.nodeToConnectedNodes.clear();
        this.tileCache.clear();
    }

    public void unlock() {
        this.networkLocks.clear();
    }

    private List<NetworkEdge> createAllEdges(BlockPos pos, BlockState state, boolean ignoreCurrBlocked) {
        this.startProfile("create_all_edges");
        List<NetworkEdge> edges = new ArrayList<>();
        for (var dir : Direction.values()) {
            var edge = this.createEdge(pos, state, dir, ignoreCurrBlocked);
            if (edge != null)
                edges.add(edge);
        }
        this.endProfile();
        return edges;
    }

    private NetworkEdge createEdge(BlockPos pos, BlockState state, Direction dir, boolean ignoreCurrBlocked) {
        if (!ignoreCurrBlocked && !state.getValue(PipeBlock.DIRECTIONS.get(dir)).isConnected())
            return null;
        var currPos = pos.relative(dir);
        var currState = this.level.getBlockState(currPos);
        if (!(currState.getBlock() instanceof PipeBlock))
            return null;
        this.startProfile("create_edge");
        var edge = new NetworkEdge();
        edge.pipes.add(pos);
        edge.pipes.add(currPos);

        while (true) {
            // if we found a vertex, we can stop since that's the next node
            // we do this here since the first offset pipe also needs to check this
            if (this.isNode(currPos)) {
                this.endProfile();
                return edge;
            }

            var found = false;
            for (var nextDir : Direction.values()) {
                if (!currState.getValue(PipeBlock.DIRECTIONS.get(nextDir)).isConnected())
                    continue;
                var offset = currPos.relative(nextDir);
                var offState = this.level.getBlockState(offset);
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

    public List<BlockPos> getOrderedNetworkNodes(BlockPos node) {
        if (!this.isNode(node))
            return Collections.emptyList();
        var ret = this.nodeToConnectedNodes.get(node);
        if (ret == null) {
            this.startProfile("compile_connected_nodes");
            var paths = this.dijkstra.getPaths(node);
            // sort destinations first by their priority (eg trash pipes should be last)
            // and then by their distance from the specified node
            ret = Streams.stream(new BreadthFirstIterator<>(this.graph, node))
                .filter(p -> this.getPipe(p) != null)
                .sorted(Comparator.<BlockPos>comparingInt(p -> this.getPipe(p).getPriority()).reversed().thenComparing(paths::getWeight))
                .collect(Collectors.toList());
            this.nodeToConnectedNodes.put(node, ret);
            this.endProfile();
        }
        return ret;
    }

    public void clearDestinationCache(List<BlockPos> nodes) {
        this.startProfile("clear_node_cache");
        // remove caches for the nodes
        for (var node : nodes)
            this.nodeToConnectedNodes.keySet().remove(node);
        // remove caches that contain the nodes as a destination
        this.nodeToConnectedNodes.values().removeIf(cached -> nodes.stream().anyMatch(cached::contains));
        this.endProfile();
    }

    public List<IPipeItem> getItemsInPipe(BlockPos pos) {
        return this.pipeItems.get(pos);
    }

    public Stream<IPipeItem> getPipeItemsOnTheWay(BlockPos goalInv) {
        this.startProfile("get_pipe_items_on_the_way");
        var ret = this.pipeItems.values().stream().filter(i -> i.getDestInventory().equals(goalInv));
        this.endProfile();
        return ret;
    }

    public int getItemsOnTheWay(BlockPos goalInv, ItemStack type, ItemEquality... equalityTypes) {
        return this.getPipeItemsOnTheWay(goalInv)
            .filter(i -> type == null || ItemEquality.compareItems(i.getContent(), type, equalityTypes))
            .mapToInt(i -> i.getItemsOnTheWay(goalInv)).sum();
    }

    public void startProfile(String name) {
        if (this.level != null)
            this.level.getProfiler().push(() -> PrettyPipes.ID + ":pipe_network_" + name);
    }

    public void endProfile() {
        if (this.level != null)
            this.level.getProfiler().pop();
    }

    public static PipeNetwork get(Level level) {
        if (level instanceof ServerLevel server) {
            var ret = server.getDataStorage().computeIfAbsent(PipeNetwork.FACTORY, "pipe_network");
            if (ret.level == null)
                ret.level = level;
            return ret;
        } else {
            if (PipeNetwork.clientNetwork == null || PipeNetwork.clientNetwork.level != level) {
                PipeNetwork.clientNetwork = new PipeNetwork();
                PipeNetwork.clientNetwork.level = level;
            }
            return PipeNetwork.clientNetwork;
        }
    }

}
