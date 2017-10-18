package com.egeio.opencv.tools;

import android.graphics.Bitmap;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.ScanInfo;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class CvUtils {

    public static Mat pointToMat(List<PointD> pointDS) {
        return Converters.vector_Point_to_Mat(pointD2point(pointDS));
    }

    public static List<Point> pointD2point(List<PointD> pointDS) {
        if (pointDS == null)
            return null;
        List<Point> pointList = new ArrayList<>();
        for (PointD pointD : pointDS) {
            pointList.add(pointD2point(pointD));
        }
        return pointList;
    }

    public static Point pointD2point(PointD pointD) {
        return new Point(pointD.x, pointD.y);
    }

    public static List<PointD> point2pointD(List<Point> points) {
        if (points == null)
            return null;
        List<PointD> pointDList = new ArrayList<>();
        for (Point point : points) {
            pointDList.add(point2pointD(point));
        }
        return pointDList;
    }

    public static PointD point2pointD(Point point) {
        return new PointD(point.x, point.y);
    }

    public static List<PointD> rotatePoints(List<PointD> pointDS, double width, double height, int angle) {
        // 此处根据旋转的角度将坐标进行变换
        List<PointD> pointDList = new ArrayList<>();
        for (PointD pointD : pointDS) {
            pointDList.add(rotatePoint(pointD, width, height, angle));
        }
        return pointDList;
    }

    public static PointD rotatePoint(PointD pointD, double width, double height, int angle) {
        PointD pointDRotated = new PointD();
        if (angle == 90) {
            pointDRotated = new PointD(height - pointD.y, pointD.x);
        } else if (angle == 180) {
            pointDRotated = new PointD(width - pointD.x, height - pointD.y);
        } else if (angle == 270) {
            pointDRotated = new PointD(pointD.y, width - pointD.x);
        } else if (angle == 0) {
            pointDRotated = new PointD(pointD.x, pointD.y);
        }
        return pointDRotated;
    }

    /**
     * @param bitmapSrc
     * @param alpha     对比度
     * @param beta      亮度
     * @return
     */
    public static Bitmap modifyContrast(Bitmap bitmapSrc, double alpha, int beta) {
        Mat mat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmapSrc, mat);
        Mat matNew = new Mat();
        mat.convertTo(matNew, -1, alpha, beta);
//        Mat matNew = OpenCVHelper.changeContrastAndBrightness(mat, 2.2, 50);
        Bitmap bitmap = Bitmap.createBitmap(matNew.width(), matNew.height(), Bitmap.Config.RGB_565);
        org.opencv.android.Utils.matToBitmap(matNew, bitmap);
        return bitmap;
    }

    public static Bitmap gray(Bitmap bitmapSrc) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmapSrc, src);
        Mat mat = new Mat();
        Imgproc.cvtColor(src, mat, Imgproc.COLOR_BGR2GRAY);
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
        org.opencv.android.Utils.matToBitmap(mat, bitmap);
//        Mat gray = new Mat(mat.size(), mat.type());
//        Imgproc.threshold(mat, gray, 250, 255, 1);
//        Bitmap bitmap = Bitmap.createBitmap(gray.width(), gray.height(), Bitmap.Config.RGB_565);
//        org.opencv.android.Utils.matToBitmap(gray, bitmap);
        return bitmap;
    }

    public static Mat formatFromScanInfo(Mat src, ScanInfo scanInfo) {
        Mat frameMat = src;
        // 旋转
//        frameMat = rotate(frameMat, scanInfo);

        // 透视变换，截取拉伸
        frameMat = warpPerspective(frameMat, scanInfo);

        // 优化
        frameMat = optimize(frameMat, scanInfo);
        return frameMat;
    }


    /**
     * 旋转
     *
     * @param mat
     * @param scanInfo
     * @return
     */
    private static Mat rotate(Mat mat, ScanInfo scanInfo) {
        ScanInfo.Angle rotateAngle = scanInfo.getRotateAngle();
        // 旋转
        Mat rotatedMat;
        switch (rotateAngle) {
            case angle_90:
                rotatedMat = new Mat();
                Core.rotate(mat, rotatedMat, Core.ROTATE_90_CLOCKWISE);
                mat.release();
                break;
            case angle_180:
                rotatedMat = new Mat();
                Core.rotate(mat, rotatedMat, Core.ROTATE_180);
                mat.release();
                break;
            case angle_270:
                rotatedMat = new Mat();
                Core.rotate(mat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE);
                mat.release();
                break;
            default:
                rotatedMat = mat;
                break;
        }
        return rotatedMat;
    }

    /**
     * 截取区域透视拉伸
     *
     * @param src
     * @param scanInfo
     * @return
     */
    private static Mat warpPerspective(Mat src, ScanInfo scanInfo) {
        Size currentSize = scanInfo.getOriginSize();
        if (scanInfo.matchSize()) {
            return src;
        }
        // 截取拉伸
        double ratioW = src.width() / currentSize.width;
        double ratioH = src.height() / currentSize.height;
        List<PointD> pointDList = scanInfo.getCurrentPointInfo().getPoints();
        List<Point> pointList = new ArrayList<>();
        for (PointD point : pointDList) {
            pointList.add(new Point(point.x * ratioW, point.y * ratioH));
        }
        Mat mat = com.egeio.opencv.tools.Utils.warpPerspective(src, pointList);
        src.release();
        return mat;
    }

    /**
     * 优化
     *
     * @param src
     * @param scanInfo
     * @return
     */
    private static Mat optimize(Mat src, ScanInfo scanInfo) {
        if (scanInfo.isOptimized()) {
            // 亮度
            // 对比度
            Mat mat = new Mat();
            src.convertTo(mat, -1, 1.1, 5);
            //
            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            mat.release();
            src.release();
            return gray;
        } else {
            return src;
        }
    }

    public static int calculateInSampleSize(
            int imageWidth, int imageheight, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (imageWidth > reqHeight || imageheight > reqWidth) {

            final int halfHeight = imageWidth / 2;
            final int halfWidth = imageheight / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Size getRotatedSize(Size size, int angle) {
        Size rotated = new Size();
        rotated.width = angle == 90 || angle == 270 ? size.height : size.width;
        rotated.height = angle == 90 || angle == 270 ? size.width : size.height;
        return rotated;
    }
}
