package edelta.lib.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By annotating a class or method with this annotation, JaCoCo will not
 * consider the annotated member.
 *
 * @author Lorenzo Bettini
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
@Documented
public @interface EdeltaGenerated {

}
