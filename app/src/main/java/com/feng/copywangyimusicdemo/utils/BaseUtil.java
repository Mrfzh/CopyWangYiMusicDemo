package com.feng.copywangyimusicdemo.utils;

import android.content.Context;

/**
 * 一些基本功能
 * @author Feng Zhaohao
 * Created on 2018/10/2
 */
public class BaseUtil {

    /**
     * 获取设备屏幕宽度
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取设备屏幕高度
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
