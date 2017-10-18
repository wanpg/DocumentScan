package com.egeio.opencv;

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
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.MatBitmapTransformation;
import com.egeio.opencv.view.PreviewImageView;
import com.egeio.opencv.view.ScanInfoView;
import com.egeio.opencv.work.ImageSaveWorker;
import com.egeio.opencv.work.SquareFindWorker;

import org.opencv.R;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends BaseScanFragment {

    private static final int MSG_AUTO_TAKE_PHOTO = 1001;

    private CameraView cameraView;
    private ScanInfoView scanInfoView;
    private ImageView thumbnail, flash;
    private PreviewImageView thumbnailPreview;
    private TextView txtNumber;
    private View viewArrow;

    private View areaInfo;
    private ImageView imageInfo;
    private TextView textInfo;

    /**
     * 预览图按照这个比率缩小进行边框查找
     */
    private final float squareFindScale = 0.125f;
    private final ArrayList<PointInfo> pointInfoArrayList = new ArrayList<>();

    private View mContainer;
    private SquareFindWorker squareFindWorker;

    Debug debug = new Debug(ScanFragment.class.getSimpleName());

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_AUTO_TAKE_PHOTO:
                    takePhoto();
                    break;
            }
        }
    };

    ScanEditInterface scanEditInterface;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        scanEditInterface = (ScanEditInterface) getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVHelper.init();
    }

    private void showInfo(final int drawableRes, final String info) {
        areaInfo.setVisibility(View.VISIBLE);
        imageInfo.setImageResource(drawableRes);
        textInfo.setText(info);
    }

    private void hideInfo() {
        areaInfo.setVisibility(View.GONE);
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
            areaInfo = mContainer.findViewById(R.id.area_info);
            imageInfo = mContainer.findViewById(R.id.image_info);
            textInfo = mContainer.findViewById(R.id.text_info);
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
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanEditInterface.toEditPreview(null);
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
        com.egeio.opencv.tools.Utils.setFullScreen(getActivity());
        com.egeio.opencv.tools.Utils.hideSystemUI(getActivity());
        if (cameraView != null) {
            cameraView.onResume();
        }
        showThumbnail();
        hideInfo();
        startSquareFind();
        changeFlashResource();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.onPause();
        }
        stopSquareFind();
        changeFlashResource();
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
        stopSquareFind();
        final PointInfo pointInfo = findBestPointInfo();
        if (pointInfo != null) {
            final Size previewSize = cameraView.getPreviewSize();
            scanInfoView.setPoint(
                    CvUtils.rotatePoints(pointInfo.getPoints(), previewSize.width * squareFindScale, previewSize.height * squareFindScale, com.egeio.opencv.tools.Utils.getCameraOrientation(getContext())),
                    squareFindScale / cameraView.getScaleRatio());
        } else {
            scanInfoView.clear();
        }
        cameraView.takePhoto(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, final Camera camera) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        hideInfo();
                    }
                });
                cameraView.stopPreviewDisplay();
                ImageSaveWorker pictureWorker = new ImageSaveWorker(
                        getContext(),
                        bytes,
                        camera,
                        pointInfo,
                        squareFindScale) {

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
                        thumbnailPreviewAnimation(bitmap, scanInfo.getRotateAngle());
                        debug.start("保存图片");
                    }

                    @Override
                    public void onImageSaved(ScanInfo scanInfo) {
                        scanEditInterface.add(scanInfo);
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
                final int scanSize = scanEditInterface.getScanInfoSize();
                setCachedNumber(scanSize);
                if (scanSize <= 0) {
                    return;
                }

                final ScanInfo scanInfo = scanEditInterface.getScanInfo(scanSize - 1);
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

    private SoftReference<Bitmap> cachedPreviewBitmap;

    private synchronized void thumbnailPreviewAnimation(final Bitmap bitmap, final ScanInfo.Angle angle) {
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
                                startSquareFind();
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

    private void startSquareFind() {
        stopSquareFind();
        new Thread(squareFindWorker = new SquareFindWorker(squareFindScale) {

            @Override
            public void startFindSquares() {
            }

            @Override
            public Mat getFrameMat() {
                return cameraView.getFrameMat(squareFindScale);
            }

            @Override
            public void onPointsFind(Size squareContainerSize, List<PointD> points) {
                PointInfo pointInfo = null;
                if (squareContainerSize != null && points != null && !points.isEmpty()) {
                    scanInfoView.setPoint(
                            CvUtils.rotatePoints(points, squareContainerSize.width, squareContainerSize.height, com.egeio.opencv.tools.Utils.getCameraOrientation(getContext())),
                            squareFindScale / cameraView.getScaleRatio());
                    synchronized (ScanFragment.this) {
                        pointInfoArrayList.add(pointInfo = new PointInfo(points));
                    }
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

    private void checkAndAutoTakePhoto(PointInfo latestPointInfo) {
        synchronized (ScanFragment.this) {
            int matchCount = 0;
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
                                if (Math.abs((contourArea - currentInfoArea) / currentInfoArea) < 0.01d) {
                                    matchCount++;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (matchCount >= 5) {
                if (!handler.hasMessages(MSG_AUTO_TAKE_PHOTO)) {
                    showInfo(R.drawable.ic_file, "请勿移动，正在扫描...");
                    handler.sendEmptyMessageDelayed(MSG_AUTO_TAKE_PHOTO, 1500);
                }
            } else if (matchCount <= 0) {
                handler.removeMessages(MSG_AUTO_TAKE_PHOTO);
                showInfo(R.drawable.ic_file, "正在识别文档...");
            } else {
                if (!handler.hasMessages(MSG_AUTO_TAKE_PHOTO)) {
                    showInfo(R.drawable.ic_file, "正在识别文档...");
                }
            }
        }
    }

    /**
     * 找到往前推最合适的point info
     *
     * @return
     */
    private PointInfo findBestPointInfo() {
        if (pointInfoArrayList.isEmpty()) {
            return null;
        }

        final long currentTimeMillis = System.currentTimeMillis();
        PointInfo pointInfoLargest = null;
        for (PointInfo pointInfo : pointInfoArrayList) {
            final long timeDis = currentTimeMillis - pointInfo.getTime();
            if (timeDis < 0 || timeDis > 500) {
                continue;
            }
            if (pointInfoLargest == null) {
                pointInfoLargest = pointInfo;
            } else {
                if (Imgproc.contourArea(CvUtils.pointToMat(pointInfo.getPoints())) >=
                        Imgproc.contourArea(CvUtils.pointToMat(pointInfoLargest.getPoints()))) {
                    pointInfoLargest = pointInfo;
                }
            }
        }
        return pointInfoLargest;
    }

    private void stopSquareFind() {
        if (squareFindWorker != null) {
            squareFindWorker.stopWork();
            squareFindWorker = null;
        }
    }
}
