package com.example.uwaseki.myapplication;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

//サーフェイスビューを使う(旧式のやり方)
public class Camera1 extends SurfaceView implements SurfaceHolder.Callback, View.OnClickListener {

    private SurfaceHolder SH;
    private Camera CM;
    public static final String TAG = MapsActivity.class.getSimpleName();

    private Button sound;

    public Camera1(Context context) {
        super(context);

        //sound = (Button)this.findViewById(R.id.soundbutton);
        //sound.setOnClickListener(this);

        SH = getHolder();
        SH.addCallback(this);
        SH.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //ここでonclickやればいける？

    }

    @Override
    public void surfaceCreated(SurfaceHolder sh) {
        try {
            CM = Camera.open();
            CM.setPreviewDisplay(SH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder sh, int i, int i1, int i2) { CM.startPreview(); }

    @Override
    public void surfaceDestroyed(SurfaceHolder sh) {
        CM.setPreviewCallback(null);
        CM.stopPreview();
        CM.release();
        CM = null;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "sound");
    }
}
