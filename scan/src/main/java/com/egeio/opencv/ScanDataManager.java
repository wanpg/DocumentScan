package com.egeio.opencv;

import com.egeio.opencv.model.ScanInfo;
import com.egeio.scan.R;

import java.util.List;
import java.util.Observable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public final class ScanDataManager extends Observable implements ResReplacement {

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

    @Override
    public int getCancel() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getCancel() : R.string.cancel;
    }

    @Override
    public int getIdentifying() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getIdentifying() : R.string.identifying;
    }

    @Override
    public int getScanning() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getScanning() : R.string.scanning;
    }

    @Override
    public int getMaxPageTip() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getMaxPageTip() : R.string.max_page_tip;
    }

    @Override
    public int getCrop() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getCrop() : R.string.crop;
    }

    @Override
    public int getOptimize() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getOptimize() : R.string.optimize;
    }

    @Override
    public int getRestore() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getRestore() : R.string.restore;
    }

    @Override
    public int getRotate() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getRotate() : R.string.rotate;
    }

    @Override
    public int getDelete() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getDelete() : R.string.delete;
    }

    @Override
    public int getComplete() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getComplete() : R.string.complete;
    }

    @Override
    public int getEditOver() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getEditOver() : R.string.edit_over;
    }

    @Override
    public int getGenerating() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getGenerating() : R.string.generating;
    }

    @Override
    public int getPdfName() {
        return DocumentScan.resReplacement != null ? DocumentScan.resReplacement.getPdfName() : R.string.pdf_name;
    }
}
