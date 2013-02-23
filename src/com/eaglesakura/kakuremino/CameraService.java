package com.eaglesakura.kakuremino;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import com.eaglesakura.kakuremino.texture.CameraSurfaceTextureListener;
import com.eaglesakura.kakuremino.ui.StatusBarMenu;

public class CameraService extends Service {

    /**
     * Window操作用マネージャ<BR>
     * ここへViewを登録することで、常にViewを表示することができる。
     */
    WindowManager windowManager;

    /**
     * 操作用のUI
     */
    StatusBarMenu statusBarMenu;

    /**
     * 表示用のViewルート
     */
    View views;

    /**
     * カメラプレビュー対象となるTextureView
     */
    TextureView textureView;

    @Override
    public void onCreate() {
        super.onCreate();
        // WindowManagerを取得する
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(this);
        views = inflater.inflate(R.layout.service_main, null);
        {

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            // レイアウトの幅 / 高さ設定
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    // レイアウトの挿入位置設定
                    // TYPE_SYSTEM_OVERLAYはほぼ最上位に位置して、ロック画面よりも上に表示される。
                    // ただし、タッチを拾うことはできない。
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    // ウィンドウ属性
                    // TextureViewを利用するには、FLAG_HARDWARE_ACCELERATED が必至となる。
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    // 透過属性を持たなければならないため、TRANSLUCENTを利用する
                    PixelFormat.TRANSLUCENT);

            // Viewを画面上に重ね合わせする
            windowManager.addView(views, params);
        }

        // TextureViewの取得
        textureView = (TextureView) views.findViewById(R.id.texture_view);

        // TextureViewの操作用リスナとUIを登録
        {
            // カメラ自体の処理はリスナ側に記述している
            CameraSurfaceTextureListener listener = new CameraSurfaceTextureListener(textureView, this);
            textureView.setSurfaceTextureListener(listener);

            // 
            // 操作用のUIを作成する
            statusBarMenu = new StatusBarMenu(this, listener, textureView);
            statusBarMenu.show();
        }
    }

    @Override
    public void onDestroy() {
        // windowからViewを排除する
        windowManager.removeView(views);

        // StatusBarに配置したUIを排除する
        statusBarMenu.dismiss();

        // 削除作業を継続させる
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
