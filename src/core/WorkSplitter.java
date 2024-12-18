package core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

public class WorkSplitter {

    public interface ComputeTask {

        void compute(int row_start, int row_end);

    }

    private boolean parallelComputeEnabled;
    private int minCellsPerThread;


    public WorkSplitter(boolean parallelComputeEnabled, int minCellsPerThread) {
        this.parallelComputeEnabled = parallelComputeEnabled;
        this.minCellsPerThread = minCellsPerThread;
    }

    public void setParallelComputeEnabled(boolean parallelComputeEnabled) {
        this.parallelComputeEnabled = parallelComputeEnabled;
    }

    public boolean isParallelComputeEnabled() {
        return parallelComputeEnabled;
    }

    public int getMinCellsPerThread() {
        return minCellsPerThread;
    }

    public void setMinCellsPerThread(int minCellsPerThread) {
        this.minCellsPerThread = minCellsPerThread;
    }

    public boolean isExecutorParallelReady(@Nullable ThreadPoolExecutor executor) {
        return executor != null && !executor.isShutdown() && executor.getCorePoolSize() >= 2 && executor.getMaximumPoolSize() >= 3;
    }

    public void compute(@Nullable ThreadPoolExecutor executor, int totalCells, int totalRows, @NotNull WorkSplitter.ComputeTask computeTask) {
        final int minCells = minCellsPerThread;

        if (!parallelComputeEnabled ||
                executor == null ||
                totalCells <= minCells ||
                !isExecutorParallelReady(executor)) {

            computeTask.compute(0, totalRows);
        } else {
            // Split rows equally among workers

            final int worker_count = Math.min(totalCells / minCells, executor.getMaximumPoolSize() - 1);
            if (worker_count < 2 || totalRows < worker_count) {
                computeTask.compute(0, totalRows);
            } else {
                List<Callable<Void>> tasks = new ArrayList<>();
                int rows_per_worker = totalCells / worker_count;

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
}
