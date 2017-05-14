package com.weibo.rxsuperhelper.rxbus.annotation;

import android.support.annotation.IntDef;
import com.weibo.rxsuperhelper.rxbus.event.ThreadMode;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by weibo on 17-4-14.
 * 用于指定能Thread模式，代替枚举的使用
 */

@IntDef({
    ThreadMode.COMPUTATION, ThreadMode.IO, ThreadMode.MAIN_THREAD,
    ThreadMode.NEW_THREAD, ThreadMode.TRAMPOLINE
}) @Retention(RetentionPolicy.RUNTIME) public @interface ThreadModes {
}
