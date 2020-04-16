package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.blocks.pipe.PipeTileEntity;
import de.ellpeck.prettypipes.items.IModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
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
            NetworkHooks.openGui((ServerPlayerEntity) player, tile.createContainer(data[0]), buf -> {
                buf.writeBlockPos(pos);
                buf.writeInt(data[0]);
            });
        });

        public final TriConsumer<BlockPos, int[], PlayerEntity> action;

        ButtonResult(TriConsumer<BlockPos, int[], PlayerEntity> action) {
            this.action = action;
        }
    }
}
