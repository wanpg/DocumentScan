package com.egeio.opencv;

import android.os.Parcel;
import android.os.Parcelable;

import com.egeio.opencv.model.ScanInfo;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class ScanDataManager implements Parcelable {

    private final ArrayList<ScanInfo> scanInfoArrayList;

    public ScanDataManager() {
        scanInfoArrayList = new ArrayList<>();
    }

    protected ScanDataManager(Parcel in) {
        scanInfoArrayList = in.createTypedArrayList(ScanInfo.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(scanInfoArrayList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScanDataManager> CREATOR = new Creator<ScanDataManager>() {
        @Override
        public ScanDataManager createFromParcel(Parcel in) {
            return new ScanDataManager(in);
        }

        @Override
        public ScanDataManager[] newArray(int size) {
            return new ScanDataManager[size];
        }
    };

    public void addScanInfo(ScanInfo scanInfo) {
        scanInfoArrayList.add(scanInfo);
    }

    public ArrayList<ScanInfo> getScanInfoArrayList() {
        return scanInfoArrayList;
    }
}
