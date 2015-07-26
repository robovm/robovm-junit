package org.robovm.junit.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ash on 26/07/2015.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@interface TargetType'' {

    String device() default "Console";
    String arch() default "Console";
}
