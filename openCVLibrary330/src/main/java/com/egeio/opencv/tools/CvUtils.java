package com.egeio.opencv.tools;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.egeio.opencv.OpenCVHelper;
import com.egeio.opencv.model.PointD;

import org.opencv.android.*;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
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
}
