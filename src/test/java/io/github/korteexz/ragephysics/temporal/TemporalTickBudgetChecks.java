package io.github.korteexz.ragephysics.temporal;

/** Harness sem biblioteca de testes externa: executado por temporalMathTest/check. */
public final class TemporalTickBudgetChecks {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Object region = new Object();
        for (double scale : new double[] {0.125, 0.17, 0.25, 0.5, 0.65, 1.0, 1.35, 1.5, 2, 2.5, 4, 5.72, 8}) {
            TemporalTickBudget budget = new TemporalTickBudget();
            int total = 0;
            for (int tick = 1; tick <= 10000; tick++) {
                int steps = budget.advance(region, 1, scale);
                total += steps;
                check(steps >= 0 && steps <= 8, "Bounded work: " + scale);
                check(budget.credit() >= 0 && budget.credit() < 1, "Bounded credit: " + scale);
                check(Math.abs(total - Math.floor(tick * scale + 1e-9)) < 1e-8, "Average rate: " + scale);
            }
        }
        TemporalTickBudget budget = new TemporalTickBudget();
        check(budget.advance(region, 1, 2.5) == 2, "2.5 first tick");
        check(budget.advance(region, 1, 2.5) == 3, "2.5 second tick");
        budget.reset();
        for (int tick = 0; tick < 3; tick++) check(budget.advance(region, 1, .25) == 0, "quarter accumulation");
        check(budget.advance(region, 1, .25) == 1, "quarter step");
        budget.advance(region, 1, .5);
        check(budget.advance(region, 1, .25) == 0 && budget.credit() == .25, "Scale change clears credit");
        check(budget.advance(new Object(), 1, .25) == 0 && budget.credit() == .25, "Region change clears credit");
        check(budget.advance(region, 2, .25) == 0 && budget.credit() == .25, "Revision change clears credit");
        check(budget.advance(null, 0, 1) == 1 && budget.credit() == 0, "Vanilla return clears credit");
        for (double invalid : new double[] {0, -.1, .124, 8.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            try {
                budget.advance(region, 1, invalid);
                throw new AssertionError("Invalid value accepted: " + invalid);
            } catch (IllegalArgumentException expected) {
                check(budget.credit() == 0, "Invalid input leaves credit intact");
            }
        }
        System.out.println("PASS: " + checks + " temporal math checks");
    }
}
