package util.live;

import org.jetbrains.annotations.NotNull;
import util.async.Async;
import util.async.Consumer;

import java.util.Collection;

public interface ListenersI<T> {

    int listenersCount();

    boolean addListener(@NotNull T listener);

    boolean removeListener(@NotNull T listener);

    boolean containsListener(@NotNull T listener);

    default boolean ensureListener(@NotNull T listener) {
        return containsListener(listener) || addListener(listener);
    }

    @NotNull
    Collection<T> listenersIterationCopy();

    default void forEachListener(@NotNull Consumer<T> action) {
        for (T l: listenersIterationCopy()) {          // safe-iteration (prevents concurrent modification)
            action.consume(l);
        }
    }

    default void dispatchListenersOnMainThread(@NotNull Consumer<T> action) {
        Async.postIfNotOnMainThread(() -> forEachListener(action));
    }

}
