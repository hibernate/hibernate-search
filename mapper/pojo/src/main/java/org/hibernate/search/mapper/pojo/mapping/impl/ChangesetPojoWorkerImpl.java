/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;

/**
 * @author Yoann Rodiere
 */
public class ChangesetPojoWorkerImpl extends PojoWorkerImpl implements ChangesetPojoWorker {

	private final SessionContext context;
	private final Map<Class<?>, ChangesetPojoTypeWorker<?>> delegates = new HashMap<>();

	public ChangesetPojoWorkerImpl(PojoProxyIntrospector introspector,
			PojoTypeManagerContainer typeManagers,
			SessionContext context) {
		super( introspector, typeManagers );
		this.context = context;
	}

	@Override
	protected ChangesetPojoTypeWorker<?> getDelegate(Class<?> clazz) {
		return delegates.computeIfAbsent( clazz, c -> getTypeManager( c ).createWorker( context ) );
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for ( ChangesetPojoTypeWorker<?> delegate : delegates.values() ) {
				futures.add( delegate.execute() );
			}
			return CompletableFuture.allOf( futures.toArray( new CompletableFuture[futures.size()] ) );
		}
		finally {
			delegates.clear();
		}
	}

}
