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
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoIndexingPlanImpl implements PojoIndexingPlan {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkIndexedTypeContextProvider indexedTypeContextProvider;
	private final PojoWorkContainedTypeContextProvider containedTypeContextProvider;
	private final AbstractPojoBackendSessionContext sessionContext;
	private final PojoRuntimeIntrospector introspector;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<Class<?>, PojoIndexedTypeIndexingPlan<?, ?, ?>> indexedTypeDelegates = new LinkedHashMap<>();
	private final Map<Class<?>, PojoContainedTypeIndexingPlan<?>> containedTypeDelegates = new LinkedHashMap<>();

	private boolean isProcessing = false;

	public PojoIndexingPlanImpl(PojoWorkIndexedTypeContextProvider indexedTypeContextProvider,
			PojoWorkContainedTypeContextProvider containedTypeContextProvider,
			AbstractPojoBackendSessionContext sessionContext,
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
	public void add(Object entity) {
		add( null, entity );
	}

	@Override
	public void add(Object providedId, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		AbstractPojoTypeIndexingPlan delegate = getDelegate( clazz );
		delegate.add( providedId, entity );
	}

	@Override
	public void addOrUpdate(Object entity) {
		addOrUpdate( null, entity );
	}

	@Override
	public void addOrUpdate(Object providedId, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		AbstractPojoTypeIndexingPlan delegate = getDelegate( clazz );
		delegate.update( providedId, entity );
	}

	@Override
	public void addOrUpdate(Object entity, String... dirtyPaths) {
		addOrUpdate( null, entity, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Object providedId, Object entity, String... dirtyPaths) {
		Class<?> clazz = getIntrospector().getClass( entity );
		AbstractPojoTypeIndexingPlan delegate = getDelegate( clazz );
		delegate.update( providedId, entity, dirtyPaths );
	}

	@Override
	public void delete(Object entity) {
		delete( null, entity );
	}

	@Override
	public void delete(Object providedId, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		AbstractPojoTypeIndexingPlan delegate = getDelegate( clazz );
		delegate.delete( providedId, entity );
	}

	@Override
	public void purge(Class<?> clazz, Object providedId) {
		AbstractPojoTypeIndexingPlan delegate = getDelegate( clazz );
		delegate.purge( providedId );
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

	private AbstractPojoTypeIndexingPlan getDelegate(Class<?> clazz) {
		AbstractPojoTypeIndexingPlan delegate = indexedTypeDelegates.get( clazz );
		if ( delegate == null ) {
			delegate = containedTypeDelegates.get( clazz );
			if ( delegate == null ) {
				delegate = createDelegate( clazz );
			}
		}
		return delegate;
	}

	private AbstractPojoTypeIndexingPlan createDelegate(Class<?> clazz) {
		Optional<? extends PojoWorkIndexedTypeContext<?, ?, ?>> indexedTypeContextOptional =
				indexedTypeContextProvider.getByExactClass( clazz );
		if ( indexedTypeContextOptional.isPresent() ) {
			PojoIndexedTypeIndexingPlan<?, ?, ?> delegate = indexedTypeContextOptional.get()
					.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy );
			indexedTypeDelegates.put( clazz, delegate );
			return delegate;
		}
		else {
			Optional<? extends PojoWorkContainedTypeContext<?>> containedTypeContextOptional =
					containedTypeContextProvider.getByExactClass( clazz );
			if ( containedTypeContextOptional.isPresent() ) {
				PojoContainedTypeIndexingPlan<?> delegate = containedTypeContextOptional.get()
						.createIndexingPlan( sessionContext );
				containedTypeDelegates.put( clazz, delegate );
				return delegate;
			}
		}
		throw log.notIndexedTypeNorAsDelegate( clazz );
	}

	private PojoIndexedTypeIndexingPlan<?, ?, ?> getOrCreateIndexedDelegateForContainedUpdate(Class<?> clazz) {
		PojoIndexedTypeIndexingPlan<?, ?, ?> delegate = indexedTypeDelegates.get( clazz );
		if ( delegate != null ) {
			return delegate;
		}

		Optional<? extends PojoWorkIndexedTypeContext<?, ?, ?>> indexedTypeManagerOptional =
				indexedTypeContextProvider.getByExactClass( clazz );
		if ( indexedTypeManagerOptional.isPresent() ) {
			delegate = indexedTypeManagerOptional.get()
					.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy );
			indexedTypeDelegates.put( clazz, delegate );
			return delegate;
		}

		throw new AssertionFailure(
				"Attempt to reindex an entity of type " + clazz + " because a contained entity was modified,"
				+ " but " + clazz + " is not indexed directly."
				+ " This is proa"
		);
	}

	private void updateBecauseOfContained(Object containingEntity) {
		// TODO ignore the event when containingEntity has provided IDs
		Class<?> clazz = getIntrospector().getClass( containingEntity );
		PojoIndexedTypeIndexingPlan<?, ?, ?> delegate = getOrCreateIndexedDelegateForContainedUpdate( clazz );
		delegate.updateBecauseOfContained( containingEntity );
	}

}
