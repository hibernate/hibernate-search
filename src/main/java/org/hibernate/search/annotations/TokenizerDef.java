// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

import org.apache.solr.analysis.TokenizerFactory;

/**
 * Define a <code>TokenizerFactory</code> and its parameters.
 *
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface TokenizerDef {

	/**
	 * @return the <code>TokenizerFactory</code> class which shall be instantiated.
	 */
	Class<? extends TokenizerFactory> factory();

	/**
	 * @return Optional parameters passed to the <code>TokenizerFactory</code>.
	 */
	Parameter[] params() default { };
}
