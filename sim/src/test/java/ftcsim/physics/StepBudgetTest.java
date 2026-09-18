package ftcsim.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The step control must advance the running world by exactly the requested slice of simulated time. */
public class StepBudgetTest {
    @Test public void stepAdvancesTheRunningWorldByTheRequestedTime() throws Exception {
        World w = new World();
        w.field = null;
        w.start();
        try {
            Thread.sleep(200);
            w.setPaused(true);
            Thread.sleep(150);
            double t0 = w.simTime();
            assertTrue(w.frozen());
            w.stepFor(0.100);
            long deadline = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < deadline && w.stepRemaining() > 0) Thread.sleep(10);
            double advanced = w.simTime() - t0;
            System.out.printf("advanced %.4f s, frozen=%b, remaining=%.4f%n", advanced, w.frozen(), w.stepRemaining());
            assertEquals(0.100, advanced, 0.010, "one 100 ms step should advance the world 100 ms");
            assertTrue(w.frozen(), "the world freezes again after the step");
            double after = w.simTime();
            Thread.sleep(200);
            assertEquals(after, w.simTime(), 1e-9, "nothing moves once the budget is spent");
        } finally { w.setPaused(false); w.stop(); }
    }
}
