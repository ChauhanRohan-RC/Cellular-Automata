package core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadPoolExecutor;

public abstract class AbstractAutomataI implements AutomataI {

    public static final boolean DEF_PARALLEL_COMPUTE_ENABLED = true;
    public static final int DEF_PARALLEL_COMPUTE_MIN_CELLS_PER_THREAD = 10000;

    @NotNull
    private final WorkSplitter workSplitter;


    protected AbstractAutomataI() {
        this.workSplitter = new WorkSplitter(DEF_PARALLEL_COMPUTE_ENABLED, DEF_PARALLEL_COMPUTE_MIN_CELLS_PER_THREAD);
    }

    @NotNull
    public final WorkSplitter getWorkSplitter() {
        return workSplitter;
    }

    /**
     * Whether Parallel compute is allowed by the underlying Automata implementation
     * */
    public abstract boolean isParallelComputeAllowed();

    protected abstract void subCompute(@NotNull NdArrayF curState,
                              @NotNull NdArrayF outState,
                              boolean wrapEnabled,
                              int row_start, int row_end);

    @Override
    public final void nextState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled) {
        final int rows = curState.shapeAt(0);
        final int cols = curState.shapeAt(1);

        final WorkSplitter.ComputeTask computeTask = (row_start, row_end) -> subCompute(curState, outState, wrapEnabled, row_start, row_end);
        if (isParallelComputeAllowed()) {
            workSplitter.compute(executor, rows * cols, rows, computeTask);
        } else {
            computeTask.compute(0, rows);       // Compute all now
        }
    }
}
