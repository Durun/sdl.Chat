package jp.ac.titech.itpro.sdl.chat;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

class SoundPlayer {
    private final SoundPool soundPool;
    private final int soundConnected;
    private final int soundDisconnected;
    private final int soundRing;

    SoundPlayer(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attrs)
                .setMaxStreams(1)
                .build();

        // Sound Effects by NHK Creative Library
        // https://www2.nhk.or.jp/archives/creative/material/view.cgi?m=D0002011524_00000
        soundConnected = soundPool.load(context, R.raw.nhk_doorbell, 1);
        // https://www2.nhk.or.jp/archives/creative/material/view.cgi?m=D0002070102_00000
        soundDisconnected = soundPool.load(context, R.raw.nhk_woodblock2, 1);

        soundRing = soundPool.load(context, R.raw.pico_001, 1);
    }

    void playConnected() {
        soundPool.play(soundConnected, 1.0f, 1.0f, 0, 0, 1);
    }

    void playDisconnected() {
        soundPool.play(soundDisconnected, 1.0f, 1.0f, 0, 0, 1);
    }

    void playRing() {
        soundPool.play(soundRing, 1.0f, 1.0f, 0, 0, 1);
    }
}
