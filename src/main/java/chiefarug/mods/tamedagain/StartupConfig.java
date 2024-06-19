package chiefarug.mods.tamedagain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static chiefarug.mods.tamedagain.TamedAgain.LGGR;
import static chiefarug.mods.tamedagain.TamedAgain.MODID;

public record StartupConfig(boolean registerStaff) {
    public static final Path location = FMLPaths.CONFIGDIR.get().resolve(MODID + "-startup.json");
    private static final Codec<StartupConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("register_staff").forGetter(StartupConfig::registerStaff)
    ).apply(instance, StartupConfig::new));
    private static @NotNull JsonObject addComments(JsonObject config) {
        config.addProperty("register_staff_comment", "If the staff item should be registered. Disabling this allows the mod to be used on servers without the clients needing it, but you must tag another item with tamedagain:staff if you want to allow a single item to tame anything.");
        config.addProperty("taming_foods_comment", "An map of entity type id to {item_tag: \"\", chance: 0.0} objects.");
        return config;
    }
    public static StartupConfig INSTANCE = load();


    private StartupConfig() {
        this(true);
        this.save();
    }

    public static void reload() {
        LGGR.debug("Reloading {} config file", MODID);
        INSTANCE = load();
        LGGR.info("Reloaded {} config file. Note that some changes need a game restart to properly load!", MODID);
    }

    private static StartupConfig load() {
        try {
            return CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(location, StandardCharsets.UTF_8)).getAsJsonObject())
                    .resultOrPartial(error -> LGGR.error("Failed to parse {} config from {}! Overwriting with default. Error message: {}", MODID, location, error))
                    .orElseGet(StartupConfig::new);
        } catch (IOException e) {
            LGGR.error("Failed to load {} config file from {}! Overwriting with default. Error message: {}", MODID, location, e);
        } catch (JsonSyntaxException e) {
            LGGR.error("Failed to parse {} config file from {}! Overwriting with default. Error message: {}", MODID, location, e);
        }
        return new StartupConfig();
    }

    private void save() {
        try {
            var result = CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .resultOrPartial(error -> LGGR.error("Failed to write {} config to {}! Error message: {}", MODID, location, error));
            if (result.isPresent()) {
                JsonObject jsonOut = addComments(result.get().getAsJsonObject());
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(location.toFile())));
                writer.setIndent("\t");
                writer.setSerializeNulls(true);
                Streams.write(jsonOut, writer);
                writer.close();
            }
        } catch (IOException e) {
            LGGR.error("Failed to save {} config file to {}! Error message: {}", MODID, location, e);
        }
    }

    public static void init() {}
}