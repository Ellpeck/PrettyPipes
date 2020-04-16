package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.misc.ItemFilter.IFilteredContainer;
import de.ellpeck.prettypipes.pipe.PipeTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nullable;
import java.util.function.Supplier;

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

    public static PacketButton fromBytes(PacketBuffer buf) {
        PacketButton packet = new PacketButton();
        packet.pos = buf.readBlockPos();
        packet.result = ButtonResult.values()[buf.readByte()];
        packet.data = buf.readVarIntArray();
        return packet;
    }

    public static void toBytes(PacketButton packet, PacketBuffer buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeByte(packet.result.ordinal());
        buf.writeVarIntArray(packet.data);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketButton message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                PlayerEntity player = ctx.get().getSender();
                message.result.action.accept(message.pos, message.data, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public enum ButtonResult {
        PIPE_TAB((pos, data, player) -> {
            PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, player.world, pos);
            if (data[0] < 0) {
                NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
            } else {
                ItemStack stack = tile.modules.getStackInSlot(data[0]);
                NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
                    @Override
                    public ITextComponent getDisplayName() {
                        return stack.getDisplayName();
                    }

                    @Nullable
                    @Override
                    public Container createMenu(int windowId, PlayerInventory inv, PlayerEntity player) {
                        return ((IModule) stack.getItem()).getContainer(stack, tile, windowId, inv, player, data[0]);
                    }
                }, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeInt(data[0]);
                });
            }
        }),
        FILTER_CHANGE((pos, data, player) -> {
            IFilteredContainer container = (IFilteredContainer) player.openContainer;
            ItemFilter filter = container.getFilter();
            filter.onButtonPacket(data[0]);
        });

        public final TriConsumer<BlockPos, int[], PlayerEntity> action;

        ButtonResult(TriConsumer<BlockPos, int[], PlayerEntity> action) {
            this.action = action;
        }
    }
}
