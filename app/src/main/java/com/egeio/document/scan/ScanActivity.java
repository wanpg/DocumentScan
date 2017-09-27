package com.egeio.document.scan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.egeio.opencv.SquaresTracker;
import com.egeio.opencv.Utils;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends Activity {

    private static final String TAG = ScanActivity.class.getSimpleName();

    private SurfaceView surfaceView;
    private ScanInfoView scanInfoView;
    private ImageView previewImageView;
    private int mPreviewFormat = ImageFormat.NV21;
    private Thread cameraThread;

    private Camera mCamera;

    byte[] mBuffer;

    boolean mCameraFrameReady = false;
    boolean mStopThread = false;

    private SquaresTracker squaresTracker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // 隐藏应用程序的标题栏，即当前activity的label
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 隐藏android系统的状态栏
        setContentView(R.layout.activity_scan);
        squaresTracker = new SquaresTracker();
        surfaceView = findViewById(R.id.surface_view);
        scanInfoView = findViewById(R.id.scan_info);
        previewImageView = findViewById(R.id.preview_info);
    }

    @Override
    protected void onPause() {
        super.onPause();
        previewImageView.setImageBitmap(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStopThread = true;
        recycleBitmap(cacheBitmap);
    }

    void recycleBitmap(Bitmap bitmap) {
        try {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setCamera(Camera.open());
        initFrame();
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(callback);
        cameraThread = new Thread(new CameraWorker());
        cameraThread.start();
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            startPreviewDisplay(surfaceHolder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            if (surfaceHolder.getSurface() == null) {
                return;
            }
            mCamera.setDisplayOrientation(Utils.getCameraOrientation(ScanActivity.this));
            Log.d(TAG, "Restart preview display[SURFACE-CHANGED]");
            stopPreviewDisplay();
            startPreviewDisplay(surfaceHolder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            synchronized (ScanActivity.this) {
                mCameraFrameReady = true;
            }
            camera.addCallbackBuffer(mBuffer);
        }
    };


    private Bitmap frameDataToBitmap(byte[] data, Camera camera) {
        ByteArrayOutputStream baos = null;
        //处理data
        Bitmap bitmap = null;
        try {
            Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
            YuvImage yuvimage = new YuvImage(data, mPreviewFormat, previewSize.width, previewSize.height, null);
            baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
            byte[] rawImage = baos.toByteArray();
            //将rawImage转换成bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        final Camera.Parameters params = mCamera.getParameters();
//        params.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
        mPreviewFormat = params.getPreviewFormat();
    }

    private void startPreviewDisplay(SurfaceHolder holder) {
        checkCamera();
        try {
            int size = mFrameWidth * mFrameHeight;
            size = size * ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat()) / 8;
            mBuffer = new byte[size];

            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.cancelAutoFocus();
        } catch (IOException e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
    }

    private void stopPreviewDisplay() {
        checkCamera();
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error while STOP preview for camera", e);
        }
    }

    private void checkCamera() {
        if (mCamera == null) {
            throw new IllegalStateException("Camera must be set when start/stop preview, call <setCamera(Camera)> to set");
        }
    }

    int mFrameWidth, mFrameHeight;

    private void initFrame() {
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size previewSize = parameters.getPreviewSize();
        int width = previewSize.width;
        int height = previewSize.height;

        mFrameWidth = Math.min(width, height);
        mFrameHeight = Math.max(width, height);
    }

    private class JavaCameraFrame implements CameraBridgeViewBase.CvCameraViewFrame {
        @Override
        public Mat gray() {
            if (mPreviewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(rgba(), mGray, Imgproc.COLOR_RGBA2GRAY);
            else if (mPreviewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(rgba(), mGray, Imgproc.COLOR_RGB2GRAY);
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");
            return mGray;
        }

        @Override
        public Mat rgba() {
            if (mPreviewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            else if (mPreviewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
            mGray = new Mat();
        }

        public void release() {
            mGray.release();
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private Mat mGray;
        private int mWidth;
        private int mHeight;
    }

    private Bitmap cacheBitmap = null;
    private final float scale = 0.25f;

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            while (!mStopThread) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 此处绘制
                if (!mStopThread) {
                    long start = System.currentTimeMillis();
                    // 检测区域
                    List<List<Point>> matList = new ArrayList<>();
                    Mat mat = null;
                    Mat mat1 = null;
                    Mat mat2 = null;
                    try {
                        mat = new Mat(mFrameWidth, mFrameHeight, CvType.CV_8UC1);
                        mat.put(0, 0, mBuffer);
                        if (!mat.empty()) {
                            mat1 = new Mat();
                            mat2 = new Mat();
                            Core.rotate(mat, mat1, Core.ROTATE_90_CLOCKWISE);
                            Imgproc.resize(mat1, mat2, new Size(), scale, scale, Imgproc.INTER_LINEAR);
                            Log.d(TAG, "耗时1:" + (System.currentTimeMillis() - start));
                            recycleBitmap(cacheBitmap);
                            cacheBitmap = Bitmap.createBitmap(mat2.width(), mat2.height(), Bitmap.Config.RGB_565);
                            org.opencv.android.Utils.matToBitmap(mat2, cacheBitmap);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    previewImageView.setImageBitmap(cacheBitmap);
                                }
                            });
                            Log.d(TAG, "耗时2:" + (System.currentTimeMillis() - start));
                            squaresTracker.findSquares(mat2, matList);
                            Log.d(TAG, "耗时3:" + (System.currentTimeMillis() - start));
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
            }
            Log.d(TAG, "Finish processing thread");
        }
    }
}
