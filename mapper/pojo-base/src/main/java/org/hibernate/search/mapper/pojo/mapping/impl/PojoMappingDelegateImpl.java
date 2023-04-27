/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
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


public class PojoMappingDelegateImpl implements PojoMappingDelegate {

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
	public TenancyMode tenancyMode() {
		return tenancyMode;
	}

	@Override
	public ProjectionRegistry projectionRegistry() {
		return searchQueryElementRegistry;
	}

	@Override
	public PojoEntityReferenceFactory createEntityReferenceFactory(PojoEntityReferenceFactoryDelegate delegate) {
		return new PojoEntityReferenceFactory( delegate, typeManagers );
	}

	@Override
	public <R extends EntityReference, E, C> PojoScopeDelegate<R, E, C> createPojoScope(
			PojoScopeMappingContext mappingContext,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		return PojoScopeDelegateImpl.create(
				mappingContext,
				typeManagers,
				targetedTypes,
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
		Set<PojoRawTypeIdentifier<?>> typeIdentifiers = new LinkedHashSet<>();
		for ( PojoIndexedTypeManager<?, ?> typeContext : typeManagers.allIndexed() ) {
			typeIdentifiers.add( typeContext.typeIdentifier() );
		}
		return Optional.of( PojoScopeDelegateImpl.create(
				mappingContext,
				typeManagers,
				typeIdentifiers,
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
