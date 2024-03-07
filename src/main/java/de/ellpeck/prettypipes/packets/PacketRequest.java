package de.ellpeck.prettypipes.packets;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.ItemTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketRequest implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(PrettyPipes.ID, "request");

    private final BlockPos pos;
    private final ItemStack stack;
    private final int nbtHash;
    private final int amount;

    public PacketRequest(BlockPos pos, ItemStack stack, int amount) {
        this.pos = pos;
        this.stack = stack;
        this.nbtHash = stack.hasTag() ? stack.getTag().hashCode() : 0;
        this.amount = amount;
    }

    public PacketRequest(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.stack = buf.readItem();
        this.nbtHash = buf.readVarInt();
        this.amount = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeItem(this.stack);
        buf.writeVarInt(this.nbtHash);
        buf.writeVarInt(this.amount);
    }

    @Override
    public ResourceLocation id() {
        return PacketRequest.ID;
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketRequest message, PlayPayloadContext ctx) {
        ctx.workHandler().execute(new Runnable() {
            @Override
            public void run() {
                var player = ctx.player().orElseThrow();
                var tile = Utility.getBlockEntity(ItemTerminalBlockEntity.class, player.level(), message.pos);
                message.stack.setCount(message.amount);
                tile.requestItem(player, message.stack, message.nbtHash);
            }
        });
    }

}
