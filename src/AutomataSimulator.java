import core.AutomataI;
import core.NdArrayF;
import core.NdArrayFloatI;
import core.NextStateGeneratorI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.async.BiConsumer;
import util.async.CancellationProvider;
import util.async.Canceller;
import util.live.Listeners;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class AutomataSimulator {

    public static final int DEF_GEN_STEPS = 1;
    public static final boolean DEF_WRAP_ENABLED = true;
    public static final boolean DEF_PAUSE_ON_RESET_OR_CLEAR = true;

    public static final RunMode DEF_SIM_Run_MODE = RunMode.LOOP;
    public static final long DEF_SIM_FRAME_RATE = 60;


    public interface Listener {

        void onSimulationFrameRateChanged(@NotNull AutomataSimulator simulator, long oldFrameRate, long newFrameRate);

        void onSimulationRunModeChanged(@NotNull AutomataSimulator simulator, @NotNull RunMode oldRunMode, @NotNull RunMode newRunMode);

        void onIsPlayingChanged(@NotNull AutomataSimulator simulator, boolean isPlaying);

        void onGenerationStepsChanged(@NotNull AutomataSimulator simulator, int prevGenSteps, int newGenSteps);

        void onWrapEnabledChanged(@NotNull AutomataSimulator simulator, boolean wrapEnabled);

        void onAutomataStateChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldState, @NotNull NdArrayFloatI newState, int generation, int stepInGeneration);

        void onAutomataGenerationChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldGen, @NotNull NdArrayFloatI newGen, int generation, int steps);

        void onAutomataCellStateChanged(AutomataSimulator simulator, @NotNull NdArrayFloatI state, int[] cellIndices);

    }

    public enum RunMode {
        /**
         * Schedule simulation task with a fixed rate using {@link ScheduledThreadPoolExecutor#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
         *
         * @see #setSimulationFrameRate(long)
         * */
        SCHEDULE_FIXED_RATE(true),

        /**
         * Run simulation task continuously in a while loop, irrespective of the Frame rate
         * */
        LOOP(false),
        ;

        public final boolean frameRateDependent;

        RunMode(boolean frameRateDependent) {
            this.frameRateDependent = frameRateDependent;
        }
    }


    @NotNull
    private AutomataI automata;

    @NotNull
    private NdArrayF state;
    @Nullable
    private NdArrayF mTempOutState;

    private int generation = 0;

    private volatile int generationSteps = DEF_GEN_STEPS;
    private volatile boolean wrapEnabled = DEF_WRAP_ENABLED;
    private volatile boolean pauseOnResetOrClear = DEF_PAUSE_ON_RESET_OR_CLEAR;

    @NotNull
    private final Listeners<Listener> mListeners = new Listeners<>();


    /* Simulation Vars */
    @NotNull
    private final Object mStateLock = new Object();

    @NotNull
    private final Object mSimTaskLock = new Object();

    @NotNull
    private final ScheduledThreadPoolExecutor mExecutor;

    @Nullable
    private Future<?> mSimFuture;
    @Nullable
    private Canceller mSimCanceller;

    private volatile boolean mIsPlaying;

    private RunMode mSimRunMode = DEF_SIM_Run_MODE;
    private long mSImFrameRate = DEF_SIM_FRAME_RATE;

//    @NotNull
//    private final CancellationProvider mSimCancellationProvider = new CancellationProvider() {
//        @Override
//        public boolean isCancelled() {
//            return !mIsPlaying || mSimFuture == null || mSimFuture.isCancelled();
//        }
//    };

