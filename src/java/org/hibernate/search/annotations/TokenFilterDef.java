//$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.solr.analysis.TokenFilterFactory;

/**
 * Define a TokenFilterFactory and its parameters
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD } )
@Documented
public @interface TokenFilterDef {
	/**
	 * Defines the TokenFilterFactory implementation used
	 */
	public abstract Class<? extends TokenFilterFactory> factory();

	/**
	 * optional parameters passed to the TokenFilterFactory
	 */
	public abstract Parameter[] params() default {};
}