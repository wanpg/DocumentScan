package com.egeio.opencv.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.egeio.opencv.ScanDataInterface;
import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.model.PointD;
import com.egeio.opencv.model.PointInfo;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.CvUtils;
import com.egeio.opencv.tools.SysUtils;
import com.egeio.opencv.tools.SystemConfig;
import com.egeio.opencv.view.DotModifyView;
import com.egeio.opencv.view.DotZoomView;
import com.egeio.opencv.view.PreviewImageView;
import com.egeio.opencv.work.Worker;
import com.egeio.scan.R;

import org.opencv.core.Size;

import java.util.List;

/**
 * Created by wangjinpeng on 2017/10/16.
 */

public class DotModifyFragment extends BaseScanFragment {

    public static Fragment createInstance(int currentIndex) {
        DotModifyFragment fragment = new DotModifyFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("INDEX", currentIndex);
        fragment.setArguments(bundle);
        return fragment;
    }

    private View mContainer;
    private DotModifyView dotModifyView;
    private PreviewImageView imagePreviewView;
    private ScanInfo scanInfo;
    private ScanDataInterface scanDataInterface;
    private ScanDataManager scanDataManager;


    private DotZoomView dotZoomView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int index = getArguments().getInt("INDEX");
        scanDataInterface = (ScanDataInterface) getActivity();
        scanDataManager = scanDataInterface.getScanDataManager();
        scanInfo = scanDataManager.getScanInfo(index);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_dot_modify, null);
            dotModifyView = mContainer.findViewById(R.id.dot_modify);
            imagePreviewView = mContainer.findViewById(R.id.image_preview);
            dotZoomView = new DotZoomView(inflater.getContext());
            ((ViewGroup) getActivity().getWindow().getDecorView()).addView(dotZoomView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            }
            SystemConfig systemConfig = new SystemConfig(getActivity());
            final int statusBarHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? systemConfig.getStatusBarHeight() : 0;
            final int navBarHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && systemConfig.getNavigationBarPosition() == 1 ? systemConfig.getNavigationBarHeight() : 0;
            dotZoomView.setPadding(
                    dotZoomView.getPaddingLeft(),
                    dotZoomView.getPaddingTop() + statusBarHeight + inflater.getContext().getResources().getDimensionPixelOffset(R.dimen.action_bar_height),
                    dotZoomView.getPaddingRight(),
                    dotZoomView.getPaddingBottom() + navBarHeight);
            TextView textCancel = mContainer.findViewById(R.id.cancel);
            textCancel.setText(scanDataManager.getCancel());
            textCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            TextView textComplete = mContainer.findViewById(R.id.complete);
            textComplete.setText(scanDataManager.getComplete());
            textComplete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final List<PointD> modifiedPoints = dotModifyView.getModifiedPoints();
                    scanInfo.setCurrentPointInfo(new PointInfo(modifiedPoints));
                    scanDataInterface.toEditPreview(scanInfo);
                }
            });
            dotModifyView.setOnDotChangeListener(new DotModifyView.OnDotChangeListener() {
                @Override
                public void onDotChange(PointD dotPointD, List<PointD> pointDList) {
                    dotZoomView.drawDot(dotPointD, pointDList);
                }
            });
        }
        return mContainer;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dotZoomView != null) {
            ((ViewGroup) getActivity().getWindow().getDecorView()).removeView(dotZoomView);
        }
    }

    private Worker imageLoadWorker;
    private boolean isFirst = true;
    private Bitmap cachedBitmap;

    @Override
    public void onResume() {
        super.onResume();
        SysUtils.showSystemUI(getActivity());
        if (isFirst) {
            new Thread(imageLoadWorker = new Worker() {
                @Override
                public void doWork() {
                    try {
                        while (true) {
                            if (dotModifyView.getWidth() > 0 && dotModifyView.getHeight() > 0) {
                                break;
                            }
                            assertWorkStopped();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        assertWorkStopped();
                        int imageViewMaxSide = Math.max(dotModifyView.getWidth(), dotModifyView.getHeight());
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        final Size originSize = scanInfo.getOriginSize();
                        options.inSampleSize = Math.max(2, CvUtils.calculateInSampleSize((int) originSize.width, (int) originSize.height, imageViewMaxSide, imageViewMaxSide));
                        cachedBitmap = BitmapFactory.decodeFile(scanInfo.getPath(), options);
                        // imageView绘制
                        imagePreviewView.setBitmap(cachedBitmap);
                        imagePreviewView.setRotateAngle(scanInfo.getRotateAngle().getValue());
                        imagePreviewView.postInvalidate();
                        // 绘制点
                        dotModifyView.setScanInfo(scanInfo);
                        // 缩放区域
                        dotZoomView.setScanInfoAndBitmap(scanInfo, cachedBitmap);
                    } catch (Exception e) {
                        if (e instanceof Worker.WorkStoppedException) {
                            // 此处回收无用的临时资源
                        }
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        isFirst = false;
    }

    @Override
    public boolean onBackPressed() {
        scanDataInterface.toEditPreview(scanInfo);
        return true;
    }
}
