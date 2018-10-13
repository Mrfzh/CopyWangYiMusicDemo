package com.feng.copywangyimusicdemo.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.feng.copywangyimusicdemo.R;
import com.feng.copywangyimusicdemo.adapter.DiscViewPagerAdapter;
import com.feng.copywangyimusicdemo.config.DisplayConfig;
import com.feng.copywangyimusicdemo.entity.MusicInfo;
import com.feng.copywangyimusicdemo.utils.BaseUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义ViewGroup，模仿网易云音乐的音乐播放界面中间的唱盘
 * @author Feng Zhaohao
 * Created on 2018/10/2
 */
public class DiscView extends RelativeLayout {

    private ImageView mDiscBackgroundImageView;     //唱盘底盘
    private ViewPager mDiscContentViewPager;    //ViewPager实现图片切换
    private DiscViewPagerAdapter mDiscViewPagerAdapter;     //ViewPager适配器
    private ImageView mNeedleImageView;     //唱针

    private List<View> mDiscViewList = new ArrayList<>();   //视图集合
    private List<MusicInfo> mMusicInfoList = new ArrayList<>();   //音乐信息集合
    private List<ObjectAnimator> mDiscAnimatorList = new ArrayList<>();  //唱盘动画集合

    private ObjectAnimator mNeedleAnimator;     //唱针动画

    private boolean mViewPagerIsOffset = false;   //标志ViewPager是否有偏移
    private boolean mNeedleIsNeedRestart = false;   //标记唱针复位后，是否需要重新移动到唱盘处

    private IPlayInfo mIPlayInfo;   //音乐信息接口

    private int mScreenWidth, mScreenHeight;    //屏幕宽高

    private NeedleStatus mNeedleStatus = NeedleStatus.IN_FAR;   //唱针状态（初始时为在远处静止）
    private MusicStatus mMusicStatus = MusicStatus.STOP;    //音乐状态（初始时为停止状态）

    public static final int DURATION_NEEDLE_ANIMATOR = 500;     //唱针动画持续时间
    public static final int DURATION_DISC_ANIMATOR = 1000 * 20;     //唱盘动画持续时间

    /**
     * 音乐当前状态
     */
    public enum MusicStatus {
        PLAY, PAUSE, STOP
        //分别是播放中、暂停中、停止
    }

    /**
     * 需要触发的音乐播放状态（相当于命令）
     */
    public enum MusicChangedStatus {
        PLAY, PAUSE, NEXT, LAST, STOP
        //分别是播放、暂停、下一首、上一首、停止
    }

    /**
     * 唱针当前状态
     */
    private enum NeedleStatus {
        TO_FAR,     //移动时：从唱盘向远处移动
        TO_NEAR,    //移动时：从远处向唱盘移动
        IN_FAR,     //静止时：在远处静止
        IN_NEAR     //静止时：在唱盘处静止
    }

    /**
     * 该接口回调用于主活动更新音乐相关的信息
     */
    public interface IPlayInfo {
        //更新标题栏的变化（歌名、歌手）
        void onMusicInfoChanged(String musicName, String author);

        //更新背景图
        void onMusicPictureChanged(int picture);

        //更新播放状态
        void onMusicStateChanged(MusicChangedStatus musicChangedStatus);
    }

    /**
     * 设置音乐播放信息监听
     * @param infoListener
     */
    public void setPlayInfoListener(IPlayInfo infoListener) {
        this.mIPlayInfo = infoListener;
    }

    public DiscView(Context context) {
        super(context);
        init(context);
    }

