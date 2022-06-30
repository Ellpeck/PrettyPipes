package de.ellpeck.prettypipes.misc;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.packets.PacketButton;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class DirectionSelector {

    public Direction direction;
    private boolean modified;

    private final ItemStack stack;
    private final PipeBlockEntity pipe;

    public DirectionSelector(ItemStack stack, PipeBlockEntity pipe) {
        this.stack = stack;
        this.pipe = pipe;
        this.load();
    }

    @OnlyIn(Dist.CLIENT)
    public AbstractWidget getButton(int x, int y) {
        return new ExtendedButton(x, y, 100, 20, new TranslatableComponent("info." + PrettyPipes.ID + ".populate"), button ->
                PacketButton.sendAndExecute(this.pipe.getBlockPos(), PacketButton.ButtonResult.DIRECTION_SELECTOR)) {
            @Override
            public Component getMessage() {
                var pipe = DirectionSelector.this.pipe;
                var dir = DirectionSelector.this.direction;
                MutableComponent msg = new TranslatableComponent("dir." + PrettyPipes.ID + "." + (dir != null ? dir.getName() : "none"));
                if (dir != null) {
                    var blockName = pipe.getItemHandler(dir) != null ? pipe.getLevel().getBlockState(pipe.getBlockPos().relative(dir)).getBlock().getName() : null;
                    if (blockName != null)
                        msg = msg.append(" (").append(blockName).append(")");
                }
                return msg;
            }
        };
    }

    public void onButtonPacket() {
        var dir = this.getValidDirection(this.direction != null ? this.direction : Direction.UP);
        if (this.direction != dir) {
            this.direction = dir;
            this.modified = true;
        }
    }

    public void save() {
        if (!this.modified)
            return;
        this.modified = false;

        var tag = new CompoundTag();
        if (this.direction != null)
            tag.putString("direction", this.direction.getName());
        this.stack.getOrCreateTag().put("direction_selector", tag);
    }

    public void load() {
        if (this.stack.hasTag()) {
            var tag = this.stack.getTag().getCompound("direction_selector");
            this.direction = Direction.byName(tag.getString("direction"));
        }

        // default to the first direction with a container
        // don't mark as modified here because we don't want to save this automatic direction
        if (this.direction == null || !this.isDirectionValid(this.direction))
            this.direction = this.getValidDirection(Direction.UP);
    }

    private boolean isDirectionValid(Direction dir) {
        if (this.pipe.getItemHandler(dir) == null)
            return false;
        return this.pipe.streamModules()
                .filter(p -> p.getLeft() != this.stack)
                .map(p -> p.getRight().getDirectionSelector(p.getLeft(), this.pipe))
                .noneMatch(p -> p != null && p.direction == dir);
    }

    private Direction getValidDirection(Direction dir) {
        for (var i = 0; i < 6; i++) {
            dir = Direction.from3DDataValue(dir.get3DDataValue() + 1);
            if (this.isDirectionValid(dir))
                return dir;
        }
        return null;
    }

    public interface IDirectionContainer {

        DirectionSelector getSelector();

    }
}
