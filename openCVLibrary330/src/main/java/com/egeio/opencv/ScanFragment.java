package com.egeio.opencv;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.MatBitmapTransformation;
import com.egeio.opencv.work.ImageSaveWorker;
import com.egeio.opencv.work.SquareFindWorker;

import org.opencv.R;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends Fragment {

    private CameraView cameraView;
    private ScanInfoView scanInfoView;
    private ImageView thumbnail, thumbnailPreview;
    /**
     * 预览图按照这个比率缩小进行边框查找
     */
    private final float squareFindScale = 0.125f;
    private final ArrayList<ScanInfo> scanInfoArrayList = new ArrayList<>();
    private final ArrayList<PointInfo> pointInfoArrayList = new ArrayList<>();

    private View mContainer;
    private SquareFindWorker squareFindWorker;

    Debug debug = new Debug(ScanFragment.class.getSimpleName());

    private Handler handler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVHelper.init();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_scan, null);
            cameraView = mContainer.findViewById(R.id.camera_view);
            scanInfoView = mContainer.findViewById(R.id.scan_info);
            thumbnail = mContainer.findViewById(R.id.thumbnail);
            thumbnailPreview = mContainer.findViewById(R.id.thumbnail_preview);
            mContainer.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().onBackPressed();
                }
            });
            mContainer.findViewById(R.id.lens).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    takePhoto();
                }
            });
        }
        return mContainer;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraView != null) {
            cameraView.onResume();
        }
        startSquareFind();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.onPause();
        }
        stopSquareFind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.release();
        }
    }

    private void takePhoto() {
        cameraView.takePhoto(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, final Camera camera) {
                stopSquareFind();
                cameraView.stopPreviewDisplay();
                ImageSaveWorker pictureWorker = new ImageSaveWorker(
                        getContext(),
                        bytes,
                        camera,
                        pointInfoArrayList.isEmpty() ? null : pointInfoArrayList.get(pointInfoArrayList.size() - 1),
                        squareFindScale) {

                    @Override
                    public void onImageCropPreview(ScanInfo scanInfo, PointInfo pointInfo) {
                        // 此处做预览预览动画
                        debug.start("生成预览缩略图");
                        Mat frameMat = cameraView.getFrameMat(1);
                        Bitmap bitmap;
                        if (pointInfo == null) {
                            //原图
                            bitmap = Bitmap.createBitmap(frameMat.width(), frameMat.height(), Bitmap.Config.RGB_565);
                            Utils.matToBitmap(frameMat, bitmap);
                        } else {
                            ArrayList<Point> points = pointInfo.getPoints();
                            List<Point> pointList = new ArrayList<>();
                            for (Point point : points) {
                                pointList.add(new Point(point.x / squareFindScale, point.y / squareFindScale));
                            }
                            Mat mat = com.egeio.opencv.tools.Utils.PerspectiveTransform(frameMat, pointList);
                            bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
                            Utils.matToBitmap(mat, bitmap);
                            mat.release();
                        }
                        frameMat.release();
                        debug.end("生成预览缩略图");
                        // dismiss loading 并且执行动画从全屏到右下角
                        thumbnailPreviewAnimation(bitmap);
                        debug.start("保存图片");
                    }

                    @Override
                    public void onImageSaved(ScanInfo scanInfo) {
                        scanInfoArrayList.add(scanInfo);
                        debug.end("保存图片");
                        showThumbnail();
                    }
                };
                new Thread(pictureWorker).start();
            }
        });
    }

    private void showThumbnail() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (scanInfoArrayList.isEmpty()) {
                    return;
                }
                ScanInfo scanInfo = scanInfoArrayList.get(scanInfoArrayList.size() - 1);
                Glide.with(ScanFragment.this)
                        .load(scanInfo.getPath())
                        .asBitmap()
                        .transform(new MatBitmapTransformation(getContext(), scanInfo))
                        .into(thumbnail);
            }
        });
    }

    private SoftReference<Bitmap> cachedPreviewBitmap;

    private synchronized void thumbnailPreviewAnimation(final Bitmap bitmap) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                debug.start("动画设置");
                cachedPreviewBitmap = new SoftReference<>(bitmap);
                thumbnailPreview.setImageBitmap(cachedPreviewBitmap.get());
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                int width1 = scanInfoView.getWidth();
                int height1 = scanInfoView.getHeight();

                int space = com.egeio.opencv.tools.Utils.dp2px(getContext(), 10);
                Size size = new Size();
                com.egeio.opencv.tools.Utils.calApproximateSize(
                        new Size(width1 - space * 2, height1 - space * 2),
                        new Size(width, height),
                        size);

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) thumbnailPreview.getLayoutParams();
                layoutParams.width = (int) size.width;
                layoutParams.height = (int) size.height;
                thumbnailPreview.requestLayout();
                thumbnailPreview.setY(0);
                thumbnailPreview.setX(0);
                TranslateAnimation translateAnimation =
                        new TranslateAnimation(
                                (float) ((width1 - size.width) / 2),
                                width1 - com.egeio.opencv.tools.Utils.dp2px(getContext(), 44 + 10),
                                (float) ((height1 - size.height) / 2),
                                height1 + com.egeio.opencv.tools.Utils.dp2px(getContext(), 64 - 44 - 10));

                translateAnimation.setDuration(300);
                translateAnimation.setStartOffset(0);
                translateAnimation.setInterpolator(new AccelerateInterpolator(1.0f));
                int tarSize = com.egeio.opencv.tools.Utils.dp2px(getContext(), 44);
                ScaleAnimation scaleAnimation =
                        new ScaleAnimation(1, (float) (tarSize / size.width), 1, (float) (tarSize / size.height));
                scaleAnimation.setDuration(300);
                scaleAnimation.setStartOffset(0);
                scaleAnimation.setInterpolator(new AccelerateInterpolator(1.0f));
                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(scaleAnimation);
                animationSet.addAnimation(translateAnimation);
                animationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        debug.d("动画开始");
                        debug.start("开始动画");
                        thumbnailPreview.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        thumbnailPreview.setVisibility(View.GONE);
                        thumbnailPreview.setImageBitmap(null);
                        com.egeio.opencv.tools.Utils.recycle(bitmap);
                        debug.end("开始动画");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cameraView.startPreviewDisplay();
                                startSquareFind();
                            }
                        }, 100);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                thumbnailPreview.startAnimation(animationSet);
                debug.end("动画设置");
            }
        });
    }

    private void startSquareFind() {
        stopSquareFind();
        new Thread(squareFindWorker = new SquareFindWorker(squareFindScale) {

            @Override
            public Mat getFrameMat() {
                return cameraView.getFrameMat(squareFindScale);
            }

            @Override
            public void onPointsFind(List<Point> points) {
                if (points != null && !points.isEmpty()) {
                    scanInfoView.setPoint(points, squareFindScale / cameraView.getScaleRatio());
                    pointInfoArrayList.add(new PointInfo(points));
                } else {
                    scanInfoView.clear();
                }
            }
        }).start();
    }

    private void stopSquareFind() {
        if (squareFindWorker != null) {
            squareFindWorker.stopWork();
            squareFindWorker = null;
        }
    }
}
