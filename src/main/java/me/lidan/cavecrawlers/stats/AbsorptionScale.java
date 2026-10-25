package me.lidan.cavecrawlers.stats;

/** Maps a native absorption pool to at most ten client-side golden hearts. */
final class AbsorptionScale {
    static final float DISPLAY_POINTS = 20;
    private float peak;

    /** Keep the denominator while damage consumes the pool; start fresh once it empties. */
    synchronized float update(float absorption) {
        if (!Float.isFinite(absorption) || absorption <= 0) {
            peak = 0;
            return 0;
        }
        peak = Math.max(peak, absorption);
        return Math.min(DISPLAY_POINTS, absorption / peak * DISPLAY_POINTS);
    }
}
