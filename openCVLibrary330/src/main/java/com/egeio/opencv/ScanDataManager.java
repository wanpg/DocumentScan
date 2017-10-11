package com.egeio.opencv;

import com.egeio.opencv.model.ScanInfo;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class ScanDataManager {

    private final ArrayList<ScanInfo> scanInfoArrayList = new ArrayList<>();

    public ScanDataManager() {
    }

    public void addScanInfo(ScanInfo scanInfo) {
        scanInfoArrayList.add(scanInfo);
    }

    public ArrayList<ScanInfo> getScanInfoArrayList() {
        return scanInfoArrayList;
    }
}
