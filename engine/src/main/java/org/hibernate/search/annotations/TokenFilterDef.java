/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.lucene.analysis.util.TokenFilterFactory;

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
	Class<? extends TokenFilterFactory> factory();

	/**
	 * @return Optional parameters passed to the <code>TokenFilterFactory</code>.
	 */
	Parameter[] params() default { };
}
