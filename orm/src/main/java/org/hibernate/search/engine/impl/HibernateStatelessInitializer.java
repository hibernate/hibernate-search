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
import org.hibernate.search.hcore.util.impl.HibernateHelper;

/**
 * To be used for Hibernate initializations which don't need a specific Session.
 * {@link #initializeCollection(Collection)} and {@link #initializeMap(Map)}
 * are not supported.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class HibernateStatelessInitializer implements InstanceInitializer {

	public static final HibernateStatelessInitializer INSTANCE = new HibernateStatelessInitializer();

	protected HibernateStatelessInitializer() {
		// should not create instances, but allow for extension
	}

	@Override
	public <T> Class<T> getClass(T entity) {
		return HibernateHelper.getClass( entity );
	}

	@Override
	public Object unproxy(Object instance) {
		return HibernateHelper.unproxy( instance );
	}

	@Override
	public <T> Collection<T> initializeCollection(Collection<T> value) {
		// supports pass-through only, would need HibernateSessionLoadingInitializer
		return value;
	}

	@Override
	public <K, V> Map<K, V> initializeMap(Map<K, V> value) {
		// supports pass-through only, would need HibernateSessionLoadingInitializer
		return value;
	}

	@Override
	public Object[] initializeArray(Object[] value) {
		// hibernate doesn't allow lazy initialization of arrays,
		// so this must be initialized already.
		return value;
	}

	@Override
	public Class<?> getClassFromWork(Work work) {
		return HibernateHelper.getClassFromWork( work );
	}

}
