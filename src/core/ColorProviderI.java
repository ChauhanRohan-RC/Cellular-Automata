package core;

import com.jogamp.common.util.IntIntHashMap;
import org.jetbrains.annotations.NotNull;
import util.U;

import java.awt.*;

public interface ColorProviderI {

    @NotNull
    static IntIntHashMap createLightColorMapHueCycle(int n, float hueStart, float hueEnd, float saturation, float brightness, boolean useHueForFirstLast) {
        final IntIntHashMap cmap = new IntIntHashMap();

        if (useHueForFirstLast) {
            for (int i = 0; i <= n; i++) {
                cmap.put(i, Color.getHSBColor(U.map(i, 0, n, hueStart, hueEnd), 1, 1).getRGB());
            }
        } else {
            cmap.put(0, U.gray255(255));
            cmap.put(n, U.gray255(0));

            // INFECTED: interpolating hue
            for (int i = 1; i < n; i++) {
                cmap.put(i, Color.getHSBColor(U.map(i, 1, n - 1, hueStart, hueEnd), saturation, brightness).getRGB());
            }
        }

        return cmap;
    }

    @NotNull
    static IntIntHashMap createLightColorMapMonochrome(int n, float hue, float v2, float v1Start, float v1End, float v1InterpolationOrder, boolean v1Saturation) {
        final IntIntHashMap cmap = new IntIntHashMap();

        // higher order interpolation

        final float v1StartOrder = (float) Math.pow(v1Start, v1InterpolationOrder);
        final float v1EndOrder = (float) Math.pow(v1End, v1InterpolationOrder);
        final float coeff = (v1EndOrder - v1StartOrder) / n;
        final float order_recip = 1 / v1InterpolationOrder;

        for (int i = 0; i <= n; i++) {
            // LINEAR Interp: U.map(i, 0, n, 1, 0.85f)

            float y_n = (coeff * i) + v1StartOrder;
            float y = (float) Math.pow(y_n, order_recip);

            final Color c = v1Saturation? Color.getHSBColor(hue, y, v2): Color.getHSBColor(hue, v2, y);
            cmap.put(i, c.getRGB());
        }

        return cmap;
    }



    int colorRGBForCell(float cellState, boolean darkMode);

    boolean isMonochromeEnabled();

    void setMonochromeEnabled(boolean monochrome);

    default void toggleMonochromeEnabled() {
        setMonochromeEnabled(!isMonochromeEnabled());
    }


}
