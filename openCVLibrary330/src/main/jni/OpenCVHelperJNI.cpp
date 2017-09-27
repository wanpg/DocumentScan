#include "OpenCVHelperJNI.h"
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>

#include <iostream>
#include <math.h>
#include <string.h>

#include <android/log.h>

#define LOG_TAG "OpenCVHelper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

extern "C" {

JNIEXPORT jintArray JNICALL Java_com_egeio_opencv_OpenCVHelper_gray(
    JNIEnv *env, jclass obj, jintArray buf, int w, int h)
{

    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
    if (cbuf == NULL)
    {
        return 0;
    }

    Mat imgData(h, w, CV_8UC4, (unsigned char *)cbuf);

    uchar *ptr = imgData.ptr(0);
    for (int i = 0; i < w * h; i++)
    {
        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
        //对于一个int四字节，其彩色值存储方式为：BGRA
        int grayScale = (int)(ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587 + ptr[4 * i + 0] * 0.114);
        ptr[4 * i + 1] = grayScale;
        ptr[4 * i + 2] = grayScale;
        ptr[4 * i + 0] = grayScale;
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, cbuf);
    env->ReleaseIntArrayElements(buf, cbuf, 0);
    return result;
}
}