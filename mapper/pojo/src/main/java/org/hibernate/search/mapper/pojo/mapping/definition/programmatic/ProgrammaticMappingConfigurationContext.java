/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

/**
 * A context to configure programmatic mapping.
 */
public interface ProgrammaticMappingConfigurationContext {

	/**
	 * Starts the definition of the mapping of a specific type.
	 *
	 * @param clazz The type to map.
	 * @return The initial step of a DSL where the type mapping can be defined.
	 */
	TypeMappingStep type(Class<?> clazz);

}
