package core;

import com.jogamp.common.util.IntIntHashMap;
import core.definition.NdArrayF;
import core.definition.automata.ColorProviderI;
import core.definition.automata.NStateAutomataI;
import org.jetbrains.annotations.NotNull;


public class BrianBrainAutomata extends NStateAutomataI {

    public static final String DISPLAY_NAME = "Brian's Brain";

    public static final boolean DEF_PARALLEL_COMPUTE_ALLOWED = true;
    public static final boolean DEF_MONOCHROME = true;

    public BrianBrainAutomata(boolean monoChrome) {
        super(2, monoChrome);
    }

    public BrianBrainAutomata() {
        this(DEF_MONOCHROME);
    }

    @Override
    public @NotNull String displayName() {
        return DISPLAY_NAME;
    }

    @Override
    public int dimensions() {
        return 2;
    }

    @Override
    protected @NotNull IntIntHashMap createLightThemeColorMap(boolean monoChrome) {
        if (monoChrome) {
            return ColorProviderI.createLightColorMapMonochrome(n, 0, 0, 1.0f, 0.0f, 1, false);
        }

        return ColorProviderI.createLightColorMapHueCycle(n, 0, 0.5f, 1, 1, false);
    }

    @Override
    public boolean isParallelComputeAllowed() {
        return DEF_PARALLEL_COMPUTE_ALLOWED;
    }

    @Override
    public void subComputeNextState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled, int row_start, int row_end) {
        final int[][] out_arr = new int[8][2];
        int cell_state, new_state;
        int neigh_count;

        for (int i = row_start; i < row_end; i++) {
            for (int j = 0; j < curState.shapeAt(1); j++) {
                cell_state = (int) curState.get(i, j);

                if (cell_state == 0) {
                    neigh_count = NdArrayF.getNeighbourIndices2D(curState.shapeAt(0), curState.shapeAt(1), i, j, wrapEnabled, out_arr);

                    float alive_count = 0;
                    for (int k = 0; k < neigh_count; k++) {
                        if (curState.get(out_arr[k]) == 2) {
                            alive_count++;
                        }
                    }

                    new_state = alive_count == 2? 2: 0;
                } else {
                    new_state = cell_state - 1;
                }

                outState.set(new_state, i, j);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
