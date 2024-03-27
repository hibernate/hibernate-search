/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * The step in a mapping definition where a type's indexing can be configured more precisely.
 */
public interface TypeMappingIndexedStep {

	/**
	 * @param backendName The name of the backend.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#backend()
	 */
	TypeMappingIndexedStep backend(String backendName);

	/**
	 * @param indexName The name of the index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#index()
	 */
	TypeMappingIndexedStep index(String indexName);

	/**
	 * @param enabled {@code true} to map the type to an index (the default),
	 * {@code false} to disable the mapping to an index.
	 * Useful to disable indexing when subclassing an indexed type.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#enabled()
	 */
	TypeMappingIndexedStep enabled(boolean enabled);

	/**
	 * Define a routing binder, responsible for creating a bridge.
	 * To pass some parameters to the bridge,
	 * use the method {@link #routingBinder(RoutingBinder, Map)} instead.
	 *
	 * @param binder A {@link RoutingBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#routingBinder()
	 */
	default TypeMappingIndexedStep routingBinder(RoutingBinder binder) {
		return routingBinder( binder, Collections.emptyMap() );
	}

	/**
	 * Define a routing binder, responsible for creating a bridge.
	 * With this method it is possible to pass a set of parameters to the binder.
	 *
	 * @param binder A {@link RoutingBinder} responsible for creating a bridge.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see Indexed#routingBinder()
	 */
	TypeMappingIndexedStep routingBinder(RoutingBinder binder, Map<String, Object> params);

}
