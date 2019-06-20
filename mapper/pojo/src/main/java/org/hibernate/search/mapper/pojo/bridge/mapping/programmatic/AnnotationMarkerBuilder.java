/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerMapping;

/**
 * A marker builder that can be initialized from an annotation.
 *
 * @param <A> The type of accepted annotations.
 * @see MarkerMapping
 */
public interface AnnotationMarkerBuilder<A extends Annotation> extends MarkerBuilder {

	/**
	 * Initialize the parameters of this builder with the attributes of the given annotation.
	 * @param annotation An annotation to extract paramaters from.
	 */
	void initialize(A annotation);

}
