/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

/**
 * A context to define programmatic mapping.
 */
public interface ProgrammaticMappingDefinitionContext {

	/**
	 * Starts the definition of the mapping of a specific type.
	 *
	 * @param clazz The type to map.
	 * @return A context to map this type.
	 */
	TypeMappingContext type(Class<?> clazz);

}
