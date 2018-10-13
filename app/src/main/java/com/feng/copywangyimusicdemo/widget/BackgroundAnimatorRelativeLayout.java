package com.feng.copywangyimusicdemo.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;

import com.feng.copywangyimusicdemo.R;

/**
 * 自定义控件，继承于RelativeLayout
 * 该控件的作用是提供背景的渐变动画，当切换歌曲的时候，背景会改变，而且是一个渐变的效果
 * @author Feng Zhaohao
 * Created on 2018/10/5
 */
public class BackgroundAnimatorRelativeLayout extends RelativeLayout {

    private static final int INDEX_BACKGROUND = 0;     //背景索引
    private static final int INDEX_FOREGROUND = 1;     //前景索引
    private static final int DURATION_ANIMATOR = 500;   //背景动画持续时间

    private LayerDrawable mLayerDrawable;   //将前景与背景组合起来，方便实现背景切换时的渐变
    private ObjectAnimator animator;    //背景动画

    private int musicPicRes = -1;

    public BackgroundAnimatorRelativeLayout(Context context) {
        super(context);
        init();
    }

    public BackgroundAnimatorRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BackgroundAnimatorRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 一系列初始化操作
     */
    private void init() {
        initLayerDrawable();
        initBackgroundAnimator();
    }

    /**
     * 初始化LayerDrawable
     */
    private void initLayerDrawable(){
        Drawable backgroundDrawable = getContext().getDrawable(R.drawable.disc_background);
        Drawable [] drawables = new Drawable[2];
        //初始化时前景与背景一致
        drawables[INDEX_BACKGROUND] = backgroundDrawable;
        drawables[INDEX_FOREGROUND] = backgroundDrawable;

        mLayerDrawable = new LayerDrawable(drawables);
    }

    /**
     * 初始化背景动画
     */
    private void initBackgroundAnimator() {
        animator = ObjectAnimator.ofFloat(this, "number", 0f, 1.0f);
        animator.setDuration(DURATION_ANIMATOR);    //设置动画持续时间
        animator.setInterpolator(new AccelerateInterpolator());     //动画加速播放
        //通过该监听器，我们可以得到当前的属性值（这里是透明度），并利用该值对动画进行操作
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int foregroundAlpha = (int) ((float)animation.getAnimatedValue() * 255);    //前景透明度
                //动态设置前景的透明度(0 - 255)，让前景逐渐显现
                mLayerDrawable.getDrawable(INDEX_FOREGROUND).setAlpha(foregroundAlpha);
                //更新布局背景
                BackgroundAnimatorRelativeLayout.this.setBackground(mLayerDrawable);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //动画结束时，原来的前景变为背景
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mLayerDrawable.setDrawable(INDEX_BACKGROUND, mLayerDrawable.getDrawable(INDEX_FOREGROUND));
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * 对外提供方法：设置前景（要显示的背景）
     * @param foreground
     */
    public void setForeground(Drawable foreground) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mLayerDrawable.setDrawable(INDEX_FOREGROUND, foreground);
        }
    }

    /**
     * 对外提供方法：开始背景动画
     */
    public void startAnimator() {
       animator.start();
    }

    /**
     * ?????????????? 不太懂这个方法的作用
     * @param musicPicRes
     * @return
     */
    public boolean isNeed2UpdateBackground(int musicPicRes) {
        if (this.musicPicRes == -1) return true;
        if (musicPicRes != this.musicPicRes) {
            return true;
        }
        return false;
    }

    private void setNumber(float arg) {

    }
}
