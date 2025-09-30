package seq.sequencermod.morph.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MorphSizeLookupServer {
    private static final Logger LOG = LoggerFactory.getLogger("Sequencer|Server");

    private record Key(Identifier typeId, EntityPose pose) {}

    private static final ConcurrentMap<Key, EntityDimensions> CACHE = new ConcurrentHashMap<>();

    private MorphSizeLookupServer() {}

    public static EntityDimensions getDimensions(ServerWorld world, Identifier typeId, EntityPose pose) {
        if (world == null || typeId == null) return null;
        final EntityPose usePose = (pose != null) ? pose : EntityPose.STANDING;
        final Key key = new Key(typeId, usePose);

        EntityDimensions cached = CACHE.get(key);
        if (cached != null) return cached;

        // 1) Пробуем data-driven реестр
        EntityDimensions dims = MorphDimensionsRegistry.get(typeId, usePose);
        if (dims == null) {
            // 2) Фолбэк: ванильный EntityType
            EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
            if (type == null) {
                LOG.warn("MorphSizeLookupServer: unknown entity type '{}'", typeId);
                return null;
            }
            try {
                Entity dummy = type.create(world);
                if (dummy != null) {
                    dims = dummy.getDimensions(usePose);
                    dummy.discard(); // на всякий случай
                } else {
                    dims = type.getDimensions();
                }
            } catch (Throwable t) {
                LOG.warn("MorphSizeLookupServer: failed to resolve dimensions for {} pose {}. Falling back to type.getDimensions().",
                        typeId, usePose, t);
                try {
                    dims = type.getDimensions();
                } catch (Throwable ignore) {
                    dims = null;
                }
            }
        }

        if (dims != null) {
            CACHE.putIfAbsent(key, dims);
        }
        return dims;
    }

    public static void invalidateCache() {
        CACHE.clear();
    }
}