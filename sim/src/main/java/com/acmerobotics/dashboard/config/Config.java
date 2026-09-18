package com.acmerobotics.dashboard.config;

import java.lang.annotation.*;

/** FTC Dashboard's @Config: public static fields of the class become tunable. FTCSim shows them in its Configurables panel. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config { String value() default ""; }
