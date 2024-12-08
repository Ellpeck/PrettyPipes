package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.ItemFilter.IFilteredContainer;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleItem;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleItem;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleItem;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static de.ellpeck.prettypipes.misc.DirectionSelector.IDirectionContainer;
import static de.ellpeck.prettypipes.pipe.modules.craft.CraftingModuleItem.*;

public record PacketButton(BlockPos pos, int result, List<Integer> data) implements CustomPacketPayload {

    public static final Type<PacketButton> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrettyPipes.ID, "button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketButton> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PacketButton::pos,
        ByteBufCodecs.INT, PacketButton::result,
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT), PacketButton::data,
        PacketButton::new);

    public PacketButton(BlockPos pos, ButtonResult result, List<Integer> data) {
        this(pos, result.ordinal(), data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PacketButton.TYPE;
    }

    public static void onMessage(PacketButton message, IPayloadContext ctx) {
        var player = ctx.player();
        ButtonResult.values()[message.result].action.accept(message.pos, message.data, player);
    }

    public static void sendAndExecute(BlockPos pos, ButtonResult result, List<Integer> data) {
        PacketDistributor.sendToServer(new PacketButton(pos, result, data));
        result.action.accept(pos, data, Minecraft.getInstance().player);
    }

    public enum ButtonResult {
        PIPE_TAB((pos, data, player) -> {
            var tile = Utility.getBlockEntity(PipeBlockEntity.class, player.level(), pos);
            if (data.getFirst() < 0) {
                player.openMenu(tile, pos);
            } else {
                var stack = tile.modules.getStackInSlot(data.getFirst());
                player.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return stack.getHoverName();
                    }

                    @Nullable
                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                        return ((IModule) stack.getItem()).getContainer(stack, tile, windowId, inv, player, data.getFirst());
                    }

                    @Override
                    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                        return false;
                    }
                }, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeInt(data.getFirst());
                });
            }
        }),
        FILTER_CHANGE((pos, data, player) -> {
            if (player.containerMenu instanceof IFilteredContainer filtered)
                filtered.getFilter().onButtonPacket(filtered, data.getFirst());
        }),
        INSERTION_TYPE_BUTTON((pos, data, player) -> {
            if (player.containerMenu instanceof CraftingModuleContainer container) {
                container.insertionType = InsertionType.values()[(container.insertionType.ordinal() + 1) % InsertionType.values().length];
                container.modified = true;
            }
        }),
        INSERT_UNSTACKED_BUTTON((pos, data, player) -> {
            if (player.containerMenu instanceof CraftingModuleContainer container) {
                container.insertUnstacked = !container.insertUnstacked;
                container.modified = true;
            }
        }),
        EMIT_REDSTONE_BUTTON((pos, data, player) -> {
            if (player.containerMenu instanceof CraftingModuleContainer container) {
                container.emitRedstone = !container.emitRedstone;
                container.modified = true;
            }
        }),
        STACK_SIZE_MODULE_BUTTON((pos, data, player) -> {
            var container = (AbstractPipeContainer<?>) player.containerMenu;
            var moduleData = container.moduleStack.getOrDefault(StackSizeModuleItem.Data.TYPE, StackSizeModuleItem.Data.DEFAULT);
            container.moduleStack.set(StackSizeModuleItem.Data.TYPE, new StackSizeModuleItem.Data(moduleData.maxStackSize(), !moduleData.limitToMaxStackSize()));
        }),
        STACK_SIZE_AMOUNT((pos, data, player) -> {
            var container = (AbstractPipeContainer<?>) player.containerMenu;
            var moduleData = container.moduleStack.getOrDefault(StackSizeModuleItem.Data.TYPE, StackSizeModuleItem.Data.DEFAULT);
            container.moduleStack.set(StackSizeModuleItem.Data.TYPE, new StackSizeModuleItem.Data(data.getFirst(), moduleData.limitToMaxStackSize()));
        }),
        CRAFT_TERMINAL_REQUEST((pos, data, player) -> {
            var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, player.level(), pos);
            tile.requestCraftingItems(player, data.getFirst(), data.get(1) > 0);
        }),
        CRAFT_TERMINAL_SEND_BACK((pos, data, player) -> {
            var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, player.level(), pos);
            tile.sendItemsBack();
        }),
        CANCEL_CRAFTING((pos, data, player) -> {
            var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, player.level(), pos);
            tile.cancelCrafting(data.getFirst() == 1);
        }),
        TAG_FILTER((pos, data, player) -> {
            var container = (FilterModifierModuleContainer) player.containerMenu;
            FilterModifierModuleItem.setFilterTag(container.moduleStack, container.getTags().get(data.getFirst()));
        }),
        DIRECTION_SELECTOR((pos, data, player) -> {
            if (player.containerMenu instanceof IDirectionContainer filtered)
                filtered.getSelector().onButtonPacket();
        });

        public final TriConsumer<BlockPos, List<Integer>, Player> action;

        ButtonResult(TriConsumer<BlockPos, List<Integer>, Player> action) {
            this.action = action;
        }
    }

}
