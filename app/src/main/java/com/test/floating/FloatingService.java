package com.test.floating;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {

    private WindowManager wm;
    private Handler handler = new Handler();
    private SharedPreferences prefs;

    private int screenW, screenH;
    private int circleSizeDp = 60;
    private int circleSize, ringT, lockVisual, lockTouch;

    private FrameLayout circleVisual;
    private View[] ring = new View[4];
    private View[] blockers = new View[4];
    private FrameLayout lockWrapper;
    private TextView lockIcon;

    private WindowManager.LayoutParams circleParams;
    private WindowManager.LayoutParams[] ringParams = new WindowManager.LayoutParams[4];
    private WindowManager.LayoutParams[] blockerParams = new WindowManager.LayoutParams[4];
    private WindowManager.LayoutParams lockParams;

    private boolean locked = false;
    private boolean blockersAdded = false;
    private int posX, posY;

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private int winType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams newParams(int w, int h, boolean touchable) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        if (!touchable) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        return new WindowManager.LayoutParams(w, h, winType(), flags,
                PixelFormat.TRANSLUCENT);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("cfg", MODE_PRIVATE);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;

        circleSizeDp = prefs.getInt("size", 60);
        if (circleSizeDp < 30) circleSizeDp = 30;
        if (circleSizeDp > 200) circleSizeDp = 200;

        circleVisual = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.argb(40, 255, 255, 255));
        bg.setStroke(dp(3), Color.argb(200, 0, 123, 255));
        circleVisual.setBackground(bg);
        circleParams = newParams(10, 10, false);
        circleParams.gravity = Gravity.TOP | Gravity.LEFT;

        for (int i = 0; i < 4; i++) {
            ring[i] = new View(this);
            ring[i].setBackgroundColor(Color.TRANSPARENT);
            ring[i].setOnTouchListener(dragTouch);
            ringParams[i] = newParams(10, 10, true);
            ringParams[i].gravity = Gravity.TOP | Gravity.LEFT;

            blockers[i] = new View(this);
            blockers[i].setBackgroundColor(Color.TRANSPARENT);
            blockers[i].setClickable(true);
            blockerParams[i] = newParams(10, 10, true);
            blockerParams[i].gravity = Gravity.TOP | Gravity.LEFT;
        }

        lockWrapper = new FrameLayout(this);
        lockIcon = new TextView(this);
        lockIcon.setGravity(Gravity.CENTER);
        GradientDrawable lbg = new GradientDrawable();
        lbg.setShape(GradientDrawable.OVAL);
        lbg.setColor(Color.argb(220, 40, 40, 40));
        lbg.setStroke(dp(2), Color.WHITE);
        lockIcon.setBackground(lbg);
        lockWrapper.addView(lockIcon, new FrameLayout.LayoutParams(10, 10));
        lockParams = newParams(10, 10, true);
        lockParams.gravity = Gravity.TOP | Gravity.LEFT;
        lockWrapper.setOnTouchListener(lockTouchL);

        applySize(circleSizeDp);

        posX = screenW / 2 - circleSize / 2;
        posY = screenH / 3;

        wm.addView(circleVisual, circleParams);
        for (int i = 0; i < 4; i++) wm.addView(ring[i], ringParams[i]);
        wm.addView(lockWrapper, lockParams);
        layout();

        Toast.makeText(this, "Ready! 🔓 unlock hai. Lock ke liye lock icon dabao.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("size")) {
            int s = intent.getIntExtra("size", 60);
            if (s < 30) s = 30;
            if (s > 200) s = 200;
            if (s != circleSizeDp) {
                circleSizeDp = s;
                applySize(s);
                layout();
            }
        }
        return START_NOT_STICKY;
    }

    private void applySize(int sizeDp) {
        circleSize = dp(sizeDp);
        ringT = dp(10);
        lockVisual = (int) (circleSize * 0.20f);
        lockTouch = Math.max((int) (lockVisual * 2.4f), dp(26));

        circleParams.width = circleSize;
        circleParams.height = circleSize;

        lockIcon.setTextSize(TypedValue.COMPLEX_UNIT_PX, lockVisual * 0.85f);
        FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) lockIcon.getLayoutParams();
        lp.width = lockVisual;
        lp.height = lockVisual;
        lockIcon.setLayoutParams(lp);
        lockParams.width = lockTouch;
        lockParams.height = lockTouch;

        if (posX > screenW - circleSize) posX = Math.max(0, screenW - circleSize);
        if (posY > screenH - circleSize) posY = Math.max(0, screenH - circleSize);
    }

    private final View.OnTouchListener dragTouch = new View.OnTouchListener() {
        private int sx, sy, px, py;
        private boolean moved;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sx = (int) e.getRawX();
                    sy = (int) e.getRawY();
                    px = posX;
                    py = posY;
                    moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) e.getRawX() - sx;
                    int dy = (int) e.getRawY() - sy;
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true;
                    if (moved) {
                        posX = clamp(px + dx, 0, screenW - circleSize);
                        posY = clamp(py + dy, 0, screenH - circleSize);
                        layout();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    return true;
            }
            return false;
        }
    };

    private final View.OnTouchListener lockTouchL = new View.OnTouchListener() {
        private int sx, sy, px, py;
        private long t0;
        private boolean moved;

        private final Runnable closeRun = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FloatingService.this,
                        "App band ho gaya", Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        };

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sx = (int) e.getRawX();
                    sy = (int) e.getRawY();
                    px = posX;
                    py = posY;
                    t0 = System.currentTimeMillis();
                    moved = false;
                    handler.removeCallbacks(closeRun);
                    handler.postDelayed(closeRun, 3000);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) e.getRawX() - sx;
                    int dy = (int) e.getRawY() - sy;
                    if (Math.abs(dx) > 15 || Math.abs(dy) > 15) {
                        if (!moved) {
                            moved = true;
                        }
                        posX = clamp(px + dx, 0, screenW - circleSize);
                        posY = clamp(py + dy, 0, screenH - circleSize);
                        layout();
                        handler.removeCallbacks(closeRun);
                        handler.postDelayed(closeRun, 3000);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(closeRun);
                    long dur = System.currentTimeMillis() - t0;
                    if (!moved && dur < 300) {
                        setLocked(!locked);
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(closeRun);
                    return true;
            }
            return false;
        }
    };

    private int clamp(int v, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, v));
    }

    private void setLocked(boolean l) {
        locked = l;
        lockIcon.setText(l ? "🔒" : "🔓");
        if (l && !blockersAdded) {
            for (int i = 0; i < 4; i++) wm.addView(blockers[i], blockerParams[i]);
            blockersAdded = true;
            wm.removeView(lockWrapper);
            wm.addView(lockWrapper, lockParams);
        } else if (!l && blockersAdded) {
            for (int i = 0; i < 4; i++) wm.removeView(blockers[i]);
            blockersAdded = false;
        }
        layout();
        Toast.makeText(this, l ? "🔒 Locked: sirf circle ke andar + lock zinda"
                : "🔓 Unlocked", Toast.LENGTH_SHORT).show();
    }

    private void layout() {
        circleParams.x = posX;
        circleParams.y = posY;
        wm.updateViewLayout(circleVisual, circleParams);

        ringParams[0].x = posX;                 ringParams[0].y = posY;
        ringParams[0].width = circleSize;       ringParams[0].height = ringT;
        ringParams[1].x = posX;                 ringParams[1].y = posY + circleSize - ringT;
        ringParams[1].width = circleSize;       ringParams[1].height = ringT;
        ringParams[2].x = posX;                 ringParams[2].y = posY + ringT;
        ringParams[2].width = ringT;            ringParams[2].height = circleSize - 2 * ringT;
        ringParams[3].x = posX + circleSize - ringT; ringParams[3].y = posY + ringT;
        ringParams[3].width = ringT;            ringParams[3].height = circleSize - 2 * ringT;
        for (int i = 0; i < 4; i++) {
            ringParams[i].width = Math.max(0, ringParams[i].width);
            ringParams[i].height = Math.max(0, ringParams[i].height);
            wm.updateViewLayout(ring[i], ringParams[i]);
        }

        lockParams.x = posX + circleSize - lockTouch / 2;
        lockParams.y = posY + circleSize / 2 - lockTouch / 2;
        wm.updateViewLayout(lockWrapper, lockParams);

        if (blockersAdded) {
            setBP(0, 0, 0, screenW, posY);
            setBP(1, 0, posY + circleSize, screenW, screenH - posY - circleSize);
            setBP(2, 0, posY, posX, circleSize);
            setBP(3, posX + circleSize, posY, screenW - posX - circleSize, circleSize);
            for (int i = 0; i < 4; i++) {
                try { wm.updateViewLayout(blockers[i], blockerParams[i]); }
                catch (Exception ignored) {}
            }
        }
    }

    private void setBP(int i, int x, int y, int w, int h) {
        blockerParams[i].x = x;
        blockerParams[i].y = y;
        blockerParams[i].width = Math.max(0, w);
        blockerParams[i].height = Math.max(0, h);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        try { wm.removeView(circleVisual); } catch (Exception ignored) {}
        try { wm.removeView(lockWrapper); } catch (Exception ignored) {}
        for (int i = 0; i < 4; i++) {
            try { wm.removeView(ring[i]); } catch (Exception ignored) {}
            try { wm.removeView(blockers[i]); } catch (Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                               }
