package com.egeio.opencv.work;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;

import static com.egeio.opencv.tools.Utils.recycle;

/**
 * image加载worker，用来将scanInfo的内容从图片加载成为bitmap
 */

public abstract class ImageLoadWorker extends Worker {

    public abstract void onImageLoaded(Bitmap bitmap, ScanInfo scanInfo);


    private Debug debug = new Debug(ImageLoadWorker.class.getSimpleName());

    /**
     * 预览的信息
     */
    private ScanInfo scanInfo;

    /**
     * 输出的尺寸
     */
    private int width, height;

    public ImageLoadWorker(ScanInfo scanInfo, int width, int height) {
        this.scanInfo = scanInfo;
        this.width = width;
        this.height = height;
    }

    public ImageLoadWorker(ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
    }

    protected void doFirst() {

    }

    protected void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void doWork() {
        Bitmap bitmapSrc = null, bitmapTemp = null;
        Mat frameMat = null;
        try {
            doFirst();

            debug.start("计算目标尺寸");
            // 此处对
            Size originSize = scanInfo.getOriginSize();

            // 计算出原图透视转换后图片的尺寸
            final PointInfo currentPointInfo = scanInfo.getCurrentPointInfo();
            final ArrayList<PointD> points = currentPointInfo.getPoints();
            Size perspectiveSize;
            if (!scanInfo.matchSize()) {
                perspectiveSize = com.egeio.opencv.tools.Utils.calPerspectiveSize(originSize.width, originSize.height, CvUtils.pointD2point(points));
            } else {
                perspectiveSize = new Size(originSize.width, originSize.height);
            }
            debug.end("计算目标尺寸");

            // 找出imageView最长的边，这样来设置图片的缩放比例
            int imageViewMaxSide = Math.max(width, height);
            final double maxPerSide = Math.max(perspectiveSize.width, perspectiveSize.height);
            final double scaleRatio = maxPerSide / imageViewMaxSide;

            assertWorkStopped();
            debug.start("加载图片Bitmap");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = Math.max(2, CvUtils.calculateInSampleSize((int) perspectiveSize.width, (int) perspectiveSize.height, imageViewMaxSide, imageViewMaxSide));
            bitmapSrc = BitmapFactory.decodeFile(scanInfo.getPath(), options);
            debug.end("加载图片Bitmap");
            debug.d("bitmapSrc--size:[" + bitmapSrc.getWidth() + ", " + bitmapSrc.getHeight() + "]");

            // 角度为0，面积匹配，没有优化的状态返回原图
            if (scanInfo.matchSize()
                    && !scanInfo.isOptimized()) {
                //原图
            } else {
                assertWorkStopped();
                debug.start("bitmap to mat");
                frameMat = new Mat();
                Utils.bitmapToMat(bitmapSrc, frameMat);
                debug.end("bitmap to mat");

                assertWorkStopped();
                debug.start("拉伸优化");
                frameMat = CvUtils.formatFromScanInfo(frameMat, scanInfo);
                debug.end("拉伸优化");

                assertWorkStopped();
                debug.start("生成bitmap");
                bitmapTemp = Bitmap.createBitmap(frameMat.width(), frameMat.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(frameMat, bitmapTemp);
                frameMat.release();
                frameMat = null;
                recycle(bitmapSrc);
                bitmapSrc = bitmapTemp;
                bitmapTemp = null;
                debug.end("生成bitmap");
            }
            assertWorkStopped();
            onImageLoaded(bitmapSrc, scanInfo);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof WorkStoppedException) {
                recycle(bitmapSrc);
            }
        } finally {
            if (frameMat != null) {
                frameMat.release();
            }
            recycle(bitmapTemp);
        }
    }
}
