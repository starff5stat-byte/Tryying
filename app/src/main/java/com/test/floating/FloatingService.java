package com.test.floating;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

public class FloatingService extends Service {

    private WindowManager wm;
    private FrameLayout circle;
    private WindowManager.LayoutParams params;
    private float dX, dY;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        circle = new FrameLayout(this);
        int size = 180;

        params = new WindowManager.LayoutParams(
            size, size,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 300;
        params.y = 500;

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.argb(40, 255, 255, 255));
        bg.setStroke(4, Color.argb(200, 0, 123, 255));
        circle.setBackground(bg);

        circle.setOnTouchListener(new View.OnTouchListener() {
            private long startTime;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        moved = false;
                        dX = event.getRawX() - params.x;
                        dY = event.getRawY() - params.y;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        moved = true;
                        params.x = (int)(event.getRawX() - dX);
                        params.y = (int)(event.getRawY() - dY);
                        wm.updateViewLayout(circle, params);
                        break;

                    case MotionEvent.ACTION_UP:
                        if (!moved && (System.currentTimeMillis() - startTime) < 300) {
                            Toast.makeText(FloatingService.this,
                                "Circle Clicked!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return true;
            }
        });

        wm.addView(circle, params);
        Toast.makeText(this, "Floating Test Running!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (circle != null) {
            wm.removeView(circle);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                          }
