package com.helpyou.itproject;

import android.content.Context;
import android.media.AudioManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AudioPlayerTest {
    private final String TAG = "AudioPlayerTest";
    AudioPlayer audioPlayer;
    Context context;
    AudioManager audioManager;
    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        audioPlayer = new AudioPlayer(context);
        audioManager = mock(AudioManager.class);
        when((AudioManager)  context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Test
    public void test1() throws InterruptedException {

        audioPlayer.playProgressTone();
        Thread.sleep(5000);
        audioPlayer.stopProgressTone();
        Thread.sleep(100);
    }

    @After
    public void tearDown() throws Exception {

    }

}