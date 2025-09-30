package seq.sequencermod.mixin.morph;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.morph.api.MorphAccess;

@Mixin(LivingEntity.class)
public abstract class ServerLivingEntityEyeHeightMixin {

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void sequencer$overrideEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;
        if (!(player instanceof MorphAccess morphAccess)) return;

        Identifier typeId = morphAccess.sequencer$getMorphTypeId();
        if (typeId == null) return;

        switch (pose) {
            case STANDING:
            case CROUCHING:
            case FALL_FLYING:
            case SWIMMING:
            case SPIN_ATTACK: {
                EntityType<?> et = Registries.ENTITY_TYPE.get(typeId);
                if (et != null) {
                    // Простой вариант: доля от высоты (можно заменить на более точный расчёт)
                    float eye = Math.max(0f, dimensions.height * 0.9f);
                    cir.setReturnValue(eye);
                }
                break;
            }
            default:
        }
    }
}