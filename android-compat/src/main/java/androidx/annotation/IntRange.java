package androidx.annotation;

import java.lang.annotation.*;

/** FTCSim desktop stand-in for androidx.annotation.IntRange. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.TYPE_USE})
public @interface IntRange { long from() default Long.MIN_VALUE; long to() default Long.MAX_VALUE; }
