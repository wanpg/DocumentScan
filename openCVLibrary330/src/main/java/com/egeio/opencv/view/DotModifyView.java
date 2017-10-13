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

public class DotModifyView extends View {

    public DotModifyView(Context context) {
        super(context);
    }

    public DotModifyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DotModifyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DotModifyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Bitmap bitmap;

}
