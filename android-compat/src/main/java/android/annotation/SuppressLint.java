package android.annotation;

import java.lang.annotation.*;

/** FTCSim desktop stand-in for android.annotation.SuppressLint. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.TYPE_USE})
public @interface SuppressLint { String[] value(); }
