package core;

public interface NdArrayFloatI {

    int dimensions();

    int shapeAt(int dimensionIndex);

    int[] shape();

    int size();

    boolean areIndicesValid(int... indices);

    float get(int... indices);

}
