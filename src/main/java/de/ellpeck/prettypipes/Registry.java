package de.ellpeck.prettypipes;

import de.ellpeck.prettypipes.entities.PipeFrameEntity;
import de.ellpeck.prettypipes.entities.PipeFrameRenderer;
import de.ellpeck.prettypipes.items.*;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeRenderer;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.pipe.containers.MainPipeContainer;
import de.ellpeck.prettypipes.pipe.containers.MainPipeGui;
import de.ellpeck.prettypipes.pipe.modules.*;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleGui;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleItem;
import de.ellpeck.prettypipes.pipe.modules.extraction.ExtractionModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.extraction.ExtractionModuleGui;
import de.ellpeck.prettypipes.pipe.modules.extraction.ExtractionModuleItem;
import de.ellpeck.prettypipes.pipe.modules.filter.FilterIncreaseModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.filter.FilterIncreaseModuleGui;
import de.ellpeck.prettypipes.pipe.modules.filter.FilterIncreaseModuleItem;
import de.ellpeck.prettypipes.pipe.modules.insertion.FilterModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.insertion.FilterModuleGui;
import de.ellpeck.prettypipes.pipe.modules.insertion.FilterModuleItem;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleGui;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleItem;
import de.ellpeck.prettypipes.pipe.modules.retrieval.RetrievalModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.retrieval.RetrievalModuleGui;
import de.ellpeck.prettypipes.pipe.modules.retrieval.RetrievalModuleItem;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleGui;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleItem;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlock;
import de.ellpeck.prettypipes.pressurizer.PressurizerContainer;
import de.ellpeck.prettypipes.pressurizer.PressurizerGui;
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlock;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlock;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalGui;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.MenuType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(bus = Bus.MOD)
public final class Registry {

