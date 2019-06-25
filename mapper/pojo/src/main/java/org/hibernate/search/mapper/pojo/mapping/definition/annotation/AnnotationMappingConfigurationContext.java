/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.util.Set;

/**
 * A context to configure annotation mapping.
 */
public interface AnnotationMappingConfigurationContext {

	/**
	 * @param annotatedType A type to scan for annotations.
	 * @return {@code this}, for method chaining.
	 */
	AnnotationMappingConfigurationContext add(Class<?> annotatedType);

	/**
	 * @param annotatedTypes A set of types to scan for annotations.
	 * @return {@code this}, for method chaining.
	 */
	AnnotationMappingConfigurationContext add(Set<Class<?>> annotatedTypes);

}
