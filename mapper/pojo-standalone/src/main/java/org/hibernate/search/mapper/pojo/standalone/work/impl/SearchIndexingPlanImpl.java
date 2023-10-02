/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.work.impl;

import java.util.BitSet;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoTypeIndexingPlan;

public class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final SearchIndexingPlanTypeContextProvider typeContextProvider;
	private final PojoRuntimeIntrospector introspector;
	private final PojoIndexingPlan delegate;
	private final ConfiguredIndexingPlanSynchronizationStrategy indexingPlanSynchronizationStrategy;

	public SearchIndexingPlanImpl(
			SearchIndexingPlanTypeContextProvider typeContextProvider, PojoRuntimeIntrospector introspector,
			PojoIndexingPlan delegate,
			ConfiguredIndexingPlanSynchronizationStrategy indexingPlanSynchronizationStrategy) {
		this.typeContextProvider = typeContextProvider;
		this.introspector = introspector;
		this.delegate = delegate;
		this.indexingPlanSynchronizationStrategy = indexingPlanSynchronizationStrategy;
	}

	@Override
	public void add(Object entity) {
		add( null, null, entity );
	}

	@Override
	public void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( getTypeIdentifier( entity ) );
		if ( typeDelegate == null ) {
			return;
		}
		typeDelegate.add( providedId, providedRoutes, entity );
	}

	@Override
	public void add(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( getTypeIdentifier( entityClass ) );
		if ( typeDelegate == null ) {
			return;
		}
		typeDelegate.add( providedId, providedRoutes, null );
	}

	@Override
	public void addOrUpdate(Object entity) {
		addOrUpdate( null, null, entity );
	}

	@Override
	public void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( getTypeIdentifier( entity ) );
		if ( typeDelegate == null ) {
			return;
		}
		typeDelegate.addOrUpdate( providedId, providedRoutes, entity, true, true, null );
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
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( typeIdentifier );
		if ( typeDelegate == null ) {
			return;
		}
		SearchIndexingPlanTypeContext<?> typeContext = typeContextProvider.forExactType( typeIdentifier );
		BitSet dirtyPaths = typeContext.dirtyFilter().filter( dirtyPathsAsStrings );
		typeDelegate.addOrUpdate( providedId, providedRoutes, entity, forceSelfDirty, forceContainingDirty, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		PojoRawTypeIdentifier<?> typeIdentifier = getTypeIdentifier( entityClass );
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( typeIdentifier );
		if ( typeDelegate == null ) {
			return;
		}
		typeDelegate.addOrUpdate( providedId, providedRoutes, null, true, true, null );
	}

	@Override
	public void delete(Object entity) {
		delete( null, null, entity );
	}

	@Override
	public void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity) {
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( getTypeIdentifier( entity ) );
		if ( typeDelegate == null ) {
			return;
		}
		typeDelegate.delete( providedId, providedRoutes, entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes) {
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( getTypeIdentifier( entityClass ) );
		if ( typeDelegate == null ) {
			return;
		}
		typeDelegate.delete( providedId, providedRoutes, null );
	}

	@Override
	public void addOrUpdateOrDelete(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes,
			boolean forceSelfDirty, boolean forceContainingDirty, String... dirtyPathsAsStrings) {
		SearchIndexingPlanTypeContext<?> typeContext = typeContextProvider.forExactClass( entityClass );
		PojoTypeIndexingPlan typeDelegate = delegate.typeIfIncludedOrNull( typeContext.typeIdentifier() );
		if ( typeDelegate == null ) {
			return;
		}
		BitSet dirtyPaths = typeContext.dirtyFilter().filter( dirtyPathsAsStrings );
		typeDelegate.addOrUpdateOrDelete( providedId, providedRoutes, forceSelfDirty, forceContainingDirty, dirtyPaths );
	}

	public void execute() {
		this.indexingPlanSynchronizationStrategy.executeAndSynchronize( delegate );
	}

	private <T> PojoRawTypeIdentifier<? extends T> getTypeIdentifier(T entity) {
		return introspector.detectEntityType( entity );
	}

	private <T> PojoRawTypeIdentifier<T> getTypeIdentifier(Class<T> entityType) {
		return typeContextProvider.forExactClass( entityType ).typeIdentifier();
	}
}
