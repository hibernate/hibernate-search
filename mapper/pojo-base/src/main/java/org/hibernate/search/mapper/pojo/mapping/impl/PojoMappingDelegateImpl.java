/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

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
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoRawTypeIdentifierResolver;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeDelegateImpl;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoSearchQueryElementRegistry;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexerImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanEventProcessingStrategy;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanEventSendingStrategy;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanLocalStrategy;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingQueueEventProcessingPlanImpl;
import org.hibernate.search.mapper.pojo.work.impl.SearchIndexingPlanFilterContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMappingDelegateImpl implements PojoMappingDelegate {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ThreadPoolProvider threadPoolProvider;
	private final FailureHandler failureHandler;
	private final TenancyMode tenancyMode;
	private final PojoTypeManagerContainer typeManagers;
	private final PojoSearchQueryElementRegistry searchQueryElementRegistry;

	public PojoMappingDelegateImpl(ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler,
			TenancyMode tenancyMode,
			PojoTypeManagerContainer typeManagers,
			PojoSearchQueryElementRegistry searchQueryElementRegistry) {
		this.threadPoolProvider = threadPoolProvider;
		this.failureHandler = failureHandler;
		this.tenancyMode = tenancyMode;
		this.typeManagers = typeManagers;
		this.searchQueryElementRegistry = searchQueryElementRegistry;
	}

	@Override
	public void close() {
		typeManagers.close();
	}

	@Override
	public ThreadPoolProvider threadPoolProvider() {
		return threadPoolProvider;
	}

	@Override
	public FailureHandler failureHandler() {
		return failureHandler;
	}

	@Override
	public PojoRawTypeIdentifierResolver typeIdentifierResolver() {
		return typeManagers;
	}

	@Override
	public PojoLoadingTypeContextProvider typeContextProvider() {
		return typeManagers;
	}

	@Override
	public TenancyMode tenancyMode() {
		return tenancyMode;
	}

	@Override
	public ProjectionRegistry projectionRegistry() {
		return searchQueryElementRegistry;
	}

	@Override
	public ProjectionMappedTypeContext mappedTypeContext(String name) {
		return typeManagers.indexedByEntityName().getOrFail( name );
	}

	@Override
	public PojoEntityReferenceFactory createEntityReferenceFactory(PojoEntityReferenceFactoryDelegate delegate) {
		return new PojoEntityReferenceFactory( delegate, typeManagers );
	}

	@Override
	public <R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScopeForClasses(
			PojoScopeMappingContext mappingContext,
			Collection<? extends Class<? extends E>> classes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		if ( classes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}
		return PojoScopeDelegateImpl.create(
				mappingContext,
				typeManagers,
				typeManagers.indexedForSuperTypeClasses( classes ),
				indexedTypeExtendedContextProvider
		);
	}

	@Override
	@SuppressWarnings("unchecked") // The cast is checked through reflection
	public <R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScopeForEntityNames(
			PojoScopeMappingContext mappingContext, Class<E> expectedSuperType, Collection<String> entityNames,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		if ( entityNames.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}
		Set<? extends PojoIndexedTypeManager<?, ?>> typeContexts = typeManagers.indexedForSuperTypeEntityNames( entityNames );
		for ( PojoIndexedTypeManager<?, ?> typeContext : typeContexts ) {
			Class<?> actualJavaType = typeContext.typeIdentifier().javaClass();
			if ( !expectedSuperType.isAssignableFrom( actualJavaType ) ) {
				throw log.invalidEntitySuperType( typeContext.name(), expectedSuperType, actualJavaType );
			}
		}
		return PojoScopeDelegateImpl.create(
				mappingContext,
				typeManagers,
				(Set<? extends PojoIndexedTypeManager<?, ? extends E>>) typeContexts,
				indexedTypeExtendedContextProvider
		);
	}

	@Override
	@Deprecated
	public <R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScope(
			PojoScopeMappingContext mappingContext,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		if ( targetedTypes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}
		return PojoScopeDelegateImpl.create(
				mappingContext,
				typeManagers,
				typeManagers.indexedForSuperTypes( targetedTypes ),
				indexedTypeExtendedContextProvider
		);
	}

	@Override
	public <R extends EntityReference, C> Optional<PojoScopeDelegate<R, Object, C>> createPojoAllScope(
			PojoScopeMappingContext mappingContext,
			PojoScopeTypeExtendedContextProvider<Object, C> indexedTypeExtendedContextProvider) {
		if ( typeManagers.allIndexed().isEmpty() ) {
			return Optional.empty();
		}
		return Optional.of( PojoScopeDelegateImpl.create(
				mappingContext,
				typeManagers,
				typeManagers.allIndexed(),
				indexedTypeExtendedContextProvider
		) );
	}

	@Override
	public PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexingPlanImpl( typeManagers, context,
				new PojoIndexingPlanLocalStrategy( commitStrategy, refreshStrategy ) );
	}

	@Override
	public PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context, PojoIndexingQueueEventSendingPlan sendingPlan) {
		return new PojoIndexingPlanImpl( typeManagers, context,
				new PojoIndexingPlanEventSendingStrategy( sendingPlan ) );
	}

	@Override
	public PojoIndexingQueueEventProcessingPlan createEventProcessingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			PojoIndexingQueueEventSendingPlan sendingPlan) {
		return new PojoIndexingQueueEventProcessingPlanImpl( typeManagers, context,
				new PojoIndexingPlanImpl( typeManagers, context,
						new PojoIndexingPlanEventProcessingStrategy( commitStrategy, refreshStrategy, sendingPlan ) ) );
	}

	@Override
	public PojoIndexer createIndexer(PojoWorkSessionContext context) {
		return new PojoIndexerImpl(
				typeManagers,
				context
		);
	}

	@Override
	public ConfiguredSearchIndexingPlanFilter configuredSearchIndexingPlanFilter(SearchIndexingPlanFilter filter,
			ConfiguredSearchIndexingPlanFilter fallback) {
		SearchIndexingPlanFilterContextImpl context = new SearchIndexingPlanFilterContextImpl( typeManagers );
		filter.apply( context );
		return context.createFilter( fallback );
	}
}
