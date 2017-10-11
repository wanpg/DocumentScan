package com.egeio.opencv.tools;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by wangjinpeng on 2017/10/10.
 */

public class Number {

    /**
     * 向量外积
     *
     * @param arr1
     * @param arr2
     * @return
     */
    public static double[] cross(double[] arr1, double[] arr2) {
        if (arr1 == null || arr2 == null) {
            return null;
        }
        if (arr1.length != arr2.length) {
            return null;
        }

        if (arr1.length == 2) {
            return new double[]{arr1[0] * arr2[1] - arr1[1] * arr2[0]};
        }

        if (arr1.length == 3) {
            return new double[]{
                    arr1[1] * arr2[2] - arr1[2] * arr2[1],
                    arr1[2] * arr2[0] - arr1[0] * arr2[2],
                    arr1[0] * arr2[1] - arr1[1] * arr2[0]
            };
        }
        return null;
    }

    /**
     * 向量点积
     *
     * @param arr1
     * @param arr2
     * @return
     */
    public static double dot(double[] arr1, double[] arr2) {
        if (arr1 == null || arr2 == null) {
            return 0;
        }
        if (arr1.length != arr2.length) {
            return 0;
        }
        RealVector realVector1 = new ArrayRealVector(arr1);
        RealVector realVector2 = new ArrayRealVector(arr2);
        return realVector1.dotProduct(realVector2);
    }

    public static double[] dot(double[] arr1, double[][] arr2) {
        if (arr1 == null || arr2 == null) {
            return null;
        }
        if (arr1.length != arr2.length) {
            return null;
        }
        RealVector realVector = new ArrayRealVector(arr1);
        RealMatrix matrix = new Array2DRowRealMatrix(arr2);
        double[] result = new double[arr2.length];
        for (int i = 0; i < arr2.length; i++) {
            result[i] = realVector.dotProduct(matrix.getRowVector(i));
        }
        return result;
    }

    /**
     * 数组乘法
     *
     * @param arr
     * @param k
     * @return
     */
    public static double[] multiply(double[] arr, double k) {
        if (arr == null) {
            return null;
        }
        double[] resultArr = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            resultArr[i] = arr[i] * k;
        }
        return resultArr;
    }

    /**
     * 数组相减
     *
     * @param arr1
     * @param arr2
     * @return
     */
    public static double[] minus(double[] arr1, double[] arr2) {
        if (arr1 == null || arr2 == null) {
            return null;
        }
        if (arr1.length != arr2.length) {
            return null;
        }
        double[] resultArr = new double[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            resultArr[i] = arr1[i] - arr2[i];
        }
        return resultArr;
    }

    /**
     * 矩阵对置
     *
     * @param array
     * @return
     */
    public static double[][] transpose(double[][] array) {
        if (array == null) {
            return null;
        }
        int ySize = array.length;
        if (ySize == 0) {
            return new double[][]{};
        }
        RealMatrix realMatrix = new Array2DRowRealMatrix(array);
        return realMatrix.transpose().getData();
    }

    /**
     * 矩阵求逆
     *
     * @param matrix
     * @return
     */
    public static RealMatrix inverseMatrix(RealMatrix matrix) {
        LUDecomposition LUDe = new LUDecomposition(matrix);
        DecompositionSolver solver = LUDe.getSolver();
        return solver.getInverse();
    }
}
