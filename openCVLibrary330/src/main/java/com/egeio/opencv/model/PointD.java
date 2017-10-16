package com.egeio.opencv.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * double 类型的point
 */

public class PointD implements Parcelable, Cloneable {

    public double x;
    public double y;

    public PointD() {
        this(0, 0);
    }

    public PointD(double x, double y) {
        this.x = x;
        this.y = y;
    }

    protected PointD(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(x);
        dest.writeDouble(y);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PointD> CREATOR = new Creator<PointD>() {
        @Override
        public PointD createFromParcel(Parcel in) {
            return new PointD(in);
        }

        @Override
        public PointD[] newArray(int size) {
            return new PointD[size];
        }
    };

    @Override
    public PointD clone() {
        PointD clone = null;
        try {
            clone = (PointD) super.clone();
        } catch (Exception ignored) {
        }
        if (clone == null) {
            return new PointD(x, y);
        }
        return clone;
    }
}
