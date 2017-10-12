package com.egeio.opencv.tools;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.ScanInfo;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class MatBitmapTransformation extends BitmapTransformation {

    private ScanInfo scanInfo;
    private Context context;

    public MatBitmapTransformation(Context context, ScanInfo scanInfo) {
        super(context);
        this.context = context;
        this.scanInfo = scanInfo;
    }

    public MatBitmapTransformation(BitmapPool bitmapPool) {
        super(bitmapPool);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        // 此处对
        ScanInfo.Angle rotateAngle = scanInfo.getRotateAngle();
        Size originSize = scanInfo.getOriginSize();
        // 角度为0，面积匹配，没有优化的状态返回原图
        if (rotateAngle == ScanInfo.Angle.angle_0
                && scanInfo.matchSize(originSize.width, originSize.height)
                && !scanInfo.isOptimized()) {
            //原图
            return toTransform;
        } else {
            Mat frameMat = new Mat();
            // 转换bitmap为mat
            Utils.bitmapToMat(toTransform, frameMat);

            // 旋转
            frameMat = rotate(frameMat, scanInfo);

            // 透视变换，截取拉伸
            frameMat = warpPerspective(frameMat, scanInfo);

            // 优化
            frameMat = optimize(frameMat, scanInfo);

            Bitmap bitmap = Bitmap.createBitmap(frameMat.width(), frameMat.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(frameMat, bitmap);
            frameMat.release();
            return bitmap;
        }
    }

    @Override
    public String getId() {
        return MatBitmapTransformation.class.getSimpleName();
    }

    /**
     * 旋转
     *
     * @param mat
     * @param scanInfo
     * @return
     */
    private Mat rotate(Mat mat, ScanInfo scanInfo) {
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
    private Mat warpPerspective(Mat src, ScanInfo scanInfo) {
        Size currentSize = scanInfo.getCurrentSize();
        if (scanInfo.matchSize(currentSize.width, currentSize.height)) {
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
    private Mat optimize(Mat src, ScanInfo scanInfo) {
        if (scanInfo.isOptimized()) {
            // 亮度
            // 对比度
            Mat mat = new Mat();
            src.convertTo(mat, -1, 1.5, 10);
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
}
