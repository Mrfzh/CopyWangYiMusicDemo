package com.feng.copywangyimusicdemo.presenter;

import com.feng.copywangyimusicdemo.base.BasePresenter;
import com.feng.copywangyimusicdemo.contract.IMainContract;
import com.feng.copywangyimusicdemo.entity.Music;
import com.feng.copywangyimusicdemo.model.MainModel;

import java.util.List;

/**
 * @author Feng Zhaohao
 * Created on 2018/10/9
 */
public class MainPresenter extends BasePresenter<IMainContract.View> implements IMainContract.Presenter{
    private IMainContract.Model mModel;

    public MainPresenter() {
        mModel = new MainModel(this);
    }

    @Override
    public void getMp3FileSuccess(List<Music> musicList) {
        if (isAttachView()) {
            getMvpView().getMp3FileSuccess(musicList);
        }
    }

    @Override
    public void getMp3FileError() {
        if (isAttachView()) {
            getMvpView().getMp3FileError();
        }
    }

    @Override
    public void getMp3File() {
        mModel.getMp3File();
    }
}
