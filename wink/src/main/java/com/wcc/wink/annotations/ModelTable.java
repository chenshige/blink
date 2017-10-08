package com.wcc.wink.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wenbiao.xie on 2016/6/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface ModelTable {
    String name() default "";

    String table() ;

    int version() default 1;

    Class model() ;
}
