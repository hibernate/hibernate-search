/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import org.apache.lucene.analysis.util.CharFilterFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a <code>CharFilterFactory</code> and its parameters
 *
 * @author Gustavo Fernandes
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface CharFilterDef {

	/**
	 * @return the <code>TokenFilterFactory</code> class which shall be instantiated.
	 */
	Class<? extends CharFilterFactory> factory();

	/**
	 * @return Optional parameters passed to the <code>CharFilterFactory</code>.
	 */
	Parameter[] params() default { };
}
