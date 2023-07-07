package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.ItemFilter.IFilteredContainer;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.modifier.FilterModifierModuleItem;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleItem;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static de.ellpeck.prettypipes.misc.DirectionSelector.IDirectionContainer;

public class PacketButton {

    private BlockPos pos;
    private ButtonResult result;
    private int[] data;

    public PacketButton(BlockPos pos, ButtonResult result, int... data) {
        this.pos = pos;
        this.result = result;
        this.data = data;
    }

    private PacketButton() {

    }

    public static PacketButton fromBytes(FriendlyByteBuf buf) {
        var packet = new PacketButton();
        packet.pos = buf.readBlockPos();
        packet.result = ButtonResult.values()[buf.readByte()];
        packet.data = buf.readVarIntArray();
        return packet;
    }

    public static void toBytes(PacketButton packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeByte(packet.result.ordinal());
        buf.writeVarIntArray(packet.data);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketButton message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                Player player = ctx.get().getSender();
                message.result.action.accept(message.pos, message.data, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendAndExecute(BlockPos pos, ButtonResult result, int... data) {
        PacketHandler.sendToServer(new PacketButton(pos, result, data));
        result.action.accept(pos, data, Minecraft.getInstance().player);
    }

    public enum ButtonResult {
        PIPE_TAB((pos, data, player) -> {
            var tile = Utility.getBlockEntity(PipeBlockEntity.class, player.level(), pos);
            if (data[0] < 0) {
                NetworkHooks.openScreen((ServerPlayer) player, tile, pos);
            } else {
                var stack = tile.modules.getStackInSlot(data[0]);
                NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return stack.getHoverName();
                    }

                    @Nullable
                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                        return ((IModule) stack.getItem()).getContainer(stack, tile, windowId, inv, player, data[0]);
                    }

                }, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeInt(data[0]);
                });
            }
        }),
        FILTER_CHANGE((pos, data, player) -> {
            if (player.containerMenu instanceof IFilteredContainer filtered)
                filtered.getFilter().onButtonPacket(filtered, data[0]);
        }),
        STACK_SIZE_MODULE_BUTTON((pos, data, player) -> {
            var container = (AbstractPipeContainer<?>) player.containerMenu;
            StackSizeModuleItem.setLimitToMaxStackSize(container.moduleStack, !StackSizeModuleItem.getLimitToMaxStackSize(container.moduleStack));
        }),
        STACK_SIZE_AMOUNT((pos, data, player) -> {
            var container = (AbstractPipeContainer<?>) player.containerMenu;
            StackSizeModuleItem.setMaxStackSize(container.moduleStack, data[0]);
        }),
        CRAFT_TERMINAL_REQUEST((pos, data, player) -> {
            var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, player.level(), pos);
            tile.requestCraftingItems(player, data[0], data[1] > 0);
        }),
        CANCEL_CRAFTING((pos, data, player) -> {
            var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, player.level(), pos);
            tile.cancelCrafting();
        }),
        TAG_FILTER((pos, data, player) -> {
            var container = (FilterModifierModuleContainer) player.containerMenu;
            FilterModifierModuleItem.setFilterTag(container.moduleStack, container.getTags().get(data[0]));
        }),
        DIRECTION_SELECTOR((pos, data, player) -> {
            if (player.containerMenu instanceof IDirectionContainer filtered)
                filtered.getSelector().onButtonPacket();
        });

        public final TriConsumer<BlockPos, int[], Player> action;

        ButtonResult(TriConsumer<BlockPos, int[], Player> action) {
            this.action = action;
        }
    }
}
