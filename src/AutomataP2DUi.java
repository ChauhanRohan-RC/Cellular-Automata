import core.AutomataI;
import core.ConwayLifeAutomata;
import core.NdArrayFloatI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import util.U;
import util.misc.Log;
import util.models.Pair;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.SimpleTimeZone;

public class AutomataP2DUi extends PApplet implements AutomataSimulator.Listener {


    public static final String TAG = "AutomataUi";

    /**
     * Custom event with a {@link Runnable} task payload to be executed on the UI thread
     *
     * @see #enqueueTask(Runnable)
     * @see #handleKeyEvent(KeyEvent)
     * */
    private static final int ACTION_EXECUTE_RUNNABLE = 121230123;


    public enum Theme {

        LIGHT(false,
                U.gray255(255),
                U.gray255(200),
                U.gray255(180),
                U.gray255(0),
                U.gray255(45),
                U.gray255(90),
                1,
                0.5f
        ),

        DARK(true,
                U.gray255(0),
                U.gray255(25),
                U.gray255(60),
                U.gray255(255),
                U.gray255(180),
                U.gray255(160),
                1,
                0.5f
        ),

        ;

        public final boolean isDark;

        public final Color backgroundDark;
        public final Color backgroundMedium;
        public final Color backgroundLight;

        public final Color foregroundDark;
        public final Color foregroundMedium;
        public final Color foregroundLight;

        public final float cellStrokeWeight;
        public final float cellStrokeWeightPlaying;

        Theme(boolean isDark,
              Color backgroundDark,
              Color backgroundMedium,
              Color backgroundLight,
              Color foregroundDark,
              Color foregroundMedium,
              Color foregroundLight,
              float cellStrokeWeight,
              float cellStrokeWeightPlaying) {

            this.isDark = isDark;
            this.backgroundDark = backgroundDark;
            this.backgroundMedium = backgroundMedium;
            this.backgroundLight = backgroundLight;
            this.foregroundDark = foregroundDark;
            this.foregroundMedium = foregroundMedium;
            this.foregroundLight = foregroundLight;
            this.cellStrokeWeight = cellStrokeWeight;
            this.cellStrokeWeightPlaying = cellStrokeWeightPlaying;
        }

    }


    /* Ui */
    private int _w, _h;
    @Nullable
    private KeyEvent mKeyEvent;
    private int mDrawNextFrameReqCount;

    @NotNull
    private Theme mTheme = Theme.LIGHT;


    @Nullable
    private AutomataSimulator mSimulator;

    boolean playing = false;
    float mCellSizePix = 20;
    float mZoom = 1;

    public AutomataP2DUi(@Nullable AutomataSimulator simulator) {
        setSimulator(simulator);
    }

    public AutomataP2DUi() {
        this(null);
    }


    @Override
    public void settings() {
        size(1600, 800, P2D);
    }

    @Override
    public void setup() {
        frameRate(60);

        // Surface
        surface.setTitle("Cellular Automata");
        surface.setResizable(true);

        invalidateFrame(2);
    }

    @Override
    public void draw() {
        preDraw();

        if (mDrawNextFrameReqCount > 0) {
            Log.d(TAG, "Drawing FRAME");
            drawFrame();
            mDrawNextFrameReqCount--;
        }

        postDraw();
    }

    public void preDraw() {
        if (_w != width || _h != height) {
            _w = width;
            _h = height;
            onResized(width, height);
        }

        /* Handle Keys [Continuous] */
        if (keyPressed && mKeyEvent != null) {
            continuousKeyPressed(mKeyEvent);
        }
    }

    public void postDraw() {
        // TODO: move it to a different thread for better control of timings
        final AutomataSimulator sim = mSimulator;
        if (playing && sim != null && frameCount % 2 == 0) {
            Log.d(TAG, "Creating GEN: " + (sim.getGeneration() + 1));
            sim.nextGeneration();
        }

    }

    protected void onResized(int w, int h) {
        Log.d(TAG, String.format("Resized: %dx=%d", w, h));
        invalidateFrame();
    }


    private void invalidateFrame(int count) {
        mDrawNextFrameReqCount = min(mDrawNextFrameReqCount + count, 5 /* MAX_DRAW_REQUESTS */);
    }

    private void invalidateFrame() {
        invalidateFrame(1);
    }


