package de.ellpeck.prettypipes;

import de.ellpeck.prettypipes.blocks.PipeBlock;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Bus.MOD)
public final class Registry {

    public static Block pipe;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                pipe = new PipeBlock().setRegistryName("pipe")
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b.getRegistryName().getNamespace().equals(PrettyPipes.ID))
                .forEach(b -> event.getRegistry().register(new BlockItem(b, new Item.Properties().group(ItemGroup.MISC)).setRegistryName(b.getRegistryName())));
    }

    public static void setup(FMLCommonSetupEvent event) {

    }

    public static void setupClient(FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(pipe, RenderType.cutout());
    }
}
