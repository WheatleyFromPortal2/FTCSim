package androidx.annotation;

import java.lang.annotation.*;

/** FTCSim desktop stand-in for androidx.annotation.RequiresPermission. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.TYPE_USE})
public @interface RequiresPermission { String value() default ""; String[] allOf() default {}; String[] anyOf() default {}; boolean conditional() default false; }
