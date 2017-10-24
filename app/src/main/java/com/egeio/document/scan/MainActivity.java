package com.egeio.document.scan;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by wangjinpeng on 2017/10/24.
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void gotoScan(View view) {
        startActivity(new Intent(this, ScanActivity.class));
    }

    public void gotoPdf(View view) {
        startActivity(new Intent(this, PdfConvertActivity.class));
    }
}
