package com.egeio.opencv;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * 照相机View，会将照相机按照比例fit给surfaceView
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = CameraView.class.getSimpleName();

    private Camera camera;
    private byte[] buffer;
    private Debug debug;
    private JavaCameraFrame javaCameraFrame;

    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        debug = new Debug(TAG);
        debug.setEnable(false);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        openCamera();
        initFrame();
        fitCameraView();
        startPreviewDisplay(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        openCamera();
        startPreviewDisplay(surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopPreviewDisplay();
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        synchronized (this) {
            if (javaCameraFrame != null) {
                javaCameraFrame.put(0, 0, buffer);
            }
        }
        camera.addCallbackBuffer(buffer);
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void release() {
    }

    private void initFrame() {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        if (javaCameraFrame != null) {
            javaCameraFrame.release();
        }
        javaCameraFrame = new JavaCameraFrame(new Mat(previewSize.height + (previewSize.height / 2), previewSize.width, CvType.CV_8UC1),
                previewSize.width, previewSize.height, camera.getParameters().getPreviewFormat());
    }

    private void openCamera() {
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
//          params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(params);
        }
        camera.setDisplayOrientation(Utils.getCameraOrientation(getContext()));
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void startPreviewDisplay(SurfaceHolder holder) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();
            int size = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
            buffer = new byte[size];
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            camera.cancelAutoFocus();
        } catch (Exception e) {
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

    public synchronized Mat getFrameMat(float scale) {
        Mat mat = null;
        try {
            debug.start("转换照相机数据");
            debug.end("转换照相机数据");
            if (!javaCameraFrame.isEmpty()) {
                if (scale == 1) {
                    mat = javaCameraFrame.rgba().clone();
                } else {
                    debug.start("缩放");
                    mat = new Mat();
                    Imgproc.resize(javaCameraFrame.rgba(), mat, new Size(), scale, scale, Imgproc.INTER_LINEAR);
                    debug.end("缩放");
                }
                debug.start("旋转");
                return rotateMat(mat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mat != null)
                mat.release();
        }
        return null;
    }

    private Mat rotateMat(Mat mat) {
        Mat matResult = null;
        int cameraOrientation = Utils.getCameraOrientation(getContext());
        if (cameraOrientation == 90) {
            matResult = new Mat();
            Core.rotate(mat, matResult, Core.ROTATE_90_CLOCKWISE);
        } else if (cameraOrientation == 180) {
            matResult = new Mat();
            Core.rotate(mat, matResult, Core.ROTATE_180);
        } else if (cameraOrientation == 270) {
            matResult = new Mat();
            Core.rotate(mat, matResult, Core.ROTATE_90_COUNTERCLOCKWISE);
        } else {
            matResult = mat.clone();
        }
        return matResult;
    }

    private float scaleRatio = 1f;

    public float getScaleRatio() {
        return scaleRatio;
    }

    private void fitCameraView() {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        if (previewSize != null) {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams != null) {
                float widthRatio = 1f * getWidth() / Math.min(previewSize.width, previewSize.height);
                float heightRatio = 1f * getHeight() / Math.max(previewSize.width, previewSize.height);
                // 以最小边放大绘制
                if (widthRatio < heightRatio) {
                    // 以高为基准绘制
                    scaleRatio = heightRatio;
                    layoutParams.width = (int) (heightRatio * Math.min(previewSize.width, previewSize.height));
                } else {
                    // 以宽为基准绘制
                    scaleRatio = widthRatio;
                    layoutParams.height = (int) (widthRatio * Math.max(previewSize.width, previewSize.height));
                }
                requestLayout();
            }
        }
    }

    private class JavaCameraFrame implements CameraBridgeViewBase.CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            if (previewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            else if (previewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height, int previewFormat) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
            this.previewFormat = previewFormat;
        }

        public void release() {
            if (mYuvFrameData != null) {
                mYuvFrameData.release();
            }
            if (mRgba != null) {
                mRgba.release();
            }
        }

        public int put(int row, int col, byte[] data) {
            if (mYuvFrameData != null) {
                return mYuvFrameData.put(row, col, data);
            }
            return -1;
        }

        public boolean isEmpty() {
            return mYuvFrameData == null || mYuvFrameData.empty();
        }

        private int previewFormat;

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    }
}