    public static final CreativeModeTab GROUP = new CreativeModeTab(PrettyPipes.ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(wrenchItem);
        }
    };

    public static Capability<PipeNetwork> pipeNetworkCapability = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static Capability<IPipeConnectable> pipeConnectableCapability = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static Item wrenchItem;
    public static Item pipeFrameItem;

    public static Block pipeBlock;
    public static BlockEntityType<PipeBlockEntity> pipeTileEntity;
    public static MenuType<MainPipeContainer> pipeContainer;

    public static Block itemTerminalBlock;
    public static BlockEntityType<ItemTerminalBlockEntity> itemTerminalTileEntity;
    public static MenuType<ItemTerminalContainer> itemTerminalContainer;

    public static Block craftingTerminalBlock;
    public static BlockEntityType<CraftingTerminalBlockEntity> craftingTerminalTileEntity;
    public static MenuType<CraftingTerminalContainer> craftingTerminalContainer;

    public static EntityType<PipeFrameEntity> pipeFrameEntity;

    public static Block pressurizerBlock;
    public static BlockEntityType<PressurizerBlockEntity> pressurizerTileEntity;
    public static MenuType<PressurizerContainer> pressurizerContainer;

    public static MenuType<ExtractionModuleContainer> extractionModuleContainer;
    public static MenuType<FilterModuleContainer> filterModuleContainer;
    public static MenuType<RetrievalModuleContainer> retrievalModuleContainer;
    public static MenuType<StackSizeModuleContainer> stackSizeModuleContainer;
    public static MenuType<FilterIncreaseModuleContainer> filterIncreaseModuleContainer;
    public static MenuType<CraftingModuleContainer> craftingModuleContainer;
    public static MenuType<FilterModifierModuleContainer> filterModifierModuleContainer;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                pipeBlock = new PipeBlock().setRegistryName("pipe"),
                itemTerminalBlock = new ItemTerminalBlock().setRegistryName("item_terminal"),
                craftingTerminalBlock = new CraftingTerminalBlock().setRegistryName("crafting_terminal"),
                pressurizerBlock = new PressurizerBlock().setRegistryName("pressurizer")
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.registerAll(
                wrenchItem = new WrenchItem().setRegistryName("wrench"),
                new Item(new Item.Properties().group(GROUP)).setRegistryName("blank_module"),
                pipeFrameItem = new PipeFrameItem().setRegistryName("pipe_frame")
        );
        registry.registerAll(createTieredModule("extraction_module", ExtractionModuleItem::new));
        registry.registerAll(createTieredModule("filter_module", FilterModuleItem::new));
        registry.registerAll(createTieredModule("speed_module", SpeedModuleItem::new));
        registry.registerAll(createTieredModule("low_priority_module", LowPriorityModuleItem::new));
        registry.registerAll(createTieredModule("high_priority_module", HighPriorityModuleItem::new));
        registry.registerAll(createTieredModule("retrieval_module", RetrievalModuleItem::new));
        registry.register(new StackSizeModuleItem("stack_size_module"));
        registry.registerAll(Arrays.stream(ItemEquality.Type.values()).map(t -> new FilterModifierModuleItem(t.name().toLowerCase(Locale.ROOT) + "_filter_modifier", t)).toArray(Item[]::new));
        registry.register(new RedstoneModuleItem("redstone_module"));
        registry.register(new FilterIncreaseModuleItem("filter_increase_modifier"));
        registry.registerAll(createTieredModule("crafting_module", CraftingModuleItem::new));
        registry.registerAll(Arrays.stream(SortingModuleItem.Type.values()).map(t -> new SortingModuleItem(t.name().toLowerCase(Locale.ROOT) + "_sorting_modifier", t)).toArray(Item[]::new));

        ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b.getRegistryName().getNamespace().equals(PrettyPipes.ID))
                .forEach(b -> registry.register(new BlockItem(b, new Item.Properties().group(GROUP)).setRegistryName(b.getRegistryName())));
    }

    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) {
        event.getRegistry().registerAll(
                pipeTileEntity = (TileEntityType<PipeBlockEntity>) TileEntityType.Builder.create(PipeBlockEntity::new, pipeBlock).build(null).setRegistryName("pipe"),
                itemTerminalTileEntity = (TileEntityType<ItemTerminalBlockEntity>) TileEntityType.Builder.create(ItemTerminalBlockEntity::new, itemTerminalBlock).build(null).setRegistryName("item_terminal"),
                craftingTerminalTileEntity = (TileEntityType<CraftingTerminalBlockEntity>) TileEntityType.Builder.create(CraftingTerminalBlockEntity::new, craftingTerminalBlock).build(null).setRegistryName("crafting_terminal"),
                pressurizerTileEntity = (TileEntityType<PressurizerBlockEntity>) TileEntityType.Builder.create(PressurizerBlockEntity::new, pressurizerBlock).build(null).setRegistryName("pressurizer")
        );
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        event.getRegistry().registerAll(
                pipeFrameEntity = (EntityType<PipeFrameEntity>) EntityType.Builder.<PipeFrameEntity>create(PipeFrameEntity::new, EntityClassification.MISC).build("pipe_frame").setRegistryName("pipe_frame")
        );
    }

    @SubscribeEvent
    public static void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        event.getRegistry().registerAll(
                pipeContainer = (MenuType<MainPipeContainer>) IForgeMenuType.create((windowId, inv, data) -> new MainPipeContainer(pipeContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("pipe"),
                itemTerminalContainer = (MenuType<ItemTerminalContainer>) IForgeMenuType.create((windowId, inv, data) -> new ItemTerminalContainer(itemTerminalContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("item_terminal"),
                craftingTerminalContainer = (MenuType<CraftingTerminalContainer>) IForgeMenuType.create((windowId, inv, data) -> new CraftingTerminalContainer(craftingTerminalContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("crafting_terminal"),
                pressurizerContainer = (MenuType<PressurizerContainer>) IForgeMenuType.create((windowId, inv, data) -> new PressurizerContainer(pressurizerContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("pressurizer"),
                extractionModuleContainer = createPipeContainer("extraction_module"),
                filterModuleContainer = createPipeContainer("filter_module"),
                retrievalModuleContainer = createPipeContainer("retrieval_module"),
                stackSizeModuleContainer = createPipeContainer("stack_size_module"),
                filterIncreaseModuleContainer = createPipeContainer("filter_increase_module"),
                craftingModuleContainer = createPipeContainer("crafting_module"),
                filterModifierModuleContainer = createPipeContainer("filter_modifier_module")
        );
    }

    private static <T extends AbstractPipeContainer<?>> MenuType<T> createPipeContainer(String name) {
        return (MenuType<T>) IForgeMenuType.create((windowId, inv, data) -> {
            PipeBlockEntity tile = Utility.getBlockEntity(PipeBlockEntity.class, inv.player.world, data.readBlockPos());
            int moduleIndex = data.readInt();
            ItemStack moduleStack = tile.modules.getStackInSlot(moduleIndex);
            return ((IModule) moduleStack.getItem()).getContainer(moduleStack, tile, windowId, inv, inv.player, moduleIndex);
        }).setRegistryName(name);
    }

    private static Item[] createTieredModule(String name, BiFunction<String, ModuleTier, ModuleItem> item) {
        List<Item> items = new ArrayList<>();
        for (ModuleTier tier : ModuleTier.values())
            items.add(item.apply(name, tier).setRegistryName(tier.name().toLowerCase(Locale.ROOT) + "_" + name));
        return items.toArray(new Item[0]);
    }

    public static void setup(FMLCommonSetupEvent event) {
        registerCap(PipeNetwork.class);
        registerCap(IPipeConnectable.class);
        PacketHandler.setup();
    }

    public static final class Client {

        public static void setup(FMLClientSetupEvent event) {
            RenderTypeLookup.setRenderLayer(pipeBlock, RenderType.getCutout());
            ClientRegistry.bindTileEntityRenderer(pipeTileEntity, PipeRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(pipeFrameEntity, PipeFrameRenderer::new);

            MenuScreens.register(pipeContainer, MainPipeGui::new);
            MenuScreens.register(itemTerminalContainer, ItemTerminalGui::new);
            MenuScreens.register(pressurizerContainer, PressurizerGui::new);
            MenuScreens.register(craftingTerminalContainer, CraftingTerminalGui::new);
            MenuScreens.register(extractionModuleContainer, ExtractionModuleGui::new);
            MenuScreens.register(filterModuleContainer, FilterModuleGui::new);
            MenuScreens.register(retrievalModuleContainer, RetrievalModuleGui::new);
            MenuScreens.register(stackSizeModuleContainer, StackSizeModuleGui::new);
            MenuScreens.register(filterIncreaseModuleContainer, FilterIncreaseModuleGui::new);
            MenuScreens.register(craftingModuleContainer, CraftingModuleGui::new);
            MenuScreens.register(filterModifierModuleContainer, FilterModifierModuleGui::new);
        }
    }

    private static <T> void registerCap(Class<T> capClass) {
        CapabilityManager.INSTANCE.register(capClass, new Capability.IStorage<T>() {
            @Nullable
            @Override
            public INBT writeNBT(Capability<T> capability, T instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<T> capability, T instance, Direction side, INBT nbt) {

            }
        }, () -> null);
    }
}
