package io.github.korteexz.ragephysics.temporal;

import java.util.EnumMap;
import io.github.korteexz.ragephysics.timestamper.config.TemporalTarget;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Registrada como SERVER. Clientes nunca decidem a simulação a partir desta cópia. */
public final class TemporalConfig {
    public static final ModConfigSpec SPEC;
    private static final EnumMap<TemporalTarget, ModConfigSpec.BooleanValue> VALUES = new EnumMap<>(TemporalTarget.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Local time simulation categories. Riding entities and their vehicles remain vanilla.")
                .push("categories");
        for (TemporalTarget target : TemporalTarget.values()) {
            builder.comment(target.implemented()
                    ? "Enable local time for " + target + "."
                    : "RESERVED / NOT IMPLEMENTED. Setting true currently has NO EFFECT: " + target + ".");
            VALUES.put(target, builder.define(target.configKey(), target.implemented()));
        }
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean enabled(TemporalTarget target) {
        return target.implemented() && VALUES.get(target).get();
    }

    private TemporalConfig() {}
}
