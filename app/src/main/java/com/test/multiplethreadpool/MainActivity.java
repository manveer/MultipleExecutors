package com.test.multiplethreadpool;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  private static final int PUT_LOG_DELAY = 1;
  private static final int WRITE_LOG_DELAY = 100;
  private static final int ROTATE_LOG_DELAY = 1000;

  private final Object mSharedObjectBetweenWriteAndRotate = new Object();

  private Handler mUiThreadHandler;
  private ScheduledExecutorService mWriteLogExecutor;
  private ScheduledExecutorService mRotateLogExecutor;

  private TextView mMyTextView;
  private TextView mMyResultTextView;
  private Button mOneExecutorButton;
  private Button mTwoExecutorButton;

  private int mCounter;

  public final class PutLog implements Runnable {
    @Override
    public void run() {
      try {
        if (mUiThreadHandler == null || mCounter == 5000) {
          // Don't put anything
          stop();
          return;
        }
        Thread.sleep(5);
        mMyTextView.setText("Hello World! " + mCounter++);
        Log.d(TAG, "Putting in task queue");
        mTaskQueue.put(
            new Runnable() {
              @Override
              public void run() {
              }
            });
        mUiThreadHandler.postDelayed(this, PUT_LOG_DELAY);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  public final class WriteLog implements Runnable {
    @Override
    public void run() {
      while(mTaskQueue.poll() != null) {
        try {
          synchronized (mSharedObjectBetweenWriteAndRotate) {
            Log.d(TAG, "Polling from task queue");
            Thread.sleep(5);
          }
        } catch (InterruptedException e) {
          //ignore
        }
      }
      mWriteLogExecutor.schedule(this, WRITE_LOG_DELAY, TimeUnit.MILLISECONDS);
    }
  }

  public final class RotateLog implements Runnable {

    @Override
    public void run() {
      synchronized (mSharedObjectBetweenWriteAndRotate) {
        try {
          Log.d(TAG, "Rotating");
          Thread.sleep(50);
          mRotateLogExecutor.schedule(this, ROTATE_LOG_DELAY, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          //ignore
        }
      }
    }
  }

  public final BlockingQueue<Runnable> mTaskQueue = new LinkedBlockingQueue<>();

  long startTime;
  public void start(
      Handler uiThreadHandler,
      ScheduledExecutorService writeLogExecutor,
      ScheduledExecutorService rotateLogExecutor) {
    mOneExecutorButton.setEnabled(false);
    mTwoExecutorButton.setEnabled(false);
    mMyResultTextView.setText("Total time: ");
    startTime = System.currentTimeMillis();
    mUiThreadHandler = uiThreadHandler;
    mWriteLogExecutor = writeLogExecutor;
    mRotateLogExecutor = rotateLogExecutor;

    mUiThreadHandler.postDelayed(new PutLog(), PUT_LOG_DELAY);
    writeLogExecutor.schedule(new WriteLog(), WRITE_LOG_DELAY, TimeUnit.MILLISECONDS);
    rotateLogExecutor.schedule(new RotateLog(), ROTATE_LOG_DELAY, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    if (!mWriteLogExecutor.isShutdown()) {
      mWriteLogExecutor.shutdownNow();
    }
    if (!mRotateLogExecutor.isShutdown()) {
      mRotateLogExecutor.shutdownNow();
    }
    mUiThreadHandler = null;
    mWriteLogExecutor = null;
    mRotateLogExecutor = null;
    long endTime = System.currentTimeMillis();
    mCounter = 0;
    Log.i(TAG, "Total time: " + (endTime - startTime));
    mMyResultTextView.setText("Total time: " + (endTime - startTime));
    mOneExecutorButton.setEnabled(true);
    mTwoExecutorButton.setEnabled(true);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mMyTextView = (TextView) findViewById(R.id.my_text_view);
    mMyResultTextView = (TextView) findViewById(R.id.result);
    mOneExecutorButton = (Button) findViewById(R.id.one_executor_button);
    mTwoExecutorButton = (Button) findViewById(R.id.two_executor_button);
    mOneExecutorButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            startWithOneExecutor();
          }
        });

    mTwoExecutorButton.findViewById(R.id.two_executor_button).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            startWithTwoExecutors();
          }
    });

  }

  void startWithOneExecutor() {
    final ScheduledExecutorService writeLogExecutorService = Executors.newScheduledThreadPool(
        1,
        NamedThreadFactory.newNamedThreadFactory("WriteLogExecutor"));

    final Handler uiThreadHandler = new Handler();
    start(uiThreadHandler, writeLogExecutorService, writeLogExecutorService);
  }

  void startWithTwoExecutors() {
    final ScheduledExecutorService writeLogExecutorService = Executors.newScheduledThreadPool(
        1,
        NamedThreadFactory.newNamedThreadFactory("WriteLogExecutor"));

    final Handler uiThreadHandler = new Handler();
    final ScheduledExecutorService rotateLogExecutorService = Executors.newScheduledThreadPool(
        1,
        NamedThreadFactory.newNamedThreadFactory("RotateLogExecutor"));
    start(uiThreadHandler, writeLogExecutorService, rotateLogExecutorService);
  }
}
