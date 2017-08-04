/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.PojoWorker;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;

/**
 * @author Yoann Rodiere
 */
abstract class PojoWorkerImpl implements PojoWorker {

	private final PojoProxyIntrospector introspector;

	public PojoWorkerImpl(PojoProxyIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public void add(Object entity) {
		add( null, entity );
	}

	@Override
	public void add(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		PojoTypeWorker<?, ?> delegate = getDelegate( clazz );
		delegate.add( id, entity );
	}

	@Override
	public void update(Object entity) {
		update( null, entity );
	}

	@Override
	public void update(Object id, Object entity) {
		Class<?> clazz = introspector.getClass( entity );
		PojoTypeWorker<?, ?> delegate = getDelegate( clazz );
		delegate.update( id, entity );
	}

	@Override
	public void delete(Class<?> clazz, Object id) {
		PojoTypeWorker<?, ?> delegate = getDelegate( clazz );
		delegate.delete( id );
	}

	protected abstract PojoTypeWorker<?, ?> getDelegate(Class<?> clazz);
}
