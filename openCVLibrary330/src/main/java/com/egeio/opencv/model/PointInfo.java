package com.egeio.opencv.model;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/9.
 */

public class PointInfo {

    private final ArrayList<Point> points;
    private final long time;

    public PointInfo(PointInfo pointInfo) {
        this(pointInfo.getPoints(), pointInfo.getTime());
    }

    public PointInfo(List<Point> points) {
        this(points, System.currentTimeMillis());
    }

    public PointInfo(List<Point> points, long time) {
        this.points = new ArrayList<>();
        if (points != null) {
            this.points.addAll(points);
        }
        this.time = time;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    public long getTime() {
        return time;
    }
}
