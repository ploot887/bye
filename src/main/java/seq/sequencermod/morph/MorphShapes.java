package seq.sequencermod.morph;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Простейший реестр “статических” форм морфов: габариты и высота глаз.
 * В твоей текущей реализации размеры берутся из MorphSizeLookupServer/MorphSizeLookup,
 * этот класс можно использовать как fallback или для быстрых пресетов.
 */
public final class MorphShapes {
    public static final class Shape {
        public final EntityDimensions dims;
        public final float eyeHeight;

        public Shape(EntityDimensions dims, float eyeHeight) {
            this.dims = dims;
            this.eyeHeight = eyeHeight;
        }
    }

    private static final Map<Identifier, Shape> SHAPES = new HashMap<>();

    static {
        // Пример: allay — компактный хитбокс
        register(new Identifier("minecraft", "allay"), EntityDimensions.changing(0.6f, 0.6f), 0.5f);
        // Пример: player-default (для сброса)
        register(new Identifier("sequencermod", "player"), EntityDimensions.changing(0.6f, 1.8f), 1.62f);
    }

    public static void register(Identifier id, EntityDimensions dims, float eyeHeight) {
        SHAPES.put(id, new Shape(dims, eyeHeight));
    }

    public static Shape get(Identifier id) {
        return SHAPES.get(id);
    }

    private MorphShapes() {}
}