package org.openftc.easyopencv;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;

/**
 * FTCSim stand-in for EasyOpenCV's camera factory. Camera frames are not simulated; only the
 * viewport-splitting helper that VisionPortal.makeMultiPortalView() needs does real work.
 */
public abstract class OpenCvCameraFactory {
    public enum ViewportSplitMethod { VERTICALLY, HORIZONTALLY }

    private static final OpenCvCameraFactory INSTANCE = new OpenCvCameraFactory() {
        private int nextId = 100;
        @Override public synchronized int[] splitLayoutForMultipleViewports(int containerId, int numViewports, ViewportSplitMethod method) {
            int[] ids = new int[Math.max(0, numViewports)];
            for (int i = 0; i < ids.length; i++) ids[i] = nextId++;
            return ids;
        }
    };

    public static OpenCvCameraFactory getInstance() { return INSTANCE; }

    private static UnsupportedOperationException notSimulated() {
        return new UnsupportedOperationException("EasyOpenCV cameras are not simulated by FTCSim; use VisionPortal with an AprilTagProcessor instead");
    }
    public OpenCvInternalCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction) { throw notSimulated(); }
    public OpenCvInternalCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction, int viewportContainerId) { throw notSimulated(); }
    public OpenCvInternalCamera2 createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction) { throw notSimulated(); }
    public OpenCvInternalCamera2 createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction, int viewportContainerId) { throw notSimulated(); }
    public OpenCvWebcam createWebcam(WebcamName cameraName) { throw notSimulated(); }
    public OpenCvWebcam createWebcam(WebcamName cameraName, int viewportContainerId) { throw notSimulated(); }
    public OpenCvSwitchableWebcam createSwitchableWebcam(WebcamName... cameraNames) { throw notSimulated(); }
    public OpenCvSwitchableWebcam createSwitchableWebcam(int viewportContainerId, WebcamName... cameraNames) { throw notSimulated(); }
    public abstract int[] splitLayoutForMultipleViewports(int containerId, int numViewports, ViewportSplitMethod method);
}
