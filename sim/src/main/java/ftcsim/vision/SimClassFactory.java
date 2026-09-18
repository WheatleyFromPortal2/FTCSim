package ftcsim.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraName;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraNameImpl;
import org.firstinspires.ftc.robotcore.internal.camera.names.BuiltinCameraNameImpl;
import org.firstinspires.ftc.robotcore.internal.camera.names.UnknownCameraNameImpl;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** The SDK's ClassFactory for the simulator: only the camera-name helpers do anything (no camera is ever opened). */
public final class SimClassFactory extends ClassFactory {
    private static final String TAG = "ClassFactory";
    private final Supplier<HardwareMap> hardwareMap;
    private final CameraManager cameraManager = new CameraManager() {
        @Override public List<WebcamName> getAllWebcams() {
            List<WebcamName> out = new ArrayList<>();
            HardwareMap hm = hardwareMap.get();
            if (hm != null) for (WebcamName n : hm.getAll(WebcamName.class)) out.add(n);
            return out;
        }
        @Override public CameraName nameFromCameraDirection(BuiltinCameraDirection direction) { return BuiltinCameraNameImpl.forCameraDirection(direction); }
        @Override public CameraName nameForUnknownCamera() { return UnknownCameraNameImpl.forUnknown(); }
        @Override public SwitchableCameraName nameForSwitchableCamera(CameraName... members) { return SwitchableCameraNameImpl.forSwitchable(members); }
        @Override public Camera requestPermissionAndOpenCamera(Deadline deadline, CameraName cameraName, Continuation<? extends Camera.StateCallback> continuation) {
            RobotLog.ww(TAG, "[sim] cameras cannot be opened directly; use VisionPortal");
            return null;
        }
        @Override public void asyncOpenCameraAssumingPermission(CameraName cameraName, Continuation<? extends Camera.StateCallback> continuation, long timeout, TimeUnit unit) {
            RobotLog.ww(TAG, "[sim] cameras cannot be opened directly; use VisionPortal");
        }
    };

    private SimClassFactory(Supplier<HardwareMap> hardwareMap) { this.hardwareMap = hardwareMap; }

    @Override public CameraManager getCameraManager() { return cameraManager; }

    /** Makes ClassFactory.getInstance() return the simulator's factory. */
    public static void install(Supplier<HardwareMap> hardwareMap) {
        try {
            Class<?> holder = Class.forName("org.firstinspires.ftc.robotcore.external.ClassFactory$InstanceHolder");
            Field f = holder.getField("theInstance");
            f.setAccessible(true);
            f.set(null, new SimClassFactory(hardwareMap));
        } catch (ReflectiveOperationException | RuntimeException e) {
            RobotLog.ww(TAG, e, "[sim] could not install the simulator ClassFactory");
        }
    }
}
