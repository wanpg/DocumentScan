package com.egeio.opencv.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.egeio.opencv.tools.CvUtils;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/9.
 */

public class PointInfo implements Parcelable {

    private final ArrayList<PointD> points;
    private final long time;

    public PointInfo(PointInfo pointInfo) {
        this(pointInfo.getPoints(), pointInfo.getTime());
    }

    public PointInfo(List<PointD> points) {
        this(points, System.currentTimeMillis());
    }

    public PointInfo(List<PointD> points, long time) {
        this.points = new ArrayList<>();
        if (points != null) {
            this.points.addAll(points);
        }
        this.time = time;
    }

    protected PointInfo(Parcel in) {
        points = in.createTypedArrayList(PointD.CREATOR);
        time = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(points);
        dest.writeLong(time);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PointInfo> CREATOR = new Creator<PointInfo>() {
        @Override
        public PointInfo createFromParcel(Parcel in) {
            return new PointInfo(in);
        }

        @Override
        public PointInfo[] newArray(int size) {
            return new PointInfo[size];
        }
    };

    public ArrayList<PointD> getPoints() {
        return points;
    }

    public long getTime() {
        return time;
    }
}
