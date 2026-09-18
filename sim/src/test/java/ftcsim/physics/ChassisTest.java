package ftcsim.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Sanity checks of the drivetrain dynamics against typical FTC robot numbers. */
public class ChassisTest {
    private static World mecanumWorld(String motor) {
        World w = new World();
        w.field = null; // no walls
        Chassis c = w.chassis;
        c.driveType = Chassis.DriveType.MECANUM;
        c.mass = 13.0; c.length = 18 * Units.INCH; c.width = 17 * Units.INCH; c.computeInertia();
        c.wheelRadius = Units.inToM(4.094) / 2; c.halfTrackWidth = Units.inToM(13.5) / 2; c.halfWheelBase = Units.inToM(11) / 2;
        c.coulombX = c.mass * Units.inToM(34.6); c.coulombY = c.mass * Units.inToM(65); c.coulombTheta = c.inertiaZ * Math.toRadians(500);
        c.viscousX = 0.35 * c.coulombX; c.viscousY = 0.35 * c.coulombY; c.viscousTheta = 0.35 * c.coulombTheta;
        MotorType t = MotorType.byName(motor);
        String[] names = { "lf", "rf", "lr", "rr" };
        MotorState[] ms = new MotorState[4];
        for (int i = 0; i < 4; i++) { ms[i] = new MotorState(i, names[i], t); ms[i].isDriveWheel = true; ms[i].setMountSign(i % 2 == 0 ? -1 : 1); w.motors.add(ms[i]); }
        c.leftFront = ms[0]; c.rightFront = ms[1]; c.leftRear = ms[2]; c.rightRear = ms[3];
        c.setPose(new Pose2(0, 0, 0));
        return w;
    }

    /** Powers in the "wheel forward" sense; converted to the raw controller frame the way the SDK + mounting would. */
    private static void drive(World w, double lf, double rf, double lr, double rr) {
        Chassis c = w.chassis;
        c.leftFront.power = lf * c.leftFront.physicalSign; c.rightFront.power = rf * c.rightFront.physicalSign;
        c.leftRear.power = lr * c.leftRear.physicalSign; c.rightRear.power = rr * c.rightRear.physicalSign;
    }

    private static void run(World w, double seconds) { int n = (int) Math.round(seconds / World.DT); for (int i = 0; i < n; i++) w.step(); }

    @Test public void forwardTopSpeedIsRealistic() {
        World w = mecanumWorld("goBILDA_435");
        drive(w, 1, 1, 1, 1);
        run(w, 3.0);
        double v = Units.mToIn(w.chassis.velocityRobot().x);
        assertTrue(v > 60 && v < 95, "forward top speed in/s: " + v);
        assertEquals(0, Math.toDegrees(w.chassis.pose().heading), 1.0, "should drive straight");
        assertTrue(Math.abs(Units.mToIn(w.chassis.velocityRobot().y)) < 1.0);
    }

    @Test public void strafeIsSlowerThanForward() {
        World w = mecanumWorld("goBILDA_435");
        drive(w, -1, 1, 1, -1); // strafe left
        run(w, 3.0);
        double vy = Units.mToIn(w.chassis.velocityRobot().y);
        assertTrue(vy > 35 && vy < 80, "strafe speed in/s: " + vy);
        assertTrue(Math.abs(Units.mToIn(w.chassis.velocityRobot().x)) < 1.0);
    }

    @Test public void turnInPlace() {
        World w = mecanumWorld("goBILDA_435");
        drive(w, -1, 1, -1, 1); // CCW
        run(w, 2.0);
        assertTrue(Math.toDegrees(w.chassis.omega()) > 200, "turn rate deg/s: " + Math.toDegrees(w.chassis.omega()));
        assertTrue(w.chassis.pose().position().norm() < 0.05, "should turn in place");
    }

    @Test public void zeroPowerFloatDeceleration() {
        World w = mecanumWorld("goBILDA_435");
        drive(w, 1, 1, 1, 1);
        run(w, 3.0);
        for (MotorState m : w.motors) { m.power = 0; m.zeroPowerBehavior = com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.FLOAT; }
        double v0 = Units.mToIn(w.chassis.velocityRobot().x);
        run(w, 0.5);
        double v1 = Units.mToIn(w.chassis.velocityRobot().x);
        double decel = (v0 - v1) / 0.5;
        assertTrue(decel > 25 && decel < 60, "float deceleration in/s^2: " + decel);
    }

    @Test public void brakeStopsQuickly() {
        World w = mecanumWorld("goBILDA_435");
        drive(w, 1, 1, 1, 1);
        run(w, 3.0);
        for (MotorState m : w.motors) { m.power = 0; m.zeroPowerBehavior = com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.BRAKE; }
        run(w, 0.6);
        assertTrue(Units.mToIn(w.chassis.velocityRobot().x) < 3, "brake should stop the robot within 0.6 s");
    }

    @Test public void encodersCountWheelTravel() {
        World w = mecanumWorld("goBILDA_312");
        drive(w, 1, 1, 1, 1);
        run(w, 2.0);
        double distance = w.chassis.pose().x;
        double revs = distance / (2 * Math.PI * w.chassis.wheelRadius);
        int expectedTicks = (int) Math.round(revs * 537.7);
        // raw ticks carry the motor-type orientation sign (the SDK undoes it); the left side is mounted reversed
        MotorState rf = w.chassis.rightFront, lf = w.chassis.leftFront;
        assertEquals(expectedTicks, rf.encoderTicks() * rf.orientationSign, expectedTicks * 0.02 + 5);
        assertEquals(-expectedTicks, lf.encoderTicks() * lf.orientationSign, expectedTicks * 0.02 + 5);
    }

    @Test public void velocityControlReachesTarget() {
        World w = new World();
        MotorState m = new MotorState(0, "flywheel", MotorType.byName("goBILDA_6000"));
        m.inertia = 0.00025; w.motors.add(m);
        m.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER;
        m.useVelocityTarget = true; m.targetVelocityTps = 1500; // ~3200 rpm
        run(w, 2.0);
        assertTrue(m.velocityTpsExact() > 1350, "should be near target after 2 s: " + m.velocityTpsExact());
        run(w, 2.0);
        assertEquals(1500, m.velocityTpsExact(), 120, "small overshoot at 4 s");
        run(w, 4.0);
        assertEquals(1500, m.velocityTpsExact(), 30, "settled at 8 s");
    }

    @Test public void wallsStopTheRobot() {
        World w = mecanumWorld("goBILDA_435");
        w.field = Field.decode();
        w.chassis.setPose(new Pose2(Units.inToM(60), 0, 0));
        drive(w, 1, 1, 1, 1);
        run(w, 2.0);
        double x = Units.mToIn(w.chassis.pose().x);
        assertTrue(x < 72 - 8, "robot centre must stay inside the wall: " + x);
        assertTrue(x > 60);
    }
}
