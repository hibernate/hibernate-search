// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.solr.analysis.TokenFilterFactory;

/**
 * Define a <code>TokenFilterFactory</code> and its parameters.
 *
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface TokenFilterDef {
	/**
	 * @return the <code>TokenFilterFactory</code> class which shall be instantiated.
	 */
	public abstract Class<? extends TokenFilterFactory> factory();

	/**
	 * @return Optional parameters passed to the <code>TokenFilterFactory</code>.
	 */
	public abstract Parameter[] params() default { };
}