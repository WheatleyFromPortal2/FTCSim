package com.pedropathing.tuning.autotune;

import java.util.*;

/** FTCSim stand-in for the tuning inputs API; values come from the fields' defaults. */
public class Inputs {
    public final String name;
    public final String description;
    private final List<Field<?>> fields = new ArrayList<>();

    public Inputs(String name, String description) {
        this.name = name;
        this.description = description;
    }

    void applyDefaults() {
        for (Field<?> field : fields) field.applyDefault();
    }

    public final StringField s(String name) { StringField field = new StringField(name); fields.add(field); return field; }
    public final NumberField<Double> d(String name) { NumberField<Double> field = new NumberField<>(name, Type.DOUBLE); fields.add(field); return field; }
    public final Field<Boolean> b(String name) { Field<Boolean> field = new Field<>(name, Type.BOOLEAN); field.withDefault(false); fields.add(field); return field; }
    public final NumberField<Integer> i(String name) { NumberField<Integer> field = new NumberField<>(name, Type.INT); fields.add(field); return field; }
    public final <Value extends Enum<Value>> Field<Value> e(String name, Class<Value> enumClass) { Field<Value> field = new EnumField<>(name, enumClass); fields.add(field); return field; }

    public enum Type { STRING, DOUBLE, BOOLEAN, INT, ENUM }

    public static class Field<Value> {
        public final String id;
        public final String name;
        public final Type type;
        protected Value defaultValue;
        private transient Value value;
        private transient boolean set;

        Field(String name, Type type) {
            this.id = Utils.nanoid();
            this.name = name;
            this.type = type;
        }

        public Field<Value> withDefault(Value defaultValue) {
            if (defaultValue == null) throw new IllegalArgumentException("Default value cannot be null.");
            this.defaultValue = defaultValue;
            return this;
        }

        public Field<Value> withoutDefault() {
            if (type == Type.BOOLEAN) throw new IllegalStateException("Boolean fields must have a default value.");
            defaultValue = null;
            return this;
        }

        public Value get() {
            if (!set) throw new IllegalStateException("Field not set. Make sure to call awaitInputs() on the inputs before accessing.");
            return value;
        }

        void applyDefault() {
            Value v = defaultValue;
            if (v == null) v = simFallback();
            if (v == null) throw new UnsupportedOperationException("Input '" + name + "' has no default and the Pedro web tuner is not simulated by FTCSim; give the field a default with withDefault(...)");
            set(v);
        }

        /** A stand-in value for fields without a default, where one is unambiguous. */
        Value simFallback() { return null; }

        void validate(Value value) { if (value == null) throw new IllegalArgumentException("Value cannot be null."); }

        void set(Value value) { validate(value); this.set = true; this.value = value; }
    }

    public static class StringField extends Field<String> {
        private boolean allowEmpty;
        StringField(String name) { super(name, Type.STRING); }
        public StringField allowEmpty() { allowEmpty = true; return this; }
        boolean allowsEmpty() { return allowEmpty; }
        @Override public StringField withDefault(String defaultValue) { super.withDefault(defaultValue); return this; }
        @Override public StringField withoutDefault() { super.withoutDefault(); return this; }
        @Override String simFallback() { return allowEmpty ? "" : null; }
        @Override void validate(String value) {
            super.validate(value);
            if (!allowEmpty && value.isEmpty()) throw new IllegalArgumentException("Value for '" + name + "' cannot be empty.");
        }
    }

    public static class NumberField<Value extends Number & Comparable<Value>> extends Field<Value> {
        public Value min;
        public Value max;
        NumberField(String name, Type type) { super(name, type); }
        public NumberField<Value> min(Value min) { this.min = min; return this; }
        public NumberField<Value> max(Value max) { this.max = max; return this; }
        @Override public NumberField<Value> withDefault(Value defaultValue) { super.withDefault(defaultValue); return this; }
        @Override public NumberField<Value> withoutDefault() { super.withoutDefault(); return this; }
        @SuppressWarnings("unchecked")
        @Override Value simFallback() {
            if (min != null) return min;
            return type == Type.INT ? (Value) Integer.valueOf(0) : (Value) Double.valueOf(0);
        }
        @Override void validate(Value value) {
            if (value != null && min != null && value.compareTo(min) < 0) throw new IllegalArgumentException("Value for '" + name + "' cannot be less than " + min + ".");
            if (value != null && max != null && value.compareTo(max) > 0) throw new IllegalArgumentException("Value for '" + name + "' cannot be greater than " + max + ".");
            super.validate(value);
        }
    }

    static class EnumField<Value extends Enum<Value>> extends Field<Value> {
        public final transient Class<Value> enumClass;
        public final List<Value> options;
        EnumField(String name, Class<Value> enumClass) {
            super(name, Type.ENUM);
            this.enumClass = enumClass;
            this.options = Collections.unmodifiableList(Arrays.asList(enumClass.getEnumConstants()));
        }
        @Override Value simFallback() { return options.isEmpty() ? null : options.get(0); }
    }
}
