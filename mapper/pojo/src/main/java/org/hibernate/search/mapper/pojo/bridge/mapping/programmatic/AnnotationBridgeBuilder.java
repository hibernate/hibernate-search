/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBridgeMapping;

/**
 * A bridge builder that can be initialized from an annotation.
 *
 * @param <B> The type of created bridges.
 * @param <A> The type of accepted annotations.
 * @see TypeBridgeMapping
 * @see PropertyBridgeMapping
 * @see RoutingKeyBridgeMapping
 */
public interface AnnotationBridgeBuilder<B, A extends Annotation> extends BridgeBuilder<B> {

	/**
	 * Initialize the parameters of this builder with the attributes of the given annotation.
	 * @param annotation An annotation to extract paramaters from.
	 */
	void initialize(A annotation);

}
