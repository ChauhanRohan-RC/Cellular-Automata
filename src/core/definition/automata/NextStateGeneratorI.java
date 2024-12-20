package core.definition.automata;

import core.definition.NdArrayF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadPoolExecutor;

public interface NextStateGeneratorI {

    void computeNextState(@Nullable ThreadPoolExecutor executor, @NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled);

}
