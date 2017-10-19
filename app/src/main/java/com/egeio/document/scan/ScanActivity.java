package com.egeio.document.scan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.webkit.MimeTypeMap;

import com.egeio.opencv.DocumentScan;
import com.egeio.opencv.ScanDataInterface;
import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.fragment.BaseScanFragment;
import com.egeio.opencv.fragment.DotModifyFragment;
import com.egeio.opencv.fragment.EditFragment;
import com.egeio.opencv.fragment.ScanFragment;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Utils;

import java.io.File;
import java.util.List;

public class ScanActivity extends AppCompatActivity implements ScanDataInterface {

    private ScanDataManager scanDataManager = new ScanDataManager();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // 隐藏应用程序的标题栏，即当前activity的label
        DocumentScan.init(this, getExternalCacheDir().getAbsolutePath());
        if (savedInstanceState != null) {
            Utils.clearFolder(Utils.getPictureFolder(this));
        }
        toCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.clearFolder(Utils.getPictureFolder(this));
    }

    @Override
    public void toCamera() {
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, new ScanFragment())
                .commit();
    }

    @Override
    public void toDotModify(ScanInfo scanInfo) {
        final List<ScanInfo> scanInfoArrayList = scanDataManager.getAll();
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
        final List<ScanInfo> scanInfoArrayList = scanDataManager.getAll();
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
    public void onPdfGenerated(String savePath) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(savePath));
        final File file = new File(savePath);
        Uri uri = FileProvider.getUriForFile(this, "com.egeio.document.scan.fileprovider", file);    //第二个参数是manifest中定义的`authorities`
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_TITLE, file.getName());
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);    //这一步很重要。给目标应用一个临时的授权。
        startActivity(intent);    //或者其它最终处理方式
    }

    @Override
    public ScanDataManager getScanDataManager() {
        return scanDataManager;
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
