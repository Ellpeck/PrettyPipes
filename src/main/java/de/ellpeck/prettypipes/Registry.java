package de.ellpeck.prettypipes;

import de.ellpeck.prettypipes.entities.PipeFrameEntity;
import de.ellpeck.prettypipes.entities.PipeFrameRenderer;
import de.ellpeck.prettypipes.items.*;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.IPipeConnectable;
import de.ellpeck.prettypipes.pipe.PipeBlock;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.PipeRenderer;
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
import de.ellpeck.prettypipes.pressurizer.PressurizerBlockEntity;
import de.ellpeck.prettypipes.pressurizer.PressurizerContainer;
import de.ellpeck.prettypipes.pressurizer.PressurizerGui;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlock;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlock;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.CraftingTerminalGui;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalContainer;
import de.ellpeck.prettypipes.terminal.containers.ItemTerminalGui;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(bus = Bus.MOD)
public final class Registry {

    public static final CreativeModeTab TAB = new CreativeModeTab(PrettyPipes.ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Registry.wrenchItem);
        }
    };

    public static Capability<PipeNetwork> pipeNetworkCapability = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static Capability<IPipeConnectable> pipeConnectableCapability = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static Item wrenchItem;
    public static Item pipeFrameItem;

    public static Block pipeBlock;
    public static BlockEntityType<PipeBlockEntity> pipeBlockEntity;
    public static MenuType<MainPipeContainer> pipeContainer;

    public static Block itemTerminalBlock;
    public static BlockEntityType<ItemTerminalBlockEntity> itemTerminalBlockEntity;
    public static MenuType<ItemTerminalContainer> itemTerminalContainer;

    public static Block craftingTerminalBlock;
    public static BlockEntityType<CraftingTerminalBlockEntity> craftingTerminalBlockEntity;
    public static MenuType<CraftingTerminalContainer> craftingTerminalContainer;

    public static EntityType<PipeFrameEntity> pipeFrameEntity;

    public static Block pressurizerBlock;
    public static BlockEntityType<PressurizerBlockEntity> pressurizerBlockEntity;
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
                Registry.pipeBlock = new PipeBlock().setRegistryName("pipe"),
                Registry.itemTerminalBlock = new ItemTerminalBlock().setRegistryName("item_terminal"),
                Registry.craftingTerminalBlock = new CraftingTerminalBlock().setRegistryName("crafting_terminal"),
                Registry.pressurizerBlock = new PressurizerBlock().setRegistryName("pressurizer")
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        var registry = event.getRegistry();
        registry.registerAll(
                Registry.wrenchItem = new WrenchItem().setRegistryName("wrench"),
                new Item(new Item.Properties().tab(Registry.TAB)).setRegistryName("blank_module"),
                Registry.pipeFrameItem = new PipeFrameItem().setRegistryName("pipe_frame")
        );
        registry.registerAll(Registry.createTieredModule("extraction_module", ExtractionModuleItem::new));
        registry.registerAll(Registry.createTieredModule("filter_module", FilterModuleItem::new));
        registry.registerAll(Registry.createTieredModule("speed_module", SpeedModuleItem::new));
        registry.registerAll(Registry.createTieredModule("low_priority_module", LowPriorityModuleItem::new));
        registry.registerAll(Registry.createTieredModule("high_priority_module", HighPriorityModuleItem::new));
        registry.registerAll(Registry.createTieredModule("retrieval_module", RetrievalModuleItem::new));
        registry.register(new StackSizeModuleItem("stack_size_module"));
        registry.registerAll(Arrays.stream(ItemEquality.Type.values()).map(t -> new FilterModifierModuleItem(t.name().toLowerCase(Locale.ROOT) + "_filter_modifier", t)).toArray(Item[]::new));
        registry.register(new RedstoneModuleItem("redstone_module"));
        registry.register(new FilterIncreaseModuleItem("filter_increase_modifier"));
        registry.registerAll(Registry.createTieredModule("crafting_module", CraftingModuleItem::new));
        registry.registerAll(Arrays.stream(SortingModuleItem.Type.values()).map(t -> new SortingModuleItem(t.name().toLowerCase(Locale.ROOT) + "_sorting_modifier", t)).toArray(Item[]::new));

        ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b.getRegistryName().getNamespace().equals(PrettyPipes.ID))
                .forEach(b -> registry.register(new BlockItem(b, new Item.Properties().tab(Registry.TAB)).setRegistryName(b.getRegistryName())));
    }

    @SubscribeEvent
    public static void registerBlockEntities(RegistryEvent.Register<BlockEntityType<?>> event) {
        event.getRegistry().registerAll(
                Registry.pipeBlockEntity = (BlockEntityType<PipeBlockEntity>) BlockEntityType.Builder.of(PipeBlockEntity::new, Registry.pipeBlock).build(null).setRegistryName("pipe"),
                Registry.itemTerminalBlockEntity = (BlockEntityType<ItemTerminalBlockEntity>) BlockEntityType.Builder.of(ItemTerminalBlockEntity::new, Registry.itemTerminalBlock).build(null).setRegistryName("item_terminal"),
                Registry.craftingTerminalBlockEntity = (BlockEntityType<CraftingTerminalBlockEntity>) BlockEntityType.Builder.of(CraftingTerminalBlockEntity::new, Registry.craftingTerminalBlock).build(null).setRegistryName("crafting_terminal"),
                Registry.pressurizerBlockEntity = (BlockEntityType<PressurizerBlockEntity>) BlockEntityType.Builder.of(PressurizerBlockEntity::new, Registry.pressurizerBlock).build(null).setRegistryName("pressurizer")
        );
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        event.getRegistry().registerAll(
                Registry.pipeFrameEntity = (EntityType<PipeFrameEntity>) EntityType.Builder.<PipeFrameEntity>of(PipeFrameEntity::new, MobCategory.MISC).build("pipe_frame").setRegistryName("pipe_frame")
        );
    }

    @SubscribeEvent
    public static void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        event.getRegistry().registerAll(
                Registry.pipeContainer = (MenuType<MainPipeContainer>) IForgeMenuType.create((windowId, inv, data) -> new MainPipeContainer(Registry.pipeContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("pipe"),
                Registry.itemTerminalContainer = (MenuType<ItemTerminalContainer>) IForgeMenuType.create((windowId, inv, data) -> new ItemTerminalContainer(Registry.itemTerminalContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("item_terminal"),
                Registry.craftingTerminalContainer = (MenuType<CraftingTerminalContainer>) IForgeMenuType.create((windowId, inv, data) -> new CraftingTerminalContainer(Registry.craftingTerminalContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("crafting_terminal"),
                Registry.pressurizerContainer = (MenuType<PressurizerContainer>) IForgeMenuType.create((windowId, inv, data) -> new PressurizerContainer(Registry.pressurizerContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("pressurizer"),
                Registry.extractionModuleContainer = Registry.createPipeContainer("extraction_module"),
                Registry.filterModuleContainer = Registry.createPipeContainer("filter_module"),
                Registry.retrievalModuleContainer = Registry.createPipeContainer("retrieval_module"),
                Registry.stackSizeModuleContainer = Registry.createPipeContainer("stack_size_module"),
                Registry.filterIncreaseModuleContainer = Registry.createPipeContainer("filter_increase_module"),
                Registry.craftingModuleContainer = Registry.createPipeContainer("crafting_module"),
                Registry.filterModifierModuleContainer = Registry.createPipeContainer("filter_modifier_module")
        );
    }

    private static <T extends AbstractPipeContainer<?>> MenuType<T> createPipeContainer(String name) {
        return (MenuType<T>) IForgeMenuType.create((windowId, inv, data) -> {
            var tile = Utility.getBlockEntity(PipeBlockEntity.class, inv.player.level, data.readBlockPos());
            var moduleIndex = data.readInt();
            var moduleStack = tile.modules.getStackInSlot(moduleIndex);
            return ((IModule) moduleStack.getItem()).getContainer(moduleStack, tile, windowId, inv, inv.player, moduleIndex);
        }).setRegistryName(name);
    }

    private static Item[] createTieredModule(String name, BiFunction<String, ModuleTier, ModuleItem> item) {
        List<Item> items = new ArrayList<>();
        for (var tier : ModuleTier.values())
            items.add(item.apply(name, tier).setRegistryName(tier.name().toLowerCase(Locale.ROOT) + "_" + name));
        return items.toArray(new Item[0]);
    }

    public static void setup(FMLCommonSetupEvent event) {
        PacketHandler.setup();
    }

    public static final class Client {

        public static void setup(FMLClientSetupEvent event) {
            ItemBlockRenderTypes.setRenderLayer(Registry.pipeBlock, RenderType.cutout());
            BlockEntityRenderers.register(Registry.pipeBlockEntity, PipeRenderer::new);
            EntityRenderers.register(Registry.pipeFrameEntity, PipeFrameRenderer::new);

            MenuScreens.register(Registry.pipeContainer, MainPipeGui::new);
            MenuScreens.register(Registry.itemTerminalContainer, ItemTerminalGui::new);
            MenuScreens.register(Registry.pressurizerContainer, PressurizerGui::new);
            MenuScreens.register(Registry.craftingTerminalContainer, CraftingTerminalGui::new);
            MenuScreens.register(Registry.extractionModuleContainer, ExtractionModuleGui::new);
            MenuScreens.register(Registry.filterModuleContainer, FilterModuleGui::new);
            MenuScreens.register(Registry.retrievalModuleContainer, RetrievalModuleGui::new);
            MenuScreens.register(Registry.stackSizeModuleContainer, StackSizeModuleGui::new);
            MenuScreens.register(Registry.filterIncreaseModuleContainer, FilterIncreaseModuleGui::new);
            MenuScreens.register(Registry.craftingModuleContainer, CraftingModuleGui::new);
            MenuScreens.register(Registry.filterModifierModuleContainer, FilterModifierModuleGui::new);
        }
    }
}
