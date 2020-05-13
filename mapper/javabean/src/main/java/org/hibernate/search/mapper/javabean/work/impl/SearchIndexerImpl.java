/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;

public class SearchIndexerImpl implements SearchIndexer {

	private final PojoRuntimeIntrospector introspector;
	private final PojoIndexer delegate;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	public SearchIndexerImpl(PojoRuntimeIntrospector introspector, PojoIndexer delegate,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		this.introspector = introspector;
		this.delegate = delegate;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public CompletableFuture<?> add(Object providedId, Object entity) {
		return delegate.add( getTypeIdentifier( entity ), providedId, entity,
				commitStrategy, refreshStrategy );
	}

	@Override
	public CompletableFuture<?> addOrUpdate(Object providedId, Object entity) {
		return delegate.addOrUpdate( getTypeIdentifier( entity ), providedId, entity,
				commitStrategy, refreshStrategy );
	}

	@Override
	public CompletableFuture<?> delete(Object providedId, Object entity) {
		return delegate.delete( getTypeIdentifier( entity ), providedId, entity,
				commitStrategy, refreshStrategy );
	}

	@Override
	public CompletableFuture<?> purge(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		return delegate.purge( getTypeIdentifier( entityClass ), providedId, providedRoutingKey,
				commitStrategy, refreshStrategy );
	}

	private <T> PojoRawTypeIdentifier<? extends T> getTypeIdentifier(T entity) {
		return introspector.getEntityTypeIdentifier( entity );
	}

	private <T> PojoRawTypeIdentifier<T> getTypeIdentifier(Class<T> entityType) {
		return PojoRawTypeIdentifier.of( entityType );
	}
}
