package core;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ConwayLifeAutomata implements AutomataI {

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
    @NotNull
    public Color colorFor(float cellState, boolean darkMode) {
        final boolean on = cellState - 0.5 > 0;
        if (darkMode) {
            return on? Color.WHITE : Color.BLACK;
        }

        return on? Color.BLACK : Color.WHITE;
    }

    @Override
    public void nextState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        final int rows = curState.shapeAt(0);
        final int cols = curState.shapeAt(1);

        final int[][] out_arr = new int[8][2];
        float cell_state;
        int neigh_count;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                cell_state = curState.get(i, j);
                neigh_count = NdArrayF.getNeighbourIndices2D(rows, cols, i, j, wrapEnabled, out_arr);

                float neigh_state_sum = 0;
                for (int k = 0; k < neigh_count; k++) {
                    neigh_state_sum += curState.get(out_arr[k]);
                }

                if (cell_state == 0) {
                    // Dead
                    outState.set(neigh_state_sum == 3? 1: 0, i, j);
                } else {
                    // Alive
                    outState.set(neigh_state_sum < 2 || neigh_state_sum > 3? 0: 1, i, j);
                }

            }
        }
    }

    @Override
    public void resetState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        outState.clear();
    }

    @Override
    public void clearState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
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
