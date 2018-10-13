package com.feng.copywangyimusicdemo.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.feng.copywangyimusicdemo.entity.MusicInfo;
import com.feng.copywangyimusicdemo.view.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener{

    //操作指令
    public static final String ACTION_OPT_MUSIC_PLAY = "ACTION_OPT_MUSIC_PLAY";
    public static final String ACTION_OPT_MUSIC_PAUSE = "ACTION_OPT_MUSIC_PAUSE";
    public static final String ACTION_OPT_MUSIC_NEXT = "ACTION_OPT_MUSIC_NEXT";
    public static final String ACTION_OPT_MUSIC_LAST = "ACTION_OPT_MUSIC_LAST";
    public static final String ACTION_OPT_MUSIC_SEEK_TO = "ACTION_OPT_MUSIC_SEEK_TO";

    //状态指令
    public static final String ACTION_STATUS_MUSIC_PLAY = "ACTION_STATUS_MUSIC_PLAY";
    public static final String ACTION_STATUS_MUSIC_PAUSE = "ACTION_STATUS_MUSIC_PAUSE";
    public static final String ACTION_STATUS_MUSIC_COMPLETE = "ACTION_STATUS_MUSIC_COMPLETE";
    public static final String ACTION_STATUS_MUSIC_DURATION = "ACTION_STATUS_MUSIC_DURATION";

    //Intent传送数据的名字
    public static final String PARAM_MUSIC_DURATION = "PARAM_MUSIC_DURATION";
    public static final String PARAM_MUSIC_SEEK_TO = "PARAM_MUSIC_SEEK_TO";
    public static final String PARAM_MUSIC_CURRENT_POSITION = "PARAM_MUSIC_CURRENT_POSITION";
    public static final String PARAM_MUSIC_IS_OVER = "PARAM_MUSIC_IS_OVER";

    private MusicReceiver mMusicReceiver = new MusicReceiver();     //音乐广播接收器

    private ArrayList<MusicInfo> mMusicInfoList = new ArrayList<>();     //音乐信息集合（有主活动传来）
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private int mCurrentMusicIndex = 0;     //当前播放的音乐索引
    private boolean mIsMusicPause = false;  //当前音乐是否暂停

    @Override
    public void onCreate() {
        super.onCreate();
        initBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initMusicInfo(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver);
    }

    /**
     * 初始化广播接收器
     */
    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        //添加接收的广播
        intentFilter.addAction(ACTION_OPT_MUSIC_PLAY);
        intentFilter.addAction(ACTION_OPT_MUSIC_PAUSE);
        intentFilter.addAction(ACTION_OPT_MUSIC_NEXT);
        intentFilter.addAction(ACTION_OPT_MUSIC_LAST);
        intentFilter.addAction(ACTION_OPT_MUSIC_SEEK_TO);
        //注册广播
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver, intentFilter);
    }

    /**
     * 初始化服务中的音乐信息
     * @param intent
     */
    private void initMusicInfo(Intent intent) {
        mMusicInfoList = intent.getParcelableArrayListExtra(MainActivity.PARAM_MUSIC_LIST);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_OPT_MUSIC_PLAY:     //播放
                    play(mCurrentMusicIndex);
                    break;
                case ACTION_OPT_MUSIC_PAUSE:    //暂停
                    mMediaPlayer.pause();
                    mIsMusicPause = true;
                    sendMusicStatusBroadcast(ACTION_STATUS_MUSIC_PAUSE);
                    break;
                case ACTION_OPT_MUSIC_NEXT:     //下一首
                    if (mCurrentMusicIndex < mMusicInfoList.size() - 1) {
                        play(mCurrentMusicIndex + 1);
                    } else {
                        mMediaPlayer.stop();     //已经是最后一首的时候停止播放
                    }
                    break;
                case ACTION_OPT_MUSIC_LAST:     //上一首
                    if (mCurrentMusicIndex != 0) {
                        play(mCurrentMusicIndex - 1);
                    }
                    break;
                case ACTION_OPT_MUSIC_SEEK_TO:  //进度跳转
                    if (mMediaPlayer.isPlaying()) {
                        //正在播放时才能跳转
                        mMediaPlayer.seekTo(intent.getIntExtra(PARAM_MUSIC_SEEK_TO, 0));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 播放操作
     * @param index 播放歌曲的索引
     */
    private void play(int index) {
        //判断要播放的歌曲是否为当前播放歌曲
        //注意：第一次播放的时候，虽然索引一样但是mIsMusicPause为false，所以还是会初始化MediaPlayer
        if (mCurrentMusicIndex == index && mIsMusicPause) {
            mMediaPlayer.start();
            mIsMusicPause = false;
        } else {
            mMediaPlayer.stop();
            mMediaPlayer = null;
            //重新初始化MediaPlayer
            if (mMusicInfoList.isEmpty()) {
                Toast.makeText(this, "数据还没准备好", Toast.LENGTH_SHORT).show();
            } else {
                mMediaPlayer = MediaPlayer.create(this, mMusicInfoList.get(index).getMusicRes());
                mMediaPlayer.start();       //开始播放
                mMediaPlayer.setOnCompletionListener(this);     //设置监听
                mCurrentMusicIndex = index;     //更新索引
                mIsMusicPause = false;
                //获取当前音乐的时间并发送广播给主活动
                int duration = mMediaPlayer.getDuration();
                sendMusicDurationBroadcast(duration);
            }
        }
        sendMusicStatusBroadcast(ACTION_STATUS_MUSIC_PLAY); //发送广播给主活动
    }

    /**
     * 发送音乐时长的广播给主活动
     * @param duration
     */
    private void sendMusicDurationBroadcast(int duration) {
        Intent intent = new Intent(ACTION_STATUS_MUSIC_DURATION);
        intent.putExtra(PARAM_MUSIC_DURATION, duration);    //传送音乐时长给主活动
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);  //发送广播给主活动
    }

    /**
     * 发送当前音乐状态（播放或暂停）的广播给主活动
     * @param action
     */
    private void sendMusicStatusBroadcast(String action) {
        Intent intent = new Intent(action);
        if (action.equals(ACTION_STATUS_MUSIC_PLAY)) {
            intent.putExtra(PARAM_MUSIC_CURRENT_POSITION, mMediaPlayer.getCurrentPosition());
            //如果是播放，则传给主活动当前的进度
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 当音乐播放到末尾的时候执行该方法
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        //音乐播放完后发送广播给主活动
        Intent intent = new Intent(ACTION_STATUS_MUSIC_COMPLETE);
        intent.putExtra(PARAM_MUSIC_IS_OVER, (mCurrentMusicIndex == mMusicInfoList.size() - 1));
        //同时携带一个布尔参数，告诉主活动该音乐是否是最后一首歌曲
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
