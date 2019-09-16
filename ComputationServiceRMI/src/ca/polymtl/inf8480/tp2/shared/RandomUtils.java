package ca.polymtl.inf8480.tp2.shared;

import java.util.Random;

public class RandomUtils {
    public static boolean probability(int multiplier, int divider) {
        if (multiplier <= 0) return false;

        int d = Math.abs(divider);

        if (multiplier > d) return true;

        int c = new Random().nextInt(d);

        return (c < multiplier);
    }

    public static boolean probability(int percent) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;

        int seed = 100 / Math.abs(percent);

        Random r = new Random();

        return (r.nextInt(seed) == 0);
    }
}
