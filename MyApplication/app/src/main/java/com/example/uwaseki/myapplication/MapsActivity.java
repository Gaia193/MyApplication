package com.example.uwaseki.myapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;
import java.util.List;

import static com.example.uwaseki.myapplication.ARunit.getSoundTitle;
import static java.sql.Types.NULL;

//音声再生
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.res.AssetFileDescriptor;
import android.widget.Toast;
import java.io.IOException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener, LocationListener, View.OnClickListener {

    private GoogleMap mMap;

    private SensorManager sensorManager;
    private float[] accelerometerValues = new float[3];
    private float[] magneticValues = new float[3];

    List<Sensor> listMag;
    List<Sensor> listAcc;

    private ARunit arView;

    private LocationManager locationManager;
    private GeomagneticField geomagneticField;

    private final static String DB_NAME = "gps_data.db";
    private final static String DB_TABLE = "gps_data";
    private final static int DB_VERSION = 1;
    private SQLiteDatabase db;
    Cursor cursor;

    EditText editText;

    private static final String TAG = "MapsActivity";

    private static double Longitude;
    private static double Latitude;
    private static float direction;


    //音声再生関連(https://akira-watson.com/android/audio-player.html)
    private MediaPlayer mediaPlayer;
    private boolean play = false;
    private String SoundTitle= null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "clicked");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //マップの表示
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        initData();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //moved before setContentView();
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        arView = new ARunit(this, cursor);
        cursor.close();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        listMag = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        listAcc = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        //カメラビュー
        setContentView(new Camera1(this));
        //ARコンテンツをほかのビューと重ねて表示
        addContentView(arView, new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT));
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        sensorManager.registerListener(this, listMag.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, listAcc.get(0), SensorManager.SENSOR_DELAY_NORMAL);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng first_gym = new LatLng(38.276102, 140.752285);
        LatLng fourth_building = new LatLng(38.275709, 140.751810);
        LatLng dormitory = new LatLng(38.277077, 140.751622);
        LatLng andolab = new LatLng(38.276485, 140.751868);
        mMap.addMarker(new MarkerOptions().position(first_gym).title("Marker in 1st gym"));
        mMap.addMarker(new MarkerOptions().position(fourth_building).title("Marker in 4th building"));
        mMap.addMarker(new MarkerOptions().position(dormitory).title("Marker in dormitory"));
        mMap.addMarker(new MarkerOptions().position(andolab).title("Marker in AndoLab"));

        LatLng newLocation = new LatLng(Latitude, Longitude);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(newLocation, 18);
        mMap.moveCamera(update);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,0,this);
        sensorManager.registerListener(this, listMag.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, listAcc.get(0), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause(){
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values.clone();
                break;
        }

        if (magneticValues != null && accelerometerValues != null) {

            float[] orientation = new float[3];
            float R[] = new float[16];
            float I[] = new float[16];

//加速度センサー、磁気センサーの値を元に、回転行列を計算する
            SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticValues);

//デバイスの向きに応じて回転行列を計算する
            SensorManager.getOrientation(R, orientation);

//ラジアンから角度へ変換
            float angle = (float)Math.floor(Math.toDegrees(orientation[0]));
            angle+=90.0;
//角度の範囲を0~360度へ調整(北0°,東90°,南180°,西270°)
            if(angle >=0){
                orientation[0]=angle;
            }else if(angle < 0){
                orientation[0]= 360 + angle;
            }
            direction = orientation[0];
            Log.d(TAG,"onsensorchanged angle="+orientation[0]+"");
        }
    }

    @Override
    public void onLocationChanged(Location arg0) {
        geomagneticField = new GeomagneticField((float) arg0.getLatitude(), (float) arg0.getLongitude(), (float) arg0.getAltitude(), new Date().getTime());
        if(Longitude == NULL && Latitude == NULL){
            Longitude = arg0.getLongitude();
            Latitude = arg0.getLatitude();
            LatLng newLocation = new LatLng(Latitude, Longitude);
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(newLocation, 18);
            mMap.moveCamera(update);
        }
        else {
            Longitude = arg0.getLongitude();
            Latitude = arg0.getLatitude();
        }
        Log.d(TAG,"onlocationchanged lat="+Latitude+" lon="+Longitude+"");
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    public void initData() {
        SQLiteOpenHelperEx helper = new SQLiteOpenHelperEx(this);
        db = helper.getWritableDatabase();

        cursor = db.query(DB_TABLE, new String[]{"info", "latitude", "longitude", "sound"}, null, null, null, null, null);
        if (cursor.getCount() < 1) {
            presetTable();
            cursor = db.query(DB_TABLE, new String[]{"info", "latitude", "longitude", "sound"}, null, null, null, null, null);
        }
    }

    @Override
    public void onClick(View v) {
        //よくわからないのでコメントアウト
        /*
        if (editText.getText().toString().equals("")) {
            Toast.makeText(this, "�e�L�X�g����͂��Ă�������", Toast.LENGTH_LONG).show();
        } else if (geoPoint == null) {
			Toast.makeText(this, "�ʒu��񂪎擾�ł��܂���", Toast.LENGTH_LONG).show();
		} else {
            ContentValues values = new ContentValues();
            values.put("info", editText.getText().toString());
            //values.put("latitude", geoPoint.getLatitudeE6());
            //values.put("longitude", geoPoint.getLongitudeE6());
            db.insert(DB_TABLE, "", values);
            cursor = db.query(DB_TABLE, new String[]{"info", "latitude", "longitude"}, null, null, null, null, null);
            arView.MakeTable(cursor);
            cursor.close();
            editText.setText("");
            Toast.makeText(this, "�e�L�X�g���o�^����܂���", Toast.LENGTH_LONG).show();
        }
        */
    }

    private void presetTable() {
        ContentValues values = new ContentValues();
        //第1体育館西側
        values.put("info", "first gym west side");
        values.put("latitude", 38276102);
        values.put("longitude", 140752285);
        values.put("sound", "amairo");
        db.insert(DB_TABLE, "", values);
        //4号棟ベランダ
        values.put("info", "4th building");
        values.put("latitude", 38275709);
        values.put("longitude", 140751810);
        values.put("sound", "hakucyou");
        db.insert(DB_TABLE, "", values);
        //寮交差点
        values.put("info", "dormitory");
        values.put("latitude", 38277077);
        values.put("longitude", 140751622);
        values.put("sound", "ifudoudou");
        db.insert(DB_TABLE, "", values);
        //安藤研究室
        values.put("info", "AndoLabo");
        values.put("latitude", 38276485);
        values.put("longitude", 140751868);
        values.put("sound", "symphony7");
        db.insert(DB_TABLE, "", values);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public class SQLiteOpenHelperEx extends SQLiteOpenHelper {
        public SQLiteOpenHelperEx(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "create table if not exists " + DB_TABLE + "(info text, latitude numeric, longitude numeric, sound text)";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("drop table if exists " + DB_TABLE);
            onCreate(db);
        }
    }

    public static double getLong(){
        Log.d(TAG,"getLong lon="+Longitude+"");
        return Longitude*1000000;
    }

    public static double getLat(){
        Log.d(TAG,"getLat lat="+Latitude+"");
        return Latitude*1000000;
    }

    public static float getDir(){
        Log.d(TAG, "getDir direction="+direction+"");
        return direction;
    }

    private boolean audioSetup(){
        boolean fileCheck = false;

        // インタンスを生成
        mediaPlayer = new MediaPlayer();

        //音楽ファイル名, あるいはパス
        SoundTitle = getSoundTitle();
        String filePath = SoundTitle +".mp3";

        // assetsから mp3 ファイルを読み込み
        try(AssetFileDescriptor afdescripter = getAssets().openFd(filePath);)
        {
            // MediaPlayerに読み込んだ音楽ファイルを指定
            mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
                    afdescripter.getStartOffset(),
                    afdescripter.getLength());
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return fileCheck;
    }

    private void audioPlay() {

        if (mediaPlayer == null) {
            // audio ファイルを読出し
            if (audioSetup()){
                Toast.makeText(getApplication(), "Rread audio file", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        else{
            // 繰り返し再生する場合
            mediaPlayer.stop();
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();
        }

        // 再生する
        mediaPlayer.start();

        // 終了を検知するリスナー
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("debug","end of audio");
                audioStop();
            }
        });
    }

    private void audioStop() {
        // 再生終了
        mediaPlayer.stop();
        // リセット
        mediaPlayer.reset();
        // リソースの解放
        mediaPlayer.release();

        mediaPlayer = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.i("audio","pressed");
                if(play == false) {
                    play = true;
                    audioPlay();
                }
                else if(mediaPlayer.isPlaying() == false){
                    audioPlay();
                }
                else if(mediaPlayer.isPlaying() == true){
                    audioStop();
                    play = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.i("audio","released");
                break;
            case MotionEvent.ACTION_MOVE:
                // something to do
                break;
            case MotionEvent.ACTION_CANCEL:
                // something to do
                break;
        }

        return false;
    }

}