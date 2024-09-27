package chiefarug.mods.tamedagain;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TamedAgain.MODID)
public class TamedAgain {

    // Helper class for a 1.19.4 feature.
    public static class ResauceLocation extends ResourceLocation {
        private ResauceLocation() {
            super(MODID, MODID);
        }

        public ResourceLocation withPath(String path) {
            return new ResourceLocation(this.namespace, path);
        }
    }

    public static final String MODID = "tamedagain";
    static final String NETWORK_VERSION = "1";
    public static final ResauceLocation MODRL = new ResauceLocation();
    public static final Logger LGGR = LogUtils.getLogger();
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final TagKey<EntityType<?>> STAFF_TAME_BLACKLIST = TagKey.create(ForgeRegistries.Keys.ENTITY_TYPES, MODRL.withPath("staff_blacklist"));
    public static final TagKey<Item> STAFF = TagKey.create(ForgeRegistries.Keys.ITEMS, MODRL.withPath("staff"));


    public TamedAgain() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        modBus.addListener(TamingFood::registerRegistry);
        modBus.addListener(TamingConfiguration::registerRegistry);
        ITEMS.register(modBus);
        forgeBus.addGenericListener(Entity.class, ITamedEntity::attachToEntity);
        StartupConfig.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.spec);
        TamedAgainNetworking.init();

        // blah blah conditional registration bad. I know. This is so it can be used entirely server side.
        if (StartupConfig.INSTANCE.registerStaff())
            ITEMS.register("staff", () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS)));
    }
}
