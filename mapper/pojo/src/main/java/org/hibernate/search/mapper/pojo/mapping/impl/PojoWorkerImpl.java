/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.PojoWorker;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.SearchException;

abstract class PojoWorkerImpl implements PojoWorker {

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoRuntimeIntrospector introspector;

	PojoWorkerImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoRuntimeIntrospector introspector) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.introspector = introspector;
	}

	@Override
	public void add(Object entity) {
		add( null, entity );
	}

	@Override
	public void add(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		PojoIndexedTypeWorker delegate = getDelegate( clazz );
		delegate.add( id, entity );
	}

	@Override
	public void update(Object entity) {
		update( null, entity );
	}

	@Override
	public void update(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		PojoIndexedTypeWorker delegate = getDelegate( clazz );
		delegate.update( id, entity );
	}

	@Override
	public void delete(Object entity) {
		delete( null, entity );
	}

	@Override
	public void delete(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		PojoIndexedTypeWorker delegate = getDelegate( clazz );
		delegate.delete( id, entity );
	}

	PojoRuntimeIntrospector getIntrospector() {
		return introspector;
	}

	<E> PojoIndexedTypeManager<?, E, ?> getIndexedTypeManager(Class<E> clazz) {
		return indexedTypeManagers.getByExactClass( clazz )
				.orElseThrow( () -> new SearchException( "Cannot work on type " + clazz + ", because it is not indexed." ) );
	}

	Set<PojoIndexedTypeManager<?, ?, ?>> getAllIndexedTypeManagers() {
		return indexedTypeManagers.getAll();
	}

	abstract PojoIndexedTypeWorker getDelegate(Class<?> clazz);
}
