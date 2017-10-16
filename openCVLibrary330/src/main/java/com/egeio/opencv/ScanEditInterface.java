package com.egeio.opencv;

import com.egeio.opencv.model.ScanInfo;

/**
 * Created by wangjinpeng on 2017/10/16.
 */

public interface ScanEditInterface {

    void toDotModify(ScanInfo scanInfo);

    void toEditPreview(ScanInfo scanInfo);
}
