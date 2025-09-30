package seq.sequencermod.server.morph;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.api.MorphAccess;
import seq.sequencermod.morph.runtime.MorphDimensionsLoader;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;
import seq.sequencermod.morph.runtime.MorphStateStore;
import seq.sequencermod.network.MorphPackets;

public final class MorphServer {
    private MorphServer() {}

    public static void bootstrap() {
        System.out.println("[Sequencer|Server] MorphServer.bootstrap: register channels " +
                MorphPackets.C2S_REQUEST_MORPH + " and " + MorphPackets.S2C_MORPH_SYNC);

        // 1) Регистрация серверного загрузчика размеров морфов (server-data)
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new MorphDimensionsLoader());
        // 2) Инвалидатор кэша лукапа размеров на /reload
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    private final Identifier id = new Identifier("sequencermod", "morph_dims_cache_invalidator");
                    @Override public Identifier getFabricId() { return id; }
                    @Override public void reload(net.minecraft.resource.ResourceManager manager) {
                        MorphSizeLookupServer.invalidateCache();
                        System.out.println("[Sequencer|Server] MorphSizeLookupServer cache invalidated on data reload");
                    }
                });

        // 3) Сетевые хэндлеры
        ServerPlayNetworking.registerGlobalReceiver(MorphPackets.C2S_REQUEST_MORPH,
                (server, player, handler, buf, rs) -> {
                    final Identifier id = buf.readIdentifier();
                    server.execute(() -> handleMorphRequest(player, id));
                });
    }

    private static boolean isAir(Identifier id) {
        return id != null && "minecraft".equals(id.getNamespace()) && "air".equals(id.getPath());
    }

    private static void handleMorphRequest(ServerPlayerEntity player, Identifier id) {
        if (player == null) return;

        System.out.println("[Sequencer|Server] C2S_REQUEST_MORPH from=" +
                player.getGameProfile().getName() + " id=" + id);

        // minecraft:air -> очистка
        if (id == null || isAir(id)) {
            MorphStateStore.clear(player.getUuid());
            if (player instanceof MorphAccess ma) {
                ma.sequencer$setMorphTypeId(null);
            }
            System.out.println("[Sequencer|Server] Morph cleared for " + player.getGameProfile().getName() +
                    " reason=" + (id == null ? "null" : "minecraft:air"));
            recalcPlayerDimensions(player, "clear");
            broadcastMorphSync(player, null);
            return;
        }

        // Валидируем entity type
        EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
        if (type == null) {
            MorphStateStore.clear(player.getUuid());
            if (player instanceof MorphAccess ma) {
                ma.sequencer$setMorphTypeId(null);
            }
            System.out.println("[Sequencer|Server] Unknown entity type " + id +
                    " -> clear morph for " + player.getGameProfile().getName());
            recalcPlayerDimensions(player, "unknown_id");
            broadcastMorphSync(player, null);
            return;
        }

        MorphStateStore.set(player.getUuid(), id.toString());
        if (player instanceof MorphAccess ma) {
            ma.sequencer$setMorphTypeId(id);
        }
        System.out.println("[Sequencer|Server] Morph set to " + id +
                " for " + player.getGameProfile().getName());
        recalcPlayerDimensions(player, "set:" + id);
        broadcastMorphSync(player, id);
    }

    public static void syncToAll(ServerPlayerEntity owner, Identifier typeIdOrNull) {
        if (owner == null) return;
        if (typeIdOrNull == null) {
            MorphStateStore.clear(owner.getUuid());
            if (owner instanceof MorphAccess ma) {
                ma.sequencer$setMorphTypeId(null);
            }
            System.out.println("[Sequencer|Server] syncToAll: cleared morph for " +
                    owner.getGameProfile().getName());
        } else {
            MorphStateStore.set(owner.getUuid(), typeIdOrNull.toString());
            if (owner instanceof MorphAccess ma) {
                ma.sequencer$setMorphTypeId(typeIdOrNull);
            }
            System.out.println("[Sequencer|Server] syncToAll: set morph " + typeIdOrNull +
                    " for " + owner.getGameProfile().getName());
        }
        recalcPlayerDimensions(owner, "syncToAll");
        broadcastMorphSync(owner, typeIdOrNull);
    }

    private static void broadcastMorphSync(ServerPlayerEntity owner, Identifier typeIdOrNull) {
        if (owner == null || owner.getServer() == null) return;

        int delivered = 0;
        for (ServerPlayerEntity p : owner.getServer().getPlayerManager().getPlayerList()) {
            PacketByteBuf out = PacketByteBufs.create();
            out.writeUuid(owner.getUuid());
            boolean active = (typeIdOrNull != null);
            out.writeBoolean(active);
            if (active) out.writeIdentifier(typeIdOrNull);
            ServerPlayNetworking.send(p, MorphPackets.S2C_MORPH_SYNC, out);
            delivered++;
        }
        System.out.println("[Sequencer|Server] S2C_MORPH_SYNC broadcasted for " +
                owner.getGameProfile().getName() +
                " active=" + (typeIdOrNull != null) +
                " type=" + typeIdOrNull +
                " to=" + delivered + " players");
    }

    private static void recalcPlayerDimensions(ServerPlayerEntity player, String reason) {
        if (player == null) return;

        EntityPose poseBefore = player.getPose();
        EntityDimensions dimsBefore = player.getDimensions(poseBefore);
        System.out.println("[Sequencer|Server] RecalcDims START (" + reason + ") player=" +
                player.getGameProfile().getName() +
                " pose=" + poseBefore + " dims=" + dimsBefore.width + "x" + dimsBefore.height);

        // Пересчитать размеры (вызовет getDimensions -> серверный миксин)
        player.calculateDimensions();

        EntityPose poseAfter = player.getPose();
        EntityDimensions dimsAfter = player.getDimensions(poseAfter);
        System.out.println("[Sequencer|Server] RecalcDims AFTER (" + reason + ") player=" +
                player.getGameProfile().getName() +
                " pose=" + poseAfter + " dims=" + dimsAfter.width + "x" + dimsAfter.height);

        // Обновить AABB и позицию
        player.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
    }
}