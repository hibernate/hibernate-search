/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.util.Set;

public interface AnnotationMappingDefinition {

	AnnotationMappingDefinition add(Class<?> annotatedType);

	AnnotationMappingDefinition add(Set<Class<?>> annotatedTypes);

}
