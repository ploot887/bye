package seq.sequencermod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.UUID;

/**
 * Общие (shared) идентификаторы и конструкторы буферов для сетевых пакетов морфов.
 * Здесь только то, что требуется как клиенту, так и серверу (без клиент-рендер импортов).
 */
public final class MorphPackets {
    private MorphPackets() {}

    public static final String MODID = "sequencermod";

    // --- Базовые (уже были) ---
    public static final Identifier C2S_REQUEST_MORPH = new Identifier(MODID, "morph_request");
    public static final Identifier S2C_MORPH_SYNC    = new Identifier(MODID, "morph_sync");

    // --- Аксолотль: новые каналы ---
    public static final Identifier C2S_AXOLOTL_PLAY_DEAD   = new Identifier(MODID, "axolotl_play_dead");
    public static final Identifier C2S_AXOLOTL_SET_VARIANT = new Identifier(MODID, "axolotl_variant");
    public static final Identifier S2C_AXOLOTL_STATE       = new Identifier(MODID, "axolotl_state");

    // ======================= Helper builders (универсальные) =======================

    public static PacketByteBuf buildPlayDeadC2S(boolean start) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(start);
        return buf;
    }

    public static PacketByteBuf buildVariantC2S(int variant) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(variant);
        return buf;
    }

    public static PacketByteBuf buildAxolotlStateS2C(UUID who, int playDeadTicks, int variant, int air) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(who);
        buf.writeVarInt(playDeadTicks);
        buf.writeVarInt(variant);
        buf.writeVarInt(air);
        return buf;
    }
}