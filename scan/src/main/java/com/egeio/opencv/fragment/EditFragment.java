package com.egeio.opencv.fragment;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.egeio.opencv.ScanDataInterface;
import com.egeio.opencv.ScanDataManager;
import com.egeio.opencv.model.ScanInfo;
import com.egeio.opencv.tools.SysUtils;
import com.egeio.opencv.view.FragmentPagerAdapter;
import com.egeio.opencv.view.LoadingInfoHolder;
import com.egeio.opencv.work.GeneratePdfWorker;
import com.egeio.scan.R;

import java.util.Locale;

/**
 * Created by wangjinpeng on 2017/9/30.
 */

public class EditFragment extends BaseScanFragment {

    public static Fragment createInstance(int index) {
        EditFragment fragment = new EditFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("INDEX", index);
        fragment.setArguments(bundle);
        return fragment;
    }

    private View mContainer;
    private ViewPager viewPager;
    private View areaCrop, areaOptimize, areaRotate, areaDelete;
    private ImageView imageOptimize;
    private TextView textOptimize;
    private View viewBack, viewNext;
    private TextView textTitle;

    private int currentIndex;

    private FragmentPagerAdapter pagerAdapter;

    private ScanDataInterface scanDataInterface;
    private ScanDataManager scanDataManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentIndex = getArguments().getInt("INDEX");
        scanDataInterface = (ScanDataInterface) getActivity();
        scanDataManager = scanDataInterface.getScanDataManager();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainer == null) {
            mContainer = inflater.inflate(R.layout.fragment_edit, null);
            SysUtils.setStatysBarPadding(mContainer);
            initView();
        }
        return mContainer;
    }

    @Override
    public void onResume() {
        super.onResume();
        SysUtils.removeFullScreen(getActivity());
        SysUtils.showSystemUI(getActivity());
    }

    private void initView() {
        viewPager = mContainer.findViewById(R.id.view_pager);
        areaCrop = mContainer.findViewById(R.id.area_crop);
        areaOptimize = mContainer.findViewById(R.id.area_optimize);
        areaRotate = mContainer.findViewById(R.id.area_rotate);
        areaDelete = mContainer.findViewById(R.id.area_delete);
        imageOptimize = mContainer.findViewById(R.id.image_optimize);
        textOptimize = mContainer.findViewById(R.id.text_optimize);
        viewBack = mContainer.findViewById(R.id.view_back);
        textTitle = mContainer.findViewById(R.id.text_title);
        viewNext = mContainer.findViewById(R.id.text_next);
        areaCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                final ScanInfo scanInfo = scanDataManager.getScanInfo(currentItem);
                scanDataInterface.toDotModify(scanInfo);
            }
        });
        areaOptimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                final ScanInfo scanInfo = scanDataManager.getScanInfo(currentItem);
                if (scanInfo != null) {
                    scanInfo.setOptimized(!scanInfo.isOptimized());
                    String tag = FragmentPagerAdapter.makeFragmentName(viewPager.getId(), pagerAdapter.getItemId(currentItem));
                    Fragment fragmentByTag = getChildFragmentManager().findFragmentByTag(tag);
                    if (fragmentByTag != null && fragmentByTag instanceof ImagePreviewFragment) {
                        ((ImagePreviewFragment) fragmentByTag).updateScanInfo(scanInfo);
                    }
                }
                changeOptimizeButton(currentItem);
            }
        });
        areaRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                final ScanInfo scanInfo = scanDataManager.getScanInfo(currentItem);
                if (scanInfo != null) {
                    int value = scanInfo.getRotateAngle().getValue();
                    value += 270;// 逆时针旋转
                    if (value >= 360) {
                        value = value % 360;
                    }
                    scanInfo.setRotateAngle(value);
                    String tag = FragmentPagerAdapter.makeFragmentName(viewPager.getId(), pagerAdapter.getItemId(currentItem));
                    Fragment fragmentByTag = getChildFragmentManager().findFragmentByTag(tag);
                    if (fragmentByTag != null && fragmentByTag instanceof ImagePreviewFragment) {
                        ((ImagePreviewFragment) fragmentByTag).rotate(scanInfo);
                    }
                }
            }
        });
        areaDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                final int originSize = scanDataManager.getScanInfoSize();
                scanDataManager.remove(currentItem);
                pagerAdapter.notifyDataSetChanged();
                final int scanInfoSize = scanDataManager.getScanInfoSize();
                if (scanInfoSize > 0) {
                    int tempCurItemIndex = 0;
                    if (currentItem == 0) {
                        tempCurItemIndex = 0;
                    } else if (currentItem == originSize - 1) {
                        // 最后一张
                        tempCurItemIndex = scanInfoSize - 1;
                    } else {
                        tempCurItemIndex = currentItem;
                    }
                    viewPager.setCurrentItem(tempCurItemIndex, true);
                    changeOptimizeButton(tempCurItemIndex);
                } else {
                    getActivity().onBackPressed();
                }
            }
        });
        viewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        viewNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePdf();
            }
        });
        pagerAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {

            @Override
            public int getCount() {
                return scanDataManager.getScanInfoSize();
            }

            @Override
            public Fragment getItem(int position) {
                return ImagePreviewFragment.createInstance(scanDataManager.getScanInfo(position));
            }

            @Override
            public long getItemId(int position) {
                return scanDataManager.getScanInfo(position).getPath().hashCode();
            }

            @Override
            public int getItemPosition(Object object) {
                if (object instanceof ImagePreviewFragment) {
                    final ScanInfo scanInfo = ((ImagePreviewFragment) object).getScanInfo();
                    final int index = scanDataManager.indexOfScanInfo(scanInfo);
                    return index != -1 ? index : POSITION_NONE;
                }
                return super.getItemPosition(object);
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                changeOptimizeButton(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setCurrentItem(currentIndex);
        changeOptimizeButton(currentIndex);
    }

    private void changeOptimizeButton(int position) {
        textTitle.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, scanDataManager.getScanInfoSize()));
        final ScanInfo scanInfo = scanDataManager.getScanInfo(position);
        imageOptimize.setImageResource(scanInfo.isOptimized() ? R.drawable.ic_reduce : R.drawable.ic_filters);
        textOptimize.setText(scanInfo.isOptimized() ? "还原" : "优化");
    }

    @Override
    public boolean onBackPressed() {
        scanDataInterface.toCamera();
        return true;
    }

    void generatePdf() {
        scanDataInterface.showLoading(true, "正在生成PDF");
        new Thread(new GeneratePdfWorker(getContext(), scanDataManager.getAll()) {
            @Override
            public void onPdfGenerated(final String savePath) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scanDataInterface.showLoading(false, null);
                        scanDataInterface.onPdfGenerated(savePath);
                    }
                });
            }
        }).start();
    }
}
