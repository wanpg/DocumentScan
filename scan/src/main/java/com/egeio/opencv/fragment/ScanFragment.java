package com.egeio.opencv.fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.egeio.opencv.ScanDataInterface;
import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.MatBitmapTransformation;
import com.egeio.opencv.tools.SysUtils;
import com.egeio.opencv.view.CameraView;
import com.egeio.opencv.view.LoadingInfoHolder;
import com.egeio.opencv.view.PreviewImageView;
import com.egeio.opencv.view.ScanInfoView;
import com.egeio.opencv.work.ImageSaveWorker;
import com.egeio.opencv.work.SquareFindWorker;
import com.egeio.scan.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.egeio.opencv.DocumentScan.MAX_PAGE_NUM;
import static com.egeio.opencv.DocumentScan.SQUARE_FIND_SCALE;
import static com.egeio.opencv.tools.CvUtils.findBestPointInfo;

public class ScanFragment extends BaseScanFragment implements Observer {

    private static final int MSG_AUTO_TAKE_PHOTO = 1001;
    private static final int MSG_SHOW_MAX_PAGE_TIP = 1002;

    private CameraView cameraView;
    private ScanInfoView scanInfoView;
    private ImageView thumbnail, flash;
    private PreviewImageView thumbnailPreview;
    private TextView txtNumber;
    private View viewArrow;

    private LoadingInfoHolder loadingInfoHolder;

    private final List<PointInfo> pointInfoArrayList = new CopyOnWriteArrayList<>();

    private View mContainer;
    private SquareFindWorker squareFindWorker;

    Debug debug = new Debug(ScanFragment.class.getSimpleName());

    final Object lockObject = new Object();

    private SoftReference<Bitmap> cachedPreviewBitmap;

