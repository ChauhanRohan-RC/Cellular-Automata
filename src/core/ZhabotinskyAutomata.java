package core;

import com.jogamp.common.util.IntIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.U;

import java.awt.*;

/**
 * An automata modelling the Belousov-Zhabotinsky Reaction
 * */
public class ZhabotinskyAutomata implements AutomataI {

    private static int toInt(float value) {
        return (int) value;
    }

    @NotNull
    private static IntIntHashMap createColorMap(int n, boolean monoChrome) {
        final IntIntHashMap cmap = new IntIntHashMap();

        if (monoChrome) {
            // Same Hue, different saturation

            // higher order interpolation
            float min_saturation = 0.4f;
            float order = 7;

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
                cmap.put(i, Color.getHSBColor(U.map(i, 1, n - 1, 0.05f, 0.45f), 1, 1).getRGB());
            }
        }

        return cmap;
    }



    public static final int DEF_N = 99;
    public static final float DEF_K1 = 2;
    public static final float DEF_K2 = 3;
    public static final int DEF_G = 35;

    private static final boolean DEF_MONOCHROME = true;


    /**
     * No of states = n + 1, where n >= 1 <br>
     * Possible integer states = [0, n]
     *
     * <pre>
     *     {@code
     *          0 : Healthy cell
     *          [1, n-1] : Infected cell (varying degree of infection)
     *          n : Ill cell
     *     }
     * </pre>
     *
     * @see #DEF_N
     */
    private final int n;

    /**
     * Constant K1, in range [1, 8]
     *
     * @see #DEF_K1
     * */
    private final float k1;

    /**
     * Constant K2, in range [1, 8]
     *
     * @see #DEF_K2
     * */
    private final float k2;

    /**
     * Constant G, representing the rate of infection spread <br>
     * Mostly, g < n
     *
     * @see #DEF_G
     * */
    private final int g;

    private boolean monoChrome = DEF_MONOCHROME;

    /**
     * Color code of each possible state, for {@code LIGHT} theme <br>
     * Colors are inverted for {@code DARK} theme using {@link U#invertColor(int)}
     * */
    @NotNull
    private IntIntHashMap colorMap;

    public ZhabotinskyAutomata(int n, float k1, float k2, int g, boolean monoChrome) {
        this.n = n;
        this.k1 = k1;
        this.k2 = k2;
        this.g = g;

        this.monoChrome = monoChrome;
        this.colorMap = createColorMap(n, monoChrome);
    }

    public ZhabotinskyAutomata(int n) {
        this(n, DEF_K1, DEF_K2, DEF_G, DEF_MONOCHROME);
    }

    public ZhabotinskyAutomata() {
        this(DEF_N, DEF_K1, DEF_K2, DEF_G, DEF_MONOCHROME);
    }


    @Override
    public int dimensions() {
        return 2;
    }

    @Override
    public int cellStateCount() {
        return n + 1;
    }

    @Override
    public float cellStateAt(int stateIndex) {
        return stateIndex;
    }

    @Override
    public int colorRGBFor(float cellState, boolean darkMode) {
        final int c = colorMap.get(toInt(cellState));
        return darkMode? U.invertColor(c) : c;
    }

    @Override
    public void resetState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
//        outState.clear();
        outState.fillRandInt(0, n + 1);
    }

    @Override
    public void clearState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        outState.clear();
    }

    @Override
    public boolean cycleCellState(@NotNull NdArrayF state, int[] cellIndices) {
        float prev = state.get(cellIndices);
        int _new = toInt(prev) + 1;
        if (_new > n) {
            _new = 0;
        }

        if (prev != _new) {
            state.set(_new, cellIndices);
            return true;
        }

        return false;
    }

    @Override
    public boolean stepCellState(@NotNull NdArrayF state, int[] cellIndices, boolean stepUp) {
        float prev = state.get(cellIndices);
        int _new = U.constrain(toInt(prev) + (stepUp? 1: -1), 0, n);
        if (prev != _new) {
            state.set(_new, cellIndices);
            return true;
        }

        return false;
    }

    @Override
    public void nextState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        final int rows = curState.shapeAt(0);
        final int cols = curState.shapeAt(1);

        final int[][] neigh_arr = new int[8][2];
        int neigh_count;
        int cell_state, new_state;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                cell_state = toInt(curState.get(i, j));

                if (cell_state == n) {
                    new_state = 0;      // ILL CELL -> HEALTHY CELL
                } else {
                    neigh_count = NdArrayF.getNeighbourIndices2D(rows, cols, i, j, wrapEnabled, neigh_arr);

                    float states_sum = cell_state;
                    int neigh_state;
                    int infected_neigh_count = 0, ill_neigh_count = 0;

                    for (int k = 0; k < neigh_count; k++) {
                        neigh_state = toInt(curState.get(neigh_arr[k]));
                        states_sum += neigh_state;

                        if (neigh_state == n) {
                            ill_neigh_count++;
                        } else if (neigh_state > 0) {
                            infected_neigh_count++;
                        }
                    }

                    if (cell_state == 0) {
                        // Healthy cell
                        new_state = toInt(infected_neigh_count / k1) + toInt(ill_neigh_count / k2);
                    } else {
                        // Infected cell
                        new_state = toInt(states_sum / (infected_neigh_count + ill_neigh_count + 1)) + g;
                    }

                    new_state = U.constrain(new_state, 0, n);
                }


                outState.set(new_state, i, j);
            }
        }

    }


    /* COLOR MAP --------------------------- */

    public boolean isMonoChrome() {
        return monoChrome;
    }

    public ZhabotinskyAutomata setMonoChrome(boolean monoChrome) {
        if (this.monoChrome != monoChrome) {
            this.monoChrome = monoChrome;
            this.colorMap = createColorMap(n, monoChrome);
        }

        return this;
    }

    public ZhabotinskyAutomata toggleMonoChrome() {
        return setMonoChrome(!isMonoChrome());
    }
}
