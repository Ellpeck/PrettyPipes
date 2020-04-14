package de.ellpeck.prettypipes.network;

import com.google.common.collect.Sets;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.blocks.pipe.PipeBlock;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
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

    public final SimpleWeightedGraph<BlockPos, NetworkEdge> graph = new SimpleWeightedGraph<>(NetworkEdge.class);
    private final DijkstraShortestPath<BlockPos, NetworkEdge> dijkstra = new DijkstraShortestPath<>(this.graph);
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
        List<NetworkEdge> neighbors = this.createAllEdges(pos, state, true);
        // if we only have one neighbor, then there can't be any new connections
        if (neighbors.size() <= 1 && !this.graph.containsVertex(pos))
            return;
        for (NetworkEdge edge : neighbors) {
            BlockPos end = edge.endPipe.getPos();
            this.refreshNode(end, this.world.getBlockState(end));
        }
        System.out.println(this.graph.toString());
    }

    private void refreshNode(BlockPos pos, BlockState state) {
        Set<NetworkEdge> edges = this.graph.edgesOf(pos);
        this.graph.removeAllEdges(new ArrayList<>(edges));

        for (NetworkEdge edge : this.createAllEdges(pos, state, false)) {
            this.graph.addEdge(edge.startPipe.getPos(), edge.endPipe.getPos(), edge);
            this.graph.setEdgeWeight(edge, edge.pipes.size());
        }
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
        PipeTileEntity startPipe = Utility.getTileEntity(PipeTileEntity.class, this.world, pos);
        if (startPipe != null) {
            edge.startPipe = startPipe;
            edge.pipes.add(startPipe);
        }
        edge.pipes.add(Utility.getTileEntity(PipeTileEntity.class, this.world, currPos));

        Set<BlockPos> seen = new HashSet<>();
        seen.add(pos);
        seen.add(currPos);
        while (true) {
            // if we found a vertex, we can stop since that's the next node
            // we do this here since the first offset pipe also needs to check this
            if (this.graph.containsVertex(currPos)) {
                edge.endPipe = edge.pipes.get(edge.pipes.size() - 1);
                return edge;
            }

            boolean found = false;
            for (Direction nextDir : Direction.values()) {
                if (!currState.get(PipeBlock.DIRECTIONS.get(nextDir)).isConnected())
                    continue;
                BlockPos offset = currPos.offset(nextDir);
                if (seen.contains(offset))
                    continue;
                seen.add(offset);
                BlockState offState = this.world.getBlockState(offset);
                if (!(offState.getBlock() instanceof PipeBlock))
                    continue;
                edge.pipes.add(Utility.getTileEntity(PipeTileEntity.class, this.world, offset));
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
