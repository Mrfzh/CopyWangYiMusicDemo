package com.feng.copywangyimusicdemo.model;

import com.feng.copywangyimusicdemo.contract.IMainContract;

/**
 * @author Feng Zhaohao
 * Created on 2018/10/9
 */
public class MainModel implements IMainContract.Model {
    private IMainContract.Presenter mPresenter;

    public MainModel(IMainContract.Presenter mPresenter) {
        this.mPresenter = mPresenter;
    }

    /**
     * 获取本地mp3文件
     */
    @Override
    public void getMp3File() {

    }
}
