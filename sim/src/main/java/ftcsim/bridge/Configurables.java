package ftcsim.bridge;

import com.qualcomm.robotcore.util.RobotLog;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Live tuning of public static fields in classes annotated with Panels'
 * {@code @Configurable} or FTC Dashboard's {@code @Config}, using reflection
 * (nested objects such as PID coefficient holders are expanded a few levels).
 */
public final class Configurables {
    public static final String TAG = "Configurables";

    public static final class Entry {
        public final String path; public final String type; public final Object value; public final boolean editable; public final List<String> options;
        Entry(String path, String type, Object value, boolean editable, List<String> options) { this.path = path; this.type = type; this.value = value; this.editable = editable; this.options = options; }
    }
    public static final class ClassEntry {
        public final String className; public final String simpleName; public final List<Entry> fields = new ArrayList<>();
        ClassEntry(String className, String simpleName) { this.className = className; this.simpleName = simpleName; }
    }

    private final List<Class<?>> classes = new ArrayList<>();

    public synchronized void setClasses(Collection<Class<?>> all) {
        classes.clear();
        for (Class<?> c : all) {
            if (hasAnnotation(c, "com.bylazar.configurables.annotations.Configurable") || hasAnnotation(c, "com.acmerobotics.dashboard.config.Config")) classes.add(c);
        }
        classes.sort(Comparator.comparing(Class::getSimpleName));
    }

