package core.definition.automata;

public abstract class AbstractAutomataI implements AutomataI {

    /**
     * Whether the colors should have same HUE, or can have different HUE's
     * */
    private boolean monoChrome;

    protected AbstractAutomataI(boolean monoChrome) {
        this.monoChrome = monoChrome;
    }


    /* COLOR MAP --------------------------- */

    @Override
    public final boolean isMonochromeEnabled() {
        return monoChrome;
    }


    protected void onMonoChromeChanged(boolean monoChrome) {

    }

    @Override
    public final void setMonochromeEnabled(boolean monoChrome) {
        if (this.monoChrome != monoChrome) {
            this.monoChrome = monoChrome;
            onMonoChromeChanged(monoChrome);
        }
    }

    /* OBJECT METHODS --------------------------- */

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "displayName=" + displayName() +
                ", dimensions=" + dimensions() +
                ", cellStates=" + cellStateCount() +
                ", parallelComputeAllowed=" + isParallelComputeAllowed() +
                '}';
    }

}
