package de.ellpeck.prettypipes.packets;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import de.ellpeck.prettypipes.Utility;
import de.ellpeck.prettypipes.terminal.CraftingTerminalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PacketGhostSlot {

    private BlockPos pos;
    private List<Entry> stacks;

    public PacketGhostSlot(BlockPos pos, List<Entry> stacks) {
        this.pos = pos;
        this.stacks = stacks;
    }

    private PacketGhostSlot() {

    }

    public static PacketGhostSlot fromBytes(FriendlyByteBuf buf) {
        var packet = new PacketGhostSlot();
        packet.pos = buf.readBlockPos();
        packet.stacks = new ArrayList<>();
        for (var i = buf.readInt(); i > 0; i--)
            packet.stacks.add(new Entry(buf));
        return packet;
    }

    public static void toBytes(PacketGhostSlot packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.stacks.size());
        for (var entry : packet.stacks)
            entry.write(buf);
    }

    @SuppressWarnings("Convert2Lambda")
    public static void onMessage(PacketGhostSlot message, Supplier<NetworkEvent.Context> ctx) {
        var doIt = (Consumer<Player>) p -> {
            var tile = Utility.getBlockEntity(CraftingTerminalBlockEntity.class, p.level(), message.pos);
            if (tile != null)
                tile.setGhostItems(message.stacks);
        };

        // this whole thing is a dirty hack for allowing the same packet to be used
        // both client -> server and server -> client without any classloading issues
        Player player = ctx.get().getSender();
        // are we on the client?
        if (player == null) {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    doIt.accept(Minecraft.getInstance().player);
                }
            });
        } else {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    doIt.accept(player);
                }
            });
        }

        ctx.get().setPacketHandled(true);
    }

    public static class Entry {

        private final List<ItemStack> stacks;
        private final TagKey<Item> tag;

        public Entry(Level level, List<ItemStack> stacks) {
            var tag = Entry.getTagForStacks(level, stacks);
            if (tag != null) {
                this.stacks = null;
                this.tag = tag;
            } else {
                this.stacks = stacks;
                this.tag = null;
            }
        }

        public Entry(FriendlyByteBuf buf) {
            if (buf.readBoolean()) {
                this.tag = null;
                this.stacks = new ArrayList<>();
                for (var i = buf.readInt(); i > 0; i--)
                    this.stacks.add(buf.readItem());
            } else {
                this.stacks = null;
                this.tag = TagKey.create(Registries.ITEM, new ResourceLocation(buf.readUtf()));
            }
        }

        public List<ItemStack> getStacks(Level level) {
            if (this.stacks != null)
                return this.stacks;
            return Streams.stream(level.registryAccess().registry(Registries.ITEM).get().getTagOrEmpty(this.tag).iterator())
                    .filter(h -> h.value() != null & h.value() != Items.AIR)
                    .map(h -> new ItemStack(h.value())).collect(Collectors.toList());
        }

        public FriendlyByteBuf write(FriendlyByteBuf buf) {
            if (this.stacks != null) {
                buf.writeBoolean(true);
                buf.writeInt(this.stacks.size());
                for (var stack : this.stacks)
                    buf.writeItem(stack);
            } else {
                buf.writeBoolean(false);
                buf.writeUtf(this.tag.location().toString());
            }
            return buf;
        }

        private static TagKey<Item> getTagForStacks(Level level, List<ItemStack> stacks) {
            return level.registryAccess().registry(Registries.ITEM).get().getTags().filter(e -> {
                var tag = e.getSecond();
                if (tag.size() != stacks.size())
                    return false;
                for (var i = 0; i < tag.size(); i++) {
                    if (stacks.get(i).getItem() != tag.get(i).value())
                        return false;
                }
                return true;
            }).map(Pair::getFirst).findFirst().orElse(null);
        }
    }
}
