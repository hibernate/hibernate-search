/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;

class ChangesetPojoWorkerImpl extends PojoWorkerImpl implements ChangesetPojoWorker {

	private final PojoContainedTypeManagerContainer containedTypeManagers;
	private final PojoSessionContext sessionContext;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<Class<?>, ChangesetPojoIndexedTypeWorker<?, ?, ?>> indexedTypeDelegates = new LinkedHashMap<>();
	private final Map<Class<?>, ChangesetPojoContainedTypeWorker<?>> containedTypeDelegates = new HashMap<>();

	ChangesetPojoWorkerImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers,
			PojoSessionContext sessionContext) {
		super( indexedTypeManagers, sessionContext.getRuntimeIntrospector() );
		this.containedTypeManagers = containedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public void prepare() {
		for ( ChangesetPojoContainedTypeWorker<?> delegate : containedTypeDelegates.values() ) {
			delegate.resolveDirty( this::updateBecauseOfContained );
		}
		for ( ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
			delegate.resolveDirty( this::updateBecauseOfContained );
		}
		for ( ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
			delegate.prepare();
		}
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			prepare();
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for ( ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate : indexedTypeDelegates.values() ) {
				futures.add( delegate.execute() );
			}
			return CompletableFuture.allOf( futures.toArray( new CompletableFuture[futures.size()] ) );
		}
		finally {
			indexedTypeDelegates.clear();
		}
	}

	@Override
	PojoTypeWorker getDelegate(Class<?> clazz) {
		PojoTypeWorker delegate = indexedTypeDelegates.get( clazz );
		if ( delegate == null ) {
			delegate = containedTypeDelegates.get( clazz );
			if ( delegate == null ) {
				delegate = createDelegate( clazz );
			}
		}
		return delegate;
	}

	private PojoTypeWorker createDelegate(Class<?> clazz) {
		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> indexedTypeManagerOptional =
				indexedTypeManagers.getByExactClass( clazz );
		if ( indexedTypeManagerOptional.isPresent() ) {
			ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate = indexedTypeManagerOptional.get()
					.createWorker( sessionContext );
			indexedTypeDelegates.put( clazz, delegate );
			return delegate;
		}
		else {
			Optional<? extends PojoContainedTypeManager<?>> containedTypeManagerOptional =
					containedTypeManagers.getByExactClass( clazz );
			if ( containedTypeManagerOptional.isPresent() ) {
				ChangesetPojoContainedTypeWorker<?> delegate = containedTypeManagerOptional.get()
						.createWorker( sessionContext );
				containedTypeDelegates.put( clazz, delegate );
				return delegate;
			}
		}
		throw new SearchException(
				"Cannot work on type " + clazz + ", because it is not indexed,"
				+ " neither directly nor as a contained entity in another type."
		);
	}

	private ChangesetPojoIndexedTypeWorker<?, ?, ?> getOrCreateIndexedDelegateForContainedUpdate(Class<?> clazz) {
		ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate = indexedTypeDelegates.get( clazz );
		if ( delegate != null ) {
			return delegate;
		}

		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> indexedTypeManagerOptional =
				indexedTypeManagers.getByExactClass( clazz );
		if ( indexedTypeManagerOptional.isPresent() ) {
			delegate = indexedTypeManagerOptional.get().createWorker( sessionContext );
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
		ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate = getOrCreateIndexedDelegateForContainedUpdate( clazz );
		delegate.updateBecauseOfContained( containingEntity );
	}

}
