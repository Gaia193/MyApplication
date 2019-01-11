package com.example.uwaseki.myapplication;
//ARの表示を行う

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.GeomagneticField;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

public class ARunit extends View{
    float pre_dire;
    float dire;
    int lon;
    int lat;
    private int ang = 30;
    private float range = 50;
    private int dis;
    private ArrayList<ARData> list;
    private static final String TAG = "ARunit";

    //音声再生関連(https://akira-watson.com/android/audio-player.html)
    //AssetFileDescriptor afdescripter = getAssets().openFd(filePath);

    public ARunit(Context context, Cursor cursor) {
        super(context);
        MakeTable(cursor);
        Display DX = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        dis = DX.getWidth();
    }



    public void MakeTable(Cursor cursor) {

        if (list != null)
            list.clear();
        list = new ArrayList<ARData>();
        //DBからリストにデータを追加
        cursor.moveToFirst();
        do {
            ARData data = new ARData();
            data.info = cursor.getString(0);
            data.latitude = cursor.getInt(1);
            data.longitude = cursor.getInt(2);
            list.add(data);
        } while (cursor.moveToNext());
    }

    class ARData {
        public String info;
        public int latitude;
        public int longitude;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        for (int i = 0; i < list.size(); i++) {

            ARData data = list.get(i);
            String info = data.info;
            int y = data.latitude;
            int x = data.longitude;
            Log.d(TAG,"ARData "+info+" "+y+" "+x+"");

            lon = (int) MapsActivity.getLong();
            lat = (int) MapsActivity.getLat();
            Log.d(TAG,"ARunit 座標取得 lat="+lat+" lon="+lon+"");

            //端末との距離計算、遠ければ処理継続
            double dx = (x - lon);
            double dy = (y - lat);
            float distance = (float) Math.sqrt(Math.pow(dy, 2) + Math.pow(dx, 2));
            //Log.d(TAG,"ARunit距離計算 "+dx+" "+dy+" "+distance+"");


            if (distance > 700) {
                Log.d(TAG,"too far "+distance+"");
                continue;
            }

            Log.d(TAG,"700以内 "+info+" "+distance+"");


            //方角計算（ラジアンを角度に）
            double angle = Math.atan2(dy, dx);
            //float degree = (float) Math.toDegrees(angle);
            float degree = (float) (angle * 180.0 / Math.PI);
            degree = -degree + 90;
            if (degree < 0) degree = 360 + degree;
            pre_dire = dire;
            //向いている方位取得
            dire = MapsActivity.getDir();
            float sub = degree - dire;
            if (sub < -180.0) sub += 360;
            if (sub > 180.0) sub -= 360;
            Log.d(TAG,"degree="+degree+" dire="+dire+" sub= "+sub+"");

            //端末とターゲットの角度が30度未満の場合
            if (Math.abs(sub) < (30)) {
                //距離に応じて文字サイズ変更
                float textSize = (float) (range - distance * 0.1);
                paint.setTextSize(textSize);
                //文字数
                float textWidth = paint.measureText(info);
                float diff = (sub / (30)) / 2;
                float left = (dis / 2 + dis * diff) - (textWidth / 2);
                drawBalloonText(canvas, paint, info, left, 55);
            }
        }
        try{
            Thread.sleep(10);
        }
        catch(InterruptedException e){}
            invalidate();
    }
    //吹き出し文字表示
    private void drawBalloonText(Canvas canvas, Paint paint, String text, float left, float top) {
        float textWidth = paint.measureText(text);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();

        float bLeft = left - 5;
        float bRight = left + textWidth + 5;
        float bTop = top + fontMetrics.ascent - 5;
        float bBottom = top + fontMetrics.descent + 5;

       RectF rectF = new RectF(bLeft, bTop, bRight, bBottom);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(rectF, 5, 5, paint);

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        Path path = new Path();
        float center = left + textWidth / 2;
        float triangleSize = paint.getTextSize() / 3;
        path.moveTo(center, bBottom + triangleSize);
        path.lineTo(center - triangleSize / 2, bBottom - 1);
        path.lineTo(center + triangleSize / 2, bBottom - 1);
        path.lineTo(center, bBottom + triangleSize);
        canvas.drawPath(path, paint);

        paint.setColor(Color.BLACK);
        canvas.drawText(text, left, top, paint);
    }
}

