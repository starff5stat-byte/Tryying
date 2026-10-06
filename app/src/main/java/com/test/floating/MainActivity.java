package com.test.floating;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button startBtn;
    private SeekBar sizeSeek;
    private TextView sizeLabel;
    private SharedPreferences prefs;
    private int sizeDp = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = findViewById(R.id.startBtn);
        sizeSeek = findViewById(R.id.sizeSeek);
        sizeLabel = findViewById(R.id.sizeLabel);
        prefs = getSharedPreferences("cfg", MODE_PRIVATE);

        sizeDp = prefs.getInt("size", 60);
        sizeSeek.setMax(170);
        sizeSeek.setProgress(sizeDp - 30);
        sizeLabel.setText("Circle size: " + sizeDp + " dp");

        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                sizeDp = 30 + p;
                sizeLabel.setText("Circle size: " + sizeDp + " dp");
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                prefs.edit().putInt("size", sizeDp).apply();
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putInt("size", sizeDp).apply();
                startBtn.setBackgroundColor(Color.parseColor("#0056b3"));
                startBtn.setText("Start Ho Raha Hai...");
                startBtn.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkAndStart();
                    }
                }, 400);
            }
        });
    }

    private void checkAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
                Toast.makeText(this, "Permission ON karo, phir back aao", Toast.LENGTH_LONG).show();
            } else {
                startFloating();
            }
        } else {
            startFloating();
        }
    }

    private void startFloating() {
        Intent i = new Intent(this, FloatingService.class);
        i.putExtra("size", sizeDp);
        startService(i);
        Toast.makeText(this, "Floating Circle Started!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloating();
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
        }
