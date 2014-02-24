package com.vanderlyn.careerday.app.careerdaygame.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {
    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private View mContainer;
    private ImageView mBirdView, mPigView;
    private float mBirdVelocityX, mBirdVelocityY;
    private float mBirdX, mBirdY;
    private float mPigX, mPigY;
    private long mLastBirdUpdate;
    private float mScreenWidth, mBirdWidth, mPigWidth;
    private float mScreenHieght, mBirdHeight, mPigHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBirdView = (ImageView)findViewById(R.id.bird);
        mPigView = (ImageView)findViewById(R.id.pig);
        mContainer = findViewById(R.id.container);

        mGestureDetector = new GestureDetector(this, new FlingGestureDetector());
        mGestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };

        mBirdView.setOnTouchListener(mGestureListener);
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mScreenWidth = mContainer.getWidth();
            mScreenHieght = mContainer.getHeight();
            mBirdWidth = mBirdView.getWidth();
            mBirdHeight = mBirdView.getHeight();
            mPigWidth = mPigView.getWidth();
            mPigHeight = mPigView.getHeight();
            mLastBirdUpdate = System.currentTimeMillis();
            mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
            positionPig();
            new Timer("mBirdView").schedule(new BirdTask(), 100, 20);
        }
    };

    class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            mBirdVelocityX = vx/15f;
            mBirdVelocityY = vy/15f;
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    private void positionPig() {
        mPigX = (float)(Math.random() * ((float)mScreenWidth - mPigWidth));
        mPigY = (float)(Math.random() * ((float)mScreenHieght - mPigHeight));
        mPigView.setX(mPigX);
        mPigView.setY(mPigY);
        mPigView.setVisibility(View.VISIBLE);
    }

    private class BirdTask extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long diff = now - mLastBirdUpdate;

            mBirdX += (mBirdVelocityX * diff) / 100;
            mBirdY += (mBirdVelocityY * diff) / 100;

            if ((mBirdX > (mScreenWidth - mBirdWidth)) || (mBirdX < 0))
                mBirdVelocityX *= -1;

            if ((mBirdY > (mScreenHieght - mBirdHeight)) || (mBirdY < 0))
                mBirdVelocityY *= -1;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBirdView.setX(mBirdX);
                    mBirdView.setY(mBirdY);
                }
            });

            if (mPigX >= mBirdX && mPigX <= (mBirdX + mBirdWidth) && mPigY >= mBirdY && mPigY <= (mBirdY + mBirdHeight)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPigView.setVisibility(View.GONE);
                    }
                });
            }

            float rate = 1f - (Math.min(diff, 400f) / 400f);
            mBirdVelocityX = mBirdVelocityX * rate;
            mBirdVelocityY = mBirdVelocityY * rate;

            if ((Math.abs(mBirdVelocityX) < 5) && (Math.abs(mBirdVelocityY) < 5)) {
                mBirdVelocityX = 0;
                mBirdVelocityY = 0;

                if (mPigView.getVisibility() == View.GONE) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            positionPig();
                        }
                    });
                }
            }

            mLastBirdUpdate = now;
        }
    };
}
