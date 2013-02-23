package com.eaglesakura.kakuremino.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.TextureView;
import android.widget.RemoteViews;

import com.eaglesakura.kakuremino.CameraService;
import com.eaglesakura.kakuremino.R;
import com.eaglesakura.kakuremino.texture.CameraSurfaceTextureListener;

/**
 * {@link TextureView} を通知バーから操作するためのMenuを提供する。<BR>
 * Menu UIはListenerを設定できないため、ボタンごとにBroadcastを行わせることでUIを実現する。<BR>
 * Menu UI -> [Broadcast] -> {@link BroadcastReceiver} -> {@link #textureView} で命令が伝えられる。
 */
public class StatusBarMenu {
    private static String TAG = StatusBarMenu.class.getSimpleName();

    /**
     * Notification BarのIDを設定する。
     */
    private static final int NOTIFICATION_ID = 0x3103 + 0;

    /**
     * 操作対象のTextureView
     */
    private TextureView textureView;

    /**
     * リモート操作用のコントローラー
     */
    RemoteViews remoteController;

    /**
     * 生成元のContext
     */
    CameraService service;

    /**
     * 
     */
    NotificationManager notifiManager;

    /**
     * 実操作用のNotification
     */
    Notification notification;

    /**
     * ブロードキャスト受け取り
     */
    BroadcastReceiver receiver = null;

    /**
     * TextureView操作クラス
     */
    CameraSurfaceTextureListener listener;

    /**
     * 
     * @param context 作成元のContext。今回はServiceになる。
     * @param 操作対象のView
     */
    public StatusBarMenu(CameraService service, CameraSurfaceTextureListener listener, TextureView textureView) {
        this.service = service;
        this.listener = listener;
        this.textureView = textureView;
        this.notifiManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        initializeNotification();
        initBroadcatsReceiver();

        // アルファ値を計算する
        {
            final int PROGRESS_MAX = service.getResources().getInteger(R.integer.alpha_max);
            final int PROGRESS_DEFAULT = service.getResources().getInteger(R.integer.alpha_default);

            final float A = (float) PROGRESS_DEFAULT / (float) PROGRESS_MAX;
            textureView.setAlpha(A);
        }
    }

    /**
     * 通知領域の準備を行う
     */
    private void initializeNotification() {
        notification = new Notification(R.drawable.ic_launcher, service.getString(R.string.app_name),
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notification.contentView = new RemoteViews(service.getPackageName(), R.layout.status_control);
        remoteController = notification.contentView;
        // アルファ値を上げるボタン
        {
            remoteController.setOnClickPendingIntent(R.id.alpha_plus,
                    PendingIntent.getBroadcast(service, 0, makeCommand(COMMAND_ALPHA_UP), 0));
        }
        // アルファ値を下げるボタン
        {
            remoteController.setOnClickPendingIntent(R.id.alpha_minus,
                    PendingIntent.getBroadcast(service, 1, makeCommand(COMMAND_ALPHA_DOWN), 0));
        }
        // 停止ボタン
        {
            remoteController.setOnClickPendingIntent(R.id.stop_service,
                    PendingIntent.getBroadcast(service, 2, makeCommand(COMMAND_SHUTDOWN), 0));
        }
    }

    /**
     * ブロードキャストの準備を行う
     */
    private void initBroadcatsReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = getCommand(intent);
                if (COMMAND_ALPHA_DOWN.equals(command)) {
                    onAlphaDown();
                } else if (COMMAND_ALPHA_UP.equals(command)) {
                    onAlphaUp();
                } else if (COMMAND_SHUTDOWN.equals(command)) {
                    // サービスを停止させる。
                    service.stopSelf();
                }
            }
        };

        // ACTION_CAMERATRANS_CONTROLL のブロードキャストにのみ反応させる
        IntentFilter filter = new IntentFilter(ACTION_CAMERATRANS_CONTROLL);
        service.registerReceiver(receiver, filter);
    }

    /**
     * コントローラーの表示を開始する。
     */
    public void show() {
        try {
            notifiManager.notify(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * UIの更新を行わせる
     */
    private void invalidate() {
        try {
            notifiManager.notify(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * コントローラーを非表示にする
     */
    public void dismiss() {
        // notificationを削除する
        try {
            notifiManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ブロードキャストレシーバを削除する
        try {
            service.unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * アルファ値をプログレスバーに反映させる
     */
    private void updateAlphaProgressBar() {
        // 遷移の最大値（100）
        final int PROGRESS_MAX = service.getResources().getInteger(R.integer.alpha_max);
        // TextureViewから現在のアルファ値を取り出す
        final float alpha = textureView.getAlpha();
        // 現在のプログレス状態を計算する
        final int PROGURESS_NOW = (int) ((float) PROGRESS_MAX * alpha);

        // プログレスバーを更新して再描画を行わせる
        remoteController.setProgressBar(R.id.alpha_progress, PROGRESS_MAX, PROGURESS_NOW, false);
        invalidate();
    }

    /**
     * アルファ値を下げる
     */
    private void onAlphaDown() {
        listener.alphaDown();
        updateAlphaProgressBar();
    }

    /**
     * アルファ値を上げる
     */
    private void onAlphaUp() {
        listener.alphaUp();
        updateAlphaProgressBar();
    }

    /**
     * コマンド用のExtra
     */
    private static String EXTRA_COMMAND = "EXTRA_COMMAND";

    /**
     * 透過アップコマンド
     */
    private static String COMMAND_ALPHA_UP = "COMMAND_ALPHA_UP";

    /**
     * 透過ダウンコマンド
     */
    private static String COMMAND_ALPHA_DOWN = "COMMAND_ALPHA_DOWN";

    /**
     * サービスのシャットダウンを行う
     */
    private static String COMMAND_SHUTDOWN = "COMMAND_SHUTDOWN";

    /**
     * アクション
     */
    private static String ACTION_CAMERATRANS_CONTROLL = "com.eaglesakura.ui.ACTION_CAMERATRANS_CONTROLL";

    /**
     * コマンドIntentを作成する
     * @param command
     * @return
     */
    private static Intent makeCommand(String command) {
        return new Intent(ACTION_CAMERATRANS_CONTROLL).putExtra(EXTRA_COMMAND, command);
    }

    /**
     * コマンド内容を取得する
     * @param intent
     * @return
     */
    private static String getCommand(Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(EXTRA_COMMAND);
    }
}
