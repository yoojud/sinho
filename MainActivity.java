package com.example.myapplication;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

// GPS
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    TextView textView;
    Button button;
    Intent intent;
    TextToSpeech textToSpeech;
    SpeechRecognizer mRecognizer;
    final int PERMISSION = 1;

    private TMapGpsManager tMapGpsManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // 딜레이 시간
    private static final int RED_LIGHT_DURATION = 5000; // 빨간불 딜레이: 10초 -3초
    private static final int GREEN_LIGHT_DURATION = 5000; // 초록불 딜레이: 15초 -5초

    private boolean isNearby = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SQLite
        DBHelper helper;
        SQLiteDatabase db;
        helper = new DBHelper(MainActivity.this);
        db = helper.getWritableDatabase();
        helper.onCreate(db);

        Button gps_btn = findViewById(R.id.gps_btn);

        // 위치 권한 체크 및 요청
        // 권한이 허용되지 않으면 요청, 허용되면 gps 초기화
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initializeGpsManager();
        }

        // TTS 초기화
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // 초기화성공
                    int result = textToSpeech.setLanguage(Locale.KOREA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // TTS 지원되지 않음
                        Toast.makeText(MainActivity.this, "TTS가 지원되지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 초기화 실패
                    Toast.makeText(MainActivity.this, "TTS가 초기화에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        })
        ;

        textView = findViewById(R.id.nearbyTextView);

        // 선언
        RelativeLayout relativeLayout = new RelativeLayout(this);
        TMapView tmapview = new TMapView(this);

        /*
        // 키값
        tmapview.setSKTMapApiKey("93q7PGjYQd253Oe16RHcR26OzEnQKF4C8wUw0tv8");
        */
    }

    // 목표 위치
    private static final double TARGET_LATITUDE = 35.2461107;
    private static final double TARGET_LONGITUDE = 128.6907611;
    private static final double DISTANCE_THRESHOLD = 0.1; // 100m


    @Override
    public void onLocationChange(Location location) { //현재 위치 변경시 호출됨
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Log.d("Location", "현재 위치: " + latitude + ", " + longitude);

        // 현재 위치와 목표 위치 간의 거리 계산
        double distance = calculateDistance(latitude, longitude, TARGET_LATITUDE, TARGET_LONGITUDE);

        // 거리가 일정 임계값 이내인지 확인하여 true 또는 false 출력
        isNearby = (distance < DISTANCE_THRESHOLD); // 100m
        Log.d("Distance", "목표 위치와의 거리: " + distance + "km, 근방 여부: " + isNearby);

        // T/F
        TextView nearbyTextView = findViewById(R.id.nearbyTextView);

        String currentNearby = "목표 위치 근방: " + isNearby;
        nearbyTextView.setText(currentNearby);

        // 현재 위치 텍스트 설정
        TextView locationTextView = findViewById(R.id.locationTextView);

        String currentLocation = "현재 위치: " + latitude + ", " + longitude;
        locationTextView.setText(currentLocation);

        if (isNearby) {
            speak("근처에 신호등이 있습니다.");
            sinho(); // isNearby가 true일 때만
        }
    }

    public void sinho() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // 초록불과 빨간불이 켜지는 시간을 초기화합니다.
        int greenLightDelay = GREEN_LIGHT_DURATION;
        int redLightDelay = RED_LIGHT_DURATION;

        if (hour >= 7 && hour < 24) {
            // 초록불 상태에서 빨간불 상태로 전환하는 로직 추가
            speak("초록불."); // 초록불 알림
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    speak("빨간불."); // 빨간불 알림
                    // 빨간불이 켜진 후에 다시 초록불로 전환하는 로직 추가
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sinho(); // 다음 반복을 위해 sinho() 호출
                        }
                    }, redLightDelay);
                }
            }, greenLightDelay);
        } else {
            speak("안내 시간이 아닙니다.");
        }
    }

    // TTS 출력
    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // 거리 계산
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344; // 마일에서 킬로미터로 변환
        return dist;
    }

    // 위치 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeGpsManager();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeGpsManager() {
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setMinTime(100);
        tMapGpsManager.setMinDistance(5);
        tMapGpsManager.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tMapGpsManager != null) {
            tMapGpsManager.CloseGps();
        }
    }

}