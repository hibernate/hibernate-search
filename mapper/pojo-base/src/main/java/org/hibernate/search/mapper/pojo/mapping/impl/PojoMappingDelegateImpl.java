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
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingQueueEventProcessingPlanImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeDelegateImpl;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexerImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.Closer;


public class PojoMappingDelegateImpl implements PojoMappingDelegate {

	private final ThreadPoolProvider threadPoolProvider;
	private final FailureHandler failureHandler;
	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;

	public PojoMappingDelegateImpl(ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler,
			PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers) {
		this.threadPoolProvider = threadPoolProvider;
		this.failureHandler = failureHandler;
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoIndexedTypeManager::close, indexedTypeManagers.all() );
			closer.pushAll( PojoContainedTypeManager::close, containedTypeManagers.all() );
		}
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
	public <R, E, C> PojoScopeDelegate<R, E, C> createPojoScope(
			PojoScopeMappingContext mappingContext,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		return PojoScopeDelegateImpl.create(
				mappingContext,
				indexedTypeManagers,
				targetedTypes,
				indexedTypeExtendedContextProvider
		);
	}

	@Override
	public <R, C> Optional<PojoScopeDelegate<R, Object, C>> createPojoAllScope(PojoScopeMappingContext mappingContext,
			PojoScopeTypeExtendedContextProvider<Object, C> indexedTypeExtendedContextProvider) {
		if ( indexedTypeManagers.all().isEmpty() ) {
			return Optional.empty();
		}
		Set<PojoRawTypeIdentifier<?>> typeIdentifiers = new LinkedHashSet<>();
		for ( PojoIndexedTypeManager<?, ?> typeManager : indexedTypeManagers.all() ) {
			typeIdentifiers.add( typeManager.typeIdentifier() );
		}
		return Optional.of( PojoScopeDelegateImpl.create(
				mappingContext,
				indexedTypeManagers,
				typeIdentifiers,
				indexedTypeExtendedContextProvider
		) );
	}

	@Override
	public PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexingPlanImpl( indexedTypeManagers, containedTypeManagers,
				context, commitStrategy, refreshStrategy, true );
	}

	@Override
	public PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context, PojoIndexingQueueEventSendingPlan sink) {
		return new PojoIndexingPlanImpl( indexedTypeManagers, containedTypeManagers, context, sink );
	}

	@Override
	public PojoIndexingQueueEventProcessingPlan createEventProcessingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexingQueueEventProcessingPlanImpl( indexedTypeManagers, context,
				new PojoIndexingPlanImpl( indexedTypeManagers, containedTypeManagers,
						context, commitStrategy, refreshStrategy,
						// When processing indexing events sent from a queue,
						// Reindexing resolution is performed before the events are sent to the queue,
						// so we don't do it again when processing the events.
						false ) );
	}

	@Override
	public PojoIndexer createIndexer(PojoWorkSessionContext context) {
		return new PojoIndexerImpl(
				indexedTypeManagers,
				context
		);
	}
}
