package seq.sequencermod.mixin.morph;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.morphs.util.MorphSizeLookup;

/**
 * Клиент: подменяет высоту глаз у ЛОКАЛЬНОГО игрока под морф.
 * В Yarn 1.20.1 поза при приседании — CROUCHING (не SNEAKING).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEyeHeightMixin {

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void sequencer$overrideEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || !(self instanceof PlayerEntity player) || mc.player != player) return;

        Identifier type = MorphClientSync.getMorphType(player.getUuid());
        if (type == null) return;

        switch (pose) {
            case STANDING:
            case CROUCHING:      // <- было SNEAKING
            case FALL_FLYING:
            case SWIMMING:
            case SPIN_ATTACK:
                float eye = MorphSizeLookup.getEyeHeight(type);
                if (eye > 0f) {
                    cir.setReturnValue(eye);
                }
                break;
            default:
                // ваниль
        }
    }
}