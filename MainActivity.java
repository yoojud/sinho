package com.example.sinho;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TTS 초기화
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    // TTS 초기화 실패
                    System.out.println("TTS 초기화 실패");
                } else {
                    // TTS 초기화 성공
                    tts.setLanguage(Locale.KOREA);
                    updateTrafficLightStatus(); // 신호등 상태 업데이트
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTS를 중지하고 리소스를 해제합니다.
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void speak(String text) {
        if (tts != null) {
            // 전달된 텍스트를 음성으로 출력
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void updateTrafficLightStatus() {
        // 현재 시간 가져오기
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY); // 현재 시간(24시간 형식) 가져옴

        // 신호등 켜지는 시간과 꺼지는 시간 설정
        int turnOnHour = 7;
        int turnOffHour = 24;

        // 초록불과 빨간불이 지속되는 시간 설정
        int greenLightDuration = 20; // 초록불 지속 시간
        int redLightDuration = 10; // 빨간불 지속 시간

        // 현재 시간에 따라 신호등 상태 업데이트
        String trafficLightColor;
        if (currentHour >= turnOnHour && currentHour < turnOffHour) {
            // 신호등이 켜져 있는 시간일 경우
            int elapsedMinutes = (currentHour - turnOnHour) * 60 + cal.get(Calendar.MINUTE); // 켜진 후 경과시간(분)
            int totalSeconds = elapsedMinutes * 60 + cal.get(Calendar.SECOND); // 초로 변환

            int cycleSeconds = greenLightDuration + redLightDuration; // 신호등 주기 시간(초)
            int remainingSeconds = totalSeconds % cycleSeconds; // 주기 내에서 남은 시간(초)

            if (remainingSeconds < greenLightDuration) {
                // 초록불 상태
                trafficLightColor = "초록불";
                speak("초록불입니다.");
            } else {
                // 빨간불 상태
                trafficLightColor = "빨간불";
                speak("빨간불입니다.");
            }
        } else {
            // 신호등이 꺼져 있는 시간일 경우
            trafficLightColor = "꺼짐";
            speak("신호등이 꺼졌습니다.");
        }
        // 신호등 상태
        System.out.println("현재 신호등 상태: " + trafficLightColor);
    }
}