package com.egeio.opencv.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.egeio.opencv.model.ScanInfo;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(scanInfo.getPath(), options);
        ScanInfo.Angle rotateAngle = scanInfo.getRotateAngle();
        int imgWidth = rotateAngle == ScanInfo.Angle.angle_90 || rotateAngle == ScanInfo.Angle.angle_270 ? options.outHeight : options.outWidth;
        int imgHeight = rotateAngle == ScanInfo.Angle.angle_90 || rotateAngle == ScanInfo.Angle.angle_270 ? options.outWidth : options.outHeight;
        if (rotateAngle == ScanInfo.Angle.angle_0
                && scanInfo.matchSize(options.outWidth, options.outHeight)) {
            //原图
            return toTransform;
        } else {
            Mat frameMat = new Mat();
            Utils.bitmapToMat(toTransform, frameMat);
            Mat rotatedMat;
            int cvRotateTypeByCamera = com.egeio.opencv.tools.Utils.getCvRotateTypeByCamera(context);
            if (cvRotateTypeByCamera != -1) {
                rotatedMat = new Mat();
                Core.rotate(frameMat, rotatedMat, cvRotateTypeByCamera);
                frameMat.release();
            } else {
                rotatedMat = frameMat;
            }
            float ratioW = 1f * rotatedMat.width() / imgWidth;
            float ratioH = 1f * rotatedMat.height() / imgHeight;
            Bitmap bitmap;
            ArrayList<Point> points = scanInfo.getCurrentPointInfo().getPoints();
            List<Point> pointList = new ArrayList<>();
            for (Point point : points) {
                pointList.add(new Point(point.x * ratioW, point.y * ratioH));
            }
            Mat mat = com.egeio.opencv.tools.Utils.PerspectiveTransform(rotatedMat, pointList);
            bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mat, bitmap);
            mat.release();
            rotatedMat.release();
            return bitmap;
        }
    }

    @Override
    public String getId() {
        return MatBitmapTransformation.class.getSimpleName();
    }
}
