package ftcsim.hardware;

/** Lets a localizer device ask the simulator to move the robot to the pose the code just assigned to it. */
public interface PoseSnapper {
    /** @param xIn x in inches in the code's frame, @param yIn y, @param headingRad heading (CCW) in the code's frame */
    void snapTo(double xIn, double yIn, double headingRad);
}
