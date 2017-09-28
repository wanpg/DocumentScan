package com.egeio.opencv;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by wangjinpeng on 2017/9/28.
 */

public class Debug {

    private final String tag;
    private boolean enable = true;

    public Debug(@NonNull String tag) {
        this.tag = tag;
    }

    private final HashMap<String, Long> cacheStartMap = new HashMap<>();

    public void start(String pointTag) {
        if (!enable) {
            return;
        }
        cacheStartMap.put(pointTag, System.currentTimeMillis());
    }

    public void end(String pointTag) {
        if (!enable) {
            return;
        }
        Long aLong = cacheStartMap.get(pointTag);
        if (aLong != null) {
            Log.d(tag, pointTag + "--cost spend time in mills : " + (System.currentTimeMillis() - aLong));
        }
    }

    public void clear() {
        cacheStartMap.clear();
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}