package com.feng.copywangyimusicdemo.adapter;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * 唱盘图片ViewPager的适配器
 * @author Feng Zhaohao
 * Created on 2018/10/2
 */
public class DiscViewPagerAdapter extends PagerAdapter {

    private List<View> mDiscViewList;   //视图集合

    public DiscViewPagerAdapter(List<View> mDiscViewList) {
        this.mDiscViewList = mDiscViewList;
    }

    /**
     * 实例化要显示的视图，并添加到容器中
     * @param container
     * @param position
     * @return
     */
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = mDiscViewList.get(position);
        container.addView(view);    //添加对应的view到容器中

        return view;
    }

    /**
     * 销毁视图
     * @param container
     * @param position
     * @param object
     */
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView(mDiscViewList.get(position));
    }

    /**
     * 判断一个页面视图是否与instantiateItem方法返回的视图是同一个视图
     * @param view
     * @param object
     * @return
     */
    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;  //固定写法
    }

    /**
     * 要显示的总页数
     * @return
     */
    @Override
    public int getCount() {
        return mDiscViewList.size();
    }
}
