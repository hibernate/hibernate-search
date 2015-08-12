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

/**
 * Defines a bridge for integrating with <a href="http://tika.apache.org">Apache Tika</a>.
 * <p>
 * The bridge supports the following data types:
 * <ul>
 * <li>{@code String} - where the string value is interpreted as a file path</li>
 * <li>{@code URI} - where the URI is interpreted as a resource URI</li>
 * <li>{@code byte[]}</li>
 * <li>{@code java.sql.Blob}</li>
 * </ul>
 *
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface TikaBridge {

	/**
	 * @return Class used for optional Tika metadata pre- and post-processing
	 */
	Class<?> metadataProcessor() default void.class;

	/**
	 * @return Class used for optionally providing a Tika parsing context
	 */
	Class<?> parseContextProvider() default void.class;
}
