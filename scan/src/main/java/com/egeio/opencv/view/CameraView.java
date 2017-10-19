package com.egeio.opencv.view;

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

import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.tools.Utils;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 照相机View，会将照相机按照比例fit给surfaceView
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = CameraView.class.getSimpleName();

    private Camera camera;
    private byte[] buffer;
    private Debug debug;
    private JavaCameraFrame javaCameraFrame;
    private float scaleRatio = 1f;

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
        toggleFlash(false);
    }

    public void release() {
    }

    private void initFrame() {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        if (javaCameraFrame != null) {
            javaCameraFrame.release();
        }
        javaCameraFrame = new JavaCameraFrame(previewSize.width, previewSize.height, camera.getParameters().getPreviewFormat());
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                camera.enableShutterSound(true);
            }
            final Camera.Parameters params = camera.getParameters();
//          params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            Camera.Size bestPreviewSize = findBestPreviewSize(params);
            Camera.Size bestPictureSize = findBestPictureSize(params, bestPreviewSize);
            params.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
            params.setPictureSize(bestPictureSize.width, bestPictureSize.height);
            camera.setParameters(params);
        }
        camera.setDisplayOrientation(Utils.getCameraOrientation(getContext()));
    }

    private Camera.Size findBestPictureSize(Camera.Parameters parameters, Camera.Size previewSize) {
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        // 从大到小排列
        Collections.sort(supportedPictureSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                int area1 = o1.width * o1.height;
                int area2 = o2.width * o2.height;
                if (area1 < area2) {
                    return 1;
                } else if (area1 > area2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        float previewRatio = 1f * previewSize.width / previewSize.height;
        for (Camera.Size size : supportedPictureSizes) {
            float ratio = 1f * size.width / size.height;
            if (Math.abs(previewRatio - ratio) < 0.01f) {
                return size;
            }
        }
        return supportedPictureSizes.get(0);
    }

    private Camera.Size findBestPreviewSize(Camera.Parameters parameters) {
        float previewRatio = 1f * getWidth() / getHeight();
        int cameraOrientation = Utils.getCameraOrientation(getContext());
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        // 从大到小排列
        Collections.sort(previewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                int area1 = o1.width * o1.height;
                int area2 = o2.width * o2.height;
                if (area1 < area2) {
                    return 1;
                } else if (area1 > area2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        List<Camera.Size> matchList = new ArrayList<>();
        for (Camera.Size size : previewSizes) {
            int width = cameraOrientation == 90 || cameraOrientation == 270 ? size.height : size.width;
            int height = cameraOrientation == 90 || cameraOrientation == 270 ? size.width : size.height;
            float ratio = 1f * width / height;
            if (Math.abs(previewRatio - ratio) < 0.3f) {
                matchList.add(0, size);
            }
        }
        if (!matchList.isEmpty()) {
            for (Camera.Size size : matchList) {
                if (size.width * size.height > getWidth() * getHeight()) {
                    return size;
                }
            }
        }
        return previewSizes.get(0);
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public void startPreviewDisplay() {
        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();
            int size = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
            if (buffer == null || buffer.length != size) {
                buffer = new byte[size];
            }
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
            camera.cancelAutoFocus();
        } catch (Exception e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
    }

    private void startPreviewDisplay(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            startPreviewDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
    }

    public void stopPreviewDisplay() {
        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error while STOP preview for camera", e);
        }
    }

    public void toggleFlash() {
        toggleFlash(!isFlashOn());
    }

    public void toggleFlash(boolean on) {
        try {
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(on ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isFlashOn() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            String flashMode = parameters.getFlashMode();
            return !Camera.Parameters.FLASH_MODE_OFF.equals(flashMode);
        }
        return false;
    }

    public synchronized Mat getFrameMat(float scale) {
        Mat mat = null;
        debug.start("转换照相机数据");
        if (javaCameraFrame != null && !javaCameraFrame.isEmpty()) {
            if (scale == 1) {
                mat = javaCameraFrame.rgba().clone();
            } else {
                debug.start("缩放");
                mat = new Mat();
                Imgproc.resize(javaCameraFrame.rgba(), mat, new Size(), scale, scale, Imgproc.INTER_LINEAR);
                debug.end("缩放");
            }
        }
        debug.end("转换照相机数据");
        return mat;
    }

    public Size getPreviewSize() {
        final Camera.Parameters parameters = camera.getParameters();
        final Camera.Size previewSize = parameters.getPreviewSize();
        return new Size(previewSize.width, previewSize.height);
    }

    public float getScaleRatio() {
        return scaleRatio;
    }

    private void fitCameraView() {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (previewSize != null && layoutParams != null) {
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

    private boolean isWaitForPhoto = false;

    public synchronized void takePhoto(final Camera.PictureCallback pictureCallback) {
        synchronized (this) {
            if (camera == null || isWaitForPhoto) {
                return;
            }
            isWaitForPhoto = true;
        }
        camera.takePicture(
                new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                },
                null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        synchronized (CameraView.this) {
                            isWaitForPhoto = false;
                        }
                        if (pictureCallback != null) {
                            pictureCallback.onPictureTaken(data, camera);
                        }
                    }
                });
    }

    private static class JavaCameraFrame implements CameraBridgeViewBase.CvCameraViewFrame {
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

        public JavaCameraFrame(int width, int height, int previewFormat) {
            this(new Mat(height + height / 2, width, CvType.CV_8UC1), width, height, previewFormat);
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height, int previewFormat) {
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

        private Mat mYuvFrameData;

        private Mat mRgba;
        private int mWidth;
        private int mHeight;
        private int previewFormat;
    }
}
