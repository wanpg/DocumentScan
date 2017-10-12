package com.egeio.opencv;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

/**
 * Created by wangjinpeng on 2017/9/15.
 */

public class OpenCVHelper {

    public static void init() {
        System.loadLibrary("EgeioOpenCV");
        OpenCVLoader.initDebug();
    }

    public static Mat changeContrastAndBrightness(Mat src, double alpha, int beta) {
        Mat dst = new Mat(src.size(), src.type());
        changeContrastAndBrightness(src.getNativeObjAddr(), dst.getNativeObjAddr(), alpha, beta);
        return dst;
    }

    public static native int[] gray(int[] buf, int w, int h);

    public static native void changeContrastAndBrightness(long src_mat_nativeObj, long dst_mat_nativeObj, double alpha, int beta);
}
