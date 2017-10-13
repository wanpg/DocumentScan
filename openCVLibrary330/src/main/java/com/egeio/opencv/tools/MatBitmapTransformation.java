package com.egeio.opencv.tools;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.egeio.opencv.model.ScanInfo;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

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

            frameMat = CvUtils.formatFromScanInfo(frameMat, scanInfo);

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
}
