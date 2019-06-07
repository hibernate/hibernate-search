/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.declaration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a property bridge to an annotation type,
 * so that whenever the annotation is found on a field or method in the domain model,
 * the property bridge mapped to the annotation will be applied.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyBridgeMapping {

	/**
	 * Map a property bridge to an annotation type.
	 *
	 * @see PropertyBridgeRef
	 * @return A reference to the property bridge to use.
	 */
	PropertyBridgeRef bridge();
}
