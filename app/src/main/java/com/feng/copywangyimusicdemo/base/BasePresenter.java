package com.feng.copywangyimusicdemo.base;

/**
 * Created by Lin Yaotian on 2018/7/12.
 */
 
//这个类的作用是帮助Presenter获取View实例
public class BasePresenter<V> {
    private V view;

    public void attachView(V view) {
        this.view = view;
    }


    public void detachView() {
        view = null;
    }


    protected V getMvpView() {
        return view;
    }


    protected boolean isAttachView() {
        return view != null;
    }

}
