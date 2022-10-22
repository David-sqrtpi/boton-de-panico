package com.example.botondepanicov1.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.botondepanicov1.R;

public class AlarmService extends Service {
    private MediaPlayer player;
    private AudioManager audioManager;
    private int originalVolume;


    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int resId = R.raw.alarma_sonora;
        player = MediaPlayer.create(this, resId);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setLooping(true);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        player.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (player.isPlaying()) {
            player.stop();
        }

        player.release();
        player = null;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
