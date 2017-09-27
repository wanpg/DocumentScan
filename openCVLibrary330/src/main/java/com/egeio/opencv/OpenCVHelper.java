package com.egeio.opencv;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.utils.Converters;

import java.util.List;

/**
 * Created by wangjinpeng on 2017/9/15.
 */

public class OpenCVHelper {

    static {
        System.loadLibrary("EgeioOpenCV");
    }

    public static void findSquares(Mat image, List<MatOfPoint2f> squares) {
        Mat contours_mat = new Mat();
        findSquares(image.getNativeObjAddr(), contours_mat.getNativeObjAddr());
        Converters.Mat_to_vector_vector_Point2f(contours_mat, squares);
        contours_mat.release();
    }

    public static native int[] gray(int[] buf, int w, int h);

    public static native void findSquares(long inputImage, long squares_mat_nativeObj);
}
