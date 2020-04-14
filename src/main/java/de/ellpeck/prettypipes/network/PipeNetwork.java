package de.ellpeck.prettypipes.network;

import com.google.common.collect.Sets;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.blocks.pipe.PipeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jheaps.tree.FibonacciHeap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PipeNetwork implements ICapabilitySerializable<CompoundNBT> {

    public final SimpleWeightedGraph<BlockPos, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    private final DijkstraShortestPath<BlockPos, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(this.graph);
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
        return new CompoundNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

    }

    public void addNode(BlockPos pos, BlockState state) {
        if (!this.graph.containsVertex(pos)) {
            this.graph.addVertex(pos);
            this.refreshNode(pos, state);
        }
    }

    public void removeNode(BlockPos pos) {
        if (this.graph.containsVertex(pos))
            this.graph.removeVertex(pos);
    }

    public void onPipeChanged(BlockPos pos, BlockState state) {
        Set<Pair<BlockPos, Integer>> neighbors = this.findConnectedNodesInPipe(pos, state);
        // if we only have one neighbor, then there can't be any new connections
        if (neighbors.size() <= 1 && !this.graph.containsVertex(pos))
            return;
        for (Pair<BlockPos, Integer> node : neighbors)
            this.refreshNode(node.getLeft(), this.world.getBlockState(node.getLeft()));
        System.out.println(this.graph);
    }

    private void refreshNode(BlockPos pos, BlockState state) {
        Set<DefaultWeightedEdge> edges = this.graph.edgesOf(pos);
        this.graph.removeAllEdges(new ArrayList<>(edges));

        for (Pair<BlockPos, Integer> node : this.findConnectedNodesInPipe(pos, state)) {
            DefaultWeightedEdge edge = this.graph.addEdge(pos, node.getLeft());
            this.graph.setEdgeWeight(edge, node.getRight());
        }
    }

    private Set<Pair<BlockPos, Integer>> findConnectedNodesInPipe(BlockPos pos, BlockState state) {
        Set<Pair<BlockPos, Integer>> set = new HashSet<>();
        this.findConnectedNodesInPipe(pos, state, set, Sets.newHashSet(pos), 0);
        return set;
    }

    private void findConnectedNodesInPipe(BlockPos pos, BlockState state, Set<Pair<BlockPos, Integer>> nodes, Set<BlockPos> seen, int iterations) {
        if (!(state.getBlock() instanceof PipeBlock))
            return;
        for (Direction dir : Direction.values()) {
            if (!state.get(PipeBlock.DIRECTIONS.get(dir)).isConnected())
                continue;
            BlockPos offset = pos.offset(dir);
            if (seen.contains(offset))
                continue;
            seen.add(offset);
            if (this.graph.containsVertex(offset)) {
                nodes.add(Pair.of(offset, iterations));
                break;
            } else {
                this.findConnectedNodesInPipe(offset, this.world.getBlockState(offset), nodes, seen, iterations + 1);
            }
        }
    }

    public static PipeNetwork get(World world) {
        return world.getCapability(Registry.pipeNetworkCapability).orElse(null);
    }
}
