/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScopeProvider;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A Hibernate Search session, bound to a particular tenant identifier (if any),
 * and with its own overridden settings regarding commits, refresh, etc.
 * <p>
 * Provides entry points to Hibernate Search operations that involve indexing and searching.
 */
@Incubating
public interface SearchSession extends SearchScopeProvider, AutoCloseable {

	/**
	 * Execute any pending work in the {@link #indexingPlan() indexing plan}
	 * and release any resource held by this session.
	 */
	@Override
	void close();

	/**
	 * Determine whether the search session is open.
	 * @return true until the search session has been closed
	 */
	boolean isOpen();

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes of all indexed entity types.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @return The created mass indexer.
	 */
	default MassIndexer massIndexer() {
		return massIndexer( Object.class );
	}

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes of
	 * indexed entity classes among the given classes and their subtypes.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @param classes An array of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @return The created mass indexer.
	 */
	default MassIndexer massIndexer(Class<?>... classes) {
		return massIndexer( Arrays.asList( classes ) );
	}

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param classes A collection of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @return A {@link MassIndexer}.
	 */
	MassIndexer massIndexer(Collection<? extends Class<?>> classes);

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to
	 * indexed entity types among the given class and its subtypes.
	 *
	 * @param clazz A class that must be an indexed entity type or a supertype of such type.
	 * @param <T> An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	default <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(Class<T> clazz) {
		return search( Collections.singleton( clazz ) );
	}

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param classes A collection of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @param <T> A supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	<T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(Collection<? extends Class<? extends T>> classes);

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes in the given scope.
	 *
	 * @param scope A scope representing all indexed types that will be targeted by the search query.
	 * @param <T> A supertype of all types in the given scope.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	<T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(SearchScope<T> scope);

	/**
	 * Create a {@link SearchSchemaManager} for all indexes.
	 *
	 * @return A {@link SearchSchemaManager}.
	 */
	default SearchSchemaManager schemaManager() {
		return schemaManager( Collections.singleton( Object.class ) );
	}

	/**
	 * Create a {@link SearchSchemaManager} for the indexes mapped to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param classes An array of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @return A {@link SearchSchemaManager}.
	 */
	default SearchSchemaManager schemaManager(Class<?>... classes) {
		return schemaManager( Arrays.asList( classes ) );
	}

	/**
	 * Create a {@link SearchSchemaManager} for the indexes mapped to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param classes A collection of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @return A {@link SearchSchemaManager}.
	 */
	SearchSchemaManager schemaManager(Collection<? extends Class<?>> classes);

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to all indexed types.
	 *
	 * @return A {@link SearchWorkspace}.
	 */
	default SearchWorkspace workspace() {
		return workspace( Collections.singleton( Object.class ) );
	}

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param classes An array of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @return A {@link SearchWorkspace}.
	 */
	default SearchWorkspace workspace(Class<?>... classes) {
		return workspace( Arrays.asList( classes ) );
	}

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param classes A collection of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @return A {@link SearchWorkspace}.
	 */
	SearchWorkspace workspace(Collection<? extends Class<?>> classes);

	/**
	 * @return The indexing plan for this session. It will be executed upon closing this session.
	 */
	SearchIndexingPlan indexingPlan();

	/**
	 * @return The indexer for this session.
	 */
	SearchIndexer indexer();

	/**
	 * @return The tenant identifier for this session.
	 * @deprecated Use {@link #tenantIdentifierValue()} instead.
	 */
	@Deprecated(forRemoval = true)
	String tenantIdentifier();

	/**
	 * @return The tenant identifier for this session.
	 */
	Object tenantIdentifierValue();

	/**
	 * Set the {@link IndexingPlanSynchronizationStrategy} to use for this session.
	 * <p>
	 * Behavior is undefined if called while entity changes are pending:
	 * be sure to call this only just after creating a session.
	 *
	 * @param synchronizationStrategy The synchronization strategy to use
	 * @see IndexingPlanSynchronizationStrategy
	 */
	void indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy synchronizationStrategy);

}
