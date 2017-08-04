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

	private final Map<Class<?>, PojoTypeManager<?, ?, ?>> typeManagers;
	private final SessionContext context;
	private final Map<Class<?>, ChangesetPojoTypeWorker<?>> delegates = new HashMap<>();

	public ChangesetPojoWorkerImpl(PojoProxyIntrospector introspector,
			Map<Class<?>, PojoTypeManager<?, ?, ?>> typeManagers,
			SessionContext context) {
		super( introspector );
		this.typeManagers = typeManagers;
		this.context = context;
	}

	@Override
	protected ChangesetPojoTypeWorker<?> getDelegate(Class<?> clazz) {
		return delegates.computeIfAbsent( clazz, c -> {
			PojoTypeManager<?, ?, ?> typeManager = typeManagers.get( c );
			return typeManager.createWorker( context );
		});
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for ( ChangesetPojoTypeWorker<?> delegate : delegates.values() ) {
				delegate.execute();
			}
			return CompletableFuture.allOf( futures.toArray( new CompletableFuture[futures.size()] ) );
		}
		finally {
			delegates.clear();
		}
	}

}
