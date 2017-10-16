package com.egeio.document.scan;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.egeio.opencv.ScanEditInterface;
import com.egeio.opencv.edit.DotModifyFragment;
import com.egeio.opencv.edit.EditFragment;
import com.egeio.opencv.model.ScanInfo;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class EditActivity extends AppCompatActivity implements ScanEditInterface {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toEditPreview(null);
    }

    @Override
    public void toDotModify(ScanInfo scanInfo) {
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, DotModifyFragment.createinstance(scanInfo))
                .commit();
    }

    @Override
    public void toEditPreview(ScanInfo scanInfo) {
        EditFragment editFragment = new EditFragment();
        editFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, editFragment)
                .commit();
    }
}
