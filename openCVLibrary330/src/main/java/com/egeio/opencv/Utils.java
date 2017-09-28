package com.egeio.opencv;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.TypedValue;
import android.view.Surface;
import android.view.WindowManager;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by wangjinpeng on 2017/9/15.
 */

public class Utils {

    public static void recycle(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                bitmap.recycle();
            } catch (Exception ignored) {
            } finally {
                bitmap = null;
            }
        }
    }

    public static int dp2px(Context context, int dp) {
        Resources resources = context.getResources();
        return (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()));
    }

    public static int getCameraOrientation(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = manager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static MatOfPoint2f copy2MatPoint2f(MatOfPoint mat) {
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        matOfPoint2f.fromArray(mat.toArray());
        return matOfPoint2f;
    }

    public static Mat copy2Mat(Mat mat) {
        Mat matCopy = new Mat();
        mat.copyTo(matCopy);
        return matCopy;
    }

    public static MatOfPoint copy2MatPoint(MatOfPoint2f mat) {
        MatOfPoint matOfPoint = new MatOfPoint();
        matOfPoint.fromArray(mat.toArray());
        return matOfPoint;
    }

    public static List<Point> findLargestList(List<List<Point>> pointsList) {
        int maxIndex = -1;
        double maxArea = 0;
        for (int i = 0; i < pointsList.size(); i++) {
            List<Point> points = pointsList.get(i);
            double contourArea = Math.abs(Imgproc.contourArea(new MatOfPoint(points.toArray(new Point[]{}))));
            if (contourArea > maxArea) {
                maxIndex = i;
                maxArea = contourArea;
            }
        }
        return pointsList.get(maxIndex);
    }
}
