package de.ellpeck.prettypipes.network;

import net.minecraft.tileentity.TileEntity;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

public class NetworkEdge extends DefaultWeightedEdge {

    public TileEntity startPipe;
    public List<TileEntity> pipes = new ArrayList<>();
    public TileEntity endPipe;
}
