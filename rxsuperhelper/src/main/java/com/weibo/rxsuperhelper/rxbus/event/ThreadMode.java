package com.weibo.rxsuperhelper.rxbus.event;

/**
 * Created by weibo on 17-4-14.
 */

public interface ThreadMode {
  /**
   * 主线程
   */
  int MAIN_THREAD = 0x1;
  /**
   * 新的线程
   */
  int NEW_THREAD = 0x2;
  /**
   * 读写线程
   */
  int IO = 0x3;
  /**
   * 计算工作默认线程
   */
  int COMPUTATION = 0x4;
  /**
   * 在当前线程中按照队列方式执行
   */
  int TRAMPOLINE = 0x5;
}
