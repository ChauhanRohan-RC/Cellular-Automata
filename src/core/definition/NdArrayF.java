package core.definition;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class NdArrayF implements NdArrayFloatI {

    public static int product(int @NotNull [] array) {
        int res = 1;

        for (int i: array) {
            res *= i;
        }

        return res;
    }

    public static void checkIndicesThrow(int[] shape, int[] indices) throws IllegalArgumentException, IndexOutOfBoundsException {
        if (shape == null || indices == null || indices.length != shape.length) {
            throw new IllegalArgumentException("Shape and indices must have the same length. Shape: " + Arrays.toString(shape) + " | Indices: " + Arrays.toString(indices));
        }

        for (int i = 0; i < indices.length; i++) {
            final int idx = indices[i];
            if (idx < 0 || idx >= shape[i]) {
                throw new IndexOutOfBoundsException(idx + " is out of bounds for dimension with size " + shape[i]);
            }
        }
    }

    // Output array must be an 8x2 array i.e. int[8][2]
    public static int getNeighbourIndices2D(final int rows, final int cols,
                                             final int pos_row, final int pos_col,
                                             final boolean wrapIndices,
                                             final int @NotNull[][] outputArray) {
//        if (shape == null || shape.length != 2) {
//            throw new IllegalArgumentException("Shape must be 2D, given: " + Arrays.toString(shape));
//        }
//
//        if (outputArray == null) {
//            outputArray = new int[8][2];
//        } else if (outputArray.length != 8) {
//            throw new IllegalArgumentException("Array must be 8 elements, given: " + outputArray.length);
//        }

        int idx = 0;

        for (int i = -1; i < 2 ; i++) {
            for (int j = -1; j < 2 ; j++) {

                if (i == 0 && j == 0)
                    continue;

                int r = pos_row + i;
                int c = pos_col + j;

                if (r < 0 || c < 0 || r >= rows || c >= cols) {
                    if (wrapIndices) {
                        r = (r + rows) % rows;
                        c = (c + cols) % cols;
                    } else {
                        continue;
                    }
                }

                outputArray[idx][0] = r;
                outputArray[idx][1] = c;
                idx++;
            }
        }

        return idx;

//        return outputArray;
    }



    private final float[] flatArray;

    private int[] shape;     // dimensions of the array
    private int[] multipliers;    // used to calculate the index in the internal array

    public NdArrayF(int... shape) {
        if (shape == null || shape.length == 0) {
            throw new IllegalArgumentException("Shape cannot be null or empty!");
        }

        int dim_multiplier = 1;

        this.multipliers = new int[shape.length];
        for (int i = shape.length - 1; i >= 0; i--) {
            final int dim = shape[i];
            if (dim <= 0) {
                throw new IllegalArgumentException("Dimension must be > 0! Given: " + Arrays.toString(shape));
            }

            this.multipliers[i] = dim_multiplier;
            dim_multiplier *= dim;
        }

        this.flatArray = new float[dim_multiplier];
        this.shape = shape;
    }

    public void reshape(int... shape) {
        if (shape == null || shape.length == 0) {
            throw new IllegalArgumentException("Shape cannot be null or empty!");
        }

        if (product(shape) != product(this.shape)) {
            throw new IllegalArgumentException("Dimensions don't match! Current shape: " + Arrays.toString(this.shape) + " , Requested shape: " + Arrays.toString(shape));
        }

        int dim_multiplier = 1;
        this.multipliers = new int[shape.length];
        for (int i = shape.length - 1; i >= 0; i--) {
            this.multipliers[i] = dim_multiplier;
            dim_multiplier *= shape[i];
        }

        this.shape = shape;
    }

    @Override
    public int dimensions() {
        return this.shape.length;
    }

    @Override
    public int shapeAt(int dimensionIndex) {
        return this.shape[dimensionIndex];
    }

    @Override
    public int[] shape() {
        return Arrays.copyOf(this.shape, this.shape.length);
    }

    @Override
    public int size() {
        return product(this.shape);
    }

    public boolean isSameShape(@NotNull NdArrayF other) {
        return Arrays.equals(this.shape, other.shape);
    }


    public void checkIndicesThrow(int... indices) throws IllegalArgumentException, IndexOutOfBoundsException {
        checkIndicesThrow(shape, indices);
    }

    @Override
    public boolean areIndicesValid(int... indices) {
        try {
            checkIndicesThrow(indices);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
            return false;
        }

        return true;
    }

    private int flattenIndex(int... indices) {
        if (indices == null || indices.length != shape.length) {
            throw new IllegalArgumentException("Shape and indices must have the same length. Shape: " + Arrays.toString(shape) + " | Indices: " + Arrays.toString(indices));
        }

        int internalIndex = 0;

        for (int i = 0; i < indices.length; i++) {
            final int idx = indices[i];
            if (idx < 0 || idx >= shape[i]) {
                throw new IndexOutOfBoundsException(idx + " is out of bounds for dimension with size " + shape[i]);
            }

            internalIndex += idx * multipliers[i];
        }

        return internalIndex;
    }

    @Override
    public float get(int... indices) {
        return flatArray[flattenIndex(indices)];
    }

    public void set(float value, int... indices) {
        flatArray[flattenIndex(indices)] = value;
    }

    public void fill(float value) {
        Arrays.fill(flatArray, 0, size(), value);
    }

    public void clear() {
        fill(0f);
    }

    public void fill(@NotNull FloatGenerator generator) {
        for (int i = 0; i < size(); i++) {
            flatArray[i] = generator.nextFloat();
        }
    }

    public void fillRandFloat(final float lowInclusive, final float highExclusive) {
        final Random rand = new Random();
        fill(() -> rand.nextFloat(lowInclusive, highExclusive));
    }

    public void fillRandInt(final int lowInclusive, final int highExclusive) {
        final Random rand = new Random();
        fill(() -> rand.nextInt(lowInclusive, highExclusive));
    }

    @Override
    public float getAverage() {
        final float size = size();
        float avg = 0;

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                avg += flatArray[i];
            }

            avg /= size;
        }

        return avg;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NdArrayF ndArrayF = (NdArrayF) o;
        return Objects.deepEquals(flatArray, ndArrayF.flatArray) && Objects.deepEquals(shape, ndArrayF.shape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(flatArray), Arrays.hashCode(shape));
    }

    @Override
    public String toString() {
        return "NdArrayF{" +
                "shape=" + Arrays.toString(shape) +
                ", flat_array=" + Arrays.toString(flatArray) +
                '}';
    }

    public static void main(String[] args) {
        // TEST
        NdArrayF a = new NdArrayF(4, 3);
        a.set(5, 1, 2);
        a.reshape(12);

        System.out.println(a.get());
    }

}
