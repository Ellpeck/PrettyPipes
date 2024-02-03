package de.ellpeck.prettypipes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PrettyPipes.ID)
public final class PrettyPipes {

    public static final String ID = "prettypipes";

    public PrettyPipes() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(Registry::setup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(Registry.Client::setup));
    }
}
