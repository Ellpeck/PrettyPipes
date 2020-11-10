package de.ellpeck.prettypipes.terminal;

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
