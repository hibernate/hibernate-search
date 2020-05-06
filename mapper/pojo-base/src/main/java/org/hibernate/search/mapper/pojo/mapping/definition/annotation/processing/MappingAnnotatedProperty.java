/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

/**
 * A property in the entity model annotated with a mapping annotation.
 *
 * @see MappingAnnotatedElement
 * @see PropertyMappingAnnotationProcessorContext#annotatedElement()
 */
public interface MappingAnnotatedProperty extends MappingAnnotatedElement {

	String name();

}
