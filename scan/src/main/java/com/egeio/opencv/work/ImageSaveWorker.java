package com.egeio.opencv.work;

import android.content.Context;
import android.hardware.Camera;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Utils;

import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

public abstract class ImageSaveWorker extends Worker {

    private Context context;
    private byte[] imageBuffer;
    private Camera camera;
    private PointInfo pointInfo;
    private float squareFindScale;

    /**
     * @param context
     * @param imageBuffer
     * @param camera
     * @param pointInfo
     * @param squareFindScale
     */
    public ImageSaveWorker(Context context,
                           byte[] imageBuffer,
                           Camera camera,
                           PointInfo pointInfo,
                           float squareFindScale) {
        this.context = context;
        this.imageBuffer = imageBuffer;
        this.camera = camera;
        this.pointInfo = pointInfo;
        this.squareFindScale = squareFindScale;
    }

    public abstract void onImageCropPreview(ScanInfo scanInfo);

    public abstract void onImageSaved(ScanInfo scanInfo);

    @Override
    public void doWork() {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size previewSize = parameters.getPreviewSize();
        Camera.Size pictureSize = parameters.getPictureSize();
        int cameraOrientation = Utils.getCameraOrientation(context);

        // 此处根据比例对point做一次位置偏移
        Size tempSize = new Size();
        double scale = Utils.calApproximateSize(
                new Size(pictureSize.width, pictureSize.height),
                new Size(previewSize.width, previewSize.height),
                tempSize);

        List<PointD> points = new ArrayList<>();
        PointInfo pointInfoTemp;
        if (pointInfo == null) {
            points.add(new PointD(0, 0));
            points.add(new PointD(pictureSize.width, 0));
            points.add(new PointD(pictureSize.width, pictureSize.height));
            points.add(new PointD(0, pictureSize.height));
            pointInfoTemp = new PointInfo(points);
        } else {
            for (PointD point : pointInfo.getPoints()) {
                points.add(new PointD(point.x / squareFindScale * scale, point.y / squareFindScale * scale));
            }
            pointInfoTemp = new PointInfo(points, pointInfo.getTime());
        }
        String savePath = Utils.getPictureFilePath(context);

        ScanInfo scanInfo = new ScanInfo(savePath, pointInfoTemp, cameraOrientation, pictureSize.width, pictureSize.height);
        onImageCropPreview(scanInfo);
        Utils.saveBufferToFile(imageBuffer, savePath);
        onImageSaved(scanInfo);
    }
}