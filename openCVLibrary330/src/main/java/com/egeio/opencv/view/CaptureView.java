package com.egeio.opencv.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by wangjinpeng on 2017/10/13.
 */

public class CaptureView extends View {

    public CaptureView(Context context) {
        super(context);
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Bitmap bitmap;

}
