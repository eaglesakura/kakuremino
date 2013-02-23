package com.eaglesakura.kakuremino;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


/**
 * カメラを常時専有するためのServiceを起動して、自身はすぐに終了させるActivity。<BR>
 * Serviceの詳細は {@link CameraViewService} を参照すること。
 *
 */
public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, CameraService.class)); // Serviceを起動
        finish(); // 自身を終了する
    }
}
