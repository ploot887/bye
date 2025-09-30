package seq.sequencermod.net.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import seq.sequencermod.net.client.morphs.MorphRuntimeFlags;
import seq.sequencermod.network.MorphPackets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class MorphClientSync {
    private MorphClientSync() {}

    private static boolean inited = false;
    private static final Map<UUID, Identifier> MORPHED = new HashMap<>();

    private static boolean isVanillaAllay(Identifier id) {
        return id != null && "minecraft".equals(id.getNamespace()) && "allay".equals(id.getPath());
    }

    private static void refreshRenderOnlyFlag(UUID who, Identifier type) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        mc.world.getPlayers().forEach(p -> {
            if (p.getUuid().equals(who)) {
                MorphRuntimeFlags.setAllayMorph(p, isVanillaAllay(type));
            }
        });
    }

    // Пересчитать размеры у локального игрока (камера/клиентские проверки)
    private static void recalcIfLocal(UUID who) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (who.equals(mc.player.getUuid())) {
            try { mc.player.calculateDimensions(); } catch (Throwable ignored) {}
        }
    }

    public static void bootstrap() {
        if (inited) return;
        inited = true;

        // S2C: синхронизация морфа
        ClientPlayNetworking.registerGlobalReceiver(MorphPackets.S2C_MORPH_SYNC, (client, handler, buf, rs) -> {
            UUID who = buf.readUuid();
            boolean active = buf.readBoolean();
            Identifier type = active ? buf.readIdentifier() : null;
            client.execute(() -> {
                if (!active) {
                    Identifier prev = MORPHED.remove(who);
                    if (isVanillaAllay(prev)) refreshRenderOnlyFlag(who, null);
                    recalcIfLocal(who);
                } else {
                    MORPHED.put(who, type);
                    refreshRenderOnlyFlag(who, type);
                    recalcIfLocal(who);
                }
            });
        });

        // Очистка при disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MORPHED.clear();
            if (client.world != null) {
                client.world.getPlayers().forEach(p -> MorphRuntimeFlags.setAllayMorph(p, false));
            }
        });

        SimpleMorphs.bootstrap();
    }

    public static boolean shouldHidePlayerModel(UUID playerUuid, boolean isLocalPlayer, boolean isFirstPerson) {
        Identifier id = MORPHED.get(playerUuid);
        boolean morphed = id != null;
        if (!morphed) return false;
        if (isVanillaAllay(id)) return true;
        if (isLocalPlayer && isFirstPerson) return false;
        return true;
    }

    public static Identifier getMorphType(UUID playerUuid) {
        return MORPHED.get(playerUuid);
    }

    // === Local (offline/dev) ===
    public static void setLocalMorph(UUID who, Identifier type) {
        if (who == null || type == null) return;
        MORPHED.put(who, type);
        refreshRenderOnlyFlag(who, type);
        recalcIfLocal(who);
    }

    public static void clearLocalMorph(UUID who) {
        if (who == null) return;
        Identifier prev = MORPHED.remove(who);
        if (isVanillaAllay(prev)) refreshRenderOnlyFlag(who, null);
        recalcIfLocal(who);
    }

    // === C2S: запрос применения морфа с клиента ===
    public static void requestMorph(Identifier typeOrAir) {
        if (!ClientPlayNetworking.canSend(MorphPackets.C2S_REQUEST_MORPH)) {
            System.out.println("[Sequencer|Client] Cannot send C2S_REQUEST_MORPH, channel not available");
            return;
        }
        System.out.println("[Sequencer|Client] C2S_REQUEST_MORPH send id=" + typeOrAir);
        PacketByteBuf out = PacketByteBufs.create();
        out.writeIdentifier(typeOrAir);
        ClientPlayNetworking.send(MorphPackets.C2S_REQUEST_MORPH, out);
    }

    public static void requestClear() {
        requestMorph(new Identifier("minecraft", "air"));
    }
}