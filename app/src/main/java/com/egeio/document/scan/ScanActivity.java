package com.egeio.document.scan;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.egeio.opencv.BaseScanFragment;
import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.ScanEditInterface;
import com.egeio.opencv.ScanFragment;
import com.egeio.opencv.ScanManagerInterface;
import com.egeio.opencv.edit.DotModifyFragment;
import com.egeio.opencv.edit.EditFragment;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Utils;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity implements ScanManagerInterface, ScanEditInterface {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // 隐藏应用程序的标题栏，即当前activity的label
        if (savedInstanceState != null) {
            Utils.clearFolder(Utils.getSaveFolder(this));
        }
        toCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.hideSystemUI(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.clearFolder(Utils.getSaveFolder(this));
    }

    private ScanDataManager scanDataManager = new ScanDataManager();

    @Override
    public ScanDataManager getManager() {
        return scanDataManager;
    }

    @Override
    public void toCamera() {
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, new ScanFragment())
                .commit();
    }

    @Override
    public void toDotModify(ScanInfo scanInfo) {
        final ArrayList<ScanInfo> scanInfoArrayList = scanDataManager.getScanInfoArrayList();
        int index = -1;
        if (scanInfo != null) {
            index = scanInfoArrayList.indexOf(scanInfo);
        }
        if (index < 0) {
            index = scanInfoArrayList.size() - 1;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, DotModifyFragment.createInstance(index))
                .commit();
    }

    @Override
    public void toEditPreview(ScanInfo scanInfo) {
        final ArrayList<ScanInfo> scanInfoArrayList = scanDataManager.getScanInfoArrayList();
        int index = -1;
        if (scanInfo != null) {
            index = scanInfoArrayList.indexOf(scanInfo);
        }
        if (index < 0) {
            index = scanInfoArrayList.size() - 1;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, EditFragment.createInstance(index))
                .commit();
    }

    @Override
    public ScanInfo getScanInfo(int index) {
        return scanDataManager.getScanInfoArrayList().get(index);
    }

    @Override
    public int indexOfScanInfo(ScanInfo info) {
        return scanDataManager.getScanInfoArrayList().indexOf(info);
    }

    @Override
    public int getScanInfoSize() {
        return scanDataManager.getScanInfoArrayList().size();
    }

    @Override
    public void remove(int index) {
        try {
            scanDataManager.getScanInfoArrayList().remove(index);
        } catch (Exception ignored) {
        }
        if (scanDataManager.getScanInfoArrayList().isEmpty()) {
            onBackPressed();
        }
    }

    @Override
    public void add(ScanInfo scanInfo) {
        scanDataManager.getScanInfoArrayList().add(scanInfo);
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(Window.ID_ANDROID_CONTENT);
        if (fragment instanceof BaseScanFragment && ((BaseScanFragment) fragment).onBackPressed()) {
            return;
        } else {
            super.onBackPressed();
        }
    }
}
