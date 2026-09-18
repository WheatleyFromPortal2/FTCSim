package org.firstinspires.ftc.vision;

import android.graphics.Bitmap;
import android.util.Size;
import com.qualcomm.robotcore.util.RobotLog;
import ftcsim.vision.CameraMount;
import ftcsim.vision.SimVision;
import ftcsim.vision.SimVisionProcessor;
import ftcsim.hardware.SimWebcamName;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FTCSim replacement for the SDK's VisionPortalImpl. There is no camera: the portal runs a 30 Hz
 * "frame" loop that feeds simulated processors (AprilTag detections computed from the true robot
 * pose). Processors written by the team are initialised but never receive frames.
 */
public class VisionPortalImpl extends VisionPortal {
    private static final String TAG = "VisionPortal";
    private static final double FPS = 30.0;

    private final CameraName cameraName;
    private volatile WebcamName activeCamera;
    private final VisionProcessor[] processors;
    private final boolean[] processorsEnabled;
    private final Size resolution;
    private volatile CameraState cameraState = CameraState.OPENING_CAMERA_DEVICE;
    private volatile Thread frameThread;
    private volatile long frames;
    private final CameraMount mount;
    private final List<String> notSimulated = new ArrayList<>();

    public VisionPortalImpl(CameraName camera, int cameraMonitorViewId, boolean autoPauseCameraMonitor, Size cameraResolution,
                            StreamFormat webcamStreamFormat, boolean autoStartStream, boolean showStats, VisionProcessor[] processors) {
        this.cameraName = camera;
        this.processors = processors == null ? new VisionProcessor[0] : processors.clone();
        this.processorsEnabled = new boolean[this.processors.length];
        java.util.Arrays.fill(processorsEnabled, true);
        this.resolution = cameraResolution != null ? cameraResolution : new Size(640, 480);
        if (camera instanceof WebcamName) activeCamera = (WebcamName) camera;
        else if (camera instanceof SwitchableCameraName) { CameraName[] members = ((SwitchableCameraName) camera).getMembers(); if (members.length > 0 && members[0] instanceof WebcamName) activeCamera = (WebcamName) members[0]; }
        this.mount = mountFor(activeCamera != null ? activeCamera : camera);
        int w = this.resolution.getWidth(), h = this.resolution.getHeight();
        for (VisionProcessor p : this.processors) {
            try { p.init(w, h, null); } catch (RuntimeException e) { RobotLog.ww(TAG, e, "[sim] %s.init() failed", p.getClass().getSimpleName()); }
            if (!(p instanceof SimVisionProcessor)) notSimulated.add(p.getClass().getSimpleName());
        }
        if (!notSimulated.isEmpty()) RobotLog.ww(TAG, "[sim] no camera frames are simulated; these processors will not produce results: %s", notSimulated);
        cameraState = CameraState.CAMERA_DEVICE_READY;
        SimVision.register(this);
        RobotLog.ii(TAG, "[sim] VisionPortal on %s (%dx%d) camera mount %s, processors %d", cameraLabel(), w, h, mount, this.processors.length);
        if (autoStartStream) resumeStreaming();
    }

    private static CameraMount mountFor(CameraName camera) {
        if (camera instanceof SimWebcamName) return ((SimWebcamName) camera).mount();
        return new CameraMount();
    }

    public String cameraLabel() {
        if (activeCamera instanceof SimWebcamName) return ((SimWebcamName) activeCamera).simName();
        if (cameraName == null) return "camera";
        return cameraName.isCameraDirection() ? "phone camera" : cameraName.toString();
    }

