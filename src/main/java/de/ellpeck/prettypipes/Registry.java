package de.ellpeck.prettypipes;

import com.mojang.datafixers.types.Func;
import de.ellpeck.prettypipes.blocks.pipe.*;
import de.ellpeck.prettypipes.items.ExtractionModuleItem;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.items.WrenchItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketHandler;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Bus.MOD)
public final class Registry {

    public static final ItemGroup GROUP = new ItemGroup(PrettyPipes.ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(wrenchItem);
        }
    };

    @CapabilityInject(PipeNetwork.class)
    public static Capability<PipeNetwork> pipeNetworkCapability;

    public static Item wrenchItem;

    public static Block pipeBlock;
    public static TileEntityType<PipeTileEntity> pipeTileEntity;
    public static ContainerType<PipeContainer> pipeContainer;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                pipeBlock = new PipeBlock().setRegistryName("pipe")
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.registerAll(
                wrenchItem = new WrenchItem().setRegistryName("wrench")
        );
        registry.registerAll(createTieredModule("extraction_module", ExtractionModuleItem::new));

        ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b.getRegistryName().getNamespace().equals(PrettyPipes.ID))
                .forEach(b -> registry.register(new BlockItem(b, new Item.Properties().group(GROUP)).setRegistryName(b.getRegistryName())));
    }

    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) {
        event.getRegistry().registerAll(
                pipeTileEntity = (TileEntityType<PipeTileEntity>) TileEntityType.Builder.create(PipeTileEntity::new, pipeBlock).build(null).setRegistryName("pipe")
        );
    }

    @SubscribeEvent
    public static void registerContainer(RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().registerAll(
                pipeContainer = (ContainerType<PipeContainer>) IForgeContainerType.create((windowId, inv, data) -> {
                    PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, inv.player.world, data.readBlockPos());
                    int openModule = data.readInt();
                    IModule module = openModule < 0 ? null : (IModule) tile.modules.getStackInSlot(openModule).getItem();
                    return tile != null ? new PipeContainer(pipeContainer, windowId, inv.player, tile, module) : null;
                }).setRegistryName("pipe")
        );
    }

    private static Item[] createTieredModule(String name, Function<ModuleTier, Item> item) {
        List<Item> items = new ArrayList<>();
        for (ModuleTier tier : ModuleTier.values())
            items.add(item.apply(tier).setRegistryName(tier.name().toLowerCase(Locale.ROOT) + "_" + name));
        return items.toArray(new Item[0]);
    }

    public static void setup(FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(PipeNetwork.class, new Capability.IStorage<PipeNetwork>() {
            @Nullable
            @Override
            public INBT writeNBT(Capability<PipeNetwork> capability, PipeNetwork instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<PipeNetwork> capability, PipeNetwork instance, Direction side, INBT nbt) {

            }
        }, () -> null);
        PacketHandler.setup();
    }

    public static void setupClient(FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(pipeBlock, RenderType.cutout());
        ClientRegistry.bindTileEntityRenderer(pipeTileEntity, PipeRenderer::new);
        ScreenManager.registerFactory(pipeContainer, PipeGui::new);
    }
}
