package seq.sequencermod.mixin.morph;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.morph.api.MorphAccess;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;

@Mixin(value = Entity.class, priority = 2000)
public abstract class ServerEntityDimensionsMixin {

    @Shadow public abstract World getWorld();
    @Shadow public abstract EntityPose getPose();

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void sequencer$useMorphDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        World w = this.getWorld();
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (!(w instanceof ServerWorld sw)) return; // только сервер
        if (!(player instanceof MorphAccess access)) return;

        Identifier morphId = access.sequencer$getMorphTypeId();
        if (morphId == null) return;

        EntityPose usePose = (pose != null ? pose : this.getPose());
        EntityDimensions dims = MorphSizeLookupServer.getDimensions(sw, morphId, usePose);

        if (dims != null) {
            // System.out.println("[Sequencer|Server] morph=" + morphId + " pose=" + usePose + " -> " + dims.width + "x" + dims.height);
            cir.setReturnValue(dims); // критично: подменяем результат
            return;
        }
        // если dims == null — оставляем ваниль
    }
}