/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Throwables;

public class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final PojoRuntimeIntrospector introspector;
	private final PojoIndexingPlan<?> delegate;

	public SearchIndexingPlanImpl(PojoRuntimeIntrospector introspector,
			PojoIndexingPlan<?> delegate) {
		this.introspector = introspector;
		this.delegate = delegate;
	}

	@Override
	public void add(Object entity) {
		add( null, null, entity );
	}

	@Override
	public void add(Object providedId, String providedRoutingKey, Object entity) {
		delegate.add( getTypeIdentifier( entity ), providedId, providedRoutingKey, entity );
	}

	@Override
	public void addOrUpdate(Object entity) {
		addOrUpdate( null, null, entity );
	}

	@Override
	public void addOrUpdate(Object providedId, String providedRoutingKey, Object entity) {
		delegate.addOrUpdate( getTypeIdentifier( entity ), providedId, providedRoutingKey, entity );
	}

	@Override
	public void addOrUpdate(Object entity, String... dirtyPaths) {
		addOrUpdate( null, null, entity, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Object providedId, String providedRoutingKey, Object entity, String... dirtyPaths) {
		delegate.addOrUpdate( getTypeIdentifier( entity ), providedId, providedRoutingKey, entity, dirtyPaths );
	}

	@Override
	public void delete(Object entity) {
		delete( null, entity );
	}

	@Override
	public void delete(Object providedId, Object entity) {
		// TODO HSEARCH-3891 expose the providedRoutingKey
		delegate.delete( getTypeIdentifier( entity ), providedId, null, entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		delegate.purge( getTypeIdentifier( entityClass ), providedId, providedRoutingKey );
	}

	public CompletableFuture<?> execute() {
		return delegate.executeAndReport().thenApply( report -> {
			report.throwable().ifPresent( t -> {
				throw Throwables.toRuntimeException( t );
			} );
			return null;
		} );
	}

	private <T> PojoRawTypeIdentifier<? extends T> getTypeIdentifier(T entity) {
		return introspector.detectEntityType( entity );
	}

	private <T> PojoRawTypeIdentifier<T> getTypeIdentifier(Class<T> entityType) {
		return PojoRawTypeIdentifier.of( entityType );
	}
}
