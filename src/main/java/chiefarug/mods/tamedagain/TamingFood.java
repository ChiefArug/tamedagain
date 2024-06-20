package chiefarug.mods.tamedagain;

import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.jetbrains.annotations.NotNull;

import static chiefarug.mods.tamedagain.TamedAgain.MODRL;

public record TamingFood(Ingredient validItems, float chance) {
    public static final Codec<Ingredient> INGREDIENT_CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Ingredient> read(DynamicOps<T> ops, T input) {
            try {
                return DataResult.success(CraftingHelper.getIngredient(ops.convertTo(JsonOps.INSTANCE, input)));
            } catch (JsonSyntaxException error) {
                return DataResult.error(error.getMessage());
            }
        }
        @Override
        public <T> T write(DynamicOps<T> ops, Ingredient value) {
            return JsonOps.INSTANCE.convertTo(ops, value.toJson());
        }
    };
    public static final Codec<TamingFood> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            INGREDIENT_CODEC.fieldOf("food_ingredient").forGetter(TamingFood::validItems),
            Codec.floatRange(0,1).fieldOf("chance").forGetter(TamingFood::chance)
    ).apply(instance, TamingFood::new));

    @NotNull // for all intents and purposes it is not null when we are accessing it
    @SuppressWarnings("NotNullFieldNotInitialized") // so just shut it up about it being null
    public static ResourceKey<Registry<TamingFood>> REGISTRY_KEY;

    static void registerRegistry(NewRegistryEvent event) {
        event.create(new RegistryBuilder<TamingFood>()
                .setName(MODRL.withPath("taming_foods"))
                .dataPackRegistry(TamingFood.CODEC), created -> REGISTRY_KEY = created.getRegistryKey());
    }



}
