package com.feng.copywangyimusicdemo.config;

/**
 * 播放界面view的一些参数
 * @author Feng Zhaohao
 * Created on 2018/10/2
 */
public class DisplayConfig {

    //屏幕宽高
    private static final float BASE_SCREEN_WIDTH = (float) 1080.0;
    private static final float BASE_SCREEN_HEIGHT = (float) 1920.0;

    //唱盘比例
    public static final float SCALE_DISC_SIZE = (float) (813.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_DISC_MARGIN_TOP = (float) (190 / BASE_SCREEN_HEIGHT);

    //唱针比例
    public static final float SCALE_NEEDLE_WIDTH = (float) (276.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_NEEDLE_HEIGHT = (float) (413.0 / BASE_SCREEN_HEIGHT);
    public static final float SCALE_NEEDLE_MARGIN_LEFT = (float) (500.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_NEEDLE_MARGIN_TOP = (float) (43.0 / BASE_SCREEN_HEIGHT);
    public static final float SCALE_NEEDLE_PIVOT_X = (float) (43.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_NEEDLE_PIVOT_Y = (float) (43.0 / BASE_SCREEN_WIDTH);

    //专辑图片比例
    public static final float SCALE_MUSIC_PIC_SIZE = (float) (533.0 / BASE_SCREEN_WIDTH);

    //唱针起始角度
    public static final float ROTATION_INIT_NEEDLE = -30;
}
