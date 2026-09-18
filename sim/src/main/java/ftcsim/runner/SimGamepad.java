package ftcsim.runner;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.internal.ui.GamepadUser;

import java.util.Map;

/**
 * A Gamepad whose state is fed from the simulator UI (virtual pad, keyboard or
 * a physical controller through the browser Gamepad API).
 */
public class SimGamepad extends Gamepad {
    private final int user;

    public SimGamepad(int user) {
        this.user = user;
        setUser(user == 1 ? GamepadUser.ONE : GamepadUser.TWO);
        setGamepadId(user == 1 ? 1 : 2);
        type = Type.XBOX_360;
        updateButtonAliases();
        refreshTimestamp();
    }

    private static float f(Object o) { return o instanceof Number ? ((Number) o).floatValue() : 0f; }
    private static boolean b(Object o) { return o instanceof Boolean ? (Boolean) o : o instanceof Number && ((Number) o).doubleValue() != 0; }

    /** Applies a JSON state object {lx,ly,rx,ry,lt,rt,a,b,x,y,lb,rb,ls,rs,du,dd,dl,dr,back,start,guide,touchpad,type}. */
    public synchronized void apply(Map<String, Object> s) {
        left_stick_x = clamp(f(s.get("lx"))); left_stick_y = clamp(f(s.get("ly")));
        right_stick_x = clamp(f(s.get("rx"))); right_stick_y = clamp(f(s.get("ry")));
        left_trigger = clamp01(f(s.get("lt"))); right_trigger = clamp01(f(s.get("rt")));
        a = b(s.get("a")); b = b(s.get("b")); x = b(s.get("x")); y = b(s.get("y"));
        left_bumper = b(s.get("lb")); right_bumper = b(s.get("rb"));
        left_stick_button = b(s.get("ls")); right_stick_button = b(s.get("rs"));
        dpad_up = b(s.get("du")); dpad_down = b(s.get("dd")); dpad_left = b(s.get("dl")); dpad_right = b(s.get("dr"));
        back = b(s.get("back")); start = b(s.get("start")); guide = b(s.get("guide")); touchpad = b(s.get("touchpad"));
        Object t = s.get("type");
        if ("ps4".equals(t) || "SONY_PS4".equals(t)) type = Type.SONY_PS4; else if ("f310".equals(t)) type = Type.LOGITECH_F310; else type = Type.XBOX_360;
        updateButtonAliases();
        refreshTimestamp();
    }

    public synchronized void clearAll() {
        left_stick_x = left_stick_y = right_stick_x = right_stick_y = 0;
        left_trigger = right_trigger = 0;
        a = b = x = y = left_bumper = right_bumper = left_stick_button = right_stick_button = false;
        dpad_up = dpad_down = dpad_left = dpad_right = back = start = guide = touchpad = false;
        updateButtonAliases();
        refreshTimestamp();
    }

    public int user() { return user; }
    private static float clamp(float v) { return Math.max(-1f, Math.min(1f, v)); }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
