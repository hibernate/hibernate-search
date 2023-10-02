/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

public interface PojoMappingDelegate extends AutoCloseable {

	@Override
	void close();

	ThreadPoolProvider threadPoolProvider();

	FailureHandler failureHandler();

	PojoRawTypeIdentifierResolver typeIdentifierResolver();

	PojoLoadingTypeContextProvider typeContextProvider();

	TenancyMode tenancyMode();

	ProjectionRegistry projectionRegistry();

	ProjectionMappedTypeContext mappedTypeContext(String name);

	EntityReferenceFactory createEntityReferenceFactory(PojoEntityReferenceFactoryDelegate delegate);

	/**
	 * Creates a {@link PojoScopeDelegate} limited to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param mappingContext The mapping context.
	 * @param classes A collection of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @param indexedTypeExtendedContextProvider A provider of extended, mapper-specific type contexts.
	 * that will be made available through {@link PojoScopeDelegate#includedIndexedTypes()}.
	 * @return A {@link PojoScopeDelegate}
	 * @param <R> The type of entity references.
	 * @param <E> A supertype of all indexed entity types to include in the scope.
	 * @param <C> The type of extended, mapper-specific type contexts.
	 */
	<R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScopeForClasses(
			PojoScopeMappingContext mappingContext,
			Collection<? extends Class<? extends E>> classes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider);

	/**
	 * Creates a {@link PojoScopeDelegate} limited to
	 * indexed entity types among the entities with the given names and their subtypes.
	 *
	 * @param mappingContext The mapping context.
	 * @param expectedSuperType A supertype of all indexed entity types to include in the scope.
	 * @param entityNames A collection of entity names.
	 * Each entity type referenced in the collection must be an indexed entity type or a supertype of such type.
	 * @param indexedTypeExtendedContextProvider A provider of extended, mapper-specific type contexts.
	 * that will be made available through {@link PojoScopeDelegate#includedIndexedTypes()}.
	 * @return A {@link PojoScopeDelegate}
	 * @param <R> The type of entity references.
	 * @param <E> A supertype of all indexed entity types to include in the scope.
	 * @param <C> The type of extended, mapper-specific type contexts.
	 */
	<R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScopeForEntityNames(
			PojoScopeMappingContext mappingContext,
			Class<E> expectedSuperType, Collection<String> entityNames,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider);

	/**
	 * Creates a {@link PojoScopeDelegate} limited to
	 * indexed entity types among the given types and their subtypes.
	 *
	 * @param mappingContext The mapping context.
	 * @param targetedTypes A collection of types.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @param indexedTypeExtendedContextProvider A provider of extended, mapper-specific type contexts.
	 * that will be made available through {@link PojoScopeDelegate#includedIndexedTypes()}.
	 * @return A {@link PojoScopeDelegate}
	 * @param <R> The type of entity references.
	 * @param <E> A supertype of all indexed entity types to include in the scope.
	 * @param <C> The type of extended, mapper-specific type contexts.
	 *
	 * @deprecated Use {@link #createPojoScopeForClasses(PojoScopeMappingContext, Collection, PojoScopeTypeExtendedContextProvider)}
	 * or {@link #createPojoScopeForEntityNames(PojoScopeMappingContext, Class, Collection, PojoScopeTypeExtendedContextProvider)}
	 * or {@link #createPojoAllScope(PojoScopeMappingContext, PojoScopeTypeExtendedContextProvider)}
	 * instead.
	 */
	@Deprecated
	<R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScope(
			PojoScopeMappingContext mappingContext,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider);

	<R extends EntityReference, C> Optional<PojoScopeDelegate<R, Object, C>> createPojoAllScope(
			PojoScopeMappingContext mappingContext,
			PojoScopeTypeExtendedContextProvider<Object, C> indexedTypeExtendedContextProvider);

	PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context, PojoIndexingQueueEventSendingPlan sendingPlan);

	PojoIndexingQueueEventProcessingPlan createEventProcessingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			PojoIndexingQueueEventSendingPlan sendingPlan);

	PojoIndexer createIndexer(PojoWorkSessionContext context);

	ConfiguredSearchIndexingPlanFilter configuredSearchIndexingPlanFilter(SearchIndexingPlanFilter filter,
			ConfiguredSearchIndexingPlanFilter fallback);
}
