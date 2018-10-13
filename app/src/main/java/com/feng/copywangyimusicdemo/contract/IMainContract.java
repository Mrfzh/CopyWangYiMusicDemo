package com.feng.copywangyimusicdemo.contract;

import com.feng.copywangyimusicdemo.entity.Music;

import java.util.List;

/**
 * @author Feng Zhaohao
 * Created on 2018/10/9
 */
public interface IMainContract {
    interface View {
        void getMp3FileSuccess(List<Music> musicList);       //获取mp3文件成功
        void getMp3FileError();         //获取mp3文件失败
    }

    interface Presenter {
        void getMp3FileSuccess(List<Music> musicList);       //获取mp3文件成功
        void getMp3FileError();         //获取mp3文件失败
        void getMp3File();      //获取本地mp3文件
    }

    interface Model {
        void getMp3File();      //获取本地mp3文件
    }
}
