package com.pedropathing.tuning.autotune;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.SimOpModeRunner;
import com.qualcomm.robotcore.util.RobotLog;

/**
 * FTCSim's replacement for the Pedro web tuner session. Procedures normally talk to a browser page served
 * by the robot; here every request is resolved immediately: confirmations are logged, inputs are filled
 * with their defaults, and a tuning OpMode runs inline with the hardware of the OpMode that started the
 * procedure.
 */
final class TuningSession {
    private static final String TAG = "PedroTuning";

    private TuningSession() {}

    static void abort(String message) throws InterruptedException {
        RobotLog.ee(TAG, "[sim] tuning procedure aborted: %s", message);
        throw new RuntimeException("Tuning procedure aborted: " + message);
    }

    static void requestConfirmation(String title, String message, Display display) throws InterruptedException {
        RobotLog.ii(TAG, "[sim] %s: %s (confirmed automatically; the Pedro web tuner is not simulated)", title, message);
    }

    static void requestInputs(Inputs inputs, Display display) throws InterruptedException {
        RobotLog.ii(TAG, "[sim] inputs \"%s\": %s", inputs.name, inputs.description);
        inputs.applyDefaults();
    }

    static void result(Procedure procedure, String name, String value) { RobotLog.ii(TAG, "[sim] %s result %s = %s", procedure.name, name, value); }
    static void code(Procedure procedure, String language, String code) { RobotLog.ii(TAG, "[sim] %s generated %s:\n%s", procedure.name, language, code); }

    /** Runs a tuning OpMode on the current thread using the active OpMode's hardware map, gamepads and telemetry. */
    static <Result> Result runInline(TuningOpMode<Result> opMode) throws InterruptedException {
        OpMode host = SimOpModeRunner.currentOpMode();
        if (host == null) throw new IllegalStateException("A tuning OpMode can only run while an OpMode is active");
        opMode.hardwareMap = host.hardwareMap;
        opMode.gamepad1 = host.gamepad1;
        opMode.gamepad2 = host.gamepad2;
        opMode.telemetry = host.telemetry;
        SimOpModeRunner.adoptInline(opMode, host);
        RobotLog.ii(TAG, "[sim] running tuning OpMode \"%s\" inline", opMode.name);
        opMode.runOpMode();
        return opMode.awaitResult();
    }
}
