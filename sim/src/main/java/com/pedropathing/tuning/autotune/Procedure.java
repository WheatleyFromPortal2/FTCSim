package com.pedropathing.tuning.autotune;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FTCSim stand-in for Pedro Pathing's tuning procedures. The web tuner is not simulated: confirmations
 * are logged, inputs take their default values and tuning OpModes run inline on the calling OpMode's
 * hardware (see {@link TuningSession}).
 */
public abstract class Procedure {
    public final String name;
    public final String description;

    private final Map<String, String> results = new LinkedHashMap<>();
    private final Map<Object, String> resultCode = new LinkedHashMap<>();
    private Display currentDisplay;

    public Procedure(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract void run() throws InterruptedException;

    final void execute() throws InterruptedException {
        results.clear();
        resultCode.clear();
        run();
    }

    protected void confirmation(String title, String message) throws InterruptedException {
        TuningSession.requestConfirmation(title, message, currentDisplay);
    }

    protected Inputs inputs(String title, String description) {
        return new Inputs(title, description);
    }

    protected void awaitInputs(Inputs inputs) throws InterruptedException {
        TuningSession.requestInputs(inputs, currentDisplay);
    }

    protected final void abort(String message) throws InterruptedException {
        TuningSession.abort(message);
    }

    protected void withDisplay(Display display, InterruptibleBlock block) throws InterruptedException {
        Display previousDisplay = this.currentDisplay;
        this.currentDisplay = display;
        try {
            block.execute();
        } finally {
            this.currentDisplay = previousDisplay;
        }
    }

    protected final void result(String name, String value) {
        results.put(name, value);
        TuningSession.result(this, name, value);
    }

    protected final void result(String name, Object value) {
        result(name, String.valueOf(value));
    }

    protected final void code(String language, String code) {
        resultCode.put(language, code);
        TuningSession.code(this, language, code);
    }

    protected final void code(Language language, String code) {
        resultCode.put(language, code);
        TuningSession.code(this, language.name(), code);
    }

    final Map<String, String> resultSnapshot() {
        return new LinkedHashMap<>(results);
    }

    final Map<Object, String> resultCodeSnapshot() {
        return new LinkedHashMap<>(resultCode);
    }

    protected final <Result> Result runOpMode(TuningOpMode<Result> opMode) throws InterruptedException {
        TuningSession.requestConfirmation(opMode.name, opMode.description, currentDisplay);
        return TuningSession.runInline(opMode);
    }

    public enum Language {
        JAVA,
        KOTLIN,
        JSON
    }
}
