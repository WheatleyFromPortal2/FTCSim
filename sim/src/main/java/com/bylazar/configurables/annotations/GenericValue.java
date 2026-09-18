package com.bylazar.configurables.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GenericValue { Class<?> tParam() default Object.class; Class<?> vParam() default Object.class; }
