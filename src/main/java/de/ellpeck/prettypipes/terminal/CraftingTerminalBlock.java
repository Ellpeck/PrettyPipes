package de.ellpeck.prettypipes.terminal;

import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.block.ContainerBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class CraftingTerminalBlock extends ItemTerminalBlock {

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new CraftingTerminalTileEntity();
    }
}
