package com.weibo.mylibrary.rxbus;

import android.support.annotation.NonNull;

import com.weibo.mylibrary.rxbus.annotation.Subscribe;
import com.weibo.mylibrary.rxbus.event.EventThread;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Android on 2016/6/6.
 */

public class RxBus {

    protected static RxBus instance;

    public static RxBus getDefault() {
        if (instance == null) {
            synchronized (RxBus.class) {
                if (instance == null) {
                    instance = new RxBus();
                }
            }
        }
        return instance;
    }

    public static boolean recyclerRxBus() {
        instance = null;
        return instance == null;
    }

    //发布者
    protected final Subject bus;

    //存放订阅者信息
    protected Map<Object, CompositeDisposable> subscriptions = new HashMap<>();

    /**
     * PublishSubject 创建一个可以在订阅之后把数据传输给订阅者Subject
     * SerializedSubject 序列化Subject为线程安全的Subject RxJava2 暂无
     */
    public RxBus() {
        bus = PublishSubject.create().toSerialized();
    }

    public void post(@NonNull Object obj) {
        bus.onNext(obj);
    }

    /**
     * 订阅事件
     *
     * @return
     */
    public <T> Observable tObservable(final Class<T> eventType) {
        return bus.ofType(eventType);
    }

    /**
     * 订阅者注册
     *
     * @param subscriber
     */
    public void register(@NonNull Object subscriber) {
        Flowable.just(subscriber)
                .filter(b -> subscriptions.get(subscriber) == null) //判断订阅者没有在序列中
                .flatMap(s -> Flowable.fromArray(s.getClass().getDeclaredMethods()))
                .map(m -> {
                    m.setAccessible(true);
                    return m;
                })
                .filter(m -> m.isAnnotationPresent(Subscribe.class))
                .subscribe(m -> addSubscription(m, subscriber));
    }

    /**
     * 添加订阅
     *
     * @param m          方法
     * @param subscriber 订阅者
     */
    protected void addSubscription(Method m, Object subscriber) {
        //获取方法内参数
        Class[] parameterType = m.getParameterTypes();
        //只获取第一个方法参数，否则默认为Object
        Class cla = Object.class;
        if (parameterType.length > 1) {
            cla = parameterType[0];
        }
        //获取注解
        Subscribe sub = m.getAnnotation(Subscribe.class);
        //订阅事件
        Disposable disposable = tObservable(cla)
                .observeOn(EventThread.getScheduler(sub.thread()))
                .subscribe(o -> {
                            try {
                                m.invoke(subscriber, o);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        },
                        e -> System.out.println("this object is not invoke"));
        putSubscriptionsData(subscriber, disposable);
    }

    /**
     * 添加订阅者到map空间来unRegister
     *
     * @param subscriber 订阅者
     * @param disposable 订阅者 Subscription
     */
    protected void putSubscriptionsData(Object subscriber, Disposable disposable) {
        CompositeDisposable subs = subscriptions.get(subscriber);
        if (subs == null) {
            subs = new CompositeDisposable();
        }
        subs.add(disposable);
        subscriptions.put(subscriber, subs);
    }

    /**
     * 解除订阅者
     *
     * @param subscriber 订阅者
     */
    public void unRegister(Object subscriber) {
        Flowable.just(subscriber)
                .filter(s -> s != null)
                .map(s -> subscriptions.get(s))
                .filter(subs -> subs != null)
                .subscribeWith(new Subscriber<CompositeDisposable>() {
                    @Override
                    public void onSubscribe(Subscription s) {

                    }

                    @Override
                    public void onNext(CompositeDisposable compositeDisposable) {
                        compositeDisposable.dispose();
                        subscriptions.remove(subscriber);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

}