    private void drawFrame() {
        final Theme theme = mTheme;

        background(theme.backgroundDark.getRGB());

        final AutomataSimulator sim = mSimulator;
        NdArrayFloatI state = null;
        if (sim != null) {
            state = sim.getState();
        }

        if (state == null) {
            // NO state
            Log.w(TAG, "No State to draw!");
        } else if (state.dimensions() != 2) {
            Log.w(TAG, String.format("Could not draw state %d with dimensions!", state.dimensions()));
        } else {

            final int rows = state.shapeAt(0), cols = state.shapeAt(1);

//            mCellSizePix = (float) min(width, height) / min(state.shapeAt(0), state.shapeAt(1));
//            cellSize = max(cellSize, min(width, height) / 60f);
            mCellSizePix = calCellSizePix(rows, cols);

            // TODO: draw only those cells that appear on screen i.e. within width and height

            AutomataI automata = sim.getAutomata();
            final int draw_rows = min(rows, ceil(height / mCellSizePix));
            final int draw_cols = min(cols, ceil(width / mCellSizePix));
            final int draw_start_row = min(floor(mPanY / mCellSizePix), rows - draw_rows);
            final int draw_start_col = min(floor(mPanX / mCellSizePix), cols - draw_cols);

            if (playing) {
//                        noStroke();
                stroke(theme.foregroundLight.getRGB());
                strokeWeight(theme.cellStrokeWeightPlaying);
            } else {
                stroke(theme.foregroundMedium.getRGB());
                strokeWeight(theme.cellStrokeWeight);
            }

            for (int i = draw_start_row; i < draw_start_row + draw_rows; i++) {
                for (int j = draw_start_col; j < draw_start_col + draw_cols; j++) {

                    fill(automata.colorFor(state.get(i, j), theme.isDark).getRGB());
                    square((j * mCellSizePix) - mPanX, (i * mCellSizePix) - mPanY, mCellSizePix);
                }
            }
        }
    }




    private static float calCellSizePix(float width, float height, float rows, float cols, float zoom, float defMinCellSizeFactor) {
        final float asp_disp = (float) width / height;
        final float asp_grid = (float) cols / rows;
        float cellSize;

        if (asp_disp >= asp_grid) {
            // FIT width
            cellSize = width / cols;
        } else {
            // Fit height
            cellSize = height / rows;
        }

        return max(cellSize, min(width, height) * defMinCellSizeFactor) * zoom;
    }

    private float calCellSizePix(float rows, float cols) {
        return calCellSizePix(width, height, rows, cols, mZoom, 1 / 60f);    // TODO: def_min_cell_size_factor
    }

    private void updateCellSize(float rows, float cols) {
        mCellSizePix = calCellSizePix(rows, cols);
    }

    private void updateCellSize() {
        final AutomataSimulator sim = mSimulator;
        if (sim != null) {
            updateCellSize(sim.getState().shapeAt(0), sim.getState().shapeAt(1));
        }
    }

    @NotNull
    private Pair.Float calMaxPanXY(float rows, float cols) {
        float cellSize = calCellSizePix(rows, cols);

        final float draw_rows = min(rows, height / cellSize);
        final float draw_cols = min(cols, width / cellSize);

        return new Pair.Float((cols - draw_cols) * mCellSizePix, (rows - draw_rows) * mCellSizePix);
    }


    private int @NotNull[] getCellIndices(float x, float y) {
        return new int[] {(int) ((y + mPanY) / mCellSizePix), (int) ((x + mPanX) / mCellSizePix)};
    }

    private float @NotNull[] getCellIndicesF(float x, float y) {
        return new float[] {((y + mPanY) / mCellSizePix), ((x + mPanX) / mCellSizePix)};
    }

    private float @NotNull[] getCellPosition(float row_index, float col_index) {
        return new float[] {(col_index * mCellSizePix) - mPanX, (row_index * mCellSizePix) - mPanY};
    }


    private int @Nullable[] stepCellState(float x, float y, boolean stepUp, int @Nullable[] prevCellIndices) {
        final AutomataSimulator sim = mSimulator;
        if (sim != null) {
            final int[] indices = getCellIndices(x, y);

            if (sim.getState().areIndicesValid(indices)) {
                invalidateFrame();
                if (!Arrays.equals(indices, prevCellIndices)) {
                    boolean changed = sim.stepCellState(indices, stepUp);
                }

                return indices;
            }
        }

        return null;
    }

