package de.ellpeck.prettypipes.events;

import de.ellpeck.prettypipes.PrettyPipes;
import de.ellpeck.prettypipes.network.PipeNetwork;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jgrapht.graph.DefaultWeightedEdge;

@Mod.EventBusSubscriber
public final class Events {

    @SubscribeEvent
    public static void onWorldCaps(AttachCapabilitiesEvent<World> event) {
        event.addCapability(new ResourceLocation(PrettyPipes.ID, "network"), new PipeNetwork(event.getObject()));
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.world.getGameTime() % 5 != 0)
            return;

        PipeNetwork network = PipeNetwork.get(event.world);
        for (DefaultWeightedEdge edge : network.graph.edgeSet()) {
            BlockPos start = network.graph.getEdgeSource(edge);
            BlockPos end = network.graph.getEdgeTarget(edge);

            RedstoneParticleData data = new RedstoneParticleData(((start.getX() * 181923 + end.getX()) % 255) / 255F, ((start.getY() * 128391 + end.getY()) % 255) / 255F, ((start.getZ() * 123891 + end.getZ()) % 255) / 255F, 1);
            ((ServerWorld) event.world).spawnParticle(data, start.getX() + 0.5F, start.getY() + 0.5F, start.getZ() + 0.5F, 1, 0, 0, 0, 0);
            ((ServerWorld) event.world).spawnParticle(data, end.getX() + 0.5F, end.getY() + 0.5F, end.getZ() + 0.5F, 1, 0, 0, 0, 0);
        }
    }
}
