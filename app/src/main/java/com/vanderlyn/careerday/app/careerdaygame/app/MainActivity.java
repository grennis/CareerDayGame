package com.vanderlyn.careerday.app.careerdaygame.app;

import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {
    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private View mContainer;
    private int mScore;
    private ImageView mBirdView, mPigView;
    private TextView mScoreView;
    private float mBirdVelocityX, mBirdVelocityY;
    private float mBirdX, mBirdY;
    private float mPigX, mPigY;
    private long mLastBirdUpdate, mNextPigUpdate;
    private float mScreenWidth, mBirdWidth, mPigWidth;
    private float mScreenHeight, mBirdHeight, mPigHeight;
    private MediaPlayer mBeepSound;
    private Animation mFadeIn;

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

        mFadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        mFadeIn.setDuration(1000);

        mBeepSound = MediaPlayer.create(this, R.raw.beep);

        mGestureDetector = new GestureDetector(this, new FlingGestureDetector());
        mGestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };

        mBirdView.setVisibility(View.INVISIBLE);
        mBirdView.setOnTouchListener(mGestureListener);
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mScreenWidth = mContainer.getWidth();
            mScreenHeight = mContainer.getHeight();
            mBirdWidth = mBirdView.getWidth();
            mBirdHeight = mBirdView.getHeight();
            mBirdX = (mScreenWidth / 2) - (mBirdWidth / 2);
            mBirdY = (mScreenHeight / 2) - (mBirdHeight / 2);
            mPigWidth = mPigView.getWidth();
            mPigHeight = mPigView.getHeight();
            mBirdView.setVisibility(View.VISIBLE);
            mBirdView.startAnimation(mFadeIn);
            mLastBirdUpdate = System.currentTimeMillis();
            mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
            positionAndShowPig();
            updateScoreText();
            new Timer("mBirdView").schedule(new UpdateGameTask(), 100, 20);
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

    private void positionAndShowPig() {
        do {
            mPigX = (float)(Math.random() * ((float)mScreenWidth - mPigWidth));
            mPigY = (float)(Math.random() * ((float) mScreenHeight - mPigHeight));
        } while (pigAndBirdOverlap());

        setXY(mPigView, mPigX, mPigY);
        mPigView.setVisibility(View.VISIBLE);
        mPigView.startAnimation(mFadeIn);
        mNextPigUpdate = System.currentTimeMillis() + 3000;
    }

    private void hidePig() {
        mPigView.setVisibility(View.INVISIBLE);
        mNextPigUpdate = System.currentTimeMillis() + (long)(1000 + (3000 * Math.random()));
    }

    private void updateScoreText() {
        mScoreView.setText("SCORE: " + mScore);
    }

    private class UpdateGameTask extends TimerTask {
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
        setXY(mBirdView, mBirdX, mBirdY);

        if (mPigView.getVisibility() == View.VISIBLE && pigAndBirdOverlap()) {
            mScore += 100;
            updateScoreText();
            hidePig();
            mBeepSound.seekTo(0);
            mBeepSound.start();
        }

        float rate = 1f - (Math.min(diff, 400f) / 400f);
        mBirdVelocityX = mBirdVelocityX * rate;
        mBirdVelocityY = mBirdVelocityY * rate;

        if ((mBirdX > (mScreenWidth - mBirdWidth)) || (mBirdX < 0)) {
            mBirdX = Math.max(1, Math.min(mScreenWidth - mBirdWidth - 1, mBirdX));
            mBirdVelocityX *= -1;
        }

        if ((mBirdY > (mScreenHeight - mBirdHeight)) || (mBirdY < 0)) {
            mBirdY = Math.max(1, Math.min(mScreenHeight - mBirdHeight - 1, mBirdY));
            mBirdVelocityY *= -1;
        }

        if (mBirdVelocityX != 0 && mBirdVelocityY != 0 && (Math.abs(mBirdVelocityX) < 5) && (Math.abs(mBirdVelocityY) < 5)) {
            mBirdVelocityX = 0;
            mBirdVelocityY = 0;
        }

        if (now > mNextPigUpdate) {
            if (mPigView.getVisibility() == View.VISIBLE) {
                mScore -= 100;
                updateScoreText();
                hidePig();
            } else {
                positionAndShowPig();
            }
        }

        mLastBirdUpdate = now;
    }

    private boolean pigAndBirdOverlap() {
        return (mPigX >= (mBirdX - mPigWidth)) && (mPigX <= (mBirdX + mBirdWidth)) && (mPigY >= (mBirdY - mPigHeight)) && (mPigY <= (mBirdY + mBirdHeight));
    }

    public static void setXY(View v, float l, float t) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins((int)l, (int)t, 0, 0);
            v.requestLayout();
        }
    }
}
