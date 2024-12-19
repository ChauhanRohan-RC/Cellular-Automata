package core;

import com.jogamp.common.util.IntIntHashMap;
import org.jetbrains.annotations.NotNull;

// TODO

public class BrianBrainAutomata extends NStateAutomataI {

    public static final boolean DEF_PARALLEL_COMPUTE_ALLOWED = true;


    public BrianBrainAutomata(boolean monoChrome) {
        super(2, DEF_PARALLEL_COMPUTE_ALLOWED, monoChrome);
    }

    @Override
    public int dimensions() {
        return 2;
    }

    @Override
    protected @NotNull IntIntHashMap createLightThemeColorMap(boolean monoChrome) {
//        if (monoChrome) {
//            return ColorProviderI.createLightColorMapMonochrome(n, 20/360f, 2, 0.2f, 1);
//        }

        return ColorProviderI.createLightColorMapHueCycle(n, 0, 0.5f, 1, 1, false);
    }

    @Override
    public boolean isParallelComputeAllowed() {
        return DEF_PARALLEL_COMPUTE_ALLOWED;
    }

    @Override
    protected void subCompute(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled, int row_start, int row_end) {

    }
}
