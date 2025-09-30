package seq.sequencermod.morph.network;

import seq.sequencermod.core.SequencerMod;
import seq.sequencermod.morph.MorphHolder;
import seq.sequencermod.morph.MorphShapes;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class MorphPackets {
    public static final Identifier C2S_REQUEST_MORPH = new Identifier(SequencerMod.MOD_ID, "morph_request");
    public static final Identifier S2C_MORPH_SYNC = new Identifier(SequencerMod.MOD_ID, "morph_sync");

    public static void registerServer() {
        SequencerMod.LOG.info("[Sequencer|Server] MorphServer.bootstrap: register channels {} and {}", C2S_REQUEST_MORPH, S2C_MORPH_SYNC);

        ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_MORPH, (server, player, handler, buf, responseSender) -> {
            Identifier morphId = buf.readIdentifier();
            server.execute(() -> onRequestMorph(server, player, morphId));
        });
    }

    private static void onRequestMorph(MinecraftServer server, ServerPlayerEntity player, Identifier morphId) {
        SequencerMod.LOG.info("[Sequencer|Server] C2S_REQUEST_MORPH from={} id={}", player.getGameProfile().getName(), morphId);

        // Простейшая валидация: форма должна существовать
        MorphShapes.Shape shape = MorphShapes.get(morphId);
        if (shape == null) {
            SequencerMod.LOG.warn("[Sequencer|Server] Unknown morph id={}, ignoring", morphId);
            return;
        }

        // Применяем на сервере
        ((MorphHolder) player).sequencer$setMorphId(morphId);
        // Пересчёт габаритов
        SequencerMod.LOG.info("[Sequencer|Server] RecalcDims START for {}", player.getEntityName());
        player.calculateDimensions();
        SequencerMod.LOG.info("[Sequencer|Server] RecalcDims AFTER for {} -> box={}", player.getEntityName(), player.getBoundingBox());

        // Шлём синк всем видящим игрока (включая самого)
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(player.getId());
        out.writeIdentifier(morphId);

        for (ServerPlayerEntity watcher : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(watcher, S2C_MORPH_SYNC, new PacketByteBuf(out.copy()));
        }
        // И обязательно самому
        ServerPlayNetworking.send(player, S2C_MORPH_SYNC, out);

        SequencerMod.LOG.info("[Sequencer|Server] S2C_MORPH_SYNC broadcasted for {} id={}", player.getEntityName(), morphId);
    }

    // Утилита для клиента: сформировать пакет запроса
    public static PacketByteBuf makeRequestBuf(Identifier morphId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeIdentifier(morphId);
        return buf;
    }

    private MorphPackets() {}
}