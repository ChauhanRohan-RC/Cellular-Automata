package core;

import com.jogamp.common.util.IntIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.U;

import java.awt.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * N-state Generalization of Conway's game of life
 * <br> <br>
 * Taking {@code n = 1, k1 = 2, k2 = k3 = k4 = 3} gives the Conway's game of Life
 * */
public class NLifeAutomata extends NStateAutomataI {

    public static final int DEF_N = 4;

    // Constants  k1, k2, k3, k4 in range [0, 8N or 900]
    public static final int DEF_K1 = 8;
    public static final int DEF_K2 = 12;
    public static final int DEF_K3 = 8;
    public static final int DEF_K4 = 9;

    private static final boolean DEF_MONOCHROME = false;

    private static final boolean DEF_PARALLEL_COMPUTE_ALLOWED = true;



    @NotNull
    private static IntIntHashMap createColorMap(int n, boolean monoChrome) {
        final IntIntHashMap cmap = new IntIntHashMap();

        if (monoChrome) {
            // Same Hue, different saturation

            // higher order interpolation
            float min_saturation = 0.4f;
            float order = 2;

            for (int i = 0; i <= n; i++) {
                // LINEAR Interp: U.map(i, 0, n, 1, 0.85f)

                float y_n = (float) ((Math.pow(min_saturation, order) - 1) * ((float) i / n) + 1);
                float y = (float) Math.pow(y_n, 1 / order);

                cmap.put(i, Color.getHSBColor(20/360f, y, 1).getRGB());
            }
        } else {
            cmap.put(0, U.gray255(255));    // HEALTHY
            cmap.put(n, U.gray255(0));        // ILL

            // INFECTED: interpolating hue
            for (int i = 1; i < n; i++) {
                cmap.put(i, Color.getHSBColor(U.map(i, 1, n - 1, 0.0f, 0.5f), 1, 1).getRGB());
            }
        }

        return cmap;
    }



    /**
     * Constants K1, K2, K3 and K4
     *
     * @see #DEF_K1
     * @see #DEF_K2
     * @see #DEF_K3
     * @see #DEF_K4
     * */
    private final int k1, k2, k3, k4;

    private final float mHalfN;

    public NLifeAutomata(int n, int k1, int k2, int k3, int k4, boolean monoChrome) {
        super(n, monoChrome);
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.k4 = k4;

        mHalfN = (float) this.n / 2;
    }

    public NLifeAutomata(int n) {
        this(n, DEF_K1, DEF_K2, DEF_K3, DEF_K4, DEF_MONOCHROME);
    }

    public NLifeAutomata() {
        this(DEF_N);
    }


    @Override
    public int dimensions() {
        return 2;
    }

    @Override
    protected @NotNull IntIntHashMap createLightThemeColorMap(boolean monoChrome) {
        return createColorMap(n, monoChrome);
    }

    @Override
    public boolean isParallelComputeAllowed() {
        return DEF_PARALLEL_COMPUTE_ALLOWED;
    }

    @Override
    protected void subCompute(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled, int row_start, int row_end) {
        final int[][] neigh_arr = new int[8][2];
        int neigh_count;
        int cell_state, new_state;

        for (int i = row_start; i < row_end; i++) {
            for (int j = 0; j < curState.shapeAt(1); j++) {
                cell_state = toInt(curState.get(i, j));

                neigh_count = NdArrayF.getNeighbourIndices2D(curState.shapeAt(0), curState.shapeAt(1), i, j, wrapEnabled, neigh_arr);
                float neigh_states_sum = 0;
                for (int k = 0; k < neigh_count; k++) {
                    neigh_states_sum += toInt(curState.get(neigh_arr[k]));
                }

                if (cell_state > mHalfN) {
                    if (neigh_states_sum >= k1 && neigh_states_sum <= k2) {
                        new_state = cell_state + 1;
                    } else {
                        new_state = cell_state - 1;
                    }
                } else {
                    if (neigh_states_sum >= k3 && neigh_states_sum <= k4) {
                        new_state = cell_state + 1;
                    } else {
                        new_state = cell_state - 1;
                    }
                }

                new_state = U.constrain(new_state, 0, n);
                outState.set(new_state, i, j);
            }
        }
    }
}
