/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work.impl;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Throwables;

public class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final SearchIndexingPlanTypeContextProvider typeContextProvider;
	private final PojoRuntimeIntrospector introspector;
	private final PojoIndexingPlan<?> delegate;

	public SearchIndexingPlanImpl(SearchIndexingPlanTypeContextProvider typeContextProvider,
			PojoRuntimeIntrospector introspector,
			PojoIndexingPlan<?> delegate) {
		this.typeContextProvider = typeContextProvider;
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
	public void add(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		delegate.add( getTypeIdentifier( entityClass ), providedId, providedRoutingKey, null );
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
	public void addOrUpdate(Object providedId, String providedRoutingKey, Object entity, String... dirtyPathsAsStrings) {
		PojoRawTypeIdentifier<?> typeIdentifier = getTypeIdentifier( entity );
		SearchIndexingPlanTypeContext typeContext = typeContextProvider.forExactType( typeIdentifier );
		BitSet dirtyPaths = typeContext == null ? null : typeContext.dirtyFilter().filter( dirtyPathsAsStrings );
		delegate.addOrUpdate( typeIdentifier, providedId, providedRoutingKey, entity, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		PojoRawTypeIdentifier<?> typeIdentifier = getTypeIdentifier( entityClass );
		delegate.addOrUpdate( typeIdentifier, providedId, providedRoutingKey, null );
	}

	@Override
	public void delete(Object entity) {
		delete( null, null, entity );
	}

	@Override
	public void delete(Object providedId, String providedRoutingKey, Object entity) {
		delegate.delete( getTypeIdentifier( entity ), providedId, providedRoutingKey, entity );
	}

	@Override
	public void delete(Class<?> entityClass, Object providedId, String providedRoutingKey) {
		delegate.delete( getTypeIdentifier( entityClass ), providedId, providedRoutingKey, null );
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
