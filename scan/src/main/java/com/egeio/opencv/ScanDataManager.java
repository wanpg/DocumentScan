package com.egeio.opencv;

import com.egeio.opencv.model.ScanInfo;

import java.util.List;
import java.util.Observable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class ScanDataManager extends Observable {

    private final CopyOnWriteArrayList<ScanInfo> scanInfoArrayList;

    public ScanDataManager() {
        scanInfoArrayList = new CopyOnWriteArrayList<>();
    }

    public List<ScanInfo> getAll() {
        return scanInfoArrayList;
    }

    public ScanInfo getScanInfo(int index) {
        return scanInfoArrayList.get(index);
    }

    public int indexOfScanInfo(ScanInfo info) {
        return scanInfoArrayList.indexOf(info);
    }

    public int getScanInfoSize() {
        return scanInfoArrayList.size();
    }

    public void remove(int index) {
        scanInfoArrayList.remove(index);
        setChanged();
        notifyObservers();
    }

    public void add(ScanInfo scanInfo) {
        scanInfoArrayList.add(scanInfo);
        setChanged();
        notifyObservers();
    }
}
