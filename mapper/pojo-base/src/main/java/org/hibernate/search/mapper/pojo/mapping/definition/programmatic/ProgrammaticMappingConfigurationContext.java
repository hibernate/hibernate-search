/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	/**
	 * Starts the definition of the mapping of a specific named type.
	 *
	 * @param typeName The name of the type. For example for a dynamic, map-based entity type,
	 * this would be the entity name.
	 * @return The initial step of a DSL where the type mapping can be defined.
	 * @throws org.hibernate.search.util.common.SearchException If the given name does not match any known type.
	 */
	TypeMappingStep type(String typeName);

}
