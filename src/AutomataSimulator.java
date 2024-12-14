import core.AutomataI;
import core.NdArrayF;
import core.NdArrayFloatI;
import core.NextStateGeneratorI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.async.BiConsumer;
import util.live.Listeners;


public class AutomataSimulator {

    interface Listener {

        void onAutomataStateChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldState, @NotNull NdArrayFloatI newState, int generation, int stepInGeneration);

        void onAutomataGenerationChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldGen, @NotNull NdArrayFloatI newGen, int generation, int steps);

        void onAutomataCellStateChanged(AutomataSimulator simulator, @NotNull NdArrayFloatI state, int[] cellIndices);

    }

    @NotNull
    private AutomataI automata;

    @NotNull
    private NdArrayF state;
    @Nullable
    private NdArrayF mTempOutState;

    private int generation = 0;

    private volatile int generationSteps = 1;
    private volatile boolean wrapEnabled = true;

    @NotNull
    private final Listeners<Listener> listeners = new Listeners<>();

    public AutomataSimulator(@NotNull AutomataI automata, int[] stateShape) {
        if (automata.dimensions() != stateShape.length) {
            throw new IllegalArgumentException("Automata and State must have same number of dimensions!!");
        }

        this.state = new NdArrayF(stateShape);
        this.automata = automata;
    }

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
        this.generationSteps = generationSteps;

        // TODO: callback
    }

    public synchronized boolean cycleCellState(int[] cellIndices) {
        final boolean changed = automata.cycleCellState(state, cellIndices);

        if (changed) {
            onCellStateChanged(state, cellIndices);
        }

        return changed;
    }

    public synchronized boolean setCellState(int[] cellIndices, float value) {
        final boolean changed = automata.setCellState(state, cellIndices, value);

        if (changed) {
            onCellStateChanged(state, cellIndices);
        }

        return changed;
    }

    public synchronized boolean stepCellState(int[] cellIndices, boolean stepUp) {
        final boolean changed = automata.stepCellState(state, cellIndices, stepUp);

        if (changed) {
            onCellStateChanged(state, cellIndices);
        }

        return changed;
    }

    public synchronized boolean setCellStateLowest(int[] cellIndices) {
        return setCellState(cellIndices, automata.lowestCellState());
    }

    public synchronized boolean setCellStateHighest(int[] cellIndices) {
        return setCellState(cellIndices, automata.highestCellState());
    }



    public synchronized void resetState() {
        generateNextStateInternal(automata::resetState, (old_state, new_state) -> {
            final int newGen = 0;
            generation = newGen;
            onStateChanged(old_state, new_state, newGen, 0);
            onGenerationChanged(old_state, new_state, newGen, 1);
        });
    }

    public synchronized void clearState() {
        generateNextStateInternal(automata::clearState, (old_state, new_state) -> {
            final int newGen = 0;
            generation = newGen;
            onStateChanged(old_state, new_state, newGen, 0);
            onGenerationChanged(old_state, new_state, newGen, 1);
        });
    }



    @NotNull
    private synchronized NdArrayF ensureOutTempState() {
        NdArrayF outState = mTempOutState;

        if (outState == null || !outState.isSameShape(state)) {
            outState = new NdArrayF(state.shape());
            mTempOutState = outState;
        }

        return outState;
    }

    private synchronized void generateNextStateInternal(@NotNull NextStateGeneratorI generator, @Nullable BiConsumer<NdArrayF, NdArrayF> callback) {
        final NdArrayF oldState = state;
        final NdArrayF newState = ensureOutTempState();

        generator.nextState(oldState, newState, wrapEnabled);

        // Switch current and temp states
        this.state = newState;
        mTempOutState = oldState;

        if (callback != null) {
            callback.consume(oldState, newState);   // Callback(old_state, new_state)
        }
    }

    public synchronized void nextGeneration() {
        final int gen = generation;
        final int steps = generationSteps;
        final NdArrayF curGenState = state;

        int step = 0;
        while (step < steps) {
            final int finalStep = step;
            generateNextStateInternal(automata, (old_state, new_state) -> onStateChanged(old_state, new_state, gen, finalStep));
            step++;
        }

        generation++;
        onGenerationChanged(curGenState, this.state, gen, steps);
    }


    protected void onStateChanged(@Nullable NdArrayFloatI oldState, @NotNull NdArrayFloatI newState, int generation, int stepInGeneration) {
        listeners.forEachListener(l -> l.onAutomataStateChanged(this, oldState, newState, generation, stepInGeneration));
    }

    protected void onGenerationChanged(@Nullable NdArrayFloatI oldGen, @NotNull NdArrayFloatI newGen, int generation, int steps) {
        listeners.forEachListener(l -> l.onAutomataGenerationChanged(this, oldGen, newGen, generation, steps));
    }

    protected void onCellStateChanged(@NotNull NdArrayFloatI state, int[] cellIndices) {
        listeners.forEachListener(l -> l.onAutomataCellStateChanged(this, state, cellIndices));
    }


    public void addListener(@NotNull Listener listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(@NotNull Listener listener) {
        return listeners.removeListener(listener);
    }

    public void ensureListener(@NotNull Listener listener) {
        listeners.ensureListener(listener);
    }
}