//$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

import org.apache.solr.analysis.TokenizerFactory;

/**
 * Define a TokenizerFactory and its parameters
 * 
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD} )
@Documented
public @interface TokenizerDef {
	/**
	 * Defines the TokenizerFactory implementation used
	 */
	Class<? extends TokenizerFactory> factory();

	/**
	 * optional parameters passed to the TokenizerFactory
	 */
	Parameter[] params() default {};
}
