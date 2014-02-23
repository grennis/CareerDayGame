package com.vanderlyn.careerday.app.careerdaygame.app;

import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
    private ImageView bird;
    private float velocityX = 60;
    private float velocityY = 60;
    private float posX = 10;
    private float posY = 10;
    private long lastTick;
    private float width;
    private float height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View container = findViewById(R.id.container);
        bird = (ImageView)findViewById(R.id.bird);

        // Gesture detection
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };

        //bird.setOnClickListener(this);
        bird.setOnTouchListener(gestureListener);


        lastTick = System.currentTimeMillis();
        new Timer("bird").schedule(new BirdTask(), 100, 20);

        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                width = container.getWidth() - bird.getWidth();
                height = container.getHeight() - bird.getHeight();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            try {
               // Toast.makeText(MainActivity.this, "vx " + vx, Toast.LENGTH_SHORT).show();
                velocityX = vx/20f;
                velocityY = vy/20f;
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    private class BirdTask extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long diff = now - lastTick;

            posX += (velocityX * diff) / 100;
            posY += (velocityY * diff) / 100;

            if (posX > width || posX < 0)
                velocityX =- velocityX;

            if (posY > height || posY < 0)
                velocityY =- velocityY;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bird.setX(posX);
                    bird.setY(posY);
                }
            });

            float rate = 1f - (Math.min(diff, 400f) / 400f);
            velocityX = velocityX * rate;
            velocityY = velocityY * rate;

            if (Math.abs(velocityX) < 5)
                velocityX = 0;

            if (Math.abs(velocityY) < 5)
                velocityY = 0;

            lastTick = now;
        }
    };
}
