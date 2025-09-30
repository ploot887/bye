package seq.sequencermod.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seq.sequencermod.core.ServerRef;
import seq.sequencermod.net.SequencerNetworking;
import seq.sequencermod.sequencer.SequenceJsonLoader;
import seq.sequencermod.sequencer.SequenceRegistry;
import seq.sequencermod.sequencer.SequenceRunnerManager;
import seq.sequencermod.server.morph.MorphServer;

import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import seq.sequencermod.morph.command.MorphDebugCommand;

public final class SequencerMod implements ModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("SequencerMod");
	public static final String MOD_ID = "sequencermod";

	@Override
	public void onInitialize() {
		// Глобальная ссылка на сервер
		ServerRef.init();

		// Серверные подсистемы
		MorphServer.bootstrap(); // если используется серверная логика морфов/S2C-синхронизация

		LOG.info("SequencerMod init");

		// 1) Регистрируем загрузчик секвенций (server datapacks)
		SequenceRegistry.init();
		SequenceJsonLoader.init(); // ВАЖНО: иначе JSON секвенции не загрузятся вообще

		// 2) Сеть (каналы C2S/S2C)
		SequencerNetworking.init();

		MorphDebugCommand.register();

		// 3) Серверный тиковый раннер секвенций
		ServerTickEvents.END_SERVER_TICK.register(SequenceRunnerManager::tick);

		// 4) Команды /seq (см. замечание про дубликаты ниже)
		CommandRegistrationCallback.EVENT.register(this::registerCommands);

		// 5) Список секвенций клиенту при входе (для GUI)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SequencerNetworking.sendSequencesToClient(handler.player);
		});

		// 6) Подчистить состояние при выходе — остановить секвенции и таймеры игрока
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SequenceRegistry.onPlayerLeft(handler.player);
		});
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
								  CommandRegistryAccess registryAccess,
								  CommandManager.RegistrationEnvironment env) {
		dispatcher.register(literal("seq")
				.then(literal("list")
						.executes(ctx -> {
							var ids = SequenceRegistry.listIds();
							ctx.getSource().sendFeedback(() ->
									Text.literal("Sequences (" + ids.size() + "): " + ids.stream().collect(Collectors.joining(", "))), false);
							return 1;
						}))
				.then(literal("play")
						.then(argument("id", StringArgumentType.string())
								.suggests((c, b) -> {
									SequenceRegistry.listIds().forEach(b::suggest);
									return b.buildFuture();
								})
								.executes(ctx -> {
									String id = StringArgumentType.getString(ctx, "id");
									ServerPlayerEntity player = ctx.getSource().getPlayer();
									if (player == null) {
										ctx.getSource().sendError(Text.literal("Player only command."));
										return 0;
									}
									if (!SequenceRegistry.isKnown(id)) {
										ctx.getSource().sendError(Text.literal("Unknown sequence: " + id));
										return 0;
									}
									SequenceRunnerManager.playFor(player, id);
									ctx.getSource().sendFeedback(() -> Text.literal("Playing sequence: " + id), false);
									return 1;
								})
						)
				)
				.then(literal("stop")
						.executes(ctx -> {
							ServerPlayerEntity player = ctx.getSource().getPlayer();
							if (player == null) {
								ctx.getSource().sendError(Text.literal("Player only command."));
								return 0;
							}
							SequenceRunnerManager.stopFor(player);
							ctx.getSource().sendFeedback(() -> Text.literal("Stopped sequence"), false);
							return 1;
						})
				)
		);
	}
}