//
//    @NotNull
//    private final Runnable mSimTask = () -> {
//        // DEBUG
//        final long now = System.currentTimeMillis();
//        if (mLastFrameMs > 0) {
//            System.out.println("Frame Time: " + (now - mLastFrameMs) + " ms");
//        }
//
//        mLastFrameMs = now;
//
//        nextGenerationSync(mSimCancellationProvider);
//    };

    public AutomataSimulator(@NotNull AutomataI automata, int[] stateShape, boolean initRandomState) {
        if (automata.dimensions() != stateShape.length) {
            throw new IllegalArgumentException("Automata and State must have same number of dimensions!!");
        }

        // State and Automata
        this.state = new NdArrayF(stateShape);
        this.automata = automata;

        // Executor
        final int cpu_count = Runtime.getRuntime().availableProcessors();
        mExecutor = new ScheduledThreadPoolExecutor(cpu_count);
        mExecutor.setMaximumPoolSize(cpu_count * 2);
        mExecutor.setRemoveOnCancelPolicy(true);
        mExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        mExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

        // Init
        if (initRandomState) {
            resetStateAsync();
        }
    }


    /* ============================  PARAMS and GETTERS  =========================== */

    public int getGeneration() {
        return generation;
    }

    public @NotNull NdArrayFloatI getState() {
        return state;
    }

    public float getCellState(int... cellIndices) {
        return state.get(cellIndices);
    }

    public @NotNull AutomataI getAutomata() {
        return automata;
    }

    public int getGenerationSteps() {
        return generationSteps;
    }

    public void setGenerationSteps(int generationSteps) {
        final int prevSteps = this.generationSteps;
        if (prevSteps != generationSteps) {
            this.generationSteps = generationSteps;
            onGenerationStepsChanged(prevSteps, generationSteps);
        }
    }

    public boolean isWrapEnabled() {
        return wrapEnabled;
    }

    public void setWrapEnabled(boolean wrapEnabled) {
        if (this.wrapEnabled != wrapEnabled) {
            this.wrapEnabled = wrapEnabled;
            onWrapEnabledChanged(wrapEnabled);
        }
    }

    public void toggleWrapEnabled() {
        setWrapEnabled(!isWrapEnabled());
    }

    public boolean isPauseOnResetOrClearEnabled() {
        return pauseOnResetOrClear;
    }

    public void setPauseOnResetOrClear(boolean pauseOnResetOrClear) {
        this.pauseOnResetOrClear = pauseOnResetOrClear;
    }


    /* ============================  CELL STATE METHODS  =========================== */

    public boolean cycleCellState(int[] cellIndices) {
        final boolean changed;

        synchronized (mStateLock) {
            changed = automata.cycleCellState(state, cellIndices);
            if (changed) {
                onCellStateChanged(state, cellIndices);
            }
        }

        return changed;
    }

    public boolean setCellState(int[] cellIndices, float value) {
        final boolean changed;

        synchronized (mStateLock) {
            changed = automata.setCellState(state, cellIndices, value);
            if (changed) {
                onCellStateChanged(state, cellIndices);
            }
        }

        return changed;
    }

    public boolean stepCellState(int[] cellIndices, boolean stepUp) {
        final boolean changed;

        synchronized (mStateLock) {
            changed = automata.stepCellState(state, cellIndices, stepUp);
            if (changed) {
                onCellStateChanged(state, cellIndices);
            }
        }

        return changed;
    }

    public boolean setCellStateLowest(int[] cellIndices) {
        return setCellState(cellIndices, automata.lowestCellState());
    }

    public boolean setCellStateHighest(int[] cellIndices) {
        return setCellState(cellIndices, automata.highestCellState());
    }


    /* ============================  STATE METHODS  =========================== */

    @NotNull
    private NdArrayF ensureOutTempState() {
        NdArrayF outState = mTempOutState;

        if (outState == null || !outState.isSameShape(state)) {
            synchronized (mStateLock) {
                outState = mTempOutState;

                if (outState == null || !outState.isSameShape(state)) {
                    outState = new NdArrayF(state.shape());
                    mTempOutState = outState;
                }
            }
        }

        return outState;
    }

    private void generateNextStateSyncInternal(@NotNull NextStateGeneratorI generator, @Nullable BiConsumer<NdArrayF, NdArrayF> callback) {
        synchronized (mStateLock) {
            final NdArrayF oldState = state;
            final NdArrayF newState = ensureOutTempState();

            generator.nextState(mExecutor, oldState, newState, wrapEnabled);

            // Switch current and temp states
            this.state = newState;
            mTempOutState = oldState;

            if (callback != null) {
                callback.consume(oldState, newState);   // Callback(old_state, new_state)
            }
        }
    }

    public void resetStateSync() {
        if (pauseOnResetOrClear) {
            setPlaying(false);
        }

        generateNextStateSyncInternal((executor, curState, outState, wrapEnabled1) -> automata.resetState(executor, curState, outState, wrapEnabled1), (old_state, new_state) -> {
            final int newGen = 0;
            generation = newGen;
            onStateChanged(old_state, new_state, newGen, 0);
            onGenerationChanged(old_state, new_state, newGen, 1);
        });
    }

    public void resetStateAsync() {
        mExecutor.execute(this::resetStateSync);
    }

    public void clearStateSync() {
        if (pauseOnResetOrClear) {
            setPlaying(false);
        }

        generateNextStateSyncInternal((executor, curState, outState, wrapEnabled1) -> automata.clearState(executor, curState, outState, wrapEnabled1), (old_state, new_state) -> {
            final int newGen = 0;
            generation = newGen;
            onStateChanged(old_state, new_state, newGen, 0);
            onGenerationChanged(old_state, new_state, newGen, 1);
        });
    }

    public void clearStateAsync() {
        mExecutor.execute(this::clearStateSync);
    }

    public void nextGenerationSync(@Nullable CancellationProvider c) {
        synchronized (mStateLock) {

            if (c != null && c.isCancelled()) {
                return;
            }

            final int gen = generation;
            final int steps = generationSteps;
            final NdArrayF curGenState = state;

            int step = 0;
            while (step < steps) {
                if (c != null && c.isCancelled()) {
                    break;
                }

                final int finalStep = step;
                generateNextStateSyncInternal(automata, (old_state, new_state) -> onStateChanged(old_state, new_state, gen, finalStep));
                step++;
            }

            if (step > 0) {
                generation = gen + 1;
                onGenerationChanged(curGenState, this.state, gen, steps);
            }
        }
    }

    /* ===================================  SIMULATION  ============================ */

    /**
     * @return number of threads that are always kept alive, irrespective of work load
     * */
    public int getCoreThreadCount() {
        return mExecutor.getCorePoolSize();
    }

    /**
     * @param coreThreadCount number of threads that should always be kept alive, irrespective of work load
     * */
    public void setCoreThreadCount(int coreThreadCount) {
        if (coreThreadCount < 1) {
            throw new IllegalArgumentException("Core Thread Count must be >= 1, given: " + coreThreadCount);
        }

        mExecutor.setCorePoolSize(coreThreadCount);
    }

    /**
     * @return maximum number of threads that can be created
     * */
    public int getMaxThreadCount() {
        return mExecutor.getMaximumPoolSize();
    }

    /**
     * @param maxThreadCount max number of threads that can be created
     * */
    public void setMaxThreadCount(int maxThreadCount) {
        if (maxThreadCount < 1 || maxThreadCount < mExecutor.getCorePoolSize()) {
            throw new IllegalArgumentException("Max Thread Count must be >= 1 and > corePoolSize (currently: " + mExecutor.getCorePoolSize() + "), given: " + maxThreadCount);
        }

        mExecutor.setMaximumPoolSize(maxThreadCount);
    }


    private void cancelSimTaskInternal() {
        final Canceller can = mSimCanceller;
        mSimCanceller = null;
        if (can != null) {
            can.cancel(true);
        }

        final Future<?> fut = mSimFuture;
        mSimFuture = null;
        if (fut != null) {
            fut.cancel(true);
            mExecutor.purge();
        }
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    private long mLastFrameMs = -1;

    private void requeueSimTaskInternal() {
        cancelSimTaskInternal();

        final Canceller canceller = Canceller.basic();
        mSimCanceller = canceller;

        final Runnable task = () -> {
            // DEBUG
//            final long prev = mLastFrameMs;
//            final long now = System.currentTimeMillis();
//            mLastFrameMs = now;

            // Main
            nextGenerationSync(canceller);

//            if (prev > 0) {
//                System.out.println("Frame Time: " + (now - prev) + " ms | Compute Time: " + (System.currentTimeMillis() - now) + " ms");
//            }
        };

        switch (mSimRunMode) {
            case SCHEDULE_FIXED_RATE -> {
                final long period_ns = (long) (1e9 / mSImFrameRate);
                mSimFuture = mExecutor.scheduleAtFixedRate(task, 0L, period_ns, TimeUnit.NANOSECONDS);
            }

            case LOOP -> {

                // LOOP the task constantly
                final Runnable looper = () -> {
                    while (!canceller.isCancelled()) {
                        task.run();
                    }
                };

                mSimFuture = mExecutor.submit(looper);
            }

            default -> {
                throw new AssertionError("Unexpected sim run mode: " + mSimRunMode);
            }
        }


    }

    public void setPlaying(boolean playing) {
        if (mIsPlaying == playing) {
            return;
        }

        synchronized (mSimTaskLock) {
            if (mIsPlaying == playing) {
                return;
            }

            mIsPlaying = playing;
            if (playing) {
                requeueSimTaskInternal();
            } else {
                cancelSimTaskInternal();
            }

            onIsPlayingChanged(playing);
        }
    }

    public void togglePlaying() {
        setPlaying(!isPlaying());
    }

    public long getSimulationFrameRate() {
        return mSImFrameRate;
    }

    public void setSimulationFrameRate(long frameRate) {
        if (frameRate <= 0) {
            throw new IllegalArgumentException("Frame rate must be positive. Given: " + frameRate);
        }

        if (mSImFrameRate == frameRate) {
            return;
        }

        synchronized (mSimTaskLock) {
            if (mSImFrameRate == frameRate) {
                return;
            }

            final long prev = mSImFrameRate;
            mSImFrameRate = frameRate;
            if (mSimRunMode.frameRateDependent && mIsPlaying) {
                requeueSimTaskInternal();
            }

            onFrameRateChanged(prev, frameRate);
        }
    }

    @NotNull
    public RunMode getSimulationRunMode() {
        return mSimRunMode;
    }

    public void setSimulationRunMode(@NotNull RunMode simRunMode) {
        if (simRunMode == null) {
            throw new NullPointerException("Simulation run mode cannot be null.");
        }

        if (mSimRunMode == simRunMode) {
            return;
        }

        synchronized (mSimTaskLock) {
            if (mSimRunMode == simRunMode) {
                return;
            }

            final RunMode prev = mSimRunMode;
            mSimRunMode = simRunMode;
            if (mIsPlaying) {
                requeueSimTaskInternal();
            }

            onSimulationRunModeChanged(prev, simRunMode);
        }
    }

    /* ==============================  CALLBACKS  ============================== */

    protected void onFrameRateChanged(long oldFrameRate, long newFrameRate) {
        mListeners.forEachListener(l -> l.onSimulationFrameRateChanged(this, oldFrameRate, newFrameRate));
    }

    protected void onSimulationRunModeChanged(@NotNull RunMode oldRunMode, @NotNull RunMode newRunMode) {
        mListeners.forEachListener(l -> l.onSimulationRunModeChanged(this, oldRunMode, newRunMode));
    }

    protected void onIsPlayingChanged(boolean isPlaying) {
        mListeners.forEachListener(l -> l.onIsPlayingChanged(this, isPlaying));
    }

    protected void onStateChanged(@Nullable NdArrayFloatI oldState, @NotNull NdArrayFloatI newState, int generation, int stepInGeneration) {
        mListeners.forEachListener(l -> l.onAutomataStateChanged(this, oldState, newState, generation, stepInGeneration));
    }

    protected void onGenerationChanged(@Nullable NdArrayFloatI oldGen, @NotNull NdArrayFloatI newGen, int generation, int steps) {
        mListeners.forEachListener(l -> l.onAutomataGenerationChanged(this, oldGen, newGen, generation, steps));
    }

    protected void onCellStateChanged(@NotNull NdArrayFloatI state, int[] cellIndices) {
        mListeners.forEachListener(l -> l.onAutomataCellStateChanged(this, state, cellIndices));
    }

    protected void onGenerationStepsChanged(int prevGenSteps, int newGenSteps) {
        mListeners.forEachListener(l -> l.onGenerationStepsChanged(this, prevGenSteps, newGenSteps));
    }

    protected void onWrapEnabledChanged(boolean wrapEnabled) {
        mListeners.forEachListener(l -> l.onWrapEnabledChanged(this, wrapEnabled));
    }

    public void addListener(@NotNull Listener listener) {
        mListeners.addListener(listener);
    }

    public boolean removeListener(@NotNull Listener listener) {
        return mListeners.removeListener(listener);
    }

    public void ensureListener(@NotNull Listener listener) {
        mListeners.ensureListener(listener);
    }
}