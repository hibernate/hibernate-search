/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Simple pass-through implementation of {@code InstanceInitializer}.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public final class SimpleInitializer implements InstanceInitializer {

	public static final SimpleInitializer INSTANCE = new SimpleInitializer();

	private SimpleInitializer() {
		//use INSTANCE as this is stateless
	}

	@Override
	public Object unproxy(Object entity) {
		return entity;
	}

	@Override
	public Class<?> getClassFromWork(Work work) {
		return work.getEntityClass() != null ?
				work.getEntityClass() :
				getClass( work.getEntity() );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> Class<T> getClass(T entity) {
		return (Class<T>) entity.getClass();
	}

	@Override
	public <T> Collection<T> initializeCollection(Collection<T> value) {
		return value;
	}

	@Override
	public <K, V> Map<K, V> initializeMap(Map<K, V> value) {
		return value;
	}

	@Override
	public Object[] initializeArray(Object[] value) {
		return value;
	}

}
