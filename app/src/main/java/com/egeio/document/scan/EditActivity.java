package com.egeio.document.scan;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.egeio.opencv.edit.EditFragment;

/**
 * Created by wangjinpeng on 2017/10/11.
 */

public class EditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EditFragment editFragment = new EditFragment();
        editFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(Window.ID_ANDROID_CONTENT, editFragment)
                .commit();
    }
}
