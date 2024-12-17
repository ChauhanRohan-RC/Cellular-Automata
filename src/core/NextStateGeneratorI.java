package core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public interface NextStateGeneratorI {

    void nextState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled);

}
