package ftcsim.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleConsumer;

/**
 * Owns all simulated physical state and advances it in real time on a fixed
 * time step. Sensor models register a step listener to sample the world.
 */
public final class World {
    public static final double DT = 0.001; // 1 kHz physics
    public final Chassis chassis = new Chassis();
    public final Battery battery = new Battery();
    public Field field = Field.decode();
    public final List<MotorState> motors = new CopyOnWriteArrayList<>();
    public final List<ServoState> servos = new CopyOnWriteArrayList<>();
    public final List<DeadWheel> deadWheels = new CopyOnWriteArrayList<>();
    private final List<DoubleConsumer> stepListeners = new CopyOnWriteArrayList<>();

    private volatile boolean running;
    private volatile boolean paused;
    private volatile double timeScale = 1.0;
    private Thread thread;
    private volatile double simTime;
    private volatile double actualStepRate;

    public double simTime() { return simTime; }
    public double stepRate() { return actualStepRate; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { paused = p; }
    public void addStepListener(DoubleConsumer l) { stepListeners.add(l); }
    public void removeStepListener(DoubleConsumer l) { stepListeners.remove(l); }

    /** Advances the simulation by one fixed step. */
    public void step() { step(DT); }

    public void step(double dt) {
        double volts = battery.volts();
        chassis.step(dt, volts, field);
        double current = chassis.driveCurrent();
        for (MotorState m : motors) {
            if (m.isDriveWheel) continue;
            m.stepLoad(dt, volts);
            current += m.currentAmps();
        }
        for (ServoState s : servos) { s.step(dt); current += s.current(); }
        Vec2 vr = chassis.velocityRobot();
        double omega = chassis.omega();
        for (DeadWheel d : deadWheels) d.step(vr, omega, dt);
        battery.step(current);
        simTime += dt;
        for (DoubleConsumer l : stepListeners) l.accept(dt);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "ftcsim-physics");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) { thread.interrupt(); thread = null; }
    }

    private void loop() {
        long stepNanos = (long) (DT * 1e9);
        long next = System.nanoTime();
        long rateWindowStart = System.nanoTime();
        int stepsInWindow = 0;
        while (running) {
            long now = System.nanoTime();
            if (paused) {
                next = now;
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
                continue;
            }
            // catch up (bounded) if we fell behind
            int catchUp = 0;
            while (now >= next && catchUp < 50) {
                step();
                stepsInWindow++;
                next += (long) (stepNanos / timeScale);
                catchUp++;
            }
            if (catchUp >= 50) next = System.nanoTime();
            long sleep = next - System.nanoTime();
            if (sleep > 2_000_000L) {
                try { Thread.sleep((sleep - 1_000_000L) / 1_000_000L); } catch (InterruptedException e) { return; }
            } else if (sleep > 0) {
                Thread.onSpinWait();
            }
            if (now - rateWindowStart > 1_000_000_000L) {
                actualStepRate = stepsInWindow * 1e9 / (now - rateWindowStart);
                stepsInWindow = 0;
                rateWindowStart = now;
            }
        }
    }
}
