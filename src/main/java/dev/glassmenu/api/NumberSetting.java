package dev.glassmenu.api;

/** A slider setting belonging to a module. */
public class NumberSetting {
    public final String key, label;
    public final double min, max, step;
    private double value;

    public NumberSetting(String key, String label, double min, double max, double step, double def) {
        this.key = key;
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        set(def);
    }

    public double get() { return value; }
    public int getInt() { return (int) Math.round(value); }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = Math.round((v - min) / step) * step + min;
        value = Math.max(min, Math.min(max, v));
    }

    public float fraction() { return (float) ((value - min) / (max - min)); }

    public String display() {
        return step >= 1 ? String.valueOf(getInt()) : String.format("%.2f", value);
    }
}
