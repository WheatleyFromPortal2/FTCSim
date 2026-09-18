package ftcsim.runner;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.lang.reflect.Modifier;

/** A registered OpMode (what the Driver Station lists). */
public final class OpModeEntry implements Comparable<OpModeEntry> {
    public enum Flavor { TELEOP, AUTONOMOUS }
    public final String name;
    public final String group;
    public final Flavor flavor;
    public final boolean disabled;
    public final String preselectTeleOp;
    public final Class<? extends OpMode> clazz;
    public final boolean linear;

    public OpModeEntry(String name, String group, Flavor flavor, boolean disabled, String preselectTeleOp, Class<? extends OpMode> clazz, boolean linear) {
        this.name = name; this.group = group; this.flavor = flavor; this.disabled = disabled; this.preselectTeleOp = preselectTeleOp; this.clazz = clazz; this.linear = linear;
    }

    /** Builds an entry from the class annotations, or returns null if the class is not an OpMode registration. */
    @SuppressWarnings("unchecked")
    public static OpModeEntry from(Class<?> c) {
        if (!OpMode.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers()) || c.isInterface()) return null;
        TeleOp t = c.getAnnotation(TeleOp.class);
        Autonomous a = c.getAnnotation(Autonomous.class);
        if (t == null && a == null) return null;
        boolean disabled = c.getAnnotation(Disabled.class) != null;
        boolean linear = com.qualcomm.robotcore.eventloop.opmode.LinearOpMode.class.isAssignableFrom(c);
        String name, group; Flavor flavor; String preselect = "";
        if (t != null) { name = t.name(); group = t.group(); flavor = Flavor.TELEOP; }
        else { name = a.name(); group = a.group(); flavor = Flavor.AUTONOMOUS; preselect = a.preselectTeleOp(); }
        if (name == null || name.isEmpty()) name = c.getSimpleName();
        if (group == null) group = "";
        return new OpModeEntry(name, group, flavor, disabled, preselect, (Class<? extends OpMode>) c, linear);
    }

    @Override public int compareTo(OpModeEntry o) {
        int c = flavor.compareTo(o.flavor);
        if (c != 0) return c;
        c = group.compareToIgnoreCase(o.group);
        if (c != 0) return c;
        return name.compareToIgnoreCase(o.name);
    }
    @Override public String toString() { return flavor + " " + (group.isEmpty() ? "" : group + "/") + name + " (" + clazz.getName() + ")"; }
}
