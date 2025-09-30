package seq.sequencermod.morph.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.runtime.MorphRuntime;
import seq.sequencermod.sequencer.SeqCommands;
import seq.sequencermod.server.morph.MorphServer;

public final class MorphCommands {
    private MorphCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            // /morph <id>
            dispatcher.register(CommandManager.literal("morph")
                    .then(CommandManager.argument("id", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                String idStr = StringArgumentType.getString(ctx, "id");
                                Identifier id = Identifier.tryParse(idStr);
                                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                                    ctx.getSource().sendError(Text.literal("Unknown entity: " + idStr));
                                    return 0;
                                }
                                var err = MorphRuntime.applyMorph(player.getUuid(), idStr, u -> true);
                                if (err != null) {
                                    ctx.getSource().sendError(Text.literal("Morph failed: " + err));
                                    return 0;
                                }
                                // Единый sync-пакет всем
                                MorphServer.syncToAll(player, id);
                                ctx.getSource().sendFeedback(() -> Text.literal("Morphed into " + idStr), true);
                                return 1;
                            })
                    )
            );

            // /unmorph
            dispatcher.register(CommandManager.literal("unmorph")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        MorphRuntime.clearMorph(player.getUuid(), u -> true);
                        MorphServer.syncToAll(player, null);
                        ctx.getSource().sendFeedback(() -> Text.literal("Unmorphed"), true);
                        return 1;
                    })
            );

            // Алиасы
            dispatcher.register(CommandManager.literal("morphic")
                    .then(CommandManager.argument("id", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                String idStr = StringArgumentType.getString(ctx, "id");
                                Identifier id = Identifier.tryParse(idStr);
                                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                                    ctx.getSource().sendError(Text.literal("Unknown entity: " + idStr));
                                    return 0;
                                }
                                var err = MorphRuntime.applyMorph(player.getUuid(), idStr, u -> true);
                                if (err != null) {
                                    ctx.getSource().sendError(Text.literal("Morph failed: " + err));
                                    return 0;
                                }
                                MorphServer.syncToAll(player, id);
                                ctx.getSource().sendFeedback(() -> Text.literal("Morphed into " + idStr), true);
                                return 1;
                            })
                    )
            );
            dispatcher.register(CommandManager.literal("unmorphic")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        MorphRuntime.clearMorph(player.getUuid(), u -> true);
                        MorphServer.syncToAll(player, null);
                        ctx.getSource().sendFeedback(() -> Text.literal("Unmorphed"), true);
                        return 1;
                    })
            );

            // /seq ...
            SeqCommands.register(dispatcher);
        });
    }
}