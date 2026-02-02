package core.definition.automata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

public class WorkSplitter {

    public interface ComputeTask {
        void compute(int row_start, int row_end);
    }

    public interface Listener {

        void onParallelComputeEnabledChanged(boolean parallelComputeEnabled);

        void onMinCellsPerThreadChanged(int oldMinCellsPerThread, int newMinCellsPerThread);
    }

    private boolean parallelComputeEnabled;
    private int minCellsPerThread;

    @Nullable
    private Listener mListener;

    public WorkSplitter(boolean parallelComputeEnabled, int minCellsPerThread) {
        this.parallelComputeEnabled = parallelComputeEnabled;
        this.minCellsPerThread = minCellsPerThread;
    }

    @Nullable
    public Listener getListener() {
        return mListener;
    }

    public void setListener(@Nullable Listener listener) {
        mListener = listener;
    }

    public boolean isParallelComputeEnabled() {
        return parallelComputeEnabled;
    }

    public void setParallelComputeEnabled(boolean parallelComputeEnabled) {
        if (parallelComputeEnabled != this.parallelComputeEnabled) {
            this.parallelComputeEnabled = parallelComputeEnabled;
            if (mListener != null) {
                mListener.onParallelComputeEnabledChanged(parallelComputeEnabled);
            }
        }

    }

    public void toggleParallelComputeEnabled() {
        setParallelComputeEnabled(!parallelComputeEnabled);
    }

    public int getMinCellsPerThread() {
        return minCellsPerThread;
    }

    public void setMinCellsPerThread(int minCellsPerThread) {
        if (minCellsPerThread != this.minCellsPerThread) {
            final int old = this.minCellsPerThread;
            this.minCellsPerThread = minCellsPerThread;
            if (mListener != null) {
                mListener.onMinCellsPerThreadChanged(old, minCellsPerThread);
            }
        }

    }

    public boolean isExecutorParallelReady(@Nullable ThreadPoolExecutor executor) {
        return executor != null && !executor.isShutdown() && executor.getCorePoolSize() >= 2 && executor.getMaximumPoolSize() >= 3;
    }

    public int getWorkerThreadCount(@Nullable ThreadPoolExecutor executor, int totalCells, int totalRows) {
        final int minCells = minCellsPerThread;

        if (!parallelComputeEnabled ||
                executor == null ||
                totalCells <= minCells ||
                !isExecutorParallelReady(executor)) {
            return 1;
        }

        final int worker_count = Math.min(totalCells / minCells, executor.getMaximumPoolSize() - 1);
        return Math.min(worker_count, totalRows);
    }

    public void compute(@Nullable ThreadPoolExecutor executor, int totalCells, int totalRows, @NotNull WorkSplitter.ComputeTask computeTask) {
        final int worker_count = getWorkerThreadCount(executor, totalCells, totalRows);
        if (executor == null || worker_count <= 1) {
            computeTask.compute(0, totalRows);
        } else {
            LinkedList<Callable<Void>> tasks = new LinkedList<>();
            int rows_per_worker = totalRows / worker_count;

            for (int i = 0; i < worker_count - 1; i++) {
                final int row_start = i * rows_per_worker;
                final int row_end = row_start + rows_per_worker;
                tasks.add(() -> {
                    computeTask.compute(row_start, row_end);
                    return null;
                });
            }

            // last worker
            tasks.add(() -> {
                computeTask.compute((worker_count - 1) * rows_per_worker, totalRows);
                return null;
            });

            try {
                executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for tasks: " + e);
            }
        }
    }
}