    private void resetZoomAndPan() {
        mPanX = mPanY = 0;
        mZoom = 1.0f;
        invalidateFrame();
    }

    /* Events and Bindings */

    /**
     * Enqueue a custom task to be executed on the UI thread
     * */
    public final void enqueueTask(@NotNull Runnable task) {
        postEvent(new KeyEvent(task, millis(), ACTION_EXECUTE_RUNNABLE, 0, (char) 0, 0, false));
    }

    public final void enqueueTasks(@Nullable Collection<? extends Runnable> tasks) {
        final Runnable chain = U.chainRunnables(tasks);     // Merge tasks to a single task

        if (chain != null) {
            enqueueTask(chain);
        }
    }

    @Override
    protected void handleKeyEvent(KeyEvent event) {
        // If this is an ESC key down event, mask it to be able to handle it myself
//        if (event.getAction() == KeyEvent.PRESS && (event.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE)) {
//            event = Control.changeKeyCode(event, Control.ESCAPE_KEY_CODE_SUBSTITUTE, Control.ESCAPE_KEY_SUBSTITUTE);
//        }

        super.handleKeyEvent(event);

        // Handle Custom Events
        if (event.getAction() == ACTION_EXECUTE_RUNNABLE && (event.getNative() instanceof Runnable task)) {
            task.run();
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        super.keyPressed(event);
        mKeyEvent = event;

        switch (event.getKeyCode()) {
            case java.awt.event.KeyEvent.VK_SPACE -> {
                playing = !playing;
                Log.d(TAG, "Playing: " + playing);
                invalidateFrame();
            }

            case java.awt.event.KeyEvent.VK_R -> {
                if (event.isControlDown()) {
                    resetZoomAndPan();
                } else {
                    AutomataSimulator sim = mSimulator;
                    if (sim != null) {
                        Log.d(TAG, "RESET_STATE");
                        playing = false;
                        Log.d(TAG, "Playing: " + playing);
                        sim.resetState();
                    }
                }
            }

            case java.awt.event.KeyEvent.VK_C -> {
                AutomataSimulator sim = mSimulator;
                if (sim != null) {
                    Log.d(TAG, "CLEAR_STATE");
                    sim.clearState();
                }
            }

            case java.awt.event.KeyEvent.VK_T -> {
                // Switch theme
                mTheme = U.cycleEnum(Theme.class, mTheme);
                invalidateFrame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        super.keyReleased(event);
        if (mKeyEvent != null && mKeyEvent.getKeyCode() == event.getKeyCode()) {
            mKeyEvent = null;
        }
    }

    public void continuousKeyPressed(@Nullable KeyEvent event) {
        if (event == null)
            return;

        // Handle Continuous Controls
    }



    private int @Nullable[] mLastMouseCellIndices;
    private float mPanX;
    private float mPanY;
    @Nullable
    private MouseEvent mPrevPanEvent;

    private boolean handlePan(float delPanX, float delPanY) {
        final AutomataSimulator sim = mSimulator;
        if (sim == null) {
            mPanX = mPanY = 0;
            return false;
        }

        final float prevPanX = mPanX;
        final float prevPanY = mPanY;
        final Pair.Float maxPanXY = calMaxPanXY(sim.getState().shapeAt(0), sim.getState().shapeAt(1));

        mPanX = constrain(mPanX + delPanX, 0, maxPanXY.first);
        mPanY = constrain(mPanY + delPanY, 0, maxPanXY.second);

        if (prevPanX != mPanX || prevPanY != mPanY) {
            Log.d(TAG,"PAN_X: " + mPanX + ", PAN_Y: " + mPanY);
            invalidateFrame();
            return true;
        }

        return false;
    }


    private void stepZoom(boolean zoomIn) {
        if (mSimulator == null)
            return;

        final float prevZoom = mZoom;
        float newZoom = prevZoom * (1 + ((zoomIn? 1:-1) * 0.05f));
        newZoom = constrain(newZoom, 0.01f, 50);

        if (newZoom != prevZoom) {
            final float[] cell = getCellIndicesF(mouseX, mouseY);
            mZoom = newZoom;
            updateCellSize();
            final float[] new_pos = getCellPosition(cell[0], cell[1]);

            Log.d(TAG, String.format("Zoom: %.2f", newZoom));

            boolean handled = handlePan(-(mouseX - new_pos[0]), -(mouseY - new_pos[1]));
            if (!handled) {
                invalidateFrame();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        super.mousePressed(event);

        final int button = event.getButton();

        if (event.isControlDown() && button == LEFT) {
            // TODO: PAN start
            return;
        }

        if (button == LEFT) {
            mLastMouseCellIndices = stepCellState(event.getX(), event.getY(), !event.isShiftDown(), null);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        super.mouseDragged(event);

        final int button = event.getButton();

        if (event.isControlDown() && button == LEFT) {
            final MouseEvent prevEvent = mPrevPanEvent;
            if (prevEvent != null) {
                handlePan(prevEvent.getX() - event.getX(), prevEvent.getY() - event.getY());
            }

            mPrevPanEvent = event;
            return;
        }

        if (button == LEFT) {
            mLastMouseCellIndices = stepCellState(event.getX(), event.getY(), !event.isShiftDown(), mLastMouseCellIndices);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        super.mouseReleased(event);

        // Invalidate
        mLastMouseCellIndices = null;
        mPrevPanEvent = null;
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        super.mouseWheel(event);

        if (event.isControlDown() && mSimulator != null) {
            stepZoom(event.getCount() < 0);
        } else if (event.isShiftDown()) {
            // PAN: X
            handlePan(event.getCount() * mCellSizePix, 0);
        } else {
            // PAN: Y
            handlePan(0, event.getCount() * mCellSizePix);
        }


//        if (event.getCount() > 0) {
//            // Scroll Down
//            Log.d(TAG, "MOUSE_SCROLL_DOWN: " + event.getCount());
//            mZoom -= 0.01f;
//        } else {
//            // Scroll Up
//            Log.d(TAG, "MOUSE_SCROLL_UP: " + event.getCount());
//            mZoom += 0.01f;
//        }
    }

    public @Nullable AutomataSimulator getSimulator() {
        return mSimulator;
    }

    public synchronized void setSimulator(@Nullable AutomataSimulator simulator) {
        final AutomataSimulator old = mSimulator;
        // Detach
        if (old != null) {
            old.removeListener(this);
        }

        mSimulator = simulator;
        // Attach
        if (simulator != null) {
            simulator.ensureListener(this);
        }

        if (simulator != old) {
            onSimulatorChanged(old, simulator);
        }

        invalidateFrame();
    }

    protected void onSimulatorChanged(@Nullable AutomataSimulator old, @Nullable AutomataSimulator _new) {
        resetZoomAndPan();
    }


    @Override
    public void onAutomataStateChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldState, @NotNull NdArrayFloatI newState, int generation, int stepInGeneration) {
        Log.d(TAG, "STATE_CHANGED: gen=" + generation + ", step=" + stepInGeneration);
        invalidateFrame();
    }

    @Override
    public void onAutomataGenerationChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldGen, @NotNull NdArrayFloatI newGen, int generation, int steps) {
        Log.d(TAG, "GEN_CHANGED: " + generation);
        invalidateFrame();
    }

    @Override
    public void onAutomataCellStateChanged(AutomataSimulator simulator, @NotNull NdArrayFloatI state, int[] cellIndices) {
        Log.d(TAG, "CELL_STATE_CHANGED: " + Arrays.toString(cellIndices));
        invalidateFrame();
    }



    // ------------------------------------------------

    public static void main(String[] args) {
        Log.init();
        Log.setDebug(true);

        AutomataSimulator simulator = new AutomataSimulator(new ConwayLifeAutomata(), new int[] {200, 300});
//        simulator.setGenerationSteps(2);

        final AutomataP2DUi app = new AutomataP2DUi(simulator);
        PApplet.runSketch(concat(new String[]{app.getClass().getName()}, args), app);

//        if (simulator != null) {
//            simulator.nextGeneration();
//            Log.d(TAG, "Next Gen START");
//            long st = System.currentTimeMillis();
//            simulator.nextGeneration();
//            long et = System.currentTimeMillis();
//            Log.d(TAG, "Next Gen END: " + (et - st) + " ms");
//        }

    }
}