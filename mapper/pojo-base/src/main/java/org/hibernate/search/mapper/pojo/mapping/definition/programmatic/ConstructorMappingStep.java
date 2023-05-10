/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

/**
 * The step in a mapping definition where a constructor can be mapped.
 */
public interface ConstructorMappingStep {

	/**
	 * @return A DSL step where the mapping can be defined
	 * for the type hosting this constructor.
	 */
	TypeMappingStep hostingType();

	/**
	 * Marks the constructor to use for projections from an index object (root or object field) to a Java object.
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor
	 * @return {@code this}, for method chaining.
	 */
	ConstructorMappingStep projectionConstructor();

	/**
	 * Starts the definition of the mapping of a specific constructor parameter.
	 *
	 * @param index The zero-based index of the parameter in this constructor.
	 * @return A DSL step where the property mapping can be defined in more details.
	 */
	MethodParameterMappingStep parameter(int index);

}
