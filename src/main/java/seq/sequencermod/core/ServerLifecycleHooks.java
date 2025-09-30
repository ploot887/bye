package seq.sequencermod.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import seq.sequencermod.core.debug.DebugTaps;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Хуки жизненного цикла сервера.
 * По умолчанию только логируют. При желании можно установить обработчики через set*().
 *
 * Рекомендуется:
 * - setOnPlayerDisconnect: остановить секвенции и снять морф с игрока.
 * - setOnServerStopping: отменить все отложенные задачи/таймеры и очистить глобальные кэши.
 */
public final class ServerLifecycleHooks implements ModInitializer {
    private static Consumer<ServerPlayerEntity> onPlayerDisconnect = p -> {
        if (DebugTaps.active.get()) {
            DebugTaps.logf("DISCONNECT: %s (%s)", p.getGameProfile().getName(), p.getUuid());
        }
    };

    private static Consumer<MinecraftServer> onServerStopping = s -> {
        if (DebugTaps.active.get()) {
            DebugTaps.log("SERVER_STOPPING");
        }
    };

    // Необязательный комбинированный хук (если нужно и сервер, и игрок)
    private static BiConsumer<MinecraftServer, ServerPlayerEntity> onPlayerDisconnectWithServer = (s, p) -> {};

    public static void setOnPlayerDisconnect(Consumer<ServerPlayerEntity> handler) {
        onPlayerDisconnect = Objects.requireNonNull(handler);
    }

    public static void setOnPlayerDisconnect(BiConsumer<MinecraftServer, ServerPlayerEntity> handler) {
        onPlayerDisconnectWithServer = Objects.requireNonNull(handler);
    }

    public static void setOnServerStopping(Consumer<MinecraftServer> handler) {
        onServerStopping = Objects.requireNonNull(handler);
    }

    @Override
    public void onInitialize() {
        // Игрок отвалился: подчистить состояния
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            try {
                onPlayerDisconnect.accept(player);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                onPlayerDisconnectWithServer.accept(server, player);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        // Сервер останавливается: отменить задачи/очистить глобалки
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                onServerStopping.accept(server);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}