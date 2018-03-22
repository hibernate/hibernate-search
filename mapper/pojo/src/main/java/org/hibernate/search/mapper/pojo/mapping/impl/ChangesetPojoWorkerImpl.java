/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;

class ChangesetPojoWorkerImpl extends PojoWorkerImpl implements ChangesetPojoWorker {

	private final PojoSessionContext sessionContext;
	// Use a LinkedHashMap for stable ordering across JVMs
	private final Map<Class<?>, ChangesetPojoIndexedTypeWorker<?, ?, ?>> delegates = new LinkedHashMap<>();

	ChangesetPojoWorkerImpl(PojoIndexedTypeManagerContainer indexedTypeManagers, PojoSessionContext sessionContext) {
		super( indexedTypeManagers, sessionContext.getRuntimeIntrospector() );
		this.sessionContext = sessionContext;
	}

	@Override
	public void prepare() {
		for ( ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate : delegates.values() ) {
			delegate.prepare();
		}
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			prepare();
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for ( ChangesetPojoIndexedTypeWorker<?, ?, ?> delegate : delegates.values() ) {
				futures.add( delegate.execute() );
			}
			return CompletableFuture.allOf( futures.toArray( new CompletableFuture[futures.size()] ) );
		}
		finally {
			delegates.clear();
		}
	}

	@Override
	ChangesetPojoIndexedTypeWorker<?, ?, ?> getDelegate(Class<?> clazz) {
		return delegates.computeIfAbsent(
				clazz,
				c -> getIndexedTypeManager( c ).createWorker( sessionContext )
		);
	}

}
