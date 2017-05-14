package com.weibo.rxsuperhelper.rxbus.event;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Android on 2016/6/8.
 */
public class EventThread {

    public static Scheduler getScheduler(int threadMode){
        Scheduler scheduler;
        switch (threadMode){
            case ThreadMode.MAIN_THREAD:scheduler= AndroidSchedulers.mainThread();break;
            case ThreadMode.NEW_THREAD:scheduler= Schedulers.newThread();break;
            case ThreadMode.IO:scheduler=Schedulers.io();break;
            case ThreadMode.COMPUTATION:scheduler=Schedulers.computation();break;
            case ThreadMode.TRAMPOLINE:scheduler=Schedulers.trampoline();break;
            default:scheduler= AndroidSchedulers.mainThread();
        }
        return scheduler;
    }
}
