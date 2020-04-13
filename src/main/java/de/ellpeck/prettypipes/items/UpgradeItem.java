package de.ellpeck.prettypipes.items;

import de.ellpeck.prettypipes.Registry;
import net.minecraft.item.Item;

public class UpgradeItem extends Item {
    public UpgradeItem() {
        super(new Properties().group(Registry.GROUP).maxStackSize(16));
    }
}