    /**
     * 标记照片正在拍照并执行动画的过程
     */
    private boolean isPictureTaking = false;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_AUTO_TAKE_PHOTO:
                    takePhoto();
                    break;
                case MSG_SHOW_MAX_PAGE_TIP:
                    loadingInfoHolder.showInfo(R.drawable.ic_tips, "扫描数量已达上限");
                    break;
            }
        }
    };

    private ScanDataInterface scanDataInterface;
    private ScanDataManager scanDataManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        scanDataInterface = (ScanDataInterface) getActivity();
        scanDataManager = scanDataInterface.getScanDataManager();
    }

    AnimatedVectorDrawableCompat autoShotDrawable;
    AnimatedVectorDrawableCompat fileLoadingDrawable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autoShotDrawable = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.anim_loading_over);
        fileLoadingDrawable = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.anim_file_loading);
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
            flash = mContainer.findViewById(R.id.flash);
            txtNumber = mContainer.findViewById(R.id.text_num);
            viewArrow = mContainer.findViewById(R.id.view_arrow);
            loadingInfoHolder = new LoadingInfoHolder(mContainer.findViewById(R.id.area_info));
            mContainer.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().onBackPressed();
                }
            });
            mContainer.findViewById(R.id.lens).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (scanDataManager.getScanInfoSize() >= MAX_PAGE_NUM) {
                        SysUtils.performVibrator(getContext());
                        loadingInfoHolder.shakeInfo();
                    } else {
                        takePhoto();
                    }
                }
            });
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanDataInterface.toEditPreview(null);
                }
            });
            flash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cameraView.toggleFlash();
                    changeFlashResource();
                }
            });
        }
        return mContainer;
    }


    void changeFlashResource() {
        flash.setImageResource(cameraView.isFlashOn() ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
    }

    @Override
    public void onResume() {
        super.onResume();
        SysUtils.setFullScreen(getActivity());
        SysUtils.hideSystemUI(getActivity());
        if (cameraView != null) {
            cameraView.onResume();
        }
        if (scanDataManager.getScanInfoSize() > MAX_PAGE_NUM) {
            handler.sendEmptyMessage(MSG_SHOW_MAX_PAGE_TIP);
        } else {
            loadingInfoHolder.hideInfo();
        }
        showThumbnail();
        startSquareFind();
        changeFlashResource();
        scanDataManager.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.onPause();
        }
        stopSquareFind();
        changeFlashResource();
        scanDataManager.deleteObserver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.release();
        }
    }

    private void takePhoto() {
        handler.removeMessages(MSG_AUTO_TAKE_PHOTO);
        synchronized (lockObject) {
            if (isPictureTaking) {
                return;
            }
            isPictureTaking = true;
        }
        final PointInfo pointInfo = findBestPointInfo(pointInfoArrayList);
        if (pointInfo != null) {
            final Size previewSize = cameraView.getPreviewSize();
            scanInfoView.setPoint(
                    CvUtils.rotatePoints(pointInfo.getPoints(), previewSize.width * SQUARE_FIND_SCALE, previewSize.height * SQUARE_FIND_SCALE, com.egeio.opencv.tools.Utils.getCameraOrientation(getContext())),
                    SQUARE_FIND_SCALE / cameraView.getScaleRatio());
        } else {
            scanInfoView.clear();
        }
        cameraView.takePhoto(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, final Camera camera) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingInfoHolder.hideInfo();
                    }
                });
                cameraView.stopPreviewDisplay();
                ImageSaveWorker pictureWorker = new ImageSaveWorker(
                        getContext(),
                        bytes,
                        camera,
                        pointInfo,
                        SQUARE_FIND_SCALE) {

                    @Override
                    public void onImageCropPreview(ScanInfo scanInfo) {
                        // 此处做预览预览动画
                        debug.start("生成预览缩略图");
                        // 未旋转的图片
                        Mat frameMat = cameraView.getFrameMat(1);
                        // 进行自动优化转换
                        frameMat = CvUtils.formatFromScanInfo(frameMat, scanInfo);
                        // 转换为Bitmap
                        Bitmap bitmap = Bitmap.createBitmap(frameMat.width(), frameMat.height(), Bitmap.Config.RGB_565);
                        Utils.matToBitmap(frameMat, bitmap);
                        frameMat.release();
                        debug.end("生成预览缩略图");
                        // dismiss loading 并且执行动画从全屏到右下角
                        scanInfoView.clear();
                        thumbnailPreviewAnimation(bitmap, scanInfo.getRotateAngle());
                        debug.start("保存图片");
                    }

                    @Override
                    public void onImageSaved(ScanInfo scanInfo) {
                        scanDataManager.add(scanInfo);
                        debug.end("保存图片");
                        showThumbnail();
                    }
                };
                pointInfoArrayList.clear();
                new Thread(pictureWorker).start();
            }
        });
    }

    private void setCachedNumber(int number) {
        txtNumber.setVisibility(number <= 0 ? View.GONE : View.VISIBLE);
        viewArrow.setVisibility(number <= 0 ? View.GONE : View.VISIBLE);
        txtNumber.setText(String.valueOf(number));
    }

    private void showThumbnail() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final int scanSize = scanDataManager.getScanInfoSize();
                setCachedNumber(scanSize);
                if (scanSize <= 0) {
                    return;
                }

                final ScanInfo scanInfo = scanDataManager.getScanInfo(scanSize - 1);
                final Size originSize = scanInfo.getOriginSize();
                // 变换后的size
                Size perSize;
                if (!scanInfo.matchSize()) {
                    perSize = com.egeio.opencv.tools.Utils.calPerspectiveSize(originSize.width, originSize.height, CvUtils.pointD2point(scanInfo.getCurrentPointInfo().getPoints()));
                } else {
                    perSize = new Size(originSize.width, originSize.height);
                }
                final int thumbnailSize = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
                double scale = thumbnailSize / Math.max(perSize.width, perSize.height);
                int tarSize = (int) (Math.max(originSize.width, originSize.height) * scale);
                Glide.with(ScanFragment.this)
                        .load(scanInfo.getPath())
                        .asBitmap()
                        .atMost()
                        .transform(new MatBitmapTransformation(getContext(), scanInfo))
                        .override(tarSize, tarSize)
                        .into(new ImageViewTarget<Bitmap>(thumbnail) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                thumbnail.setImageBitmap(resource);
                                thumbnail.setRotation(scanInfo.getRotateAngle().getValue());
                            }
                        });
            }
        });
    }

    private void thumbnailPreviewAnimation(final Bitmap bitmap, final ScanInfo.Angle angle) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                debug.start("动画设置");
                cachedPreviewBitmap = new SoftReference<>(bitmap);
                thumbnailPreview.setBitmap(cachedPreviewBitmap.get());
                thumbnailPreview.setRotateAngle(angle.getValue());
                thumbnailPreview.postInvalidate();

                final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

                int thumbnailSize = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
                int thumbnailMargin = getResources().getDimensionPixelOffset(R.dimen.thumbnail_margin);
                int width1 = displayMetrics.widthPixels - thumbnailMargin * 2;//scanInfoView.getWidth();
                int height1 = displayMetrics.heightPixels - thumbnailMargin * 2;// scanInfoView.getHeight();

                float startX = 0;
                float startY = 0;

                thumbnailPreview.setX(startX);
                thumbnailPreview.setY(startY);
                thumbnailPreview.setAlpha(0.6f);
                thumbnailPreview.setScaleX(1);
                thumbnailPreview.setScaleY(1);

                AnimatorSet animatorSet = new AnimatorSet();
                final ObjectAnimator alpha = ObjectAnimator.ofFloat(thumbnailPreview, "alpha", 0.6f, 1.0f, 1.0f).setDuration(500);
                final ObjectAnimator translationX = ObjectAnimator.ofFloat(thumbnailPreview, "translationX",
                        startX, width1 - startX - thumbnailSize * 1.5f).setDuration(300);
                final ObjectAnimator translationY = ObjectAnimator.ofFloat(thumbnailPreview, "translationY",
                        startY, height1 - startY - thumbnailSize * 1.5f).setDuration(300);
                final ObjectAnimator scaleX = ObjectAnimator.ofFloat(thumbnailPreview, "scaleX", 1, (float) (thumbnailSize / width1)).setDuration(250);
                final ObjectAnimator scaleY = ObjectAnimator.ofFloat(thumbnailPreview, "scaleY", 1, (float) (thumbnailSize / height1)).setDuration(250);
                animatorSet.play(alpha).before(translationX);
                animatorSet.play(translationX).with(translationY).with(scaleX).with(scaleY);
                animatorSet.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        debug.d("动画开始");
                        debug.start("开始动画");
                        thumbnailPreview.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbnailPreview.setVisibility(View.GONE);
                        thumbnailPreview.setBitmap(null);
                        com.egeio.opencv.tools.Utils.recycle(bitmap);
                        debug.end("开始动画");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cameraView.startPreviewDisplay();
                                synchronized (lockObject) {
                                    isPictureTaking = false;
                                    lockObject.notify();
                                }
                            }
                        }, 100);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animatorSet.start();
                debug.end("动画设置");
            }
        });
    }

    private void stopSquareFind() {
        if (squareFindWorker != null) {
            squareFindWorker.stopWork();
            squareFindWorker = null;
        }
    }

    private void startSquareFind() {
        stopSquareFind();
        new Thread(squareFindWorker = new SquareFindWorker(SQUARE_FIND_SCALE, lockObject) {

            @Override
            public boolean enableToFind() {
                return scanDataManager.getScanInfoSize() < MAX_PAGE_NUM && !isPictureTaking;
            }

            @Override
            public Mat getFrameMat() {
                return cameraView.getFrameMat(SQUARE_FIND_SCALE);
            }

            @Override
            public void onPointsFind(Size squareContainerSize, List<PointD> points) {
                PointInfo pointInfo = null;
                if (enableToFind() && squareContainerSize != null && points != null && !points.isEmpty()) {
                    scanInfoView.setPoint(
                            CvUtils.rotatePoints(points, squareContainerSize.width, squareContainerSize.height, com.egeio.opencv.tools.Utils.getCameraOrientation(getContext())),
                            SQUARE_FIND_SCALE / cameraView.getScaleRatio());
                    pointInfoArrayList.add(pointInfo = new PointInfo(points));
                } else {
                    scanInfoView.clear();
                }
                assertWorkStopped();
                final PointInfo finalPointInfo = pointInfo;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        checkAndAutoTakePhoto(finalPointInfo);
                    }
                });
            }
        }).start();
    }

    @MainThread
    private void checkAndAutoTakePhoto(PointInfo latestPointInfo) {
        int matchCount = 0;
        int notMatchCount = 0;
        if (latestPointInfo != null) {
            final int size = pointInfoArrayList.size();
            if (size > 2) {
                final long currentTimeMillis = System.currentTimeMillis();
                double currentInfoArea = Imgproc.contourArea(CvUtils.pointToMat(latestPointInfo.getPoints()));
                for (int i = size - 1; i >= 0; i--) {
                    PointInfo pointInfo = pointInfoArrayList.get(i);
                    final long timeDis = currentTimeMillis - pointInfo.getTime();
                    if (timeDis < 0 || timeDis > 500) {
                        if (latestPointInfo != pointInfo) {
                            final double contourArea = Imgproc.contourArea(CvUtils.pointToMat(pointInfo.getPoints()));
                            if (Math.abs((contourArea - currentInfoArea) / currentInfoArea) < 0.015d) {
                                matchCount++;
                            } else {
                                notMatchCount++;
                            }
                            if (notMatchCount >= 2) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        synchronized (lockObject) {
            if (isPictureTaking) {
                return;
            }
        }
        if (matchCount >= 5) {
            if (!handler.hasMessages(MSG_AUTO_TAKE_PHOTO)) {
                autoShotDrawable.stop();
                loadingInfoHolder.showInfo(autoShotDrawable, "请勿移动，正在扫描...");
                autoShotDrawable.start();
                handler.sendEmptyMessageDelayed(MSG_AUTO_TAKE_PHOTO, 1000);
            }
        } else if (matchCount <= 0) {
            handler.removeMessages(MSG_AUTO_TAKE_PHOTO);
            loadingInfoHolder.showInfo(fileLoadingDrawable, "正在识别文档...");
            fileLoadingDrawable.start();
        } else {
            if (!handler.hasMessages(MSG_AUTO_TAKE_PHOTO)) {
                loadingInfoHolder.showInfo(fileLoadingDrawable, "正在识别文档...");
                fileLoadingDrawable.start();
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        final int scanInfoSize = scanDataManager.getScanInfoSize();
        if (scanInfoSize >= MAX_PAGE_NUM) {
            handler.sendEmptyMessage(MSG_SHOW_MAX_PAGE_TIP);
        } else {
            synchronized (lockObject) {
                lockObject.notify();
            }
        }
    }
}
