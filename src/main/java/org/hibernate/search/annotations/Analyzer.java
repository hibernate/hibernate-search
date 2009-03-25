// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Define an Analyzer for a given entity, method, field or Field
 * The order of precedence is as such:
 *  - @Field
 *  - field / method
 *  - entity
 *  - default
 *
 * Either describe an explicit implementation through the <code>impl</code> parameter
 * or use an external @AnalyzerDef definition through the <code>def</code> parameter
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD} )
@Documented
public @interface Analyzer {
	Class<?> impl() default void.class;
	String definition() default "";
}
