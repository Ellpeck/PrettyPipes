package de.ellpeck.prettypipes;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PrettyPipes.ID)
public final class PrettyPipes {

    public static final String ID = "prettypipes";

    public PrettyPipes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(Registry::setup);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> bus.addListener(Registry.Client::setup));
    }
}
