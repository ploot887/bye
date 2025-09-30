package seq.sequencermod.mixin.morph.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seq.sequencermod.morph.api.MorphAccess;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;

@Mixin(Entity.class)
public abstract class Entity_GetDimensions_Mixin {
    private static final Logger SEQ_LOG = LoggerFactory.getLogger("Sequencer|Server");
    private static final Identifier ALLAY = new Identifier("minecraft", "allay");

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void sequencer$overrideMorphDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity)(Object)this;

        // Только сервер и только игрок (чтобы не трогать остальные сущности)
        if (!(self.getWorld() instanceof ServerWorld sw)) return;
        if (!(self instanceof ServerPlayerEntity)) return;

        // Должен быть установлен морф
        if (!(self instanceof MorphAccess ma)) return;
        Identifier typeId = ma.sequencer$getMorphTypeId();
        if (typeId == null) return;

        // 1) Пытаемся получить размеры из серверного лукапа/реестра
        EntityDimensions dims = MorphSizeLookupServer.getDimensions(sw, typeId, pose);

        // 2) Явная принудительная подмена для эллея, если не нашли через лукап
        if (dims == null && ALLAY.equals(typeId)) {
            dims = EntityDimensions.changing(0.35f, 0.60f);
        }

        if (dims != null) {
            // Подменяем результат и логируем один раз на позу
            if (SEQ_LOG.isDebugEnabled()) {
                SEQ_LOG.debug("MorphDimsMixin: override {} pose={} -> {}x{}",
                        typeId, pose, dims.width, dims.height);
            }
            cir.setReturnValue(dims);
        }
    }
}