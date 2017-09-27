package com.egeio.opencv;

import org.opencv.android.OpenCVLoader;

/**
 * Created by wangjinpeng on 2017/9/15.
 */

public class OpenCVHelper {

    public static void init() {
        System.loadLibrary("EgeioOpenCV");
        OpenCVLoader.initDebug();
    }

    public static native int[] gray(int[] buf, int w, int h);
}
