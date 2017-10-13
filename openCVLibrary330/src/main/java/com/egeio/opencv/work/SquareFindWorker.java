package com.egeio.opencv.work;

import android.util.Log;

import com.egeio.opencv.model.PointD;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.Debug;
import com.egeio.opencv.SquaresTracker;
import com.egeio.opencv.tools.Utils;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public abstract class SquareFindWorker extends Worker {

    private Debug debug = new Debug(SquareFindWorker.class.getSimpleName());
    private SquaresTracker squaresTracker;

    public abstract Mat getFrameMat();

    public abstract void onPointsFind(Size squareContainerSize, List<PointD> points);

    private float defaultScale;

    public SquareFindWorker(float defaultScale) {
        this.defaultScale = defaultScale;
        squaresTracker = new SquaresTracker();
    }

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
            Size size = null;
            try {
                debug.start("获取当前帧");
                frameMat = getFrameMat();
                size = frameMat.size();
                debug.start("获取当前帧");
                if (isWorkerStopped()) {
                    break;
                }
                if (frameMat != null && !frameMat.empty()) {
                    debug.start("寻找多边形");
                    matList.clear();
//                    squaresTracker.findSquares(frameMat, matList, defaultScale);
                    matList.addAll(squaresTracker.findLargestSquares(frameMat, defaultScale));
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
                List<Point> largestList = Utils.findLargestList(matList);
                onPointsFind(size, CvUtils.point2pointD(largestList));
            } else {
                onPointsFind(null, null);
            }
            Log.d(TAG, "获取到了几个点" + matList.size());
        }
        Log.d(TAG, "Finish processing thread");
    }
}