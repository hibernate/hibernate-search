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
	 * With this method it is possible to pass a set of parameters to the binder.
	 *
	 * @param binder A {@link TypeBinder} responsible for creating a bridge.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see TypeBinder
	 */
	TypeMappingStep binder(TypeBinder binder, Map<String, Object> params);

	/**
	 * Starts the definition of the mapping of the main constructor of this type.
	 * <p>
	 * The main constructor only exists if this type defines a single constructor
	 * and that constructor accepts at least one argument.
	 *
	 * @return A DSL step where the property mapping can be defined in more details.
	 * @throws org.hibernate.search.util.common.SearchException If this type doesn't have a main constructor.
	 */
	ConstructorMappingStep mainConstructor();

	/**
	 * Starts the definition of the mapping of the constructor of this type accepting arguments with the given types.
	 *
	 * @param parameterTypes The type of parameters of a constructor in the type being mapped.
	 * @return A DSL step where the property mapping can be defined in more details.
	 * @throws org.hibernate.search.util.common.SearchException If this type does not declare a constructor
	 * with the given types.
	 */
	ConstructorMappingStep constructor(Class<?>... parameterTypes);

	/**
	 * Starts the definition of the mapping of a specific property.
	 *
	 * @param propertyName The name of a property in the type being mapped.
	 * @return A DSL step where the property mapping can be defined in more details.
	 */
	PropertyMappingStep property(String propertyName);

}
