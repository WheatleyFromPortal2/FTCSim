package androidx.annotation;

import java.lang.annotation.*;

/** FTCSim desktop stand-in for androidx.annotation.FloatRange. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.TYPE_USE})
public @interface FloatRange { double from() default Double.NEGATIVE_INFINITY; double to() default Double.POSITIVE_INFINITY; boolean fromInclusive() default true; boolean toInclusive() default true; }
