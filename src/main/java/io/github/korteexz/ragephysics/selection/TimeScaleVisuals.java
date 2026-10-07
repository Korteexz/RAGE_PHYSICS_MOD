package io.github.korteexz.ragephysics.selection;

/** Matemática compartilhada pela GUI e pelo preview; sem efeitos na simulação. */
public final class TimeScaleVisuals {
    public static final double MIN = 0.125;
    public static final double MAX = 8.0;
    private static final int BLUE = 0x4080FF;
    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF4040;

    public static boolean isValid(double scale) {
        return Double.isFinite(scale) && scale >= MIN && scale <= MAX;
    }

    /** Posição vanilla [0, 1]: o centro exato (0.5) corresponde a 1x. */
    public static double fromSlider(double position) {
        return Math.pow(2.0, (Math.clamp(position, 0.0, 1.0) * 2.0 - 1.0) * 3.0);
    }

    public static double toSlider(double scale) {
        return (Math.log(Math.clamp(scale, MIN, MAX)) / Math.log(2.0) / 3.0 + 1.0) / 2.0;
    }

    public static int color(double scale) {
        double signed = toSlider(scale) * 2.0 - 1.0;
        int target = signed < 0.0 ? BLUE : RED;
        double amount = Math.abs(signed);
        int red = interpolate(WHITE >> 16 & 255, target >> 16 & 255, amount);
        int green = interpolate(WHITE >> 8 & 255, target >> 8 & 255, amount);
        int blue = interpolate(WHITE & 255, target & 255, amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int interpolate(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
    }

    private TimeScaleVisuals() {}
}
