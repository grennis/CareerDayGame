package com.vanderlyn.careerday.app.careerdaygame.app;

import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {
    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private View mContainer;
    private int mScore;
    private boolean mTouchedPig;
    private ImageView mBirdView, mPigView;
    private TextView mScoreView;
    private float mBirdVelocityX, mBirdVelocityY;
    private float mBirdX, mBirdY;
    private float mPigX, mPigY;
    private long mLastBirdUpdate, mLastPigUpdate;
    private float mScreenWidth, mBirdWidth, mPigWidth;
    private float mScreenHieght, mBirdHeight, mPigHeight;
    private MediaPlayer mBeepSound;
    private MediaPlayer mBuzzSound;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBirdView = (ImageView)findViewById(R.id.bird);
        mPigView = (ImageView)findViewById(R.id.pig);
        mScoreView = (TextView)findViewById(R.id.score);
        mContainer = findViewById(R.id.container);
        mBeepSound = MediaPlayer.create(this, R.raw.beep);

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
            updateScoreText();
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
        do {
            mPigX = (float)(Math.random() * ((float)mScreenWidth - mPigWidth));
            mPigY = (float)(Math.random() * ((float)mScreenHieght - mPigHeight));
        } while (pigAndBirdOverlap());

        mPigView.setX(mPigX);
        mPigView.setY(mPigY);
        mPigView.setVisibility(View.VISIBLE);
        mLastPigUpdate = System.currentTimeMillis();
    }

    private void updateScoreText() {
        mScoreView.setText("SCORE: " + mScore);
    }

    private class BirdTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateGame();
                }
            });
        }
    };

    private void updateGame() {
        long now = System.currentTimeMillis();
        long diff = now - mLastBirdUpdate;

        mBirdX += (mBirdVelocityX * diff) / 100;
        mBirdY += (mBirdVelocityY * diff) / 100;
        mBirdView.setX(mBirdX);
        mBirdView.setY(mBirdY);

        if (!mTouchedPig && pigAndBirdOverlap()) {
            mScore += 100;
            updateScoreText();
            mPigView.setVisibility(View.GONE);
            mBeepSound.start();
            mTouchedPig = true;
        }

        float rate = 1f - (Math.min(diff, 400f) / 400f);
        mBirdVelocityX = mBirdVelocityX * rate;
        mBirdVelocityY = mBirdVelocityY * rate;

        if ((mBirdX > (mScreenWidth - mBirdWidth)) || (mBirdX < 0)) {
            mBirdX = Math.max(1, Math.min(mScreenWidth - mBirdWidth - 1, mBirdX));
            mBirdVelocityX *= -1;
        }

        if ((mBirdY > (mScreenHieght - mBirdHeight)) || (mBirdY < 0)) {
            mBirdY = Math.max(1, Math.min(mScreenHieght - mBirdHeight - 1, mBirdY));
            mBirdVelocityY *= -1;
        }

        if (mBirdVelocityX != 0 && mBirdVelocityY != 0 && (Math.abs(mBirdVelocityX) < 5) && (Math.abs(mBirdVelocityY) < 5)) {
            mBirdVelocityX = 0;
            mBirdVelocityY = 0;

            if (mTouchedPig) {
                mTouchedPig = false;

                positionPig();
            } else {
                mScore -= 50;
                updateScoreText();
                positionPig();
            }
        } else {
            if (now - mLastPigUpdate > 1500) {
                mScore -= 10;
                updateScoreText();
                positionPig();
            }
        }

        mLastBirdUpdate = now;
    }

    private boolean pigAndBirdOverlap() {
        return (mPigX >= (mBirdX - mPigWidth)) && (mPigX <= (mBirdX + mBirdWidth)) && (mPigY >= (mBirdY - mPigHeight)) && (mPigY <= (mBirdY + mBirdHeight));
    }
}
