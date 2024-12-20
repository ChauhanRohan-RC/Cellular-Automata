package core.definition.automata;

import com.jogamp.common.util.IntIntHashMap;
import core.definition.NdArrayF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.U;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Base class for N-State Automata <br>
 * Cells can have integer states in range [0, n], n >= 1. Total States = n + 1
 * */
public abstract class NStateAutomataI extends AbstractAutomataI {

    protected static int toInt(float value) {
        return (int) value;
    }

    /**
     * Possible Cell states (int) = [0, n] , where n >= 1<br>
     * No of states = n + 1 <br>
     *
     * <pre>
     *     {@code
     *          0 : Healthy cell
     *          [1, n-1] : Infected cell (varying degree of infection)
     *          n : Dead cell
     *     }
     * </pre>
     */
    public final int n;

    /**
     * Color code of each possible state, for {@code LIGHT} theme <br>
     * Colors are inverted for {@code DARK} theme using {@link U#invertColor(int)}
     * */
    @Nullable
    private IntIntHashMap mColorMap;

    protected NStateAutomataI(int n, boolean monoChrome) {
        super(monoChrome);
        this.n = n;
    }


    @Override
    public final int cellStateCount() {
        return n + 1;
    }

    @Override
    public final float cellStateAt(int stateIndex) {
        return stateIndex;
    }

    @Override
    public final float lowestCellState() {
        return 0;
    }

    @Override
    public final float highestCellState() {
        return n;
    }


    @Override
    public void resetState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
//        outState.clear();
        outState.fillRandInt(0, n + 1);
    }

    @Override
    public void clearState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        outState.clear();
    }

    @Override
    public final boolean cycleCellState(@NotNull NdArrayF state, int[] cellIndices) {
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
    public final boolean stepCellState(@NotNull NdArrayF state, int[] cellIndices, boolean stepUp) {
        float prev = state.get(cellIndices);
        int _new = U.constrain(toInt(prev) + (stepUp? 1: -1), 0, n);
        if (prev != _new) {
            state.set(_new, cellIndices);
            return true;
        }

        return false;
    }


    /* COLORS */

    @NotNull
    protected abstract IntIntHashMap createLightThemeColorMap(boolean monoChrome);

    @Override
    public final int colorRGBForCell(float cellState, boolean darkMode) {
        final int c = getColorMap().get(toInt(cellState));
        return darkMode? U.invertColor(c) : c;
    }

    @NotNull
    private IntIntHashMap getColorMap() {
        if (mColorMap == null) {
            recreateColorMap();
        }

        return mColorMap;
    }

    private void recreateColorMap() {
        this.mColorMap = createLightThemeColorMap(isMonochromeEnabled());
    }

    @Override
    protected void onMonoChromeChanged(boolean monoChrome) {
        recreateColorMap();
    }
}
