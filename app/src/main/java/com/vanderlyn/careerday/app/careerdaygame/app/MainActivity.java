package com.vanderlyn.careerday.app.careerdaygame.app;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

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
    private long mNextPigUpdate, mRestorePigTime;
    private float mScreenWidth, mBirdWidth, mPigWidth;
    private float mScreenHeight, mBirdHeight, mPigHeight;
    private MediaPlayer mBeepSound, mUpgradeSound, mRestoreSound;
    private Animation mFadeIn;

    private static final String STATE_SCORE = "score";

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
        mUpgradeSound = MediaPlayer.create(this, R.raw.upgrade);
        mRestoreSound = MediaPlayer.create(this, R.raw.restore);

        mGestureDetector = new GestureDetector(this, new FlingGestureDetector());
        mGestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        };

        registerForContextMenu(mBirdView);

        mBirdView.setVisibility(View.INVISIBLE);
        mBirdView.setOnTouchListener(mGestureListener);
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);

        if (savedInstanceState != null) {
            mScore = savedInstanceState.getInt(STATE_SCORE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_SCORE, mScore);
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
            mBirdVelocityX = 0.1f; // Trigger it to move
            mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
            positionAndShowPig();
            updateScoreText();
            new Thread(mGameThread).start();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        openContextMenu(mBirdView);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.upgrades, menu);
        menu.findItem(R.id.upgrade_big_pig).setEnabled(mScore >= 500);
        menu.findItem(R.id.upgrade_spikes).setEnabled(mScore >= 1500);
        menu.findItem(R.id.upgrade_bombs).setEnabled(mScore >= 2000);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.upgrade_big_pig && mScore >= 500) {
            mScore -= 500;
            updateScoreText();
            makePigLarge();
        }

        return super.onContextItemSelected(item);
    }

    private Runnable mGameThread = new Runnable() {
        @Override
        public void run() {
            long lastTick = System.currentTimeMillis();

            while (true) {
                long now = System.currentTimeMillis();
                updateGameState(now, now - lastTick);
                lastTick = System.currentTimeMillis();

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void updateGameState(long time, long diff) {
        boolean willUpdateScoreText = false;
        boolean willHidePig = false;
        boolean willShowPig = false;
        boolean willMoveBird = false;
        boolean willRestorePig = false;

        if (mPigView.getVisibility() == View.VISIBLE && pigAndBirdOverlap()) {
            mScore += 100;
            willUpdateScoreText = true;
            willHidePig = true;
            mBeepSound.seekTo(0);
            mBeepSound.start();
        }

        if (mBirdVelocityX != 0f || mBirdVelocityY != 0f) {
            willMoveBird = true;
            mBirdX += (mBirdVelocityX * diff) / 100;
            mBirdY += (mBirdVelocityY * diff) / 100;

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

            if ((Math.abs(mBirdVelocityX) < 5) && (Math.abs(mBirdVelocityY) < 5)) {
                mBirdVelocityX = 0;
                mBirdVelocityY = 0;
            }
        }

        if (time > mNextPigUpdate) {
            if (mPigView.getVisibility() == View.VISIBLE) {
                mScore -= 100;
                willUpdateScoreText = true;
                willHidePig = true;
            } else {
                willShowPig = true;
            }
        }

        if (mRestorePigTime != 0 && time > mRestorePigTime) {
            willRestorePig = true;
        }

        if (willMoveBird || willHidePig || willShowPig || willUpdateScoreText || willRestorePig) {
            updateUI(willMoveBird, willHidePig, willShowPig, willUpdateScoreText, willRestorePig);
        }
    }

    private void updateUI(final boolean willMoveBird, final boolean willHidePig, final boolean willShowPig, final boolean willUpdateScoreText, final boolean willRestorePig) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (willRestorePig)
                    restorePig();

                if (willMoveBird)
                    setXY(mBirdView, mBirdX, mBirdY);

                if (willUpdateScoreText)
                    updateScoreText();

                if (willHidePig)
                    hidePig();

                if (willShowPig)
                    positionAndShowPig();
            }
        });
    }

    private boolean pigAndBirdOverlap() {
        return (mPigX >= (mBirdX - mPigWidth)) && (mPigX <= (mBirdX + mBirdWidth)) && (mPigY >= (mBirdY - mPigHeight)) && (mPigY <= (mBirdY + mBirdHeight));
    }

    private void makePigLarge() {
        mPigWidth *=2;
        mPigHeight *= 2;
        mPigView.getLayoutParams().width *= 2;
        mPigView.getLayoutParams().height *= 2;
        mRestorePigTime = System.currentTimeMillis() + 15000;
        mUpgradeSound.start();
    }

    private void restorePig() {
        mRestorePigTime = 0;
        mPigWidth /=2;
        mPigHeight /= 2;
        mPigView.getLayoutParams().width /= 2;
        mPigView.getLayoutParams().height /= 2;
        mRestoreSound.start();
    }

    public static void setXY(View v, float l, float t) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins((int)l, (int)t, 0, 0);
            v.requestLayout();
        }
    }
}
