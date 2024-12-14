package core;

import org.jetbrains.annotations.NotNull;

public interface NextStateGeneratorI {

    void nextState(@NotNull NdArrayF curState, @NotNull NdArrayF outState, boolean wrapEnabled);

}
