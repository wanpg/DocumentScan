package com.egeio.opencv.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.egeio.opencv.tools.CvUtils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by wangjinpeng on 2017/10/9.
 */

public class ScanInfo implements Parcelable {

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


    private final int originWidth, originHeight;

    /**
     * 原始检测的四个点
     */
    private final PointInfo originPointInfo;

    /**
     * 处理后的四个点
     */
    private PointInfo currentPointInfo;

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


    public ScanInfo(String path, PointInfo pointInfo, int angle, int originWidth, int originHeight) {
        this.path = path;
        this.originPointInfo = new PointInfo(pointInfo);
        this.originAngle = Angle.valueOf(angle);
        this.originWidth = originWidth;
        this.originHeight = originHeight;
        rotateAngle = originAngle;
        currentPointInfo = new PointInfo(originPointInfo);
    }

    protected ScanInfo(Parcel in) {
        path = in.readString();
        originPointInfo = in.readParcelable(PointInfo.class.getClassLoader());
        currentPointInfo = in.readParcelable(PointInfo.class.getClassLoader());
        isOptimized = in.readByte() != 0;
        originAngle = Angle.valueOf(in.readInt());
        rotateAngle = Angle.valueOf(in.readInt());
        originWidth = in.readInt();
        originHeight = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeParcelable(originPointInfo, flags);
        dest.writeParcelable(currentPointInfo, flags);
        dest.writeByte((byte) (isOptimized ? 1 : 0));
        dest.writeInt(originAngle.value);
        dest.writeInt(rotateAngle.value);
        dest.writeInt(originWidth);
        dest.writeInt(originHeight);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScanInfo> CREATOR = new Creator<ScanInfo>() {
        @Override
        public ScanInfo createFromParcel(Parcel in) {
            return new ScanInfo(in);
        }

        @Override
        public ScanInfo[] newArray(int size) {
            return new ScanInfo[size];
        }
    };

    public PointInfo getCurrentPointInfo() {
        return currentPointInfo;
    }

    public void setCurrentPointInfo(PointInfo currentPointInfo) {
        this.currentPointInfo = currentPointInfo;
    }

    public String getPath() {
        return path;
    }

    public Angle getRotateAngle() {
        return rotateAngle;
    }

    public void setRotateAngle(int value) {
        this.rotateAngle = Angle.valueOf(value);
    }

    public boolean isOptimized() {
        return isOptimized;
    }

    public void setOptimized(boolean optimized) {
        isOptimized = optimized;
    }

    public boolean matchSize() {
        Mat mat = CvUtils.pointToMat(currentPointInfo.getPoints());
        double area = Imgproc.contourArea(mat);
        if (Math.abs(area - originWidth * originHeight) < 0.1d) {
            return true;
        }
        return false;
    }

    /**
     * 获取path存储的图片的宽高
     *
     * @return
     */
    public Size getOriginSize() {
        return new Size(originWidth, originHeight);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ScanInfo && ((ScanInfo) obj).getPath().equals(path);
    }
}
