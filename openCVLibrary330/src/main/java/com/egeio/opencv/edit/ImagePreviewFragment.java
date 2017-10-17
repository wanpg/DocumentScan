package com.egeio.opencv.edit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.view.PreviewImageView;
import com.egeio.opencv.work.Worker;

import org.opencv.R;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class ImagePreviewFragment extends Fragment {

    public static Fragment createInstance(ScanInfo scanInfo) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("SCAN_INFO", scanInfo);
        fragment.setArguments(bundle);
        return fragment;
    }

    private View mContainer;
    private ScanInfo scanInfo;
    private PreviewImageView imageView;
    private boolean isFirst = true;
    Debug debug = new Debug(ImagePreviewFragment.class.getSimpleName());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanInfo = getArguments().getParcelable("SCAN_INFO");
    }

    public ScanInfo getScanInfo() {
        return scanInfo;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_image_preview, null);
            imageView = mContainer.findViewById(R.id.image);
        }
        return mContainer;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirst) {
            showImage();
        }
        isFirst = false;
    }

    public void updateScanInfo(ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
        showImage();
    }

    public void rotate(ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
        if (hasShownImage) {
            imageView.setRotateAngle(scanInfo.getRotateAngle().getValue());
            imageView.postInvalidate();
        } else {
            showImage();
        }
    }

    private Worker imageLoadWorker;
    private Bitmap cachedBitmap;
    private boolean hasShownImage = false;

    @Override
    public void onDestroy() {
        super.onDestroy();
        com.egeio.opencv.tools.Utils.recycle(cachedBitmap);
    }

    private void showImage() {
        if (imageLoadWorker != null) {
            imageLoadWorker.stopWork();
        }
        new Thread(imageLoadWorker = new Worker() {
            @Override
            public void doWork() {
                try {
                    debug.start("等待imageView绘制");
                    if (imageView.getHeight() <= 0 || imageView.getWidth() <= 0) {
                        while (true) {
                            if (imageView.getWidth() > 0 && imageView.getHeight() > 0) {
                                break;
                            }
                            assertWorkStopped();
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    debug.end("等待imageView绘制");

                    debug.start("计算目标尺寸");
                    final ScanInfo.Angle rotateAngle = scanInfo.getRotateAngle();
                    // 此处对
                    Size originSize = scanInfo.getOriginSize();

                    // 计算出原图透视转换后图片的尺寸
                    final PointInfo currentPointInfo = scanInfo.getCurrentPointInfo();
                    final ArrayList<PointD> points = currentPointInfo.getPoints();
                    final Size perspectiveSize = com.egeio.opencv.tools.Utils.calPerspectiveSize(originSize.width, originSize.height, CvUtils.pointD2point(points));
                    debug.end("计算目标尺寸");

                    // 根据转换后的size 和 imageView的最小的size
                    final int imageViewWidth = imageView.getWidth();
                    final int imageViewHeight = imageView.getHeight();

                    // 找出imageView最长的边，这样来设置图片的缩放比例
                    int imageViewMaxSide = Math.max(imageViewWidth, imageViewHeight);
                    final double maxPerSide = Math.max(perspectiveSize.width, perspectiveSize.height);
                    final double scaleRatio = maxPerSide / imageViewMaxSide;

                    assertWorkStopped();
                    debug.start("加载图片Bitmap");
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    options.inSampleSize = Math.max(2, CvUtils.calculateInSampleSize((int) perspectiveSize.width, (int) perspectiveSize.height, imageViewMaxSide, imageViewMaxSide));
                    Bitmap bitmapSrc = BitmapFactory.decodeFile(scanInfo.getPath(), options);
                    debug.end("加载图片Bitmap");
                    debug.d("bitmapSrc--size:[" + bitmapSrc.getWidth() + ", " + bitmapSrc.getHeight() + "]");

                    // 角度为0，面积匹配，没有优化的状态返回原图
                    if (scanInfo.matchSize(originSize.width, originSize.height)
                            && !scanInfo.isOptimized()) {
                        //原图
                    } else {
                        assertWorkStopped();
                        debug.start("bitmap to mat");
                        Mat frameMat = new Mat();
                        Utils.bitmapToMat(bitmapSrc, frameMat);
                        debug.end("bitmap to mat");

                        assertWorkStopped();
                        debug.start("拉伸优化");
                        frameMat = CvUtils.formatFromScanInfo(frameMat, scanInfo);
                        debug.end("拉伸优化");

                        assertWorkStopped();
                        debug.start("生成bitmap");
                        Bitmap bitmap = Bitmap.createBitmap(frameMat.width(), frameMat.height(), Bitmap.Config.RGB_565);
                        Utils.matToBitmap(frameMat, bitmap);
                        frameMat.release();
                        com.egeio.opencv.tools.Utils.recycle(bitmapSrc);
                        bitmapSrc = bitmap;
                        debug.end("生成bitmap");
                    }
                    assertWorkStopped();
                    onImageLoaded(bitmapSrc, rotateAngle);
                } catch (Exception e) {
                    if (e instanceof WorkStoppedException) {
                        // 此处对bitmap等进行回收
                    }
                }
            }
        }).start();
    }

    private synchronized void onImageLoaded(final Bitmap bitmap, final ScanInfo.Angle angle) {
        hasShownImage = true;
        com.egeio.opencv.tools.Utils.recycle(cachedBitmap);
        cachedBitmap = bitmap;
        imageView.setBitmap(cachedBitmap);
        imageView.setRotateAngle(scanInfo.getRotateAngle().getValue());
        imageView.postInvalidate();
    }
}
