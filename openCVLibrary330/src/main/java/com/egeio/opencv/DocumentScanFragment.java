package com.egeio.opencv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/9/27.
 */

public class DocumentScanFragment extends Fragment {

    private static final String TAG = DocumentScanFragment.class.getSimpleName();

    private FrameLayout frameLayout;
    private SurfaceView surfaceView;
    private ScanInfoView scanInfoView;
    private Camera camera;
    byte[] mBuffer;
    private SquaresTracker squaresTracker;
    private final float scale = 0.25f;

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
            surfaceView = new SurfaceView(context);
            frameLayout.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            scanInfoView = new ScanInfoView(context);
            frameLayout.addView(scanInfoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            SurfaceHolder holder = surfaceView.getHolder();
            holder.addCallback(callback);
        }
        return frameLayout;
    }

    CameraWorker cameraWorker;

    @Override
    public void onResume() {
        super.onResume();
        if (cameraWorker != null) {
            cameraWorker.stopWork();
        }
        new Thread(cameraWorker = new CameraWorker()).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraWorker != null) {
            cameraWorker.stopWork();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera = null;
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            openCamera();
            initFrame(camera);
            startPreviewDisplay(surfaceHolder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            openCamera();
            initFrame(camera);
            stopPreviewDisplay();
            startPreviewDisplay(surfaceHolder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            stopPreviewDisplay();
            releaseCamera();
        }
    };


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            camera.addCallbackBuffer(mBuffer);
        }
    };

    public void openCamera() {
        if (camera != null) {
            try {
                camera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
                camera = null;
            }
        }
        if (camera == null) {
            camera = Camera.open();
            final Camera.Parameters params = camera.getParameters();
//        params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(params);
        }

        camera.setDisplayOrientation(Utils.getCameraOrientation(getActivity()));
    }

    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void startPreviewDisplay(SurfaceHolder holder) {
        try {
            int size = frameWidth * frameHeight;
            size = size * ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat()) / 8;
            mBuffer = new byte[size];

            camera.addCallbackBuffer(mBuffer);
            camera.setPreviewCallbackWithBuffer(previewCallback);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            camera.cancelAutoFocus();
        } catch (IOException e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
    }

    private void stopPreviewDisplay() {
        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error while STOP preview for camera", e);
        }
    }

    private int frameWidth, frameHeight;

    private void initFrame(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size previewSize = parameters.getPreviewSize();
        int width = previewSize.width;
        int height = previewSize.height;

        // FIXME: 2017/9/27 此处要根据屏幕角度和摄像头角度进行设置
        frameWidth = Math.min(width, height);
        frameHeight = Math.max(width, height);
    }

    private class CameraWorker implements Runnable {

        boolean isWorkerStopped = false;

        public synchronized void stopWork() {
            isWorkerStopped = true;
        }

        @Override
        public void run() {
            while (true) {
                if (isWorkerStopped) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isWorkerStopped) {
                    break;
                }
                // 此处绘制
                long start = System.currentTimeMillis();
                // 检测区域
                List<List<Point>> matList = new ArrayList<>();
                Mat mat = null;
                Mat mat1 = null;
                Mat mat2 = null;
                try {
                    mat = new Mat(frameWidth, frameHeight, CvType.CV_8UC1);
                    mat.put(0, 0, mBuffer);
                    if (!mat.empty()) {
                        mat1 = new Mat();
                        mat2 = new Mat();
                        // FIXME: 2017/9/27 此处旋转需要按照相机角度
                        Core.rotate(mat, mat1, Core.ROTATE_90_CLOCKWISE);
                        Imgproc.resize(mat1, mat2, new Size(), scale, scale, Imgproc.INTER_LINEAR);
                        if (isWorkerStopped) {
                            break;
                        }
                        Log.d(TAG, "耗时1:" + (System.currentTimeMillis() - start));
                        squaresTracker.findSquares(mat2, matList);
                        Log.d(TAG, "耗时2:" + (System.currentTimeMillis() - start));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (mat2 != null)
                        mat2.release();
                    if (mat1 != null)
                        mat1.release();
                    if (mat != null)
                        mat.release();
                }
                if (isWorkerStopped) {
                    break;
                }
                Log.d(TAG, "获取到了几个点" + matList.size());
                if (matList.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("获取点的坐标");
                    for (int i = 0; i < matList.size(); i++) {
                        List<Point> pointList = matList.get(i);
                        stringBuilder.append("第").append(i).append("组：");
                        for (Point point : pointList) {
                            stringBuilder.append(point.toString());
                        }
                        stringBuilder.append("====");
                    }
                    Log.d(TAG, stringBuilder.toString());
                    scanInfoView.setPoint(Utils.findLargestList(matList), scale);
                } else {
                    scanInfoView.clear();
                }
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
