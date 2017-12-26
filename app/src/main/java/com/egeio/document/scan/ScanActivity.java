package com.egeio.document.scan;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.egeio.opencv.DocumentScan;
import com.egeio.opencv.ResReplacement;
import com.egeio.opencv.ScanDataInterface;
import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.fragment.BaseScanFragment;
import com.egeio.opencv.fragment.DotModifyFragment;
import com.egeio.opencv.fragment.EditFragment;
import com.egeio.opencv.fragment.ScanFragment;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.Utils;

import java.util.List;

public class ScanActivity extends AppCompatActivity implements ScanDataInterface {

    private ScanDataManager scanDataManager = new ScanDataManager();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // 隐藏应用程序的标题栏，即当前activity的label
        DocumentScan.init(this, getExternalCacheDir().getAbsolutePath(), 10, new ResReplacement() {
            @Override
            public int getCancel() {
                return R.string.cancel;
            }

            @Override
            public int getIdentifying() {
                return R.string.identifying;
            }

            @Override
            public int getScanning() {
                return R.string.scanning;
            }

            @Override
            public int getMaxPageTip() {
                return R.string.max_page_tip;
            }

            @Override
            public int getCrop() {
                return R.string.crop;
            }

            @Override
            public int getOptimize() {
                return R.string.optimize;
            }

            @Override
            public int getRestore() {
                return R.string.restore;
            }

            @Override
            public int getRotate() {
                return R.string.rotate;
            }

            @Override
            public int getDelete() {
                return R.string.delete;
            }

            @Override
            public int getComplete() {
                return R.string.complete;
            }

            @Override
            public int getEditOver() {
                return R.string.edit_over;
            }

            @Override
            public int getGenerating() {
                return R.string.generating;
            }

            @Override
            public int getPdfName() {
                return R.string.pdf_name;
            }
        });
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
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out, R.anim.slide_left_in, R.anim.slide_right_out)
                .replace(Window.ID_ANDROID_CONTENT, ScanResultFragment.instance(savePath))
                .addToBackStack(null)
                .commit();
    }


    @Override
    public ScanDataManager getScanDataManager() {
        return scanDataManager;
    }

    AlertDialog alertDialog;

    @Override
    public void showLoading(boolean shown, String msg) {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        alertDialog = null;
        if (shown) {
            alertDialog = new AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setCancelable(false)
                    .create();
            alertDialog.show();
        }
    }

    @Override
    public void onCameraException(Exception e) {

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
