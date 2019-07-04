/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBridgeRef;

/**
 * Maps a routing key bridge to an annotation type,
 * so that whenever the annotation is found on a type in the domain model,
 * the routing bridge mapped to the annotation will be applied.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoutingKeyBridgeMapping {

	/**
	 * Map a routing key bridge to an annotation type.
	 *
	 * @see RoutingKeyBridgeRef
	 * @return A reference to the routing key binder to use.
	 */
	RoutingKeyBridgeRef bridge();

}
