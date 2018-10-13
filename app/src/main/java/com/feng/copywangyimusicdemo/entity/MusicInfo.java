package com.feng.copywangyimusicdemo.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 音乐信息
 * @author Feng Zhaohao
 * Created on 2018/10/2
 */
public class MusicInfo implements Parcelable {
    private String musicName;   //音乐名
    private String author;  //作者
    private int musicPicture;   //音乐图片
    private int musicRes;      //音乐资源

    public MusicInfo(String musicName, String author, int musicPicture, int musicRes) {
        this.musicName = musicName;
        this.author = author;
        this.musicPicture = musicPicture;
        this.musicRes = musicRes;
    }

    public int getMusicRes() {
        return musicRes;
    }

    public void setMuusicRes(int musicRes) {
        this.musicRes = musicRes;
    }

    public int getMusicPicture() {
        return musicPicture;
    }

    public void setMusicPicture(int musicPicture) {
        this.musicPicture = musicPicture;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.musicName);
        dest.writeString(this.author);
        dest.writeInt(this.musicPicture);
        dest.writeInt(this.musicRes);
    }

    protected MusicInfo(Parcel in) {
        this.musicName = in.readString();
        this.author = in.readString();
        this.musicPicture = in.readInt();
        this.musicRes = in.readInt();
    }

    public static final Parcelable.Creator<MusicInfo> CREATOR = new Parcelable.Creator<MusicInfo>() {
        @Override
        public MusicInfo createFromParcel(Parcel source) {
            return new MusicInfo(source);
        }

        @Override
        public MusicInfo[] newArray(int size) {
            return new MusicInfo[size];
        }
    };
}
