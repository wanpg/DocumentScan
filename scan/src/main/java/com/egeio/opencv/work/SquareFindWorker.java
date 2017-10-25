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
    private long lastFindTime;

    public abstract boolean enableToFind();

    public abstract Mat getFrameMat();

    public abstract void onPointsFind(Size squareContainerSize, List<PointD> points);

    private final Object lockObject;

    public SquareFindWorker(Object lockObject) {
        this.lockObject = lockObject;
        squaresTracker = new SquaresTracker();
        lastFindTime = System.currentTimeMillis();
    }

    @Override
    public void doWork() {
        while (true) {
            synchronized (lockObject) {
                try {
                    while (!enableToFind() && !isWorkerStopped()) {
                        lockObject.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            long lastFindDis = System.currentTimeMillis() - lastFindTime;
            if (lastFindDis < 100) {
                try {
                    Thread.sleep(100 - lastFindDis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lastFindTime = System.currentTimeMillis();
            try {
                assertWorkStopped();
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
                    assertWorkStopped();
                    if (frameMat != null && !frameMat.empty()) {
                        debug.start("寻找多边形");
                        matList.clear();
//                        matList.addAll(squaresTracker.findLargestSquares(frameMat, defaultScale));
                        matList.addAll(squaresTracker.findLargestSquares1(frameMat));
//                        squaresTracker.findLargestSquares2(frameMat, matList, defaultScale);
                        debug.end("寻找多边形");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (frameMat != null) {
                        frameMat.release();
                    }
                }
                assertWorkStopped();
                if (matList.size() > 0) {
                    List<Point> largestList = Utils.findLargestList(matList);
                    onPointsFind(size, CvUtils.point2pointD(largestList));
                } else {
                    onPointsFind(null, null);
                }
                Log.d(TAG, "获取到了几个点" + matList.size());
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof WorkStoppedException) {
                    // FIXME: 2017/10/18 此处保证数据回收
                    break;
                }
            }
        }
    }
}