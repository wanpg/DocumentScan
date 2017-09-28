package com.egeio.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/9/27.
 */

public class DocumentScanFragment extends Fragment {

    private static final String TAG = DocumentScanFragment.class.getSimpleName();

    private FrameLayout frameLayout;
    private CameraView cameraView;
    private ScanInfoView scanInfoView;
    private ImageView previewImageView;
    private SquaresTracker squaresTracker;
    private final float scale = 0.125f;

    private Debug debug = new Debug(TAG);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVHelper.init();
        squaresTracker = new SquaresTracker();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (frameLayout == null) {
            Context context = inflater.getContext();
            frameLayout = new FrameLayout(context);
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.topMargin = 300;
            cameraView = new CameraView(context);
            frameLayout.addView(cameraView, layoutParams);
            scanInfoView = new ScanInfoView(context);
            frameLayout.addView(scanInfoView, layoutParams);
            previewImageView = new ImageView(context);
            previewImageView.setScaleType(ImageView.ScaleType.FIT_START);
            int size = Utils.dp2px(context, 300);
            frameLayout.addView(previewImageView, new FrameLayout.LayoutParams(size, size));
        }
        return frameLayout;
    }

    CameraWorker cameraWorker;

    @Override
    public void onResume() {
        super.onResume();
        if (cameraView != null) {
            cameraView.onResume();
        }
        if (cameraWorker != null) {
            cameraWorker.stopWork();
        }
        new Thread(cameraWorker = new CameraWorker()).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.onPause();
        }
        if (cameraWorker != null) {
            cameraWorker.stopWork();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.release();
        }
    }

    private Bitmap cacheBitmap;

    private void showSquaresPreview(Mat mat) {
        debug.start("转换bitmap");
        Utils.recycle(cacheBitmap);
        cacheBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
        org.opencv.android.Utils.matToBitmap(mat, cacheBitmap);
        FragmentActivity activity = getActivity();
        debug.end("转换bitmap");
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (cacheBitmap != null
                            && !cacheBitmap.isRecycled()) {
                        previewImageView.setImageBitmap(cacheBitmap);
                    }
                }
            });
        }
    }

    private class CameraWorker extends Worker {
        @Override
        public void doWork() {
            while (true) {
                if (isWorkerStopped()) {
                    break;
                }
                debug.clear();
                // 检测区域
                List<List<Point>> matList = new ArrayList<>();
                Mat frameMat = null;
                try {
                    debug.start("获取当前帧");
                    frameMat = cameraView.getFrameMat(scale);
                    debug.start("获取当前帧");
                    if (isWorkerStopped()) {
                        break;
                    }
                    if (frameMat != null && !frameMat.empty()) {
                        showSquaresPreview(frameMat);
                        debug.start("寻找多边形");
                        squaresTracker.findSquares(frameMat, matList, scale);
                        debug.end("寻找多边形");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (frameMat != null) {
                        frameMat.release();
                    }
                }
                if (isWorkerStopped()) {
                    break;
                }
                if (matList.size() > 0) {
                    scanInfoView.setPoint(Utils.findLargestList(matList), scale / cameraView.getScaleRatio());
                } else {
                    scanInfoView.clear();
                }
                Log.d(TAG, "获取到了几个点" + matList.size());
            }
            Log.d(TAG, "Finish processing thread");
        }
    }


    public static class ScanInfoView extends View {
        public ScanInfoView(Context context) {
            super(context);
        }

        public ScanInfoView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public ScanInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public ScanInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        private final List<Point> pointList = new ArrayList<>();
        float scale = 1f;

        public void setPoint(List<Point> pointList, float scale) {
            this.scale = scale;
            this.pointList.clear();
            if (pointList != null) {
                this.pointList.addAll(pointList);
            }
            postInvalidate();
        }

        Paint paint;

        private void drawPoint(Canvas canvas, List<Point> pointList) {
            if (canvas != null && pointList != null && pointList.size() == 4) {
                if (paint == null) {
                    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(Color.YELLOW);
                    paint.setAlpha(128);
                    paint.setStrokeWidth(15);
                }

                Path path = new Path();
                path.moveTo((float) pointList.get(0).x / scale, (float) pointList.get(0).y / scale);
                path.lineTo((float) pointList.get(1).x / scale, (float) pointList.get(1).y / scale);
                path.lineTo((float) pointList.get(2).x / scale, (float) pointList.get(2).y / scale);
                path.lineTo((float) pointList.get(3).x / scale, (float) pointList.get(3).y / scale);
                path.lineTo((float) pointList.get(0).x / scale, (float) pointList.get(0).y / scale);

                canvas.drawPath(path, paint);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!pointList.isEmpty()) {
                drawPoint(canvas, pointList);
            }
        }

        public void clear() {
            this.pointList.clear();
            postInvalidate();
        }
    }
}
