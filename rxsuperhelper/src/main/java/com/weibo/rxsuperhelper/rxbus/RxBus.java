package com.weibo.rxsuperhelper.rxbus;

import android.support.annotation.NonNull;
import com.weibo.rxsuperhelper.rxbus.annotation.Subscribe;
import com.weibo.rxsuperhelper.rxbus.event.EventThread;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by Android on 2016/6/6.
 */

public class RxBus {

  public static RxBus getDefault() {
    return RxBusHelper.instance;
  }

  private static class RxBusHelper {
    private static final RxBus instance = new RxBus();
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
   */
  public <T> Observable tObservable(final Class<T> eventType) {
    return bus.ofType(eventType);
  }

  /**
   * 订阅者注册
   */
  public void register(@NonNull final Object subscriber) {
    Flowable.just(subscriber).filter(new Predicate<Object>() {
      @Override public boolean test(@NonNull Object o) throws Exception {
        return subscriptions.get(o) == null;
      }
    }) //判断订阅者没有在序列中
        .flatMap(new Function<Object, Publisher<Method>>() {
          @Override public Publisher<Method> apply(@NonNull Object o) throws Exception {
            return Flowable.fromArray(o.getClass().getDeclaredMethods());
          }
        }).map(new Function<Method, Method>() {
      @Override public Method apply(@NonNull Method m) throws Exception {
        m.setAccessible(true);
        return m;
      }
    }).filter(new Predicate<Method>() {
      @Override public boolean test(@NonNull Method m) throws Exception {
        return m.isAnnotationPresent(Subscribe.class);
      }
    }).subscribe(new Consumer<Method>() {
      @Override public void accept(@NonNull Method m) throws Exception {
        addSubscription(m, subscriber);
      }
    });
  }

  /**
   * 添加订阅
   *
   * @param m 方法
   * @param subscriber 订阅者
   */
  protected void addSubscription(final Method m, final Object subscriber) {
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
    Disposable disposable = tObservable(cla).observeOn(EventThread.getScheduler(sub.thread()))
        .subscribe(new Consumer() {
          @Override public void accept(@NonNull Object o) throws Exception {
            try {
              m.invoke(subscriber, o);
            } catch (IllegalAccessException e) {
              e.printStackTrace();
            } catch (InvocationTargetException e) {
              e.printStackTrace();
            }
          }
        }, new Consumer<Throwable>() {
          @Override public void accept(@NonNull Throwable throwable) throws Exception {
            System.out.println("this object is not invoke");
          }
        });
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
  public void unRegister(final Object subscriber) {
    Flowable.just(subscriber).filter(new Predicate<Object>() {
      @Override public boolean test(@NonNull Object o) throws Exception {
        return o != null;
      }
    }).map(new Function<Object, CompositeDisposable>() {
      @Override public CompositeDisposable apply(@NonNull Object o) throws Exception {
        return subscriptions.get(o);
      }
    }).filter(new Predicate<CompositeDisposable>() {
      @Override public boolean test(@NonNull CompositeDisposable o) throws Exception {
        return o != null;
      }
    }).subscribeWith(new Subscriber<CompositeDisposable>() {
      @Override public void onSubscribe(Subscription s) {

      }

      @Override public void onNext(CompositeDisposable compositeDisposable) {
        compositeDisposable.dispose();
        subscriptions.remove(subscriber);
      }

      @Override public void onError(Throwable t) {

      }

      @Override public void onComplete() {

      }
    });
  }
}
