package de.ellpeck.prettypipes;

import de.ellpeck.prettypipes.entities.PipeFrameEntity;
import de.ellpeck.prettypipes.entities.PipeFrameRenderer;
import de.ellpeck.prettypipes.items.*;
import de.ellpeck.prettypipes.misc.DirectionSelector;
import de.ellpeck.prettypipes.misc.ItemEquality;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.misc.ModuleClearingRecipe;
import de.ellpeck.prettypipes.packets.*;
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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiFunction;

import static net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.MOD)
public final class Registry {

    public static BlockCapability<IPipeConnectable, Direction> pipeConnectableCapability = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pipe_connectable"), IPipeConnectable.class);

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
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pipe"), Registry.pipeBlock = new PipeBlock(Properties.of().strength(2).sound(SoundType.STONE).noOcclusion()));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "item_terminal"), Registry.itemTerminalBlock = new ItemTerminalBlock(Properties.of().strength(3).sound(SoundType.STONE)));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "crafting_terminal"), Registry.craftingTerminalBlock = new CraftingTerminalBlock(Properties.of().strength(3).sound(SoundType.STONE)));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pressurizer"), Registry.pressurizerBlock = new PressurizerBlock(Properties.of().strength(3).sound(SoundType.STONE)));
        });

        event.register(Registries.ITEM, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "wrench"), Registry.wrenchItem = new WrenchItem());
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "blank_module"), new Item(new Item.Properties()));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pipe_frame"), Registry.pipeFrameItem = new PipeFrameItem());
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "stack_size_module"), new StackSizeModuleItem());
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "redstone_module"), new RedstoneModuleItem());
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "filter_increase_modifier"), new FilterIncreaseModuleItem());

            Registry.registerTieredModule(h, "extraction_module", ExtractionModuleItem::new);
            Registry.registerTieredModule(h, "filter_module", FilterModuleItem::new);
            Registry.registerTieredModule(h, "speed_module", SpeedModuleItem::new);
            Registry.registerTieredModule(h, "low_priority_module", LowPriorityModuleItem::new);
            Registry.registerTieredModule(h, "high_priority_module", HighPriorityModuleItem::new);
            Registry.registerTieredModule(h, "retrieval_module", RetrievalModuleItem::new);
            Registry.registerTieredModule(h, "crafting_module", CraftingModuleItem::new);

            for (var type : ItemEquality.Type.values()) {
                var name = type.name().toLowerCase(Locale.ROOT) + "_filter_modifier";
                h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, name), new FilterModifierModuleItem(name, type));
            }
            for (var type : SortingModuleItem.Type.values()) {
                var name = type.name().toLowerCase(Locale.ROOT) + "_sorting_modifier";
                h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, name), new SortingModuleItem(name, type));
            }

            BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(b -> b.getKey().location().getNamespace().equals(PrettyPipes.ID))
                .forEach(b -> h.register(b.getKey().location(), new BlockItem(b.getValue(), new Item.Properties())));
        });

        event.register(Registries.BLOCK_ENTITY_TYPE, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pipe"), Registry.pipeBlockEntity = BlockEntityType.Builder.of(PipeBlockEntity::new, Registry.pipeBlock).build(null));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "item_terminal"), Registry.itemTerminalBlockEntity = BlockEntityType.Builder.of(ItemTerminalBlockEntity::new, Registry.itemTerminalBlock).build(null));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "crafting_terminal"), Registry.craftingTerminalBlockEntity = BlockEntityType.Builder.of(CraftingTerminalBlockEntity::new, Registry.craftingTerminalBlock).build(null));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pressurizer"), Registry.pressurizerBlockEntity = BlockEntityType.Builder.of(PressurizerBlockEntity::new, Registry.pressurizerBlock).build(null));
        });

        event.register(Registries.ENTITY_TYPE, h ->
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pipe_frame"), Registry.pipeFrameEntity = EntityType.Builder.<PipeFrameEntity>of(PipeFrameEntity::new, MobCategory.MISC).build("pipe_frame")));

        event.register(Registries.MENU, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pipe"), Registry.pipeContainer = IMenuTypeExtension.create((windowId, inv, data) -> new MainPipeContainer(Registry.pipeContainer, windowId, inv.player, data.readBlockPos())));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "item_terminal"), Registry.itemTerminalContainer = IMenuTypeExtension.create((windowId, inv, data) -> new ItemTerminalContainer(Registry.itemTerminalContainer, windowId, inv.player, data.readBlockPos())));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "crafting_terminal"), Registry.craftingTerminalContainer = IMenuTypeExtension.create((windowId, inv, data) -> new CraftingTerminalContainer(Registry.craftingTerminalContainer, windowId, inv.player, data.readBlockPos())));
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "pressurizer"), Registry.pressurizerContainer = IMenuTypeExtension.create((windowId, inv, data) -> new PressurizerContainer(Registry.pressurizerContainer, windowId, inv.player, data.readBlockPos())));

            Registry.extractionModuleContainer = Registry.registerPipeContainer(h, "extraction_module");
            Registry.filterModuleContainer = Registry.registerPipeContainer(h, "filter_module");
            Registry.retrievalModuleContainer = Registry.registerPipeContainer(h, "retrieval_module");
            Registry.stackSizeModuleContainer = Registry.registerPipeContainer(h, "stack_size_module");
            Registry.filterIncreaseModuleContainer = Registry.registerPipeContainer(h, "filter_increase_module");
            Registry.craftingModuleContainer = Registry.registerPipeContainer(h, "crafting_module");
            Registry.filterModifierModuleContainer = Registry.registerPipeContainer(h, "filter_modifier_module");
        });

        event.register(BuiltInRegistries.CREATIVE_MODE_TAB.key(), h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "tab"), CreativeModeTab.builder()
                .title(Component.translatable("item_group." + PrettyPipes.ID + ".tab"))
                .icon(() -> new ItemStack(Registry.wrenchItem))
                .displayItems((params, output) -> BuiltInRegistries.ITEM.entrySet().stream()
                    .filter(b -> b.getKey().location().getNamespace().equals(PrettyPipes.ID))
                    .sorted(Comparator.comparing(b -> b.getValue().getClass().getSimpleName()))
                    .forEach(b -> output.accept(b.getValue()))).build()
            );
        });

        event.register(Registries.RECIPE_SERIALIZER, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "module_clearing"), ModuleClearingRecipe.SERIALIZER);
        });

        event.register(Registries.DATA_COMPONENT_TYPE, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "crafting_module_contents"), CraftingModuleItem.Contents.TYPE);
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "item_filter_data"), ItemFilter.Data.TYPE);
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "stack_size_module_data"), StackSizeModuleItem.Data.TYPE);
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "direction_selector_data"), DirectionSelector.Data.TYPE);
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "filter_modifier_data"), FilterModifierModuleItem.Data.TYPE);
            h.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "sorting_module_data"), SortingModuleItem.Data.TYPE);
        });
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.pipeBlockEntity, (e, d) -> e);
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.pressurizerBlockEntity, (e, d) -> e);
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.itemTerminalBlockEntity, (e, d) -> e);
        event.registerBlockEntity(Registry.pipeConnectableCapability, Registry.craftingTerminalBlockEntity, (e, d) -> e);

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.pressurizerBlockEntity, (e, d) -> e.storage);
    }

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PrettyPipes.ID);
        registrar.playBidirectional(PacketItemEnterPipe.TYPE, PacketItemEnterPipe.CODEC, PacketItemEnterPipe::onMessage);
        registrar.playBidirectional(PacketButton.TYPE, PacketButton.CODEC, PacketButton::onMessage);
        registrar.playBidirectional(PacketCraftingModuleTransfer.TYPE, PacketCraftingModuleTransfer.CODEC, PacketCraftingModuleTransfer::onMessage);
        registrar.playBidirectional(PacketGhostSlot.TYPE, PacketGhostSlot.CODEC, PacketGhostSlot::onMessage);
        registrar.playToServer(PacketFilterSlot.TYPE, PacketFilterSlot.CODEC, PacketFilterSlot::onMessage);
        registrar.playBidirectional(PacketNetworkItems.TYPE, PacketNetworkItems.CODEC, PacketNetworkItems::onMessage);
        registrar.playBidirectional(PacketRequest.TYPE, PacketRequest.CODEC, PacketRequest::onMessage);
    }

    private static <T extends AbstractPipeContainer<?>> MenuType<T> registerPipeContainer(RegisterEvent.RegisterHelper<MenuType<?>> helper, String name) {
        var type = (MenuType<T>) IMenuTypeExtension.create((windowId, inv, data) -> {
            var tile = Utility.getBlockEntity(PipeBlockEntity.class, inv.player.level(), data.readBlockPos());
            var moduleIndex = data.readInt();
            var moduleStack = tile.modules.getStackInSlot(moduleIndex);
            return ((IModule) moduleStack.getItem()).getContainer(moduleStack, tile, windowId, inv, inv.player, moduleIndex);
        });
        helper.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, name), type);
        return type;
    }

    private static void registerTieredModule(RegisterEvent.RegisterHelper<Item> helper, String name, BiFunction<String, ModuleTier, ModuleItem> item) {
        for (var tier : ModuleTier.values())
            helper.register(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, tier.name().toLowerCase(Locale.ROOT) + "_" + name), item.apply(name, tier));
    }

    @EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT)
    public static final class Client {

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            BlockEntityRenderers.register(Registry.pipeBlockEntity, PipeRenderer::new);
            EntityRenderers.register(Registry.pipeFrameEntity, PipeFrameRenderer::new);
        }

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(Registry.pipeContainer, MainPipeGui::new);
            event.register(Registry.itemTerminalContainer, ItemTerminalGui::new);
            event.register(Registry.pressurizerContainer, PressurizerGui::new);
            event.register(Registry.craftingTerminalContainer, CraftingTerminalGui::new);
            event.register(Registry.extractionModuleContainer, ExtractionModuleGui::new);
            event.register(Registry.filterModuleContainer, FilterModuleGui::new);
            event.register(Registry.retrievalModuleContainer, RetrievalModuleGui::new);
            event.register(Registry.stackSizeModuleContainer, StackSizeModuleGui::new);
            event.register(Registry.filterIncreaseModuleContainer, FilterIncreaseModuleGui::new);
            event.register(Registry.craftingModuleContainer, CraftingModuleGui::new);
            event.register(Registry.filterModifierModuleContainer, FilterModifierModuleGui::new);
        }

    }

}