    private void frameLoop() {
        ftcsim.hardware.HardwareBus.markExemptThread();
        final Thread self = Thread.currentThread();
        long next = System.nanoTime();
        while (frameThread == self && !self.isInterrupted()) {
            long now = System.nanoTime();
            for (int i = 0; i < processors.length; i++) {
                if (!processorsEnabled[i] || !(processors[i] instanceof SimVisionProcessor)) continue;
                try { ((SimVisionProcessor) processors[i]).simFrame(mount, resolution.getWidth(), resolution.getHeight(), now); }
                catch (RuntimeException e) { RobotLog.ee(TAG, e, "[sim] processor failed"); }
            }
            frames++;
            next += (long) (1e9 / FPS);
            long sleep = next - System.nanoTime();
            if (sleep > 0) { try { Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L)); } catch (InterruptedException e) { return; } }
            else next = System.nanoTime();
        }
    }

    @Override public void setProcessorEnabled(VisionProcessor processor, boolean enabled) {
        for (int i = 0; i < processors.length; i++) if (processors[i] == processor) { processorsEnabled[i] = enabled; return; }
        throw new IllegalArgumentException("Processor not attached to this VisionPortal");
    }
    @Override public boolean getProcessorEnabled(VisionProcessor processor) {
        for (int i = 0; i < processors.length; i++) if (processors[i] == processor) return processorsEnabled[i];
        throw new IllegalArgumentException("Processor not attached to this VisionPortal");
    }
    @Override public CameraState getCameraState() { return cameraState; }
    @Override public void saveNextFrameRaw(String filename) { RobotLog.ii(TAG, "[sim] saveNextFrameRaw(%s): no frames are simulated", filename); }
    @Override public synchronized void stopStreaming() {
        if (cameraState == CameraState.CAMERA_DEVICE_CLOSED) return;
        Thread t = frameThread; frameThread = null;
        if (t != null) t.interrupt();
        cameraState = CameraState.CAMERA_DEVICE_READY;
    }
    @Override public synchronized void resumeStreaming() {
        if (cameraState == CameraState.CAMERA_DEVICE_CLOSED || frameThread != null) return;
        cameraState = CameraState.STARTING_STREAM;
        Thread t = new Thread(this::frameLoop, "ftcsim-vision-" + cameraLabel());
        t.setDaemon(true);
        frameThread = t;
        t.start();
        cameraState = CameraState.STREAMING;
    }
    @Override public void stopLiveView() {}
    @Override public void resumeLiveView() {}
    @Override public float getFps() { return cameraState == CameraState.STREAMING ? (float) FPS : 0f; }

    @SuppressWarnings("unchecked")
    @Override public <T extends CameraControl> T getCameraControl(Class<T> controlType) {
        if (controlType == null || !controlType.isInterface()) return null;
        InvocationHandler h = new InvocationHandler() {
            private final Map<String, Object> values = new LinkedHashMap<>();
            @Override public Object invoke(Object proxy, Method m, Object[] args) {
                String n = m.getName();
                if (n.equals("toString")) return "Sim" + controlType.getSimpleName();
                if (n.equals("hashCode")) return System.identityHashCode(proxy);
                if (n.equals("equals")) return proxy == args[0];
                Class<?> r = m.getReturnType();
                if (n.startsWith("set") && args != null && args.length > 0) { values.put(n.substring(3), args[0]); return r == boolean.class ? Boolean.TRUE : null; }
                if (n.startsWith("get") && values.containsKey(n.substring(3))) { Object v = values.get(n.substring(3)); if (r.isInstance(v) || r.isPrimitive()) return v; }
                if (n.startsWith("is")) return Boolean.TRUE;
                if (r == boolean.class) return Boolean.TRUE;
                if (r == int.class) return n.contains("Max") ? 100 : n.contains("Min") ? 1 : 0;
                if (r == long.class) return n.contains("Max") ? 100L : n.contains("Min") ? 1L : 0L;
                if (r == double.class) return n.contains("Max") ? 100.0 : n.contains("Min") ? 1.0 : 0.0;
                if (r == float.class) return 0f;
                if (r.isEnum()) { Object[] c = r.getEnumConstants(); return c.length > 0 ? c[0] : null; }
                return null;
            }
        };
        return (T) Proxy.newProxyInstance(controlType.getClassLoader(), new Class<?>[] { controlType }, h);
    }

    @Override public void setActiveCamera(WebcamName webcamName) { activeCamera = webcamName; }
    @Override public WebcamName getActiveCamera() { return activeCamera; }

    @Override public void getFrameBitmap(Continuation<? extends Consumer<Bitmap>> continuation) {
        final Bitmap bitmap = Bitmap.createBitmap(resolution.getWidth(), resolution.getHeight(), Bitmap.Config.ARGB_8888);
        continuation.dispatch(consumer -> consumer.accept(bitmap));
    }

    @Override public synchronized void close() {
        stopStreaming();
        cameraState = CameraState.CAMERA_DEVICE_CLOSED;
        SimVision.unregister(this);
    }

    /** Values shown in the simulator's device panel. */
    public Map<String, Object> view() {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("state", cameraState.name());
        v.put("fps", getFps());
        v.put("frames", frames);
        v.put("resolution", resolution.getWidth() + "x" + resolution.getHeight());
        v.put("mount", mount.toString());
        List<String> ps = new ArrayList<>();
        for (int i = 0; i < processors.length; i++) {
            String s = processors[i].getClass().getSimpleName() + (processorsEnabled[i] ? "" : " (disabled)");
            if (processors[i] instanceof SimVisionProcessor) s += " " + ((SimVisionProcessor) processors[i]).simSummary(); else s += " (not simulated)";
            ps.add(s);
        }
        v.put("processors", ps);
        return v;
    }
}
