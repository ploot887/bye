package seq.sequencermod.mixin.morph;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.morphs.util.MorphSizeLookup;

/**
 * Клиент: подменяет размеры у ЛОКАЛЬНОГО игрока,
 * чтобы камера/клиентские проверки соответствовали морфу.
 * В Yarn 1.20.1 поза при приседании — CROUCHING (не SNEAKING).
 */
@Mixin(value = Entity.class, priority = 2000)
public abstract class EntityDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void sequencer$overrideDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || !(self instanceof PlayerEntity player) || mc.player != player) return;

        Identifier type = MorphClientSync.getMorphType(player.getUuid());
        if (type == null) return;

        // Нормализуем позу (на всякий случай, если придёт null)
        EntityPose usePose = (pose != null) ? pose : player.getPose();

        // Для стандартных поз — применяем размеры морфа (универсальные)
        switch (usePose) {
            case STANDING:
            case CROUCHING:
            case FALL_FLYING:
            case SWIMMING:
            case SPIN_ATTACK:
                EntityDimensions dims = MorphSizeLookup.getDimensions(type);
                if (dims != null) {
                    cir.setReturnValue(dims);
                }
                break;
            default:
                // прочие позы — ваниль
        }
    }
}