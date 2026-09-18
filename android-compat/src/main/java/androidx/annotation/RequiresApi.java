package androidx.annotation;

import java.lang.annotation.*;

/** FTCSim desktop stand-in for androidx.annotation.RequiresApi. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.TYPE_USE})
public @interface RequiresApi { int value() default 1; int api() default 1; }
