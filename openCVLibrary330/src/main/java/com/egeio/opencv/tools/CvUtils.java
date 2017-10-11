package com.egeio.opencv.tools;

import com.egeio.opencv.model.PointD;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class CvUtils {

    public static Mat pointToMat(List<PointD> pointDS) {
        return Converters.vector_Point_to_Mat(pointD2point(pointDS));
    }

    public static List<Point> pointD2point(List<PointD> pointDS) {
        if (pointDS == null)
            return null;
        List<Point> pointList = new ArrayList<>();
        for (PointD pointD : pointDS) {
            pointList.add(pointD2point(pointD));
        }
        return pointList;
    }

    public static Point pointD2point(PointD pointD) {
        return new Point(pointD.x, pointD.y);
    }

    public static List<PointD> point2pointD(List<Point> points) {
        if (points == null)
            return null;
        List<PointD> pointDList = new ArrayList<>();
        for (Point point : points) {
            pointDList.add(point2pointD(point));
        }
        return pointDList;
    }

    public static PointD point2pointD(Point point) {
        return new PointD(point.x, point.y);
    }
}