    public DiscView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DiscView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mScreenWidth = BaseUtil.getScreenWidth(context);
        mScreenHeight = BaseUtil.getScreenHeight(context);
    }

    /**
     * 设置音乐数据
     * @param mMusicInfo 主活动传来的音乐信息
     */
    public void setMusicData(List<MusicInfo> mMusicInfo) {
        if (mMusicInfo.isEmpty()) {
            return;
        }
        //清空原来的数据
        mMusicInfoList.clear();
        mDiscAnimatorList.clear();
        mDiscViewList.clear();
        //添加音乐信息
        mMusicInfoList.addAll(mMusicInfo);

//        int i = 0;
        for (MusicInfo musicInfo : mMusicInfoList) {
            View discLayout = LayoutInflater.from(getContext()).inflate(R.layout.view_disc_content,
                    mDiscContentViewPager, false);
            //  an object that provides a set of LayoutParams values for root of the returned
            //  hierarchy (if <em>attachToRoot</em> is false.)
            // 由官方注释可知，当第三个参数为false时，第二个参数指定的对象的作用是给要返回的对象
            //（这里指discLayout）提供一组 LayoutParams

            ImageView disc = discLayout.findViewById(R.id.iv_disc_content_picture);
            disc.setImageDrawable(getDiscDrawable(musicInfo.getMusicPicture()));    //设置合成后的唱盘图片

            mDiscAnimatorList.add(getDiscAnimator(disc));   //添加至唱盘动画集合
            mDiscViewList.add(discLayout);      //添加至唱盘view集合
        }
        mDiscViewPagerAdapter.notifyDataSetChanged();   //ViewPager更新数据

        //接口回调，更新音乐信息和图片信息
        if (mIPlayInfo != null) {
            mIPlayInfo.onMusicInfoChanged(mMusicInfoList.get(0).getMusicName(),
                    mMusicInfoList.get(0).getAuthor());
            mIPlayInfo.onMusicPictureChanged(mMusicInfoList.get(0).getMusicPicture());
        }
    }


    /**
     * 对外方法：判断是否正在播放音乐
     * @return
     */
    public boolean isPlaying() {
        return mMusicStatus == MusicStatus.PLAY;
    }

    /**
     * 对外方法：根据当前的状态设置新的状态
     */
    public void playOrPause() {
        if (mMusicStatus == MusicStatus.PLAY) {
            pause();
        } else {
            play();
        }
    }

    private void pause() {
        mMusicStatus = MusicStatus.PAUSE;
        pauseAnimator();
    }

    private void play() {
//        mMusicStatus = MusicStatus.PLAY;
//         加上这句后，首次点击播放不能播放第一首歌曲，原因是要等到唱针动画结束后，音乐才开始播放
        playAnimator();
    }

    public void stop() {
        mMusicStatus = MusicStatus.STOP;
        pauseAnimator();
    }

    /**
     * 对外方法：点击下一首歌曲时，切换到下一张图片
     */
    public void next() {
        int currentItem = mDiscContentViewPager.getCurrentItem();   //当前item
        if (currentItem == mMusicInfoList.size() - 1) {
            Toast.makeText(getContext(), "已经是最后一首了", Toast.LENGTH_SHORT).show();
        } else {
            selectMusicWithButton();
            mDiscContentViewPager.setCurrentItem(currentItem + 1, true);
            //第二个参数设置为true时，会平滑切换
        }
    }

    /**
     * 对外方法：点击上一首歌曲时，切换到上一张图片
     */
    public void last() {
        int currentItem = mDiscContentViewPager.getCurrentItem();   //当前item
        if (currentItem == 0) {
            Toast.makeText(getContext(), "已经是第一首了", Toast.LENGTH_SHORT).show();
        } else {
            selectMusicWithButton();
            mDiscContentViewPager.setCurrentItem(currentItem - 1, true);
        }
    }

    /**
     * 播放上/下一首时，根据当前音乐状态，执行相关操作
     */
    private void selectMusicWithButton() {
        if (mMusicStatus == MusicStatus.PLAY) {
            mNeedleIsNeedRestart = true;    //这个有必要，因为此时唱针要执行两次动画
            pauseAnimator();    //?????不太懂
        } else if (mMusicStatus == MusicStatus.PAUSE) {
            play();             //??????同样不同懂
        }
    }

    /**
     * 得到唱盘的动画
     * @param disc
     * @return
     */
    private ObjectAnimator getDiscAnimator(ImageView disc) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(disc, View.ROTATION, 0, 360); //唱盘动画，旋转一周
        animator.setRepeatCount(ValueAnimator.INFINITE);    //设置动画重复次数，此处设置无限重复
        animator.setDuration(DURATION_DISC_ANIMATOR);   //设置动画持续时间
        animator.setInterpolator(new LinearInterpolator());     //设置速度变化，此处为匀速

        return animator;
    }

    /**
     * 得到唱盘图片，唱盘图片由空心圆盘及音乐专辑图片“合成”得到
     * @param pictureRes 图片资源
     * @return
     */
    private Drawable getDiscDrawable(int pictureRes) {
        int discSize = (int) (DisplayConfig.SCALE_DISC_SIZE * mScreenWidth);    //唱盘尺寸
        int pictureSize = (int) (DisplayConfig.SCALE_MUSIC_PIC_SIZE * mScreenWidth);    //专辑图片尺寸

        Bitmap discBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.disc), discSize, discSize, false);   //圆形底盘Bitmap
        Bitmap pictureBitmap = getPictureBitmap(pictureSize, pictureRes);   //图片Bitmap

        BitmapDrawable discBitmapDrawable = new BitmapDrawable(getResources(), discBitmap); //生成圆盘对象的BitmapDrawable
        //要注意的是：Android4.1之后废弃了此构造方法：public BitmapDrawable(Bitmap bitmap)
        //要使用这个新的构造函数：public BitmapDrawable(Resources res, Bitmap bitmap)
        //该构造方法可以正确设置其目标的密度

        RoundedBitmapDrawable roundedPicBitmapDrawable = RoundedBitmapDrawableFactory
                .create(getResources(), pictureBitmap);     //生成圆形图片

        //圆盘和图片都变成Drawable后就可以进行合成
        Drawable [] drawables = new Drawable[2];
        drawables[0] = roundedPicBitmapDrawable;    //图片
        drawables[1] = discBitmapDrawable;      //圆盘
        LayerDrawable layerDrawable = new LayerDrawable(drawables);
        //LayerDrawable是一种有多个Drawable组合而成的Drawable，组合顺序是由数组索引决定的
        //索引最大的Drawable对象将会被绘制在最上面
        //这里通过LayerDrawable把圆盘和图片合成

        int pictureMargin = (int) ((DisplayConfig.SCALE_DISC_SIZE - DisplayConfig.SCALE_MUSIC_PIC_SIZE)
                                    * mScreenWidth / 2);    //图片在整个唱盘中的margin
        layerDrawable.setLayerInset(0, pictureMargin, pictureMargin, pictureMargin, pictureMargin);
        // setLayerInset的作用就是将某层（层数从0开始计数）相对于上一层进行向里偏移。
        // 当然如果传入的数值为负数，就是向外偏移了，不过这时上层就遮挡住下层了，失去了使用layer的意义了。
        // 这里的作用是调整图片四周的边距，让其在唱盘中间
        // 共传入五个参数，第一个参数是进行偏移的drawable的在数组中的下标
        // 第二至第五个参数分别是向左、向上、向右、向下的偏移量

        return layerDrawable;
    }

    /**
     * @param pictureSize   图片尺寸（这里是直径）
     * @param pictureRes  图片资源
     * @return 图片Bitmap
     */
    private Bitmap getPictureBitmap(int pictureSize, int pictureRes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(getResources(), pictureRes, options);
        int imageWidth = options.outWidth;

        int sample = imageWidth / pictureSize;
        int dstSample = 1;
        if (sample > dstSample) {
            dstSample = sample;
        }
        options.inJustDecodeBounds = false;
        //设置图片采样率
        options.inSampleSize = dstSample;
        //设置图片解码格式
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                pictureRes, options), pictureSize, pictureSize, true);
    }

    /**
     * Finalize inflating a view from XML.  This is called as the last phase
     * of inflation, after all child views have been added.
     *
     * 大概意思就是当View中所有的子控件均被映射成xml后触发该方法,
     * 也就是说会在Activity中调用setContentView之后就会调用onFinishInflate这个方法，
     * 这个方法就代表自定义控件中的子控件映射完成了，然后可以进行一些初始化控件的操作
     *
     * 比如：可以通过findViewById 得到控件，然后进行一系列的初始化操作，
     * 当然在这个方法里面是得不到控件的高宽的，控件的高宽是必须在调用了onMeasure方法之后才能得到，
     * 而onFinishInflate方法是在setContentView之后、onMeasure之前
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        initDiscBackground();
        initViewPager();
        initNeedle();
        initObjectAnimator();
    }

    /**
     * 初始化唱盘的底盘（半透明圆形部分）
     */
    private void initDiscBackground() {
        mDiscBackgroundImageView = findViewById(R.id.iv_disc_background);

        mDiscBackgroundImageView.setImageDrawable(getDiscBackgroundDrawable()); //设置背景

        int marginTop = (int) (DisplayConfig.SCALE_DISC_MARGIN_TOP * mScreenHeight);    //底盘顶部margin
        RelativeLayout.LayoutParams lp = (LayoutParams) mDiscBackgroundImageView.getLayoutParams();
        lp.setMargins(0, marginTop, 0, 0);
        mDiscBackgroundImageView.setLayoutParams(lp);   //设置底盘margin
    }

    /**
     * 初始化ViewPager视图切换
     */
    private void initViewPager() {
        mDiscContentViewPager = findViewById(R.id.vp_disc_content);
        mDiscContentViewPager.setOverScrollMode(OVER_SCROLL_NEVER);     //禁止OverScroll
        mDiscContentViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            int lastPositionOffsetPixels = 0;   //记录上一偏移量
            int currentItem = 0;   //记录当前视图索引

            /**
             * @param position 当前视图的索引
             * @param positionOffset 视图的偏移，范围是[0, 1)，代表偏移百分比
             * @param positionOffsetPixels 和第二个参数差不多，同样是视图的偏移，不过这里单位是像素
             */
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //左滑
                if (lastPositionOffsetPixels > positionOffsetPixels) {
                    //判断移动距离是否大于一半
                    if (positionOffset < 0.5) {
                        notifyMusicInfoChanged(position);   //不变
                    } else {
                        notifyMusicInfoChanged(mDiscContentViewPager.getCurrentItem());
                    }
                }

                //右滑
                if (lastPositionOffsetPixels < positionOffsetPixels) {
                    //同样判断移动距离是否大于一半
                    if (positionOffset < 0.5) {
                        notifyMusicInfoChanged(position);
                    } else {
                        //notifyMusicInfoChanged(position + 1);
                        notifyMusicInfoChanged(mDiscContentViewPager.getCurrentItem());
                    }
                }

                lastPositionOffsetPixels = positionOffsetPixels;    //更新偏移量
            }

            /**
             * 该方法是在一个页面跳到另一页面时，跳转完后执行
             * @param position 新页面位置
             */
            @Override
            public void onPageSelected(int position) {
                resetOtherDiscAnimator(position);   //重置其他页面
                notifyMusicPicChanged(position);    //更新图片信息
                if (position > currentItem) {
                    notifyMusicStatusChanged(MusicChangedStatus.NEXT);  //播放下一首
                } else {
                    notifyMusicStatusChanged(MusicChangedStatus.LAST);  //播放上一首
                }
                currentItem = position;     //更新索引
            }

            /**
             * 此方法是在状态改变的时候调用，其中state这个参数有三种状态：
             * SCROLL_STATE_DRAGGING（1） 表示用户手指“按在屏幕上并且开始拖动”的状态
             * （注意：手指按下但是还没有拖动的时候还不是这个状态，只有按下并且手指开始拖动后log才打出。）
             * SCROLL_STATE_IDLE（0） 滑动动画做完的状态。
             * SCROLL_STATE_SETTLING（2） 在“手指离开屏幕”的状态。
             * 一般SCROLL_STATE_IDLE和SCROLL_STATE_SETTLING可以放在一起讨论
             *
             * @param state 状态参数
             */
            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    //开始拖动
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        mViewPagerIsOffset = true;  //更新ViewPager状态
                        pauseAnimator();    //拖动时暂停动画
                        break;
                    //滑动完成
                    case ViewPager.SCROLL_STATE_IDLE:
                    case ViewPager.SCROLL_STATE_SETTLING:
                        mViewPagerIsOffset = false;  //更新ViewPager状态
                        if (mMusicStatus == MusicStatus.PLAY) {
                            playAnimator();     //处于播放状态时开始动画
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        mDiscViewPagerAdapter = new DiscViewPagerAdapter(mDiscViewList);
        mDiscContentViewPager.setAdapter(mDiscViewPagerAdapter);    //设置适配器

        RelativeLayout.LayoutParams lp = (LayoutParams) mDiscContentViewPager.getLayoutParams();
        int marginTop = (int) (DisplayConfig.SCALE_DISC_MARGIN_TOP * mScreenHeight);
        lp.setMargins(0, marginTop, 0, 0);
        mDiscContentViewPager.setLayoutParams(lp);  //设置margin
    }

    /**
     * 初始化唱针
     */
    private void initNeedle() {
        mNeedleImageView = findViewById(R.id.iv_disc_needle);

        //以下是唱针的一些参数
        int needleWidth = (int) (DisplayConfig.SCALE_NEEDLE_WIDTH * mScreenWidth);
        int needleHeight = (int) (DisplayConfig.SCALE_NEEDLE_HEIGHT * mScreenHeight);
        //marginTop设置为负数，隐藏上面一部分（只留下半圆）
        int marginTop = (int) (DisplayConfig.SCALE_NEEDLE_MARGIN_TOP * mScreenHeight) * -1;
        int marginLeft = (int) (DisplayConfig.SCALE_NEEDLE_MARGIN_LEFT * mScreenWidth);
        //唱针的轴点坐标
        int pivotX = (int) (DisplayConfig.SCALE_NEEDLE_PIVOT_X * mScreenWidth);
        int pivotY = (int) (DisplayConfig.SCALE_NEEDLE_PIVOT_Y * mScreenHeight);

        //设置图片
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.disc_needle);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, needleWidth, needleHeight, false);
        mNeedleImageView.setImageBitmap(scaledBitmap);

        //设置margin
        RelativeLayout.LayoutParams lp = (LayoutParams) mNeedleImageView.getLayoutParams();
        lp.setMargins(marginLeft, marginTop, 0, 0);
        mNeedleImageView.setLayoutParams(lp);

        //设置轴点
        mNeedleImageView.setPivotX(pivotX);
        mNeedleImageView.setPivotY(pivotY);
        //设置旋转角度（绕轴点旋转）
        mNeedleImageView.setRotation(DisplayConfig.ROTATION_INIT_NEEDLE);
    }

    /**
     * 初始化唱针动画
     */
    private void initObjectAnimator() {
        mNeedleAnimator = ObjectAnimator.ofFloat(mNeedleImageView, View.ROTATION,
                DisplayConfig.ROTATION_INIT_NEEDLE, 0);     //唱针动画（旋转动画）
        mNeedleAnimator.setDuration(DURATION_NEEDLE_ANIMATOR);      //设置唱针动画时间

        mNeedleAnimator.setInterpolator(new AccelerateInterpolator());
        //该方法可以控制动画的速度变化
        //AccelerateInterpolator就是一个加速运动的Interpolator

        mNeedleAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                //根据动画开始时唱针的状态，就可以得到开始后唱针的状态
                if (mNeedleStatus == NeedleStatus.IN_FAR) {
                    mNeedleStatus = NeedleStatus.TO_NEAR;
                } else if (mNeedleStatus == NeedleStatus.IN_NEAR) {
                    mNeedleStatus = NeedleStatus.TO_FAR;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //判断唱针是靠近唱盘，还是离开唱盘
                if (mNeedleStatus == NeedleStatus.TO_NEAR) {
                    mNeedleStatus = NeedleStatus.IN_NEAR;   //更新唱针状态
                    playDiscAnimator(mDiscContentViewPager.getCurrentItem());   //开始唱盘动画
                    mMusicStatus = MusicStatus.PLAY;    //音乐状态为播放中
                    //注意：一定要在执行完playDiscAnimator操作后，才将mMusicStatus的状态设为MusicStatus.PLAY
                    //因为如果在playDiscAnimator方法前设置MusicStatus.PLAY，playDiscAnimator方法就不会执行音乐播放回调
                    //这样的话，第一次点击播放的时候就会出错
                } else if (mNeedleStatus == NeedleStatus.TO_FAR) {
                    mNeedleStatus = NeedleStatus.IN_FAR;
                    //?????? 若移动时，该首音乐已经停止，则当进入新的页面后，需要重新播放一次唱针动画
                    if (mMusicStatus == MusicStatus.STOP) {
                        mNeedleIsNeedRestart = true;
                    }
                }

                if (mNeedleIsNeedRestart) {
                    mNeedleIsNeedRestart = false;

                    //只有在ViewPager不在偏移状态时，才开始播放动画
                    if (!mViewPagerIsOffset) {
                        DiscView.this.post(new Runnable() {
                            @Override
                            public void run() {
                                playAnimator();
                            }
                        });
                    }
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
     * 更新歌曲信息
     * @param position
     */
    private void notifyMusicInfoChanged(int position) {
        if (mIPlayInfo != null) {
            mIPlayInfo.onMusicInfoChanged(mMusicInfoList.get(position).getMusicName(),
                    mMusicInfoList.get(position).getAuthor());
        }
    }

    /**
     * 更新歌曲图片
     * @param position
     */
    private void notifyMusicPicChanged(int position) {
        if (mIPlayInfo != null) {
            mIPlayInfo.onMusicPictureChanged(mMusicInfoList.get(position).getMusicPicture());
        }
    }

    /**
     * 更新歌曲状态
     * @param musicChangedStatus
     */
    private void notifyMusicStatusChanged(MusicChangedStatus musicChangedStatus) {
        if (mIPlayInfo != null) {
            mIPlayInfo.onMusicStateChanged(musicChangedStatus);
        }
    }


    /**
     * 重置其他页面的动画和图片效果
     * @param position 当前页面索引
     */
    private void resetOtherDiscAnimator(int position) {
        for (int i = 0; i < mDiscViewList.size(); i++) {
            if (position == i) {
                continue;
            }
            mDiscAnimatorList.get(i).cancel();  //取消其他页面的动画
            ImageView imageView = mDiscViewList.get(position)
                    .findViewById(R.id.iv_disc_content_picture);    //背景图
            imageView.setRotation(0);   //将图片旋转角度复原
        }
    }

    /**
     * 暂停唱针动画
     */
    private void pauseAnimator() {
        //播放时暂停动画（即唱针停在唱盘处时）
        if (mNeedleStatus == NeedleStatus.IN_NEAR) {
            pauseDiscAnimator(mDiscContentViewPager.getCurrentItem());  //暂停唱盘动画
        }
        //唱针往唱盘移动时暂停动画
        else if (mNeedleStatus == NeedleStatus.TO_NEAR) {
            mNeedleAnimator.reverse();  //唱针复位

            //????? 若动画在没结束时执行reverse方法，则不会执行监听器的onStart方法，此时需要手动设置
            mNeedleStatus = NeedleStatus.TO_FAR;
        }

        //只有当音乐状态改变时，才执行相关命令
        if (mMusicStatus == MusicStatus.PAUSE) {
            notifyMusicStatusChanged(MusicChangedStatus.PAUSE);
        } else if (mMusicStatus == MusicStatus.STOP) {
            notifyMusicStatusChanged(MusicChangedStatus.STOP);
        }
    }

    /**
     * 播放唱针动画
     */
    private void playAnimator() {
        //唱针在远处时，直接开始播放动画
        if (mNeedleStatus == NeedleStatus.IN_FAR) {
            mNeedleAnimator.start();
        }

        //唱针处于向远处移动时，设置标记，先移动完再播放动画
        //这种状况可能是手指快速滑动，到了下一个页面的时候，唱针还处于离开唱盘向远处的状态
        //需要等唱针完全到达远处后，才再一次开始播放动画
        else if (mNeedleStatus == NeedleStatus.TO_FAR) {
            mNeedleIsNeedRestart = true;
        }
    }

    /**
     * 播放唱盘动画
     * @param position
     */
    private void playDiscAnimator(int position) {
        ObjectAnimator animator = mDiscAnimatorList.get(position);
        //若动画之前是暂定的则继续进行，否则从头开始
        if (animator.isPaused()) {
            animator.resume();
        } else {
            animator.start();
        }

        //只用当前音乐不是播放状态，才回调音乐播放
        //如果当前已经是播放状态，那就开始唱盘动画的时候就不用回调音乐播放
        if (mMusicStatus != MusicStatus.PLAY) {
            notifyMusicStatusChanged(MusicChangedStatus.PLAY);
        }
    }

    /**
     * 暂停唱盘动画
     * @param position
     */
    private void pauseDiscAnimator(int position) {
        ObjectAnimator animator = mDiscAnimatorList.get(position);
        animator.pause();   //暂停动画
        mNeedleAnimator.reverse();      //唱针复位
    }

    /**
     * 得到唱盘底盘的半透明圆形背景
     * @return
     */
    private Drawable getDiscBackgroundDrawable() {
        int discSize = (int) (mScreenWidth * DisplayConfig.SCALE_DISC_SIZE);    //宽度

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.disc_background);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, discSize, discSize, false);
        //根据已存在的Bitmap，创建一个新尺寸的Bitmap
        //第一个参数是已存在的Bitmap，第二、三个参数是新的宽高，第四个参数可以不管，传入false即可

        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory
                .create(getResources(), scaledBitmap);
        //裁剪为圆形drawable

        return roundedBitmapDrawable;
    }
}
