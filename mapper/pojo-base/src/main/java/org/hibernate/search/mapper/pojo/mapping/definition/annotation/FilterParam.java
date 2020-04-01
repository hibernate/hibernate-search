/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;

/**
 * A reference to a POJO property using its name,
 * and to one or more target value(s) in that property using a {@link FilterFactory}.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterParam {

	/**
	 * @return The name of the referenced property.
	 */
	String name();

	/**
	 * @return The value of the referenced property.
	 */
	String value();

}
