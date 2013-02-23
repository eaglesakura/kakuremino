package com.eaglesakura.kakuremino.texture;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.WindowManager;

/**
 * TextureViewの状態遷移に対応するListener
 */
public class CameraSurfaceTextureListener implements SurfaceTextureListener {
    /**
     * ログ用タグ
     */
    private static final String TAG = CameraSurfaceTextureListener.class.getSimpleName();

    /**
     * TODO {@link TextureView#setAlpha(float)} で指定できる最低の透過値を設定する
     */
    private static final float VIEW_ALPHA_MIN = 0.1f;

    /**
     * TODO {@link TextureView#setAlpha(float)} で指定できる最大の透過値を設定する
     */
    private static final float VIEW_ALPHA_MAX = 0.8f;

    /**
     * TODO 透過度の遷移量を調整可能。
     * Alphaの一度に動く量を定義する。
     * {@link StatusBarMenu#VIEW_ALPHA_MIN} <= {@link StatusBarMenu#VIEW_ALPHA_MAX} の間で値は遷移する。
     */
    private static final float ALPHA_MOVE = 0.1f;

    /**
     * 作成元のContext
     */
    Context context;

    /**
     * ハードウェアカメラ
     */
    Camera camera = null;

    /**
     * プレビューのアスペクト比
     */
    double previewAspect = 0;

    /**
     * 操作対象のTextureView
     */
    TextureView textureView;

    public CameraSurfaceTextureListener(TextureView textureView, Context context) {
        this.context = context;
        this.textureView = textureView;
    }

    /**
     * サーフェイスの準備ができた場合に呼び出される。<BR>
     * 「隠れ蓑」では<BR>
     * １．カメラの取得<BR>
     * ２．カメラのプレビュー対象設定<BR>
     * ３．プレビュー開始
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // カメラを取得する。
        // カメラを開いている間、他のアプリ（例えばカメラアプリ）はカメラを利用できない。
        camera = Camera.open();

        try {
            // プレビュー対象のテクスチャを設定する。
            camera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("camera initialize error!!");
        }

        // カメラのプレビュー解像度、回転角を更新する
        updateCamera(width, height);
    }

    /**
     * {@link SurfaceTexture#release()} を自動的に呼びたい場合はtrueを返す。<BR>
     * 基本的にtrueを返せば問題ないが、細かいリソースコントロールを行いたい場合はfalseを返して自由なタイミングでrelease()を行える。
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        camera.stopPreview(); // カメラのプレビュー表示を終了させる
        camera.release(); // カメラの利用を終了させる

        // SurfaceTextureの廃棄は自動で行わせる
        return true;
    }

    /**
     * {@link android.view.TextureView}のサイズが更新された際に呼び出される。<BR>
     * 例えば、アプリやHome画面が縦横を切り替えた場合に発生する。<BR>
     * このタイミングでカメラの内容を更新している。
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");

        // カメラのプレビュー解像度、回転角を更新する
        updateCamera(width, height);
    }

    /**
     * {@link android.view.TextureView}の描画内容が更新された際に呼び出される。<BR>
     * カメラと連携させた場合、秒間数十回呼び出されることになるため、内部の処理は重くならないように注意が必要になる。
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //        Log.d(TAG, "onSurfaceTextureUpdated");
    }

    /**
     * アルファ値を上昇（不透明化）させる。
     */
    public void alphaUp() {
        final float a = textureView.getAlpha();
        textureView.setAlpha(Math.min(VIEW_ALPHA_MAX, a + ALPHA_MOVE));
    }

    /**
     * アルファ値を減少（透明化）させる。
     */
    public void alphaDown() {
        final float a = textureView.getAlpha();
        textureView.setAlpha(Math.max(VIEW_ALPHA_MIN, a - ALPHA_MOVE));
    }

    /**
     * カメラのプレビュー停止 -> カメラのパラメータ指定 -> カメラプレビュー開始を行う
     */
    private void updateCamera(int displayWidth, int displayHeight) {
        {
            // アスペクトを設定する
            // カメラのハードウェアは基本的に横オンリーのため、長い方をwidthとして扱う
            previewAspect = (double) Math.max(displayWidth, displayHeight)
                    / (double) Math.min(displayWidth, displayHeight);
        }
        // カメラのプレビューを開始する
        camera.stopPreview();
        {
            // カメラの向き設定
            updateCameraDisplayOrientation();
            // アスペクト比を修正
            updateCameraPreviewAspect();
        }
        camera.startPreview();
    }

    /**
     * カメラのプレビュー向きと端末の傾きを合わせる。
     */
    private void updateCameraDisplayOrientation() {
        // ディスプレイ
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // 端末の回転角
        final int deviceRotate = windowManager.getDefaultDisplay().getRotation();

        // 端末の回転状態に合わせて、カメラのプレビューも回転させる
        switch (deviceRotate) {
            case Surface.ROTATION_0:
                camera.setDisplayOrientation(90);
                break;
            case Surface.ROTATION_90:
                camera.setDisplayOrientation(0);
                break;
            case Surface.ROTATION_180:
                camera.setDisplayOrientation(270);
                break;
            case Surface.ROTATION_270:
                camera.setDisplayOrientation(180);
                break;
        }

    }

    /**
     * 正しいカメラアスペクト比になるようなプレビューサイズを設定する。
     * サンプルのアスペクト比は近似であり、正確性は保証しない。
     */
    private void updateCameraPreviewAspect() {
        Parameters params = camera.getParameters();
        // プレビューに用いるカメラの解像度（width&height）
        // 端末画面の解像度とプレビュー解像度が完全一致する場合は少ないため、アスペクト（縦横）比が近い解像度を利用する
        Size previewSize = null;

        {
            // サポートしているプレビュー解像度一覧を取り出す
            List<Size> sizes = params.getSupportedPreviewSizes();

            double minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                double aspect = (double) Math.max(size.width, size.height) / (double) Math.min(size.width, size.height);
                double tempDiff = Math.abs(previewAspect - aspect);
                // 比率の差が少ない、より小さいプレビューサイズを選ぶ
                if (tempDiff < minDiff) {
                    minDiff = tempDiff;
                    previewSize = size;
                }
            }
            Log.d(TAG, String.format("preview result-size(%d, %d)", previewSize.width, previewSize.height));
            Log.d(TAG, String.format("aspect-size(%f :: %f)", minDiff + previewAspect, previewAspect));
        }
        // プレビューサイズを指定する
        params.setPreviewSize(previewSize.width, previewSize.height);
        camera.setParameters(params);
    }
}
