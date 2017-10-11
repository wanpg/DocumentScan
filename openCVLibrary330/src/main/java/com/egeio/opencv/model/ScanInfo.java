package com.egeio.opencv.model;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.Serializable;

/**
 * Created by wangjinpeng on 2017/10/9.
 */

public class ScanInfo implements Serializable {

    public enum Angle {
        angle_0(0), angle_90(90), angle_180(180), angle_270(270);

        int value;

        public int getValue() {
            return value;
        }

        Angle(int value) {
            this.value = value;
        }

        public static Angle valueOf(int value) {
            switch (value) {
                case 90:
                    return angle_90;
                case 180:
                    return angle_180;
                case 270:
                    return angle_270;
                default:
                    return angle_0;
            }
        }
    }

    /**
     * 源文件文件路径，经过初始的旋转和裁剪
     */
    private final String path;

    /**
     * 原始检测的四个点
     */
    private final PointInfo originPointInfo;

    /**
     * 处理后的四个点
     */
    private final PointInfo currentPointInfo;

    /**
     * 初始角度
     */
    private final Angle originAngle;

    /**
     * 当前旋转角度
     */
    private Angle rotateAngle;

    /**
     * 是否经过优化，默认进行优化
     */
    private boolean isOptimized = true;


    public ScanInfo(String path, PointInfo pointInfo, int angle) {
        this.path = path;
        this.originPointInfo = new PointInfo(pointInfo);
        this.originAngle = Angle.valueOf(angle);
        rotateAngle = originAngle;
        currentPointInfo = new PointInfo(originPointInfo);
    }

    public PointInfo getCurrentPointInfo() {
        return currentPointInfo;
    }

    public String getPath() {
        return path;
    }

    public Angle getRotateAngle() {
        return rotateAngle;
    }

    public void setRotateAngle(Angle rotateAngle) {
        this.rotateAngle = rotateAngle;
    }

    public void setRotateAngle(int rotateAngle) {
        this.rotateAngle = Angle.valueOf(rotateAngle);
    }

    public boolean isOptimized() {
        return isOptimized;
    }

    public void setOptimized(boolean optimized) {
        isOptimized = optimized;
    }

    public boolean matchSize(int width, int height) {
        Mat mat = Converters.vector_Point_to_Mat(currentPointInfo.getPoints());
        double area = Imgproc.contourArea(mat);
        if (Math.abs(area - width * height) < 0.1d) {
            return true;
        }
        return false;
    }
}
