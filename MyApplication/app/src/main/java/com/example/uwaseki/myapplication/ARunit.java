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
    private float range = 80;
    private int dis;
    private ArrayList<ARData> list;
    private static final String TAG = "ARunit";
    private static String SoundTitle = null;
    private static String ImageTitle = null;

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
            data.sound = cursor.getString(3);
            data.image = cursor.getString(4);
            list.add(data);
        } while (cursor.moveToNext());
    }

    class ARData {
        public String info;
        public int latitude;
        public int longitude;
        public String sound;
        public String image;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float mindis = 99999;

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        for (int i = 0; i < list.size(); i++) {
            ARData data = list.get(i);
            String info = data.info;
            int y = data.latitude;
            int x = data.longitude;
            Log.d(TAG,"ARData "+info+" "+y+" "+x+"" +data.sound);

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

            if (distance < 300) {
                if(mindis > distance) {
                    mindis = distance;
                    SoundTitle = data.sound;
                    ImageTitle = data.image;
                    Log.i(TAG,"sound data"+SoundTitle);
                    Log.i(TAG,"image data"+ImageTitle);
                }
            }



            //方角計算（ラジアンを角度に） ターゲットの方位
            double angle = Math.atan2(dy, dx);
            //float degree = (float) Math.toDegrees(angle);
            float degree = (float) (angle * 180.0 / Math.PI);
            degree = -degree + 90;
            if (degree < 0) degree = 360 + degree;
            pre_dire = dire;
            //向いている方位取得
            dire = MapsActivity.getDir();
            dire = (dire + pre_dire)/2;
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
                //float diff = (sub / (30)) / 2;
                float pos = canvas.getWidth()/2 - textSize;
                if(Math.abs(sub) < 10){
                    pos = canvas.getWidth()/2 - textSize;
                }
                else if(sub < 0){
                    pos = canvas.getWidth()/6;
                }
                else if(sub > 0){
                    pos = canvas.getWidth()/6 * 4;
                }
                drawBalloonText(canvas, paint, info, pos, (float)(textSize * 1.5));
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

    public static String getSoundTitle(){
        return SoundTitle;
    }

    public static String getImageTitle(){
        return ImageTitle;
    }
}

