package com.example.myapplication;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
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

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    TextView textView;
    Button button;
    Intent intent;
    TextToSpeech textToSpeech;
    SpeechRecognizer mRecognizer;
    final int PERMISSION = 1;

    //현재위치
    //LocationTextView locationTextView;

    private TMapGpsManager tMapGpsManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // 딜레이 시간
    private static final int INITIAL_DELAY = 5000; // 초기 딜레이: 5초
    private static final int RED_LIGHT_DELAY = 10000; // 빨간불 딜레이: 10초
    private static final int GREEN_LIGHT_DELAY = 15000; // 초록불 딜레이: 15초
    private static final int REPEAT_COUNT = 5; // 반복 횟수
    private int repeatCount = 0; // 반복 횟수 초기화

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button gps_btn = findViewById(R.id.gps_btn);

        // 위치 권한 체크 및 요청
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
                    // 초기화 성공
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
        });


        /*// 안드로이드 6.0버전 이상인지 체크해서 퍼미션 체크
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }*/

        textView = findViewById(R.id.nearbyTextView);
        //button = findViewById(R.id.button);

        // 선언
        RelativeLayout relativeLayout = new RelativeLayout(this);
        TMapView tmapview = new TMapView(this);

        /*
        // 키값
        tmapview.setSKTMapApiKey("93q7PGjYQd253Oe16RHcR26OzEnQKF4C8wUw0tv8");
        */
    }

    // 목표 위치
    private static final double TARGET_LATITUDE = 35.239775;
    private static final double TARGET_LONGITUDE = 128.688406;
    private static final double DISTANCE_THRESHOLD = 0.1; // 100m

    @Override
    public void onLocationChange(Location location) { //현재 위치 변경시 호출됨

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Log.d("Location", "현재 위치: " + latitude + ", " + longitude);

        // 현재 위치와 목표 위치 간의 거리 계산
        double distance = calculateDistance(latitude, longitude, TARGET_LATITUDE, TARGET_LONGITUDE);

        // 거리가 일정 임계값 이내인지 확인하여 true 또는 false 출력
        boolean isNearby = (distance < DISTANCE_THRESHOLD); // 100m
        Log.d("Distance", "목표 위치와의 거리: " + distance + "km, 근방 여부: " + isNearby);

        // T/F
        TextView nearbyTextView = findViewById(R.id.nearbyTextView);

        String currentNearby = "목표 위치 근방: " + isNearby;
        nearbyTextView.setText(currentNearby);


        // 현재 위치 텍스트 설정
        TextView locationTextView = findViewById(R.id.locationTextView);

        String currentLocation = "현재 위치: " + latitude + ", " + longitude;
        locationTextView.setText(currentLocation);

        // 거리가 가까우면서 초록불 상태인 경우
        if (isNearby) {
            speak("근처에 신호등이 있습니다.");
            // 반복 횟수가 지정된 횟수보다 적은 경우에만 처리
            if (repeatCount < REPEAT_COUNT) {
                repeatCount++; // 반복 횟수 증가

                // 초록불 상태에서 빨간불 상태로 전환
                speak("초록불입니다. 건너가세요.");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speak("빨간불입니다. 10초 기다리세요.");

                        // 빨간불 상태에서 초록불 상태로 전환
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                speak("초록불입니다. 건너가세요.");
                                onLocationChange(location); // 다음 반복을 위해 다시 호출
                            }
                        }, GREEN_LIGHT_DELAY);
                    }
                }, RED_LIGHT_DELAY);
            } else {
                // 지정된 횟수만큼 반복 완료한 경우에는 종료
                repeatCount = 0; // 반복 횟수 초기화
            }
        }
    }

    // TTS 출력
    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // 딜레이가 있는 TTS 출력
    private void speakWithDelay(final String text, int delayMillis) {
        new Handler().postDelayed(()-> speak(text), delayMillis);
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