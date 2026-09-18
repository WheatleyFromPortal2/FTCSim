package org.firstinspires.ftc.vision.opencv;

import android.graphics.Canvas;
import com.qualcomm.robotcore.util.RobotLog;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/** FTCSim stand-in: camera frames are not simulated, so no blobs are ever located. */
class ColorBlobLocatorProcessorImpl extends ColorBlobLocatorProcessor {
    private final List<Blob> blobs = new ArrayList<>();

    ColorBlobLocatorProcessorImpl(ColorRange targetColorRange, ImageRegion roi, ContourMode contourMode, MorphOperationType morphOperationType,
                                  int erodeSize, int dilateSize, boolean drawContours, int blurSize, int boxFitColor, int roiColor, int contourColor, int extra) {
        RobotLog.ii("ColorBlobLocator", "[sim] ColorBlobLocatorProcessor created; camera frames are not simulated so getBlobs() stays empty");
    }

    @Override public void init(int width, int height, CameraCalibration calibration) {}
    @Override public Object processFrame(Mat frame, long captureTimeNanos) { return null; }
    @Override public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {}
    @Override public void addFilter(BlobFilter filter) {}
    @Override public void removeFilter(BlobFilter filter) {}
    @Override public void removeAllFilters() {}
    @Override public void setSort(BlobSort sort) {}
    @Override public List<Blob> getBlobs() { return blobs; }
}
