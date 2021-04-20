/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * The step in a mapping definition where a type can be mapped,
 * and where properties of that type can be referenced to map them more precisely.
 */
public interface TypeMappingStep {

	/**
	 * Maps an entity type to an index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 */
	TypeMappingIndexedStep indexed();

	/**
	 * Define a type binder, responsible for creating a bridge.
	 * To pass some parameters to the bridge,
	 * use the method {@link #binder(TypeBinder, Map)} instead.
	 *
	 * @param binder A {@link TypeBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see TypeBinder
	 */
	default TypeMappingStep binder(TypeBinder binder) {
		return binder( binder, Collections.emptyMap() );
	}

	/**
	 * Define a type binder, responsible for creating a bridge.
	 * With this method it is possible to pass a set of parameters to the binder,
	 * so that they can be used by the bridge.
	 *
	 * @param binder A {@link TypeBinder} responsible for creating a bridge.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see TypeBinder
	 */
	TypeMappingStep binder(TypeBinder binder, Map<String, Object> params);

	/**
	 * Starts the definition of the mapping of a specific property.
	 *
	 * @param propertyName The name of a property in this type.
	 * @return A DSL step where the property mapping can be defined in more details.
	 */
	PropertyMappingStep property(String propertyName);

}
