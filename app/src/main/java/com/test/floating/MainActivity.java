package com.test.floating;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.startBtn);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                        );
                        startActivityForResult(intent, 100);
                        Toast.makeText(MainActivity.this,
                            "Permission enable karo!", Toast.LENGTH_LONG).show();
                    } else {
                        startFloating();
                    }
                } else {
                    startFloating();
                }
            }
        });
    }

    private void startFloating() {
        startService(new Intent(this, FloatingService.class));
        Toast.makeText(this, "Floating Circle Started!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startFloating();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    }
