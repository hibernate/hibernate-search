/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoIndexingPlanImpl implements PojoIndexingPlan {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkIndexedTypeContextProvider indexedTypeContextProvider;
	private final PojoWorkContainedTypeContextProvider containedTypeContextProvider;
	private final PojoWorkSessionContext sessionContext;
	private final PojoRuntimeIntrospector introspector;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeIdentifier<?>, PojoIndexedTypeIndexingPlan<?, ?, ?>> indexedTypeDelegates = new LinkedHashMap<>();
	private final Map<PojoRawTypeIdentifier<?>, PojoContainedTypeIndexingPlan<?>> containedTypeDelegates = new LinkedHashMap<>();

	private boolean isProcessing = false;

	public PojoIndexingPlanImpl(PojoWorkIndexedTypeContextProvider indexedTypeContextProvider,
			PojoWorkContainedTypeContextProvider containedTypeContextProvider,
			PojoWorkSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.containedTypeContextProvider = containedTypeContextProvider;
		this.sessionContext = sessionContext;
		this.introspector = sessionContext.getRuntimeIntrospector();
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, Object entity) {
		AbstractPojoTypeIndexingPlan delegate = getDelegate( typeIdentifier );
		delegate.add( providedId, entity );
	}

	@Override
	public void addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, Object entity) {
		AbstractPojoTypeIndexingPlan delegate = getDelegate( typeIdentifier );
		delegate.update( providedId, entity );
	}

	@Override
	public void addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, Object entity, String... dirtyPaths) {
		AbstractPojoTypeIndexingPlan delegate = getDelegate( typeIdentifier );
		delegate.update( providedId, entity, dirtyPaths );
	}

	@Override
	public void delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, Object entity) {
		AbstractPojoTypeIndexingPlan delegate = getDelegate( typeIdentifier );
		delegate.delete( providedId, entity );
	}

	@Override
	public void purge(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey) {
		AbstractPojoTypeIndexingPlan delegate = getDelegate( typeIdentifier );
		delegate.purge( providedId, providedRoutingKey );
	}

	@Override
	public void process() {
		if ( isProcessing ) {
			throw log.recursiveIndexingPlanProcess();
		}

		isProcessing = true;
		try {
			for ( PojoContainedTypeIndexingPlan<?> delegate : containedTypeDelegates.values() ) {
				delegate.resolveDirty( this::updateBecauseOfContained );
			}
			for ( PojoIndexedTypeIndexingPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
				delegate.resolveDirty( this::updateBecauseOfContained );
			}
			for ( PojoIndexedTypeIndexingPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
				delegate.process();
			}
		}
		finally {
			isProcessing = false;
		}
	}

	@Override
	public CompletableFuture<IndexIndexingPlanExecutionReport> executeAndReport() {
		try {
			process();
			List<CompletableFuture<IndexIndexingPlanExecutionReport>> futures = new ArrayList<>();
			for ( PojoIndexedTypeIndexingPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
				futures.add( delegate.executeAndReport() );
			}
			return IndexIndexingPlanExecutionReport.allOf( futures );
		}
		finally {
			indexedTypeDelegates.clear();
		}
	}

	@Override
	public void discard() {
		try {
			for ( PojoIndexedTypeIndexingPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
				delegate.discard();
			}
		}
		finally {
			indexedTypeDelegates.clear();
		}
	}

	@Override
	public void discardNotProcessed() {
		for ( PojoIndexedTypeIndexingPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
			delegate.discardNotProcessed();
		}
	}

	private PojoRuntimeIntrospector getIntrospector() {
		return introspector;
	}

	private AbstractPojoTypeIndexingPlan getDelegate(PojoRawTypeIdentifier<?> typeIdentifier) {
		AbstractPojoTypeIndexingPlan delegate = indexedTypeDelegates.get( typeIdentifier );
		if ( delegate == null ) {
			delegate = containedTypeDelegates.get( typeIdentifier );
			if ( delegate == null ) {
				delegate = createDelegate( typeIdentifier );
			}
		}
		return delegate;
	}

	private AbstractPojoTypeIndexingPlan createDelegate(PojoRawTypeIdentifier<?> typeIdentifier) {
		Optional<? extends PojoWorkIndexedTypeContext<?, ?, ?>> indexedTypeContextOptional =
				indexedTypeContextProvider.getByExactType( typeIdentifier );
		if ( indexedTypeContextOptional.isPresent() ) {
			PojoIndexedTypeIndexingPlan<?, ?, ?> delegate = indexedTypeContextOptional.get()
					.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy );
			indexedTypeDelegates.put( typeIdentifier, delegate );
			return delegate;
		}
		else {
			Optional<? extends PojoWorkContainedTypeContext<?>> containedTypeContextOptional =
					containedTypeContextProvider.getByExactType( typeIdentifier );
			if ( containedTypeContextOptional.isPresent() ) {
				PojoContainedTypeIndexingPlan<?> delegate = containedTypeContextOptional.get()
						.createIndexingPlan( sessionContext );
				containedTypeDelegates.put( typeIdentifier, delegate );
				return delegate;
			}
		}
		throw log.notIndexedTypeNorAsDelegate( typeIdentifier );
	}

	private PojoIndexedTypeIndexingPlan<?, ?, ?> getOrCreateIndexedDelegateForContainedUpdate(PojoRawTypeIdentifier<?> typeIdentifier) {
		PojoIndexedTypeIndexingPlan<?, ?, ?> delegate = indexedTypeDelegates.get( typeIdentifier );
		if ( delegate != null ) {
			return delegate;
		}

		Optional<? extends PojoWorkIndexedTypeContext<?, ?, ?>> indexedTypeManagerOptional =
				indexedTypeContextProvider.getByExactType( typeIdentifier );
		if ( indexedTypeManagerOptional.isPresent() ) {
			delegate = indexedTypeManagerOptional.get()
					.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy );
			indexedTypeDelegates.put( typeIdentifier, delegate );
			return delegate;
		}

		throw new AssertionFailure(
				"Attempt to reindex an entity of type " + typeIdentifier + " because a contained entity was modified,"
				+ " but this entity type is not indexed directly."
				+ " There is a bug in Hibernate Search, please report it."
		);
	}

	private void updateBecauseOfContained(Object containingEntity) {
		// TODO ignore the event when containingEntity has provided IDs
		PojoRawTypeIdentifier<?> typeIdentifier = getIntrospector().getEntityTypeIdentifier( containingEntity );
		if ( typeIdentifier == null ) {
			throw new AssertionFailure(
					"Attempt to reindex entity " + containingEntity + " because a contained entity was modified,"
							+ " but this entity type is not indexed directly."
							+ " There is a bug in Hibernate Search, please report it."
			);
		}
		PojoIndexedTypeIndexingPlan<?, ?, ?> delegate = getOrCreateIndexedDelegateForContainedUpdate( typeIdentifier );
		delegate.updateBecauseOfContained( containingEntity );
	}

}
