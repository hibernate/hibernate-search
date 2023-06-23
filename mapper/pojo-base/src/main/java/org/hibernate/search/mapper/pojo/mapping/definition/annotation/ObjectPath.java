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

/**
 * A path from a root object to one or more target value(s).
 * <p>
 * Each element of the path is a {@link PropertyValue},
 * representing a property and a way to extra values from that property.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectPath {

	/**
	 * @return A list of components in the paths, each representing a property and a way to extract its value.
	 */
	PropertyValue[] value();

}
