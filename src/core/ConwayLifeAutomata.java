package core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.concurrent.ThreadPoolExecutor;

public class ConwayLifeAutomata extends AbstractAutomataI {

    private static final int COLOR_ON = Color.BLACK.getRGB();
    private static final int COLOR_OFF = Color.WHITE.getRGB();

    public static final boolean DEF_PARALLEL_COMPUTE_ALLOWED = true;

    public ConwayLifeAutomata() {
        super(DEF_PARALLEL_COMPUTE_ALLOWED, true);
    }

    @Override
    public int dimensions() {
        return 2;
    }

    @Override
    public int cellStateCount() {
        return 2;
    }

    @Override
    public float cellStateAt(int stateIndex) {
        return stateIndex;      // 0 or 1
    }

    @Override
    public int colorRGBForCell(float cellState, boolean darkMode) {
        final boolean on = cellState - 0.5 > 0;
        if (darkMode) {
            return on? COLOR_OFF : COLOR_ON;
        }

        return on? COLOR_ON : COLOR_OFF;
    }

    @Override
    protected void onMonoChromeChanged(boolean monoChrome) {
        super.onMonoChromeChanged(monoChrome);
    }

    @Override
    public boolean isParallelComputeAllowed() {
        return DEF_PARALLEL_COMPUTE_ALLOWED;
    }

    @Override
    protected void subCompute(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled, int row_start, int row_end) {
        final int[][] out_arr = new int[8][2];
        int cell_state, new_state;
        int neigh_count;

        for (int i = row_start; i < row_end; i++) {
            for (int j = 0; j < curState.shapeAt(1); j++) {
                cell_state = (int) curState.get(i, j);
                neigh_count = NdArrayF.getNeighbourIndices2D(curState.shapeAt(0), curState.shapeAt(1), i, j, wrapEnabled, out_arr);

                float neigh_state_sum = 0;
                for (int k = 0; k < neigh_count; k++) {
                    neigh_state_sum += curState.get(out_arr[k]);
                }

                if (cell_state == 0) {
                    // Dead
                    new_state = neigh_state_sum == 3? 1: 0;
                } else {
                    // Alive
                    new_state = neigh_state_sum < 2 || neigh_state_sum > 3? 0: 1;
                }

                outState.set(new_state, i, j);
            }
        }
    }

    @Override
    public void resetState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
//        outState.clear();
        outState.fillRandInt(0, 2);
    }

    @Override
    public void clearState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        outState.clear();
    }

    @Override
    public boolean cycleCellState(@NotNull NdArrayF state, int[] cellIndices) {
        float val = state.get(cellIndices);
        state.set(val - 0.5 > 0? 0 : 1, cellIndices);
        return true;
    }

    @Override
    public boolean stepCellState(@NotNull NdArrayF state, int[] cellIndices, boolean stepUp) {
        float val = state.get(cellIndices);
        float newVal = stepUp? 1: 0;
        state.set(newVal, cellIndices);
        return val != newVal;
    }
}
