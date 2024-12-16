package core;

public interface NdArrayFloatI {

    @FunctionalInterface
    interface FloatGenerator {

        float nextFloat();

    }

    int dimensions();

    int shapeAt(int dimensionIndex);

    int[] shape();

    int size();

    boolean areIndicesValid(int... indices);

    float get(int... indices);

}
