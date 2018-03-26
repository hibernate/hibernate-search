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

class StreamPojoWorkerImpl extends PojoWorkerImpl implements StreamPojoWorker {

	private final PojoSessionContext sessionContext;
	private final Map<Class<?>, StreamPojoTypeWorker<?, ?, ?>> delegates = new ConcurrentHashMap<>();
	private volatile boolean addedAll = false;

	StreamPojoWorkerImpl(PojoTypeManagerContainer typeManagers,
			PojoSessionContext sessionContext) {
		super( typeManagers, sessionContext.getRuntimeIntrospector() );
		this.sessionContext = sessionContext;
	}

	@Override
	public void flush() {
		for ( StreamPojoTypeWorker<?, ?, ?> delegate : getAllDelegates() ) {
			delegate.flush();
		}
	}

	@Override
	public void flush(Class<?> clazz) {
		getDelegate( clazz ).flush();
	}

	@Override
	public void optimize() {
		for ( StreamPojoTypeWorker<?, ?, ?> delegate : getAllDelegates() ) {
			delegate.optimize();
		}
	}

	@Override
	public void optimize(Class<?> clazz) {
		getDelegate( clazz ).optimize();
	}

	@Override
	StreamPojoTypeWorker<?, ?, ?> getDelegate(Class<?> clazz) {
		return delegates.computeIfAbsent( clazz, c -> getTypeManager( clazz ).createStreamWorker( sessionContext ) );
	}

	private Iterable<StreamPojoTypeWorker<?, ?, ?>> getAllDelegates() {
		if ( !addedAll ) {
			getAllTypeManagers().forEach( manager -> getDelegate( manager.getClass() ) );
			addedAll = true;
		}
		return delegates.values();
	}
}
