/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.util.SearchException;

class StreamPojoWorkerImpl extends PojoWorkerImpl implements StreamPojoWorker {

	private final PojoSessionContext sessionContext;
	private final Map<Class<?>, StreamPojoIndexedTypeWorker<?, ?, ?>> delegates = new ConcurrentHashMap<>();
	private volatile boolean addedAll = false;

	StreamPojoWorkerImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoSessionContext sessionContext) {
		super( indexedTypeManagers, sessionContext.getRuntimeIntrospector() );
		this.sessionContext = sessionContext;
	}

	@Override
	public void flush() {
		for ( StreamPojoIndexedTypeWorker<?, ?, ?> delegate : getAllDelegates() ) {
			delegate.flush();
		}
	}

	@Override
	public void flush(Class<?> clazz) {
		getDelegate( clazz ).flush();
	}

	@Override
	public void optimize() {
		for ( StreamPojoIndexedTypeWorker<?, ?, ?> delegate : getAllDelegates() ) {
			delegate.optimize();
		}
	}

	@Override
	public void optimize(Class<?> clazz) {
		getDelegate( clazz ).optimize();
	}

	@Override
	StreamPojoIndexedTypeWorker<?, ?, ?> getDelegate(Class<?> clazz) {
		return delegates.computeIfAbsent( clazz, c -> getIndexedTypeManager( clazz ).createStreamWorker( sessionContext ) );
	}

	private <E> PojoIndexedTypeManager<?, E, ?> getIndexedTypeManager(Class<E> clazz) {
		return indexedTypeManagers.getByExactClass( clazz )
				.orElseThrow( () -> new SearchException( "Cannot work on type " + clazz + ", because it is not indexed." ) );
	}

	private Iterable<StreamPojoIndexedTypeWorker<?, ?, ?>> getAllDelegates() {
		if ( !addedAll ) {
			getAllIndexedTypeManagers().forEach( manager -> getDelegate( manager.getClass() ) );
			addedAll = true;
		}
		return delegates.values();
	}
}
