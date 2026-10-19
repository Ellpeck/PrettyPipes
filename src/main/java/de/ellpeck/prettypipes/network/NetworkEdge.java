package de.ellpeck.prettypipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

public class NetworkEdge extends DefaultWeightedEdge implements ValueIOSerializable {

    public final List<BlockPos> pipes = new ArrayList<>();

    public NetworkEdge() {
    }

    public NetworkEdge(ValueInput input) {
        this.deserialize(input);
    }

    public BlockPos getStartPipe() {
        return this.pipes.getFirst();
    }

    public BlockPos getEndPipe() {
        return this.pipes.getLast();
    }

    @Override
    public void serialize(ValueOutput output) {
        var list = output.list("pipes", BlockPos.CODEC);
        for (var pos : this.pipes)
            list.add(pos);
    }

    @Override
    public void deserialize(ValueInput input) {
        for (var pos : input.listOrEmpty("pipes", BlockPos.CODEC))
            this.pipes.add(pos);
    }


}
