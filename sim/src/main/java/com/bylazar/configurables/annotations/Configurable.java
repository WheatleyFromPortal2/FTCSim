package com.bylazar.configurables.annotations;

import java.lang.annotation.*;

/** Marks a class whose public static fields are tunable at runtime (Panels). FTCSim exposes them in its Configurables panel. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configurable {}
