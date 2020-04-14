package de.ellpeck.prettypipes.events;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class Events {

    @SubscribeEvent
    public static void onWorldCaps(AttachCapabilitiesEvent<World> event) {
        event.addCapability(new ResourceLocation(PrettyPipes.ID, "network"), new PipeNetwork(event.getObject()));
    }
}
