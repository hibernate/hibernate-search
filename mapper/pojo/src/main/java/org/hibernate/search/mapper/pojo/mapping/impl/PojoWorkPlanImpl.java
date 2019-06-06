/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoWorkPlanImpl implements PojoWorkPlan {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private final PojoRuntimeIntrospector introspector;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<Class<?>, PojoIndexedTypeWorkPlan<?, ?, ?>> indexedTypeDelegates = new LinkedHashMap<>();
	private final Map<Class<?>, PojoContainedTypeWorkPlan<?>> containedTypeDelegates = new HashMap<>();

	PojoWorkPlanImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers,
			AbstractPojoSessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
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
	public void add(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		AbstractPojoTypeWorkPlan delegate = getDelegate( clazz );
		delegate.add( id, entity );
	}

	@Override
	public void update(Object entity) {
		update( null, entity );
	}

	@Override
	public void update(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		AbstractPojoTypeWorkPlan delegate = getDelegate( clazz );
		delegate.update( id, entity );
	}

	@Override
	public void update(Object entity, String... dirtyPaths) {
		update( null, entity, dirtyPaths );
	}

	@Override
	public void update(Object id, Object entity, String... dirtyPaths) {
		Class<?> clazz = getIntrospector().getClass( entity );
		AbstractPojoTypeWorkPlan delegate = getDelegate( clazz );
		delegate.update( id, entity, dirtyPaths );
	}

	@Override
	public void delete(Object entity) {
		delete( null, entity );
	}

	@Override
	public void delete(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		AbstractPojoTypeWorkPlan delegate = getDelegate( clazz );
		delegate.delete( id, entity );
	}

	@Override
	public void prepare() {
		for ( PojoContainedTypeWorkPlan<?> delegate : containedTypeDelegates.values() ) {
			delegate.resolveDirty( this::updateBecauseOfContained );
		}
		for ( PojoIndexedTypeWorkPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
			delegate.resolveDirty( this::updateBecauseOfContained );
		}
		for ( PojoIndexedTypeWorkPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
			delegate.prepare();
		}
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			prepare();
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for ( PojoIndexedTypeWorkPlan<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
				futures.add( delegate.execute() );
			}
			return CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[0] ) );
		}
		finally {
			indexedTypeDelegates.clear();
		}
	}

	private PojoRuntimeIntrospector getIntrospector() {
		return introspector;
	}

	private AbstractPojoTypeWorkPlan getDelegate(Class<?> clazz) {
		AbstractPojoTypeWorkPlan delegate = indexedTypeDelegates.get( clazz );
		if ( delegate == null ) {
			delegate = containedTypeDelegates.get( clazz );
			if ( delegate == null ) {
				delegate = createDelegate( clazz );
			}
		}
		return delegate;
	}

	private AbstractPojoTypeWorkPlan createDelegate(Class<?> clazz) {
		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> indexedTypeManagerOptional =
				indexedTypeManagers.getByExactClass( clazz );
		if ( indexedTypeManagerOptional.isPresent() ) {
			PojoIndexedTypeWorkPlan<?, ?, ?> delegate = indexedTypeManagerOptional.get()
					.createWorkPlan( sessionContext, commitStrategy, refreshStrategy );
			indexedTypeDelegates.put( clazz, delegate );
			return delegate;
		}
		else {
			Optional<? extends PojoContainedTypeManager<?>> containedTypeManagerOptional =
					containedTypeManagers.getByExactClass( clazz );
			if ( containedTypeManagerOptional.isPresent() ) {
				PojoContainedTypeWorkPlan<?> delegate = containedTypeManagerOptional.get()
						.createWorkPlan( sessionContext );
				containedTypeDelegates.put( clazz, delegate );
				return delegate;
			}
		}
		throw log.notIndexedTypeNorAsDelegate( clazz );
	}

	private PojoIndexedTypeWorkPlan<?, ?, ?> getOrCreateIndexedDelegateForContainedUpdate(Class<?> clazz) {
		PojoIndexedTypeWorkPlan<?, ?, ?> delegate = indexedTypeDelegates.get( clazz );
		if ( delegate != null ) {
			return delegate;
		}

		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> indexedTypeManagerOptional =
				indexedTypeManagers.getByExactClass( clazz );
		if ( indexedTypeManagerOptional.isPresent() ) {
			delegate = indexedTypeManagerOptional.get()
					.createWorkPlan( sessionContext, commitStrategy, refreshStrategy );
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
		PojoIndexedTypeWorkPlan<?, ?, ?> delegate = getOrCreateIndexedDelegateForContainedUpdate( clazz );
		delegate.updateBecauseOfContained( containingEntity );
	}

}
