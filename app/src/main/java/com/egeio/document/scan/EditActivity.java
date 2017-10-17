package com.egeio.document.scan;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.ScanEditInterface;
import com.egeio.opencv.edit.DotModifyFragment;
import com.egeio.opencv.edit.EditFragment;
import com.egeio.opencv.model.ScanInfo;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class EditActivity extends AppCompatActivity implements ScanEditInterface {

    private ScanDataManager scanDataManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanDataManager = getIntent().getParcelableExtra("SCAN_DATA");
        toEditPreview(null);
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
    public int getScanSize() {
        return scanDataManager.getScanInfoArrayList().size();
    }

    @Override
    public void remove(int index) {
        scanDataManager.getScanInfoArrayList().remove(index);
    }
}
