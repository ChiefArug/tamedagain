package chiefarug.mods.tamedagain;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

import static chiefarug.mods.tamedagain.TamedAgain.LGGR;
import static chiefarug.mods.tamedagain.TamedAgain.MODRL;
import static chiefarug.mods.tamedagain.capability.ITamedEntity.CAPABILITY;
import static net.minecraftforge.network.NetworkRegistry.acceptMissingOr;

public class TamedAgainNetworking {

    static void init() {}

    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            MODRL, () -> TamedAgain.NETWORK_VERSION,
            // if the mod is on the client, then it has to be on the server
            TamedAgain.NETWORK_VERSION::equals,
            // if the mod is on the server, then the packet handler doesn't care what the client has, or even if they are modded
            // do note that because of the startup config option to register a staff item, this doesnt definitively mean a client will be able to join
            acceptMissingOr(TamedAgain.NETWORK_VERSION::equals)
    );

    static {
        NETWORK.messageBuilder(PacketType.class, 0)
                .encoder(TamedAgainNetworking::encode)
                .decoder(TamedAgainNetworking::decode)
                .consumerNetworkThread(TamedAgainNetworking::handle)
                .add();
    }
    public enum PacketType {
        INVALID {
            @Override
            public void apply(Player player, ITamedEntity capability) {
                throw new IllegalArgumentException("Cannot apply from INVALID PacketType");
            }
        },
        SIT {
            @Override
            public void apply(Player player, ITamedEntity capability) {
                capability.setSitting(player, true);
            }
        },
        FOLLOW {
            @Override
            public void apply(Player player, ITamedEntity capability) {
                capability.setSitting(player, false);
            }
        },
        TOGGLE {
            @Override
            public void apply(Player player, ITamedEntity capability) {
                capability.toggleSitting(player);
            }
        };
        public abstract void apply(Player player, ITamedEntity capability);
        private static final PacketType[] VALUES = values();
    }

    private static PacketType decode(FriendlyByteBuf bytes) {
        // use a custom 'number' system
        int id = bytes.readByte();
        if (id >= PacketType.VALUES.length) {
            LGGR.debug("Dropping packet as it contained an invalid id. Expected incl. range of 0-{} but found {} instead!", PacketType.VALUES.length - 1, id);
            return PacketType.VALUES[0];
        }

        return PacketType.VALUES[id + 1];
    }

    private static void encode(PacketType packet, FriendlyByteBuf bytes) {
        if (packet == PacketType.INVALID)
            throw new IllegalArgumentException("Cannot encode INVALID PacketType");
        bytes.writeByte(packet.ordinal() - 1);
    }

    private static boolean handle(PacketType packet, Supplier<NetworkEvent.Context> ctx) {
        if (packet == PacketType.INVALID) return false;
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) return false;
        ServerPlayer sender = context.getSender();
        if (sender == null) return false;
        context.enqueueWork(() -> {
            //TODO: make size a config option
            AABB searchBox = sender.getBoundingBox().expandTowards(5, 2, 5);
            List<Entity> nearby = sender.level.getEntities((Entity) null, searchBox, entity -> entity.getCapability(CAPABILITY).isPresent());
            for (Entity entity : nearby) {
                packet.apply(sender, entity.getCapability(CAPABILITY).resolve().get());
            }
        });
        return true;
    }
}
