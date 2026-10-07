package io.github.korteexz.ragephysics.temporal;

/** Crédito local fracionário. Não conhece Minecraft, GUI, networking ou relógio global. */
public final class TemporalTickBudget {
    public static final double MIN_SCALE = 0.125;
    public static final double MAX_SCALE = 8.0;
    private Object context;
    private long revision;
    private double scale = 1.0;
    private double credit;

    public static boolean isValid(double scale) {
        return Double.isFinite(scale) && scale >= MIN_SCALE && scale <= MAX_SCALE;
    }

    public int advance(Object context, long revision, double scale) {
        if (!isValid(scale)) {
            throw new IllegalArgumentException("Invalid local time scale: " + scale);
        }
        if (scale == 1.0) {
            reset();
            return 1;
        }
        if (this.context != context || this.revision != revision || this.scale != scale) {
            credit = 0.0;
            this.context = context;
            this.revision = revision;
            this.scale = scale;
        }
        credit += scale;
        // Compensa somente erro de ponto flutuante, nunca arredonda a escala.
        int steps = (int) Math.floor(credit + 1.0e-10);
        credit = Math.max(0.0, credit - steps);
        return steps;
    }

    public double credit() {
        return credit;
    }

    public void reset() {
        context = null;
        revision = 0;
        scale = 1.0;
        credit = 0.0;
    }
}
