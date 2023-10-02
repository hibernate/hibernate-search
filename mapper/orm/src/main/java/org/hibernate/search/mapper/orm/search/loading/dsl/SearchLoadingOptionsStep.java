/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.search.loading.dsl;

import java.util.function.Consumer;

import jakarta.persistence.EntityGraph;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

/**
 * The DSL entry point passed to consumers in
 * {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#loading(Consumer)},
 * allowing the definition of loading options (fetch size, cache lookups, ...).
 */
public interface SearchLoadingOptionsStep {

	/**
	 * Set the fetch size for this query,
	 * i.e. the amount of entities to load for each query to the database.
	 * <p>
	 * Higher numbers mean fewer queries, but larger result sets.
	 *
	 * @param fetchSize The fetch size. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 * @see Query#setFetchSize(int)
	 */
	SearchLoadingOptionsStep fetchSize(int fetchSize);

	/**
	 * Set the strategy for cache lookup before query results are loaded.
	 *
	 * @param strategy The strategy.
	 * @return {@code this} for method chaining.
	 */
	SearchLoadingOptionsStep cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy);

	/**
	 * Customize fetching/loading of entity attributes and associations
	 * according to the given entity graph, with the given semantic.
	 *
	 * @param graph The graph to apply.
	 * @param semantic The semantic to use when applying the graph.
	 * @return {@code this} for method chaining.
	 * @see org.hibernate.Session#createEntityGraph(Class)
	 * @see org.hibernate.Session#createEntityGraph(String)
	 * @see org.hibernate.Session#getEntityGraph(String)
	 */
	SearchLoadingOptionsStep graph(EntityGraph<?> graph, GraphSemantic semantic);

	/**
	 * Customize fetching/loading of entity attributes and associations
	 * according to the entity graph with the given name, with the given semantic.
	 *
	 * @param graphName The name of the graph to apply.
	 * @param semantic The semantic to use when applying the graph.
	 * @return {@code this} for method chaining.
	 * @see jakarta.persistence.NamedEntityGraph
	 */
	SearchLoadingOptionsStep graph(String graphName, GraphSemantic semantic);

}
