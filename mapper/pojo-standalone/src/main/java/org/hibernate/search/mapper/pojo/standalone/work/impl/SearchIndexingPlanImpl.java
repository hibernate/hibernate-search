/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.work.impl;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Throwables;

public class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final SearchIndexingPlanTypeContextProvider typeContextProvider;
	private final PojoRuntimeIntrospector introspector;
	private final PojoIndexingPlan delegate;
	private final EntityReferenceFactory<EntityReference> entityReferenceFactory;

	public SearchIndexingPlanImpl(SearchIndexingPlanTypeContextProvider typeContextProvider,
			PojoRuntimeIntrospector introspector,
			PojoIndexingPlan delegate,
			EntityReferenceFactory<EntityReference> entityReferenceFactory) {
		this.typeContextProvider = typeContextProvider;
		this.introspector = introspector;
		this.delegate = delegate;
		this.entityReferenceFactory = entityReferenceFactory;
	}

	@Override
	public void add(Object entity) {
		add( null, null, entity );
	}

	@Override
	public void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		delegate.add( getTypeIdentifier( entity ), providedId, providedRoutes, entity );
	}

	@Override
	public void add(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		delegate.add( getTypeIdentifier( entityClass ), providedId, providedRoutes, null );
	}

	@Override
	public void addOrUpdate(Object entity) {
		addOrUpdate( null, null, entity );
	}

	@Override
	public void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		delegate.addOrUpdate( getTypeIdentifier( entity ), providedId, providedRoutes, entity,
				true, true, null );
	}

	@Override
	public void addOrUpdate(Object entity, String... dirtyPaths) {
		addOrUpdate( null, null, entity, false, false, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			String... dirtyPaths) {
		addOrUpdate( providedId, providedRoutes, entity, false, false, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			boolean forceSelfDirty, boolean forceContainingDirty, String... dirtyPathsAsStrings) {
		PojoRawTypeIdentifier<?> typeIdentifier = getTypeIdentifier( entity );
		SearchIndexingPlanTypeContext<?> typeContext = typeContextProvider.forExactType( typeIdentifier );
		BitSet dirtyPaths = typeContext == null ? null : typeContext.dirtyFilter().filter( dirtyPathsAsStrings );
		delegate.addOrUpdate( typeIdentifier, providedId, providedRoutes, entity,
				forceSelfDirty, forceContainingDirty, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		PojoRawTypeIdentifier<?> typeIdentifier = getTypeIdentifier( entityClass );
		delegate.addOrUpdate( typeIdentifier, providedId, providedRoutes, null,
				true, true, null );
	}

	@Override
	public void delete(Object entity) {
		delete( null, null, entity );
	}

	@Override
	public void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		delegate.delete( getTypeIdentifier( entity ), providedId, providedRoutes, entity );
	}

	@Override
	public void delete(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		delegate.delete( getTypeIdentifier( entityClass ), providedId, providedRoutes, null );
	}

	@Override
	public void addOrUpdateOrDelete(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes,
			boolean forceSelfDirty, boolean forceContainingDirty, String... dirtyPathsAsStrings) {
		PojoRawTypeIdentifier<?> typeIdentifier = getTypeIdentifier( entityClass );
		SearchIndexingPlanTypeContext<?> typeContext = typeContextProvider.forExactType( typeIdentifier );
		BitSet dirtyPaths = typeContext == null ? null : typeContext.dirtyFilter().filter( dirtyPathsAsStrings );
		delegate.addOrUpdateOrDelete( typeIdentifier, providedId, providedRoutes,
				forceSelfDirty, forceContainingDirty, dirtyPaths );
	}

	public CompletableFuture<?> execute() {
		return delegate.executeAndReport( entityReferenceFactory ).thenApply( report -> {
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
