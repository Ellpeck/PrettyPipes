package de.ellpeck.prettypipes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(PrettyPipes.ID)
public final class PrettyPipes {

    public static final String ID = "prettypipes";

    public PrettyPipes(IEventBus eventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT)
            eventBus.addListener(Registry.Client::setup);
    }

}