    private static boolean hasAnnotation(Class<?> c, String name) {
        try {
            for (java.lang.annotation.Annotation a : c.getAnnotations()) if (a.annotationType().getName().equals(name)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public synchronized List<ClassEntry> snapshot() {
        List<ClassEntry> out = new ArrayList<>();
        for (Class<?> c : classes) {
            ClassEntry ce = new ClassEntry(c.getName(), c.getSimpleName());
            try {
                for (Field f : c.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers()) || !Modifier.isPublic(f.getModifiers())) continue;
                    if (hasAnnotation(f, "com.bylazar.configurables.annotations.IgnoreConfigurable")) continue;
                    f.setAccessible(true);
                    Object v = f.get(null);
                    collect(ce, f.getName(), f.getType(), v, 0);
                }
            } catch (Throwable t) {
                RobotLog.ww(TAG, "cannot read %s: %s", c.getName(), t.toString());
            }
            out.add(ce);
        }
        return out;
    }

    private static boolean hasAnnotation(Field f, String name) {
        for (java.lang.annotation.Annotation a : f.getAnnotations()) if (a.annotationType().getName().equals(name)) return true;
        return false;
    }

    private static boolean isSimple(Class<?> t) {
        return t.isPrimitive() || Number.class.isAssignableFrom(t) || t == Boolean.class || t == String.class || t == Character.class || t.isEnum();
    }

    private static void collect(ClassEntry ce, String path, Class<?> type, Object value, int depth) {
        if (isSimple(type)) {
            List<String> options = null;
            if (type.isEnum()) { options = new ArrayList<>(); for (Object o : type.getEnumConstants()) options.add(((Enum<?>) o).name()); }
            ce.fields.add(new Entry(path, typeName(type), value instanceof Enum ? ((Enum<?>) value).name() : value, true, options));
            return;
        }
        if (value == null) { ce.fields.add(new Entry(path, type.getSimpleName(), null, false, null)); return; }
        if (type.isArray()) {
            Class<?> comp = type.getComponentType();
            int n = Array.getLength(value);
            for (int i = 0; i < Math.min(n, 64); i++) collect(ce, path + "[" + i + "]", comp, Array.get(value, i), depth + 1);
            return;
        }
        if (depth >= 3) { ce.fields.add(new Entry(path, type.getSimpleName(), String.valueOf(value), false, null)); return; }
        if (value instanceof Collection || value instanceof Map) { ce.fields.add(new Entry(path, type.getSimpleName(), String.valueOf(value), false, null)); return; }
        Class<?> vt = value.getClass();
        boolean any = false;
        for (Class<?> k = vt; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(value);
                    if (isSimple(f.getType()) || f.getType().isArray() || (v != null && !(v instanceof Collection) && !(v instanceof Map))) {
                        collect(ce, path + "." + f.getName(), f.getType(), v, depth + 1);
                        any = true;
                    }
                } catch (Throwable ignored) {}
            }
        }
        if (!any) ce.fields.add(new Entry(path, type.getSimpleName(), String.valueOf(value), false, null));
    }

    private static String typeName(Class<?> t) {
        if (t == int.class || t == Integer.class) return "int";
        if (t == long.class || t == Long.class) return "long";
        if (t == double.class || t == Double.class) return "double";
        if (t == float.class || t == Float.class) return "float";
        if (t == boolean.class || t == Boolean.class) return "boolean";
        if (t == String.class) return "String";
        if (t.isEnum()) return "enum";
        return t.getSimpleName();
    }

    /** Sets a value by class name and path (e.g. "Constants.drivePIDF.P" or "Tunables.farLaunchWaits[1]"). */
    public synchronized boolean set(String className, String path, Object newValue) {
        Class<?> c = null;
        for (Class<?> k : classes) if (k.getName().equals(className) || k.getSimpleName().equals(className)) c = k;
        if (c == null) return false;
        try {
            List<String> tokens = tokenize(path);
            Object holder = null;
            Class<?> holderClass = c;
            Field field = null;
            int arrayIndex = -1;
            for (int i = 0; i < tokens.size(); i++) {
                String tok = tokens.get(i);
                boolean last = i == tokens.size() - 1;
                if (tok.startsWith("[")) {
                    int idx = Integer.parseInt(tok.substring(1, tok.length() - 1));
                    Object arr = field.get(holder);
                    if (last) { arrayIndex = idx; holder = arr; }
                    else { holder = Array.get(arr, idx); holderClass = holder.getClass(); field = null; }
                    continue;
                }
                Field f = findField(holderClass, tok);
                if (f == null) return false;
                f.setAccessible(true);
                if (last) { field = f; }
                else { holder = f.get(holder); if (holder == null) return false; holderClass = holder.getClass(); field = f; }
            }
            if (arrayIndex >= 0) {
                Class<?> comp = holder.getClass().getComponentType();
                Array.set(holder, arrayIndex, convert(newValue, comp));
                return true;
            }
            if (field == null) return false;
            Object converted = convert(newValue, field.getType());
            field.set(Modifier.isStatic(field.getModifiers()) ? null : holder, converted);
            return true;
        } catch (Throwable t) {
            RobotLog.ww(TAG, "cannot set %s.%s: %s", className, path, t.toString());
            return false;
        }
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static List<String> tokenize(String path) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '.') { if (cur.length() > 0) out.add(cur.toString()); cur.setLength(0); }
            else if (ch == '[') { if (cur.length() > 0) out.add(cur.toString()); cur.setLength(0); cur.append('['); }
            else if (ch == ']') { cur.append(']'); out.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object v, Class<?> t) {
        String s = String.valueOf(v).trim();
        if (t == int.class || t == Integer.class) return (int) Math.round(Double.parseDouble(s));
        if (t == long.class || t == Long.class) return Math.round(Double.parseDouble(s));
        if (t == double.class || t == Double.class) return Double.parseDouble(s);
        if (t == float.class || t == Float.class) return Float.parseFloat(s);
        if (t == short.class || t == Short.class) return (short) Math.round(Double.parseDouble(s));
        if (t == byte.class || t == Byte.class) return (byte) Math.round(Double.parseDouble(s));
        if (t == boolean.class || t == Boolean.class) return v instanceof Boolean ? v : Boolean.parseBoolean(s);
        if (t == char.class || t == Character.class) return s.isEmpty() ? ' ' : s.charAt(0);
        if (t == String.class) return String.valueOf(v);
        if (t.isEnum()) return Enum.valueOf((Class<Enum>) t, s);
        throw new IllegalArgumentException("unsupported type " + t);
    }
}
