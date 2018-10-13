package com.feng.copywangyimusicdemo.view;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.feng.copywangyimusicdemo.R;
import com.feng.copywangyimusicdemo.contract.IMainContract;
import com.feng.copywangyimusicdemo.entity.Music;
import com.feng.copywangyimusicdemo.entity.MusicInfo;
import com.feng.copywangyimusicdemo.presenter.MainPresenter;
import com.feng.copywangyimusicdemo.service.MusicService;
import com.feng.copywangyimusicdemo.utils.BaseUtil;
import com.feng.copywangyimusicdemo.utils.PicUtil;
import com.feng.copywangyimusicdemo.widget.BackgroundAnimatorRelativeLayout;
import com.feng.copywangyimusicdemo.widget.DiscView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DiscView.IPlayInfo, IMainContract.View{

    private TextView mMusicNameTextView;    //音乐名
    private TextView mSingerNameTextView;   //歌手名
    private DiscView mDiscView;             //唱盘控件
    private TextView mNowTimeTextView;      //当前时间
    private TextView mAllTimeTextView;      //总时间
    private SeekBar mSeekBar;               //进度条
    private ImageView mLastImageView;       //上一首歌
    private ImageView mNextImageView;       //下一首歌
    private ImageView mPlayOrPauseImageView;    //播放或暂停
    private BackgroundAnimatorRelativeLayout mRootLayout;   //根布局

    private ArrayList<MusicInfo> mMusicInfoList = new ArrayList<>();     //音乐信息集合

    private MusicReceiver mMusicReceiver = new MusicReceiver();     //音乐广播

    private static final int MUSIC_MESSAGE = 0;  //通过发送该message不断更新进度条

    public static final String PARAM_MUSIC_LIST = "PARAM_MUSIC_LIST";   //参数，传递给服务音乐信息

    private IMainContract.Presenter mPresenter;

    @SuppressLint("HandlerLeak")
    private Handler mMusicHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MUSIC_MESSAGE:
                    mSeekBar.setProgress(mSeekBar.getProgress() + 1000);    //每一次设置都比上一次多一秒
                    mNowTimeTextView.setText(duration2Time(mSeekBar.getProgress()));    //设置当前时间
                    startUpdateSeekBarProgress();   //再一次更新时间
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        makeStatusBarTransparent();
        initMusicInfo();
        startMusicService();
        initView();
        initMusicReceiver();

        mPresenter = new MainPresenter();
        ((MainPresenter) mPresenter).attachView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver); //取消注册
        if (mPresenter != null) {
            ((MainPresenter) mPresenter).detachView();
        }
    }



    private void initView() {
        mMusicNameTextView = findViewById(R.id.tv_main_music_name);
        mSingerNameTextView = findViewById(R.id.tv_main_author_name);
        mNowTimeTextView = findViewById(R.id.tv_main_now_time);
        mAllTimeTextView = findViewById(R.id.tv_main_all_time);

        mLastImageView = findViewById(R.id.iv_main_last);
        mLastImageView.setOnClickListener(this);
        mPlayOrPauseImageView = findViewById(R.id.iv_main_play_and_pause);
        mPlayOrPauseImageView.setOnClickListener(this);
        mNextImageView = findViewById(R.id.iv_main_next);
        mNextImageView.setOnClickListener(this);

        mRootLayout = findViewById(R.id.rv_main_root_layout);

        mDiscView = findViewById(R.id.disc_view);
        mDiscView.setPlayInfoListener(this);
        mDiscView.setMusicData(mMusicInfoList);

        mSeekBar = findViewById(R.id.sb_main_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mNowTimeTextView.setText(duration2Time(progress));  //更新当前时间
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopUpdateSeekBarProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekTo(seekBar.getProgress());
                startUpdateSeekBarProgress();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_main_last:
                mDiscView.last();
                break;
            case R.id.iv_main_play_and_pause:
                mDiscView.playOrPause();
                break;
            case R.id.iv_main_next:
                mDiscView.next();
                break;
            default:
                break;
        }
    }

    @Override
    public void onMusicInfoChanged(String musicName, String author) {
        mMusicNameTextView.setText(musicName);
        mSingerNameTextView.setText(author);
    }

    @Override
    public void onMusicPictureChanged(int picture) {
        try2UpdateMusicPicBackground(picture);
    }

    @Override
    public void onMusicStateChanged(DiscView.MusicChangedStatus musicChangedStatus) {
        switch (musicChangedStatus) {
            case PLAY:  //播放
                sendBroadcast(MusicService.ACTION_OPT_MUSIC_PLAY);  //发送广播给服务
                startUpdateSeekBarProgress();   //不断更新进度条
                break;
            case PAUSE: //暂停
                sendBroadcast(MusicService.ACTION_OPT_MUSIC_PAUSE);
                stopUpdateSeekBarProgress();    //停止更新进度条
                break;
            case LAST:  //上一首
                mRootLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(MusicService.ACTION_OPT_MUSIC_LAST);
                    }
                }, DiscView.DURATION_NEEDLE_ANIMATOR);  //唱针动画完成后再发送广播
                stopUpdateSeekBarProgress();    //停止更新进度条
                //当前时间和总时间归零
                mNowTimeTextView.setText(duration2Time(0));
                mAllTimeTextView.setText(duration2Time(0));
                break;
            case NEXT:   //下一首（和上一首的操作基本一样）
                mRootLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(MusicService.ACTION_OPT_MUSIC_NEXT);
                    }
                }, DiscView.DURATION_NEEDLE_ANIMATOR);
                stopUpdateSeekBarProgress();
                mNowTimeTextView.setText(duration2Time(0));
                mAllTimeTextView.setText(duration2Time(0));
                break;
            case STOP:
                stopUpdateSeekBarProgress();
                mNowTimeTextView.setText(duration2Time(0));
                mAllTimeTextView.setText(duration2Time(0));
                mSeekBar.setProgress(0);    //seekBar进度归零
                mPlayOrPauseImageView.setSelected(false);
                break;
            default:
                break;
        }
    }

    private void seekTo(int position) {
        Intent intent = new Intent(MusicService.ACTION_OPT_MUSIC_SEEK_TO);
        intent.putExtra(MusicService.PARAM_MUSIC_SEEK_TO,position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 开始不断更新进度条
     */
    private void startUpdateSeekBarProgress() {
        stopUpdateSeekBarProgress();    //避免重复发送message
        mMusicHandle.sendEmptyMessageDelayed(MUSIC_MESSAGE, 1000);  //延迟1秒发送消息
    }

    /**
     * 停止更新进度条
     */
    private void stopUpdateSeekBarProgress() {
        mMusicHandle.removeMessages(MUSIC_MESSAGE);
    }

    /**
     * 根据时长格式化成时间文本
     * @param duration
     * @return
     */
    private String duration2Time(int duration) {
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        return (min < 10 ? "0" + min : min + "") + ":" + (sec < 10 ? "0" + sec : sec + "");
    }

    /**
     * 发送相应的广播
     * @param action
     */
    private void sendBroadcast(String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
    }

    /**
     * 更新背景图片
     * @param pictureRes
     */
    private void try2UpdateMusicPicBackground(final int pictureRes) {
        if (mRootLayout.isNeed2UpdateBackground(pictureRes)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Drawable foregroundDrawable = getForegroundDrawable(pictureRes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRootLayout.setForeground(foregroundDrawable);  //设置新的背景
                            mRootLayout.startAnimator();    //开始背景动画
                        }
                    });
                }
            }).start();
        }
    }

    /**
     * 获得前景（新的背景）的 Drawable
     * @param pictureRes
     * @return
     */
    private Drawable getForegroundDrawable(int pictureRes) {
        //得到屏幕的宽高比，以便按比例切割图片一部分
        float widthHeightSize = (float) (BaseUtil.getScreenWidth(this) * 1.0
                                            / BaseUtil.getScreenHeight(this) * 1.0);
        //得到图片的bitmap
        Bitmap bitmap = getForegroundBitmap(pictureRes);

        int cropBitmapWidth = (int) (widthHeightSize * bitmap.getHeight());   //裁剪后bitmap的宽度
        int cropBitmapWidthX = (bitmap.getWidth() - cropBitmapWidth) / 2;    //需要裁剪的宽度的一半

        //切割部分图片
        Bitmap cropBitmap = Bitmap.createBitmap(bitmap, cropBitmapWidthX, 0, cropBitmapWidth, bitmap.getHeight());
        //该createBitmap方法作用是从原图片上切割得到新的图片
        //第一个参数是图片bitmap，第二、三个参数是新图片左上角的x、y坐标
        //第四、五个参数分别是新图片的宽度、高度（不能大于原图片）

        //缩小图片
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(cropBitmap, cropBitmap.getWidth()/50,
                cropBitmap.getHeight()/50, false);

        //模糊化
        Bitmap blurBitmap = PicUtil.doBlur(scaledBitmap, 8, true);

        Drawable foregroundDrawable = new BitmapDrawable(getResources(), blurBitmap);
        //加入灰色遮罩层，避免图片过亮影响其他控件
        foregroundDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

        return foregroundDrawable;
    }

    /**
     * 获得前景的Bitmap
     * @param musicPicRes
     * @return
     */
    private Bitmap getForegroundBitmap(int musicPicRes) {
        int screenWidth = BaseUtil.getScreenWidth(this);
        int screenHeight = BaseUtil.getScreenHeight(this);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(getResources(), musicPicRes, options);
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        if (imageWidth < screenWidth && imageHeight < screenHeight) {
            return BitmapFactory.decodeResource(getResources(), musicPicRes);
        }

        int sample = 2;
        int sampleX = imageWidth / screenWidth;
        int sampleY = imageHeight / screenHeight;

        if (sampleX > sampleY && sampleY > 1) {
            sample = sampleX;
        } else if (sampleY > sampleX && sampleX > 1) {
            sample = sampleY;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return BitmapFactory.decodeResource(getResources(), musicPicRes, options);
    }

    /**
     * 设置透明状态栏
     */
    private void makeStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 初始化音乐信息
     */
    private void initMusicInfo() {
        MusicInfo musicInfo1 = new MusicInfo("寻", "三亩地", R.raw.ic_music1, R.raw.music1);
        MusicInfo musicInfo2 = new MusicInfo("Nightingale", "YANI", R.raw.ic_music2, R.raw.music2);
        MusicInfo musicInfo3 = new MusicInfo("盗将行", "花粥/马雨阳", R.raw.ic_music3, R.raw.music3);
        mMusicInfoList.add(musicInfo1);
        mMusicInfoList.add(musicInfo2);
        mMusicInfoList.add(musicInfo3);
    }

    /**
     * 开启音乐服务
     */
    private void startMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(PARAM_MUSIC_LIST, mMusicInfoList);
        startService(intent);   //开启服务
    }

    /**
     * 初始化广播接收器
     */
    private void initMusicReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAY);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PAUSE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_DURATION);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_COMPLETE);
        //注册本地广播
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver, intentFilter);
    }

    /**
     * MusicReceiver:接收服务发来的广播，进行相关操作
     */
    class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                //播放
                case MusicService.ACTION_STATUS_MUSIC_PLAY:
                    mPlayOrPauseImageView.setSelected(true);    //图标改为播放中
                    int currentProgress = intent.getIntExtra(MusicService.PARAM_MUSIC_CURRENT_POSITION, 0);
                    mSeekBar.setProgress(currentProgress);      //设置进度条
                    if (!mDiscView.isPlaying()) {
                        mDiscView.playOrPause();    //更新唱盘控件
                    }
                    break;
                //暂停
                case MusicService.ACTION_STATUS_MUSIC_PAUSE:
                    mPlayOrPauseImageView.setSelected(false);   //图标改为暂停中
                    if (mDiscView.isPlaying()) {
                        mDiscView.playOrPause();    //更新唱盘控件
                    }
                    break;
                //初始化进度条
                case MusicService.ACTION_STATUS_MUSIC_DURATION:
                    int totalDuration = intent.getIntExtra(MusicService.PARAM_MUSIC_DURATION, 0);   //总时间
                    mSeekBar.setProgress(0);    //seekBar进度先设为0
                    mSeekBar.setMax(totalDuration);     //设置seekBar的最大进度
                    mAllTimeTextView.setText(duration2Time(totalDuration));     //设置总时间
                    mNowTimeTextView.setText(duration2Time(0));     //当前时间归零
                    startUpdateSeekBarProgress();   //开始不断更新进度条
                    break;
                //播放完一首歌后，自动播放下一首
                case MusicService.ACTION_STATUS_MUSIC_COMPLETE:
                    if (intent.getBooleanExtra(MusicService.PARAM_MUSIC_IS_OVER, true)) {
                        //先判断是否是最后一首，若是最后一首则停止
                        mDiscView.stop();
                    } else {
                        mDiscView.next();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void getMp3FileSuccess(List<Music> musicList) {

    }

    @Override
    public void getMp3FileError() {

    }
}

