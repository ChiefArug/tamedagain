package chiefarug.mods.tamedagain;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static chiefarug.mods.tamedagain.TamedAgain.MODID;
import static net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MODID)
public class TamedAgainClient {


    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    static class KeyMappings {
        public static final String KEY_CATEGORY = "key.categories.tamedagain";
        private static final Lazy<KeyMapping> SIT = Lazy.of(() -> makeKeyMapping("sit"));
        private static final Lazy<KeyMapping> FOLLOW = Lazy.of(() -> makeKeyMapping("follow"));
        private static final Lazy<KeyMapping> TOGGLE = Lazy.of(() -> makeKeyMapping("toggle"));

        @SubscribeEvent
        static void registerKeyMapping(RegisterKeyMappingsEvent event) {
            event.register(SIT.get());
            event.register(FOLLOW.get());
            event.register(TOGGLE.get());
        }

        private static KeyMapping makeKeyMapping(String name) {
            return new KeyMapping("key.tamedagain." + name, IN_GAME, InputConstants.UNKNOWN, KEY_CATEGORY);
        }
    }

    @SubscribeEvent
    static void handleKeyMappings(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        while (KeyMappings.SIT.get().consumeClick()) {
            TamedAgainNetworking.NETWORK.sendToServer(TamedAgainNetworking.PacketType.SIT);
        }
        while (KeyMappings.FOLLOW.get().consumeClick()) {
            TamedAgainNetworking.NETWORK.sendToServer(TamedAgainNetworking.PacketType.FOLLOW);
        }
        while (KeyMappings.TOGGLE.get().consumeClick()) {
            TamedAgainNetworking.NETWORK.sendToServer(TamedAgainNetworking.PacketType.TOGGLE);
        }
    }
}
