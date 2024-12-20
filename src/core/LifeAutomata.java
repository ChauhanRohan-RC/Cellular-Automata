package core;

import core.definition.NdArrayF;
import core.definition.automata.AbstractAutomataI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 2D automata with 2 cell states: 0 (dead) and 1 (alive). Each cell has 8 neighbours
 * <br><br>
 * After each time step... <br>
 * 1. A dead cell is born if it has x alive neighbours. <br>
 * 2. An alive cell survives if it has y alive neighbours.  <br>
 * <br>
 * Rules are written in B[x]/S[y] notation, where B and S stands for BORN and SURVIVE <br>
 * Examples <br>
 * 1. Conway's Game of Life: B3/S23  (born if 3 alive neighbours, survive if 2 or 3 alive neighbours)
 * 2. Seeds: B2/S   (born if 2 alive neighbours, never survive)
 */
public class LifeAutomata extends AbstractAutomataI {

    public static final boolean DEF_PARALLEL_COMPUTE_ALLOWED = true;
    public static final boolean DEF_MONOCHROME = true;

    @FunctionalInterface
    public interface NewCellStateProvider {
        int getNewState(int state, int neighbourCount, int aliveNeighbourCount);
    }

    public enum Rule {

        // B2/S23
        CONWAY_LIFE("Conway Life",
                (state, neighbourCount, aliveNeighbourCount) -> {
                    if (state == 0) return aliveNeighbourCount == 3 ? 1 : 0;      // born: 2
                    return aliveNeighbourCount == 2 || aliveNeighbourCount == 3 ? 1 : 0;    // survive: 2,3
                }
        ),

        // B34/s34
        LIFE_34("Life-34",
                (state, neighbourCount, aliveNeighbourCount) -> {
                    return aliveNeighbourCount == 3 || aliveNeighbourCount == 4 ? 1 : 0;
                }
        ),

        // B2/S
        SEEDS("Life-Seeds",
                (state, neighbourCount, aliveNeighbourCount) -> {
                    if (state == 0) return aliveNeighbourCount == 2 ? 1 : 0;      // born: 2
                    return 0;    // survive: NEVER
                }
        ),

        // B1357/S1357
        REPLICATOR("Life-Replicator",
                (state, neighbourCount, aliveNeighbourCount) -> {
                    return aliveNeighbourCount % 2 == 1 ? 1 : 0;
                }
        ),

        // B3/S012345678
        FLAKES("Life-Flakes",
                (state, neighbourCount, aliveNeighbourCount) -> {
                    return state == 1 || aliveNeighbourCount == 3 ? 1 : 0;
                },
                0xFFFFFFFF, 0xFF25A7DF,
                0xFF000000, 0xFF22DFFF
        ),

        // B35678/S5678
        DIAMOEBA("Life-Diamoeba",
                (state, neighbourCount, aliveNeighbourCount) -> {
            if (aliveNeighbourCount >= 5) return 1;

            if (state == 0 && aliveNeighbourCount == 3) return 1;

            return 0;
        });


        @NotNull
        public final String displayName;
        @NotNull
        public final NewCellStateProvider cellStateProvider;
        public final int lightColorOff;
        public final int lightColorOn;
        public final int darkColorOff;
        public final int darkColorOn;

        Rule(@NotNull String displayName, @NotNull NewCellStateProvider cellStateProvider, int lightColorOff, int lightColorOn, int darkColorOff, int darkColorOn) {
            this.displayName = displayName;
            this.cellStateProvider = cellStateProvider;
            this.lightColorOff = lightColorOff;
            this.lightColorOn = lightColorOn;
            this.darkColorOff = darkColorOff;
            this.darkColorOn = darkColorOn;
        }

        Rule(@NotNull String displayName, @NotNull NewCellStateProvider cellStateProvider) {
            this(displayName, cellStateProvider, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFFFFFFFF);
        }

        public int colorFor(boolean on, boolean darkMode) {
            return on ? darkMode ? darkColorOn : lightColorOn : darkMode ? darkColorOff : lightColorOff;
        }
    }


    @NotNull
    private final Rule rule;

    public LifeAutomata(@NotNull Rule rule) {
        super(DEF_MONOCHROME);
        this.rule = rule;
    }

    @Override
    public @NotNull String displayName() {
        return rule.displayName;
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
        return rule.colorFor(on, darkMode);
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
    public void subComputeNextState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled, int row_start, int row_end) {
        final int[][] out_arr = new int[8][2];
        int cell_state;
        int neigh_count;

        for (int i = row_start; i < row_end; i++) {
            for (int j = 0; j < curState.shapeAt(1); j++) {
                cell_state = (int) curState.get(i, j);
                neigh_count = NdArrayF.getNeighbourIndices2D(curState.shapeAt(0), curState.shapeAt(1), i, j, wrapEnabled, out_arr);

                float neigh_state_sum = 0;
                for (int k = 0; k < neigh_count; k++) {
                    neigh_state_sum += curState.get(out_arr[k]);
                }

                outState.set(rule.cellStateProvider.getNewState(cell_state, neigh_count, (int) neigh_state_sum), i, j);
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
        state.set(val - 0.5 > 0 ? 0 : 1, cellIndices);
        return true;
    }

    @Override
    public boolean stepCellState(@NotNull NdArrayF state, int[] cellIndices, boolean stepUp) {
        float val = state.get(cellIndices);
        float newVal = stepUp ? 1 : 0;
        state.set(newVal, cellIndices);
        return val != newVal;
    }

    @Override
    public String toString() {
        return "LifeAutomata{" +
                "dimension=" + 2 +
                ", cellStates=" + 2 +
                ", rule=" + rule.displayName +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LifeAutomata that = (LifeAutomata) o;
        return rule == that.rule;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rule);
    }
}
