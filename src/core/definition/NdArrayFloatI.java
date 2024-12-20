package core.definition;

/**
 * Immutable interface of an N-Dimensional float array
 * <br>
 * NOTE: ONLY GETTER METHODS
 * */
public interface NdArrayFloatI {

    @FunctionalInterface
    interface FloatGenerator {

        float nextFloat();

    }

    int dimensions();

    int shapeAt(int dimensionIndex);

    int[] shape();

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    boolean areIndicesValid(int... indices);

    float get(int... indices);

    float getAverage();

}
