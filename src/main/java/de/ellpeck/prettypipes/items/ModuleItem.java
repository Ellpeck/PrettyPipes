package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.pipe.modules.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ModuleItem extends Item implements IModule {

    private final String name;

    public ModuleItem(String name) {
        super(new Properties().group(Registry.GROUP).maxStackSize(16));
        this.name = name;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        Utility.addTooltip(this.name, tooltip);
    }

    @Override
    public void tick(ItemStack module, PipeTileEntity tile) {

    }

    @Override
    public boolean canNetworkSee(ItemStack module, PipeTileEntity tile) {
        return true;
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeTileEntity tile, ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxInsertionAmount(ItemStack module, PipeTileEntity tile, ItemStack stack, IItemHandler destination) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPriority(ItemStack module, PipeTileEntity tile) {
        return 0;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeTileEntity tile, int windowId, PlayerInventory inv, PlayerEntity player, int moduleIndex) {
        return null;
    }

    @Override
    public float getItemSpeedIncrease(ItemStack module, PipeTileEntity tile) {
        return 0;
    }

    @Override
    public boolean canPipeWork(ItemStack module, PipeTileEntity tile) {
        return true;
    }
}
