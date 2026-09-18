package com.qualcomm.ftccommon;

import android.content.Context;
import com.qualcomm.robotcore.util.RobotLog;
import org.firstinspires.ftc.robotcore.external.function.Consumer;

import java.io.File;

/**
 * FTCSim replacement for the SDK's SoundPlayer. Sounds cannot be played on the
 * desktop; every request is logged (and shown in the UI log) instead.
 */
public class SoundPlayer {
    public static final String TAG = "SoundPlayer";
    public static boolean TRACE = true;
    public static final int msSoundTransmissionFreshness = 400;

    private static final SoundPlayer theInstance = new SoundPlayer(3, 6);
    protected float masterVolume = 1.0f;

    public static SoundPlayer getInstance() { return theInstance; }
    public SoundPlayer(int simultaneousStreams, int cacheSize) {}

    public void close() {}
    public void prefillSoundCache(int... resourceIds) {}
    public void startPlaying(Context context, int resId) { note("startPlaying(resource " + resId + ")"); }
    public void startPlaying(Context context, File file) { note("startPlaying(" + file + ")"); }
    public void startPlaying(Context context, int resId, PlaySoundParams params, Consumer<Integer> runWhenStarted, Runnable runWhenFinished) {
        note("startPlaying(resource " + resId + ")");
        if (runWhenStarted != null) runWhenStarted.accept(1000);
        if (runWhenFinished != null) runWhenFinished.run();
    }
    public void startPlaying(Context context, File file, PlaySoundParams params, Consumer<Integer> runWhenStarted, Runnable runWhenFinished) {
        note("startPlaying(" + file + ")");
        if (runWhenStarted != null) runWhenStarted.accept(1000);
        if (runWhenFinished != null) runWhenFinished.run();
    }
    public void stopPlayingAll() { note("stopPlayingAll()"); }
    public void stopPlayingLoops() { note("stopPlayingLoops()"); }
    public boolean preload(Context context, int resourceId) { return true; }
    public boolean preload(Context context, File file) { return file != null && file.exists(); }
    public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }
    public float getMasterVolume() { return masterVolume; }
    public void play(Context context, int resId) { startPlaying(context, resId); }
    public void play(Context context, int resId, boolean waitForCompletion) { startPlaying(context, resId); }
    public void play(Context context, int resId, float volume, int loop, float rate) { startPlaying(context, resId); }
    public void play(Context context, File file, float volume, int loop, float rate) { startPlaying(context, file); }
    public boolean isLocalSoundOn() { return true; }

    private void note(String what) { RobotLog.ii(TAG, "[sim] sound request: %s (audio is not available in FTCSim)", what); }

    public static class PlaySoundParams {
        public float volume = 1.0f;
        public boolean waitForNonLoopingSoundsToFinish = true;
        public int loopControl = 0;
        public float rate = 1.0f;
        public PlaySoundParams() {}
        public PlaySoundParams(boolean wait) { this.waitForNonLoopingSoundsToFinish = wait; }
        public PlaySoundParams(PlaySoundParams them) { volume = them.volume; waitForNonLoopingSoundsToFinish = them.waitForNonLoopingSoundsToFinish; loopControl = them.loopControl; rate = them.rate; }
        public boolean isLooping() { return loopControl == -1; }
    }
}
