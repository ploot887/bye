package seq.sequencermod.mixin.morph;

import seq.sequencermod.morph.MorphHolder;
import seq.sequencermod.morph.MorphShapes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Инжекторы:
 * - getDimensions: подменяет EntityDimensions при активном морфе
 * - getActiveEyeHeight: подменяет высоту глаз под морф
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements MorphHolder {
    @Unique private @Nullable Identifier sequencer$morphId;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public Identifier sequencer$getMorphId() {
        return sequencer$morphId;
    }

    @Override
    public void sequencer$setMorphId(Identifier id) {
        this.sequencer$morphId = id;
        // calculateDimensions() вызывается извне после установки, чтобы учесть позу
        // Здесь специально не дергаем, чтобы не делать это дважды.
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void sequencer$overrideDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (sequencer$morphId == null) return;
        MorphShapes.Shape shape = MorphShapes.get(sequencer$morphId);
        if (shape == null) return;

        // Пример: для лежачих поз оставляем ваниллу, для остальных — форма морфа.
        if (pose == EntityPose.SLEEPING) return;

        cir.setReturnValue(shape.dims);
    }

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void sequencer$overrideEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (sequencer$morphId == null) return;
        MorphShapes.Shape shape = MorphShapes.get(sequencer$morphId);
        if (shape == null) return;

        if (pose == EntityPose.SLEEPING) return;

        cir.setReturnValue(shape.eyeHeight);
    }
}