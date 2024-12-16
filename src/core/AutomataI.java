package core;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface AutomataI extends NextStateGeneratorI {

    int dimensions();

    int cellStateCount();

    float cellStateAt(int stateIndex);

    int colorRGBFor(float cellState, boolean darkMode);

    default float lowestCellState() {
        return cellStateAt(0);
    }

    default float highestCellState() {
        return cellStateAt(cellStateCount() - 1);
    }

    // TODO: can init random state as seed
    void resetState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled);

    void clearState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled);

    /**
     * Changes a cell state on UI Events
     *
     * @return {@code true} if the state is changed, otherwise {@code false}
     * */
    boolean cycleCellState(@NotNull NdArrayF state, int[] cellIndices);

    /**
     * Sets a particular cell state
     *
     * @return {@code true} if the state is changed, otherwise {@code false}
     * */
    default boolean setCellState(@NotNull NdArrayF state, int[] cellIndices, float cellState) {
        final float prev = state.get(cellIndices);
        state.set(cellState, cellIndices);
        return prev != cellState;
    }

    boolean stepCellState(@NotNull NdArrayF state, int[] cellIndices, boolean stepUp);
}
