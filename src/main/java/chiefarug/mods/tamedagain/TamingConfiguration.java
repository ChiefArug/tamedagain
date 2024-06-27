package chiefarug.mods.tamedagain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.Optional;

import static chiefarug.mods.tamedagain.TamedAgain.MODRL;
import static net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES;

public record TamingConfiguration(float followStart, float followEnd, float teleportStart, double speedMod, int targetGoalIndex, boolean flying, Optional<Integer> followGoalIndex) {
    public static final Codec<TamingConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("follow_start").forGetter(TamingConfiguration::followStart),
            Codec.FLOAT.fieldOf("follow_end").forGetter(TamingConfiguration::followEnd),
            Codec.FLOAT.fieldOf("teleport_start").forGetter(TamingConfiguration::teleportStart),
            Codec.DOUBLE.fieldOf("speed_mod").forGetter(TamingConfiguration::speedMod),
            Codec.INT.fieldOf("target_goal_index").forGetter(TamingConfiguration::targetGoalIndex),
            Codec.BOOL.fieldOf("flying").forGetter(TamingConfiguration::flying),
            Codec.INT.optionalFieldOf("follow_goal_index").forGetter(TamingConfiguration::followGoalIndex)
    ).apply(instance, TamingConfiguration::new));
    public static final ResourceLocation DEFAULT_LOCATION = MODRL.withPath("default");
    public static TamingConfiguration FALLBACK = new TamingConfiguration(8, 2, 12, 1.5, 0, false, Optional.empty());

    public static TamingConfiguration getConfiguration(EntityType<?> type, RegistryAccess registryAccess) {
        var configurations = registryAccess.registryOrThrow(KEY);
        var entities = registryAccess.registryOrThrow(ENTITY_TYPES);
        // try get the entities one, else look for the default one, else use the fallback one.
        return configurations
                .getOptional(entities.getKey(type))
                .orElseGet(() -> configurations.getOptional(DEFAULT_LOCATION).orElse(FALLBACK));
    }

    public static ResourceKey<Registry<TamingConfiguration>> KEY;

    static void registerRegistry(NewRegistryEvent event) {
        event.create(new RegistryBuilder<TamingConfiguration>()
                .setName(MODRL.withPath("taming_configuration"))
                .dataPackRegistry(TamingConfiguration.CODEC), created -> KEY = created.getRegistryKey());
    }
}
