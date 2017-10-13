package com.egeio.opencv.tools;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;

/**
 * Created by wangjinpeng on 2017/10/13.
 */

public class AnimationUtils {


    public static Animator alpha(View view, float startAlpha, float endAlpha) {
        return ObjectAnimator.ofFloat(view, "alpha", startAlpha, endAlpha);
    }

}
