package com.test.multiplethreadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory implements ThreadFactory {
  private final String mName;
  private final ThreadFactory mUnderlyingThreadFactory;
  private final AtomicInteger mThreadCount;

  public static NamedThreadFactory newNamedThreadFactory(String name) {
    return new NamedThreadFactory(name);
  }

  private NamedThreadFactory(String name) {
    mName = name;
    mUnderlyingThreadFactory = Executors.defaultThreadFactory();
    mThreadCount = new AtomicInteger();
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread newThread = mUnderlyingThreadFactory.newThread(r);
    newThread.setName(mName + "-" + mThreadCount.getAndIncrement());
    return newThread;
  }
}
