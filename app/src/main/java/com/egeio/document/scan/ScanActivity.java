package com.egeio.document.scan;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.ScanFragment;
import com.egeio.opencv.ScanManagerInterface;
import com.egeio.opencv.tools.Utils;

public class ScanActivity extends AppCompatActivity implements ScanManagerInterface {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // 隐藏应用程序的标题栏，即当前activity的label
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 隐藏android系统的状态栏
        if (savedInstanceState != null) {
            Utils.clearFolder(Utils.getSaveFolder(this));
        }
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, new ScanFragment())
                .commit();
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

    @Override
    public void gotoEdit() {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("SCAN_DATA", scanDataManager);
        startActivity(intent);
    }

    ScanDataManager scanDataManager = new ScanDataManager();

    @Override
    public ScanDataManager getManager() {
        return scanDataManager;
    }
}
