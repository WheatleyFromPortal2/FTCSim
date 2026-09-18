package org.firstinspires.ftc.vision.opencv;

import android.graphics.Canvas;
import com.qualcomm.robotcore.util.RobotLog;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.core.Mat;

/** FTCSim stand-in: without camera frames the analysis always reports black. */
class PredominantColorProcessorImpl extends PredominantColorProcessor {
    private final Result result;

    PredominantColorProcessorImpl(ImageRegion roi, Swatch[] swatches) {
        Swatch black = null;
        for (Swatch s : Swatch.values()) if (s.name().equals("BLACK")) black = s;
        if (black == null && swatches != null && swatches.length > 0) black = swatches[0];
        result = new Result(black, 0xFF000000, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }, new int[] { 16, 128, 128 });
        RobotLog.ii("PredominantColor", "[sim] PredominantColorProcessor created; camera frames are not simulated so the analysis reports black");
    }

    @Override public void init(int width, int height, CameraCalibration calibration) {}
    @Override public Object processFrame(Mat frame, long captureTimeNanos) { return null; }
    @Override public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {}
    @Override public Result getAnalysis() { return result; }
}
