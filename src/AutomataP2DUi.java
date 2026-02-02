import core.*;
import core.definition.NdArrayFloatI;
import core.definition.automata.AutomataI;
import core.simulator.AutomataSimulator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import util.U;
import util.misc.Log;
import util.models.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * MOUSE CONTROLS <br>
 * 1. {@code L-MOUSE [press|drag]} : Increase cell state <br>
 * 2. {@code SHIFT + L-MOUSE [press|drag]} : Decrease cell state <br>
 * 3. {@code CTRL + [SHIFT] + L-MOUSE [press|drag]} : Enable snap mode (horizontal, vertical, diagonal) <br>
 * 4. {@code R-MOUSE [drag]} : Pan <br>
 * 5. {@code MWHEEL} : Scroll-Y <br>
 * 6. {@code SHIFT + MWHEEL} : Scroll-X <br>
 * 6. {@code CTRL + MWHEEL} : Zoom <br>
 * 
 * <br>
 * For keyboard controls, see {@link #keyPressed(KeyEvent)} and {@link #continuousKeyPressed(KeyEvent)}
 * */
public class AutomataP2DUi extends PApplet implements AutomataSimulator.Listener {

    public static final String TAG = "AutomataUi";

    /**
     * Custom event with a {@link Runnable} task payload to be executed on the UI thread
     *
     * @see #enqueueTask(Runnable)
     * @see #handleKeyEvent(KeyEvent)
     */
    private static final int ACTION_EXECUTE_RUNNABLE = 121230123;

    /**
     * Maximum number of frame draw requests at once
     *
     * @see #invalidateFrame(int)
     * */
    private static final int MAX_FRAME_DRAW_REQUEST = 5;

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

        public final int backgroundDark;
        public final int backgroundMedium;
        public final int backgroundLight;

        public final int foregroundDark;
        public final int foregroundMedium;
        public final int foregroundLight;

        public final float cellStrokeWeight;
        public final float cellStrokeWeightPlaying;

        Theme(boolean isDark,
              int backgroundDark,
              int backgroundMedium,
              int backgroundLight,
              int foregroundDark,
              int foregroundMedium,
              int foregroundLight,
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

    public interface CellDrawTask {

        void initCellDrawStyle(@NotNull PGraphics g, float strokeWeight, int strokeColor);

        void drawCell(@NotNull PGraphics g, float x, float y, float cellSize, float cellState, int cellColor);

    }

    public enum CellDrawer {
        SQUARE(new CellDrawTask() {
            @Override
            public void initCellDrawStyle(@NotNull PGraphics g, float strokeWeight, int strokeColor) {
                if (strokeWeight == 0) {
                    g.noStroke();
                } else {
                    g.stroke(strokeColor);
                    g.strokeWeight(strokeWeight);
                }
            }

            @Override
            public void drawCell(@NotNull PGraphics g, float x, float y, float cellSize, float cellState, int cellColor) {
                g.fill(cellColor);
                g.square(x, y, cellSize);
            }
        }),


        CIRCLE(new CellDrawTask() {
            @Override
            public void initCellDrawStyle(@NotNull PGraphics g, float strokeWeight, int strokeColor) {
                // Don't draw stroke
                g.noStroke();
            }

            @Override
            public void drawCell(@NotNull PGraphics g, float x, float y, float cellSize, float cellState, int cellColor) {
                g.fill(cellColor);
                g.circle(x + cellSize / 2, y + cellSize / 2, cellSize);
            }
        }),
        ;

        @NotNull
        public final CellDrawTask cellDrawTask;

        CellDrawer(@NotNull CellDrawTask cellDrawTask) {
            this.cellDrawTask = cellDrawTask;
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

//    boolean playing = false;
    boolean mDrawCellStroke = true;
    @NotNull
    AutomataP2DUi.CellDrawer mCellDrawer = CellDrawer.SQUARE;

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

//        final core.simulator.AutomataSimulator sim = mSimulator;
//        if (playing && sim != null && frameCount % 2 == 0) {
//            Log.d(TAG, "Creating GEN: " + (sim.getGeneration() + 1));
//            sim.nextGenerationSync(null);
//        }

    }

    protected void onResized(int w, int h) {
        Log.d(TAG, String.format("Resized: %dx=%d", w, h));
        invalidateFrame();
    }

    private void invalidateFrame(int count) {
        mDrawNextFrameReqCount = min(mDrawNextFrameReqCount + count, MAX_FRAME_DRAW_REQUEST);
    }

    private void invalidateFrame() {
        invalidateFrame(1);
    }

    private void postInvalidateFrame() {
        enqueueTask(this::invalidateFrame);
    }


    private void drawFrame() {
        final Theme theme = mTheme;

        background(theme.backgroundDark);

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
            mCellSizePix = calCellSizePix(rows, cols);

            final int draw_rows = min(rows, ceil(height / mCellSizePix));
            final int draw_cols = min(cols, ceil(width / mCellSizePix));
            final int draw_start_row = min(floor(mPanY / mCellSizePix), rows - draw_rows);
            final int draw_start_col = min(floor(mPanX / mCellSizePix), cols - draw_cols);

            final AutomataI automata = sim.getAutomata();
            final PGraphics graphics = getGraphics();
            final CellDrawer cell_drawer = mCellDrawer;

            // Drawing stroke is expensive
            final int strokeColor;
            final float strokeWeight;
            if (mDrawCellStroke) {
                if (sim.isPlaying()) {
                    strokeColor = theme.foregroundLight;
                    strokeWeight = theme.cellStrokeWeightPlaying;
//                    strokeWeight = 0;
                } else {
                    strokeColor = theme.foregroundMedium;
                    strokeWeight = theme.cellStrokeWeight;
                }
            } else {
                strokeColor = 0;
                strokeWeight = 0;   // No Stroke
            }

            mCellDrawer.cellDrawTask.initCellDrawStyle(graphics, strokeWeight, strokeColor);

            float cell_state;
            int cell_color;
            for (int i = draw_start_row; i < draw_start_row + draw_rows; i++) {
                for (int j = draw_start_col; j < draw_start_col + draw_cols; j++) {
                    cell_state = state.get(i, j);
                    cell_color = automata.colorRGBForCell(cell_state, theme.isDark);

                    cell_drawer.cellDrawTask.drawCell(graphics,
                            (j * mCellSizePix) - mPanX,
                            (i * mCellSizePix) - mPanY,
                            mCellSizePix,
                            cell_state,
                            cell_color);
                }
            }
        }
    }


    private static float calCellSizePix(float width, float height, float rows, float cols, float zoom, float defMinCellSizeFactor) {
        final float asp_disp = width / height;
        final float asp_grid = cols / rows;
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


    private int @NotNull [] getCellIndices(float x, float y) {
        return new int[]{(int) ((y + mPanY) / mCellSizePix), (int) ((x + mPanX) / mCellSizePix)};
    }

    private float @NotNull [] getCellIndicesF(float x, float y) {
        return new float[]{((y + mPanY) / mCellSizePix), ((x + mPanX) / mCellSizePix)};
    }

    private float @NotNull [] getCellPosition(float row_index, float col_index) {
        return new float[]{(col_index * mCellSizePix) - mPanX, (row_index * mCellSizePix) - mPanY};
    }

    private boolean stepCellState(int @NotNull[] cellIndices, boolean stepUp, int @Nullable [] prevCellIndices) {
        final AutomataSimulator sim = mSimulator;
        if (sim != null && sim.getState().areIndicesValid(cellIndices) && !Arrays.equals(cellIndices, prevCellIndices)) {
            return sim.stepCellState(cellIndices, stepUp);
        }

        return false;
    }


//    private int @Nullable [] stepCellState(float x, float y, boolean stepUp, int @Nullable [] prevCellIndices) {
//        return stepCellState(getCellIndices(x, y), stepUp, prevCellIndices);
//    }
    
    /* UNDO ------------------------------- */

    private record CellStepRecord(int @NotNull[] cellIndices, boolean stepUp) { }

    // History of cell state changes
    @NotNull
    private final LinkedList<List<CellStepRecord>> mCellStepQueue = new LinkedList<>();

    /**
     * @return numbers of cells whose states have been changed
     * */
    private int undoLastCellSteps() {
        final List<CellStepRecord> last = mCellStepQueue.pollLast();
        if (last == null || last.isEmpty())
            return 0;

        int count = 0;
        for (CellStepRecord rec : last) {
            if (stepCellState(rec.cellIndices(), !rec.stepUp, null)) {
                count++;
            }
        }

        return count;
    }

    
    
    /* KEY Events  --------------------------------------- */

    /**
     * Enqueue a custom task to be executed on the UI thread
     */
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
                AutomataSimulator sim = mSimulator;
                if (sim != null) {
                    sim.togglePlaying();
                }

                invalidateFrame();
            }

            case java.awt.event.KeyEvent.VK_R -> {
                if (event.isControlDown()) {
                    resetZoomAndPan();
                } else {
                    AutomataSimulator sim = mSimulator;
                    if (sim != null) {
                        sim.resetStateAsync();
                        Log.d(TAG, "RESET_STATE");
                    }
                }
            }

            case java.awt.event.KeyEvent.VK_C -> {
                AutomataSimulator sim = mSimulator;
                if (sim != null) {
                    sim.clearStateAsync();
                    Log.d(TAG, "CLEAR_STATE");
                }
            }

            case java.awt.event.KeyEvent.VK_T -> cycleTheme();

            case java.awt.event.KeyEvent.VK_O -> toggleDrawCellStroke();

            case java.awt.event.KeyEvent.VK_S -> cycleCellDrawer();

            // Periodic Boundary Condition
            case java.awt.event.KeyEvent.VK_P -> {
                AutomataSimulator sim = mSimulator;
                if (sim != null) {
                    if (event.isControlDown()) {
                        sim.getWorkSplitter().toggleParallelComputeEnabled();
                    } else {
                        sim.toggleWrapEnabled();
                    }
                }
            }

            case java.awt.event.KeyEvent.VK_M -> {
                AutomataSimulator sim = mSimulator;
                if (sim != null) {
                    sim.getAutomata().toggleMonochromeEnabled();
                    Log.d(TAG, "MONOCHROME: " + sim.getAutomata().isMonochromeEnabled());
                    invalidateFrame();
                }
            }

            case java.awt.event.KeyEvent.VK_Z -> {
                if (event.isControlDown()) {
                    undoLastCellSteps();
                }
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


    /* MOUSE EVENTS  --------------------------------- */

    private int @Nullable [] mLastMouseCellIndices;
    private float mPanX;
    private float mPanY;
    @Nullable
    private MouseEvent mPrevPanEvent;

    private int @Nullable[] mStickAnchorIndices;
    @Nullable
    private MouseEvent mStickAnchorEvent;
    private int mStickDragMode;
    @NotNull
    private final LinkedList<CellStepRecord> mMouseEventCellStepList = new LinkedList<>();
    
    
    @Override
    public void mousePressed(MouseEvent event) {
        super.mousePressed(event);

        final int button = event.getButton();
        mMouseEventCellStepList.clear();

        if (button == LEFT) {
            final boolean stepUp = !event.isShiftDown();
            final int[] cellIndices = getCellIndices(event.getX(), event.getY());
            final boolean changed = stepCellState(cellIndices, stepUp, null);
            mLastMouseCellIndices = cellIndices;

            mStickDragMode = 0;
            if (event.isControlDown()) {
                mStickAnchorEvent = event;
                mStickAnchorIndices = cellIndices;
            } else {
                mStickAnchorEvent = null;
                mStickAnchorIndices = null;
            }

            if (changed) {
                mMouseEventCellStepList.add(new CellStepRecord(cellIndices, stepUp));
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        super.mouseDragged(event);

        final int button = event.getButton();

        if (button == RIGHT) {
            final MouseEvent prevEvent = mPrevPanEvent;
            if (prevEvent != null) {
                handlePan(prevEvent.getX() - event.getX(), prevEvent.getY() - event.getY());
            }

            mPrevPanEvent = event;
            return;
        }

        if (button == LEFT) {
            final MouseEvent anchorEvent = mStickAnchorEvent;
            final int[] anchorIndices = mStickAnchorIndices;
            final int[] cellIndices = getCellIndices(event.getX(), event.getY());

            if (event.isControlDown()) {
                if (anchorEvent != null && anchorIndices != null) {
                    if (!Arrays.equals(cellIndices, anchorIndices)) {
                        if (mStickDragMode == 0) {
                            final double angle = Math.atan2(Math.abs(event.getY() - anchorEvent.getY()), Math.abs(event.getX() - anchorEvent.getX()));
                            final double axisAngStick = Math.PI / 18f;
                            if (angle <= axisAngStick) {
                                mStickDragMode = 1;     // stick X
                            } else if (angle >= (Math.PI / 2) - axisAngStick) {
                                mStickDragMode = 2;     // stick Y
                            } else {
                                mStickDragMode = 3;     // Stick Diagonal
                            }
                        }

                        if (mStickDragMode == 1) {
                            cellIndices[0] = anchorIndices[0];      // stick X (match y: rows)
                        } else if (mStickDragMode == 2) {
                            cellIndices[1] = anchorIndices[1];      // stick Y (match x: cols)
                        } else {
                            int dx = cellIndices[0] - anchorIndices[0];
                            int dy = cellIndices[1] - anchorIndices[1];

                            final int del = Math.min(Math.abs(dx), Math.abs(dy));
                            cellIndices[0] = anchorIndices[0] + (U.signum(dx) * del);
                            cellIndices[1] = anchorIndices[1] + (U.signum(dy) * del);
                        }
                    }

                } else {
                    mStickAnchorEvent = event;
                    mStickAnchorIndices = cellIndices;
                    mStickDragMode = 0;
                }
            } else {
                mStickAnchorEvent = null;
                mStickAnchorIndices = null;
                mStickDragMode = 0;
            }

            final boolean stepUp = !event.isShiftDown();
            final boolean changed = stepCellState(cellIndices, stepUp, mLastMouseCellIndices);
            mLastMouseCellIndices = cellIndices;

            if (changed) {
                mMouseEventCellStepList.add(new CellStepRecord(cellIndices, stepUp));
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        super.mouseReleased(event);

        if (!mMouseEventCellStepList.isEmpty()) {
            mCellStepQueue.addLast(new LinkedList<>(mMouseEventCellStepList));
        }

        // Invalidate
        mLastMouseCellIndices = null;
        mPrevPanEvent = null;
        mStickAnchorEvent = null;
        mStickAnchorIndices = null;
        mStickDragMode = 0;
        mMouseEventCellStepList.clear();
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
    }


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
            Log.d(TAG, "PAN_X: " + mPanX + ", PAN_Y: " + mPanY);
            invalidateFrame();
            return true;
        }

        return false;
    }


    private void stepZoom(boolean zoomIn) {
        if (mSimulator == null)
            return;

        final float prevZoom = mZoom;
        float newZoom = prevZoom * (1 + ((zoomIn ? 1 : -1) * 0.05f));
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

    private void resetZoomAndPan() {
        mPanX = mPanY = 0;
        mZoom = 1.0f;
        invalidateFrame();
    }


    /* Getters and Setters ------------------------------------------------------- */

    @NotNull
    public Theme getTheme() {
        return mTheme;
    }

    public void setTheme(@NotNull Theme theme) {
        if (mTheme == theme) {
            return;
        }

        final Theme old = mTheme;
        mTheme = theme;
        onThemeChanged(old, theme);
    }

    public void cycleTheme() {
        setTheme(U.cycleEnum(Theme.class, mTheme));
    }

    protected void onThemeChanged(@NotNull Theme old, @NotNull Theme _new) {
        Log.d(TAG, "THEME: " + _new);
        postInvalidateFrame();
    }


    @NotNull
    public CellDrawer getCellDrawer() {
        return mCellDrawer;
    }

    public void setCellDrawer(@NotNull CellDrawer cellDrawer) {
        if (mCellDrawer == cellDrawer) {
            return;
        }

        final CellDrawer old = mCellDrawer;
        mCellDrawer = cellDrawer;
        onCellDrawerChanged(old, cellDrawer);
    }

    public void cycleCellDrawer() {
        setCellDrawer(U.cycleEnum(CellDrawer.class, mCellDrawer));
    }

    protected void onCellDrawerChanged(@NotNull CellDrawer old, @NotNull CellDrawer _new) {
        Log.d(TAG, "CELL_SHAPE: " + _new);
        postInvalidateFrame();
    }


    public boolean isDrawCellStrokeEnabled() {
        return mDrawCellStroke;
    }

    public void setDrawCellStroke(boolean drawCellStroke) {
        if (mDrawCellStroke == drawCellStroke) {
            return;
        }

        mDrawCellStroke = drawCellStroke;
        onDrawCellStrokeChanged(drawCellStroke);
    }

    public void toggleDrawCellStroke() {
        setDrawCellStroke(!isDrawCellStrokeEnabled());
    }

    protected void onDrawCellStrokeChanged(boolean drawCellStroke) {
        Log.d(TAG, "CELL_STROKE: " + drawCellStroke);
        postInvalidateFrame();
    }
    
    
    /* SIMULATOR  ------------------------------------------------- */
    
    public @Nullable AutomataSimulator getSimulator() {
        return mSimulator;
    }

    public synchronized void setSimulator(@Nullable AutomataSimulator simulator) {
        final AutomataSimulator old = mSimulator;
        if (old == simulator)
            return;

        // Detach
        if (old != null) {
            old.removeListener(this);
        }

        mSimulator = simulator;
        // Attach
        if (simulator != null) {
            simulator.ensureListener(this);
        }

        onSimulatorChanged(old, simulator);
    }

    protected void onSimulatorChanged(@Nullable AutomataSimulator old, @Nullable AutomataSimulator _new) {
        enqueueTask(() -> {
            mCellStepQueue.clear();
            resetZoomAndPan();
//            invalidateFrame();
        });
    }

    @Override
    public void onAutomataChanged(@NotNull AutomataSimulator simulator, @NotNull AutomataI oldAutomata, @NotNull AutomataI newAutomata) {
        Log.d(TAG, "AUTOMATA CHANGED: " + oldAutomata + " -> " + newAutomata);

        enqueueTask(() -> {
            mCellStepQueue.clear();
            invalidateFrame();
        });
    }

    @Override
    public void onSimulationFrameRateChanged(@NotNull AutomataSimulator simulator, long oldFrameRate, long newFrameRate) {
        Log.d(TAG, "SIM_FRAME_RATE: " + newFrameRate + " fps");
    }

    @Override
    public void onSimulationRunModeChanged(@NotNull AutomataSimulator simulator, AutomataSimulator.@NotNull RunMode oldRunMode, AutomataSimulator.@NotNull RunMode newRunMode) {
        Log.d(TAG, "SIM_RUN_MODE: " + newRunMode);
    }

    @Override
    public void onIsPlayingChanged(@NotNull AutomataSimulator simulator, boolean isPlaying) {
        Log.d(TAG, "SIM_PLAYING: " + isPlaying);
        
        enqueueTask(() -> {
            mCellStepQueue.clear();
            invalidateFrame();
        });
    }

    @Override
    public void onGenerationStepsChanged(@NotNull AutomataSimulator simulator, int prevGenSteps, int newGenSteps) {
        Log.d(TAG, "GENERATION_STEPS: " + newGenSteps);
    }

    @Override
    public void onWrapEnabledChanged(@NotNull AutomataSimulator simulator, boolean wrapEnabled) {
        Log.d(TAG, "PERIODIC_BC: " + wrapEnabled);
//        postInvalidateFrame();
    }

    @Override
    public void onAutomataStateChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldState, @NotNull NdArrayFloatI newState, int generation, int stepInGeneration) {
        Log.d(TAG, "STATE_CHANGED: gen=" + generation + ", step=" + stepInGeneration);
        enqueueTask(() -> {
//            mCellStepQueue.clear();
            invalidateFrame();
        });
    }

    @Override
    public void onAutomataGenerationChanged(AutomataSimulator simulator, @Nullable NdArrayFloatI oldGen, @NotNull NdArrayFloatI newGen, int generation, int steps) {
        Log.d(TAG, "GEN_CHANGED: " + generation);
        postInvalidateFrame();
    }

    @Override
    public void onAutomataCellStateChanged(AutomataSimulator simulator, @NotNull NdArrayFloatI state, int[] cellIndices) {
        Log.d(TAG, "CELL_STATE_CHANGED: " + Arrays.toString(cellIndices));
        postInvalidateFrame();
    }

    @Override
    public void onSimulatorThreadCountChanged(@NotNull AutomataSimulator simulator) {
        Log.d(TAG, "THREAD_COUNT: %d (core), %d (max), %d (workers)".formatted(simulator.getCoreThreadCount(), simulator.getMaxThreadCount(), simulator.getWorkerThreadCount()));
    }

    @Override
    public void onParallelComputeEnabledChanged(@NotNull AutomataSimulator simulator, boolean parallelComputeEnabled) {
        Log.d(TAG, "PARALLEL_COMPUTE_READY: %b [Threads: %d (core), %d (max), %d (workers)]".formatted(simulator.isParallelComputeReady(), simulator.getCoreThreadCount(), simulator.getMaxThreadCount(), simulator.getWorkerThreadCount()));
    }




    // ------------------------------------------------

    public static void main(String[] args) {
        Log.init();
        Log.setDebug(true);

        final int[] state_shape = {300, 460};       // TODO: able to change in UI using simulator.setSTateShape()
//        final AutomataI automata = new ZhabotinskyAutomata();
        final AutomataI automata = new LifeAutomata(LifeAutomata.Rule.CONWAY_LIFE);
//        final AutomataI automata = new BrianBrainAutomata();
//        final AutomataI automata = new NLifeAutomata();

        final AutomataSimulator simulator = new AutomataSimulator(automata, state_shape, true);
        simulator.setSimulationFrameRate(10);
        simulator.setSimulationRunMode(AutomataSimulator.RunMode.SCHEDULE_FIXED_RATE);
//        simulator.setGenerationSteps(2);

        final AutomataP2DUi app = new AutomataP2DUi(simulator);
        PApplet.runSketch(concat(new String[]{app.getClass().getName()}, args), app);
    }
}